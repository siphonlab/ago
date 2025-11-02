package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.NotImplementedException;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.*;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

import static org.siphonlab.ago.TypeCode.*;

public class InstanceJsonDeserializer extends JsonDeserializer<Instance<?>> {
    private final AgoEngine agoEngine;
    private final BoxTypes boxTypes;
    private final boolean serializeSlots;
    private final boolean serializeObjectAsReference;


    enum ParseState {
        None,
        FieldName,
        FieldValue,

        CollectionElement       // {"@type": "List<int", "@elements" : [1,2, 3]}, or `[1,2,3]` if type known
    }

    static final class ParseContext {
        private ParseState parseState;
        private AgoClass currClass;
        private Instance<?> currObject;
        private AgoSlotDef slotDef;

        private List<Object> collection;
        private TypeInfo elementType;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("(ParseContext ").append(parseState).append(' ');
            if(currClass != null) sb.append(currClass).append(' ');
            if(currObject != null) sb.append(currObject).append(' ');
            if(slotDef != null) sb.append(slotDef).append(' ');
            sb.setCharAt(sb.length() - 1, ')');
            return sb.toString();
        }

        ParseContext(){

        }

        ParseContext(ParseState parseState, AgoClass currClass){
            this.parseState = parseState;
            this.currClass = currClass;
        }

        public ParseState getParseState() {
            return parseState;
        }

        public void setParseState(ParseState parseState) {
            this.parseState = parseState;
        }

        public AgoClass getCurrClass() {
            return currClass;
        }

        public void setCurrClass(AgoClass currClass) {
            this.currClass = currClass;
        }

        public Instance<?> getCurrObject() {
            return currObject;
        }

        public void setCurrObject(Instance<?> currObject) {
            this.currObject = currObject;
        }

        public AgoSlotDef getSlotDef() {
            return slotDef;
        }

        public void setSlotDef(AgoSlotDef slotDef) {
            this.slotDef = slotDef;
        }
    }

    public InstanceJsonDeserializer(AgoEngine agoEngine, BoxTypes boxTypes, boolean serializeObjectAsReference, boolean serializeSlots) {
        this.agoEngine = agoEngine;
        this.boxTypes = boxTypes;
        this.serializeSlots = serializeSlots;
        this.serializeObjectAsReference = serializeObjectAsReference;
    }

    @Override
    public Instance<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        if(serializeObjectAsReference) throw new NotImplementedException("serializeObjectAsReference not implemented yet");

        JsonToken token;
        CallFrame<?> callFrame = ((AgoJsonParser) p).getCallFrame();

        final ArrayDeque<ParseContext> parseContextStack = new ArrayDeque<>();

        ParseState currentState = ParseState.None;
        ParseContext currContext = new ParseContext(currentState, ((AgoJsonParser) p).getInitialClass());

        while ((token = p.getCurrentToken()) != null) {
            switch (currentState){
                case None:
                    switch (token) {
                        case START_OBJECT:  currentState  = ParseState.FieldName; break;
                        case START_ARRAY:
                            currentState = ParseState.CollectionElement;        //TODO validate collection type
                            resolveElementType(currContext, p);
                            break;
                        default:    throw new JsonParseException(p, "unexpected token '%s' at '%s'".formatted(token, currentState));
                    };
                    break;
                case FieldName:
                    switch (token){
                        case FIELD_NAME:
                            String fieldName = p.getValueAsString();
                            if (fieldName.equals("@type")) {
                                var n = p.nextToken();
                                if (Objects.requireNonNull(n) == JsonToken.VALUE_STRING) {
                                    String typeName = p.getValueAsString();
                                    AgoClass deferClass = agoEngine.getClass(typeName);
                                    if (currContext.getCurrClass() != null) {
                                        if (!deferClass.isThatOrDerivedFrom(currContext.getCurrClass())) {
                                            throw new JsonParseException(p, "type '%s' not match '%s'".formatted(deferClass, currContext.currClass));
                                        }
                                    }
                                    currContext.setCurrClass(deferClass);       // object type provided
                                    currContext.currObject = agoEngine.createInstance(currContext.currClass, callFrame);
                                } else {
                                    throw new JsonParseException(p, "class name expected");
                                }
                            } else {
                                if(currContext.currObject == null) currContext.currObject = agoEngine.createInstance(currContext.currClass, callFrame);
                                if(fieldName.equals("@id")){
                                    readObjectId(currContext.currObject, p, callFrame);
                                } else if (fieldName.equals("@elements")) {
                                    var n = p.nextToken();
                                    if (n != JsonToken.START_ARRAY) {
                                        throw new JsonParseException(p, "array expected");
                                    } else {
                                        currentState = ParseState.CollectionElement;    //TODO validate collection type
                                        resolveElementType(currContext, p);
                                    }
                                } else {
                                    if(serializeSlots){
                                        int slotIndex = Integer.parseInt(fieldName.substring(fieldName.lastIndexOf('_') + 1));
                                        currContext.setSlotDef(currContext.currClass.getSlotDefs()[slotIndex]);
                                    } else {
                                        AgoField field = currContext.currClass.findField(fieldName);
                                        currContext.setSlotDef(currContext.currClass.getSlotDefs()[field.getSlotIndex()]);
                                    }
                                    // enter slot value
                                    currentState = ParseState.FieldValue;
                                }
                            }
                            break;
                        case END_OBJECT:
                            if (currContext.currObject == null)
                                currContext.currObject = agoEngine.createInstance(currContext.currClass, callFrame);

                            Instance<?> currObject = currContext.currObject;
                            if (!parseContextStack.isEmpty()) {
                                currContext = parseContextStack.pop();
                                acceptObject(currContext, currObject);
                            } else {
                                return acceptObject(currObject);
                            }
                            break;
                        default:
                            throw new JsonParseException(p, "unexpected token '%s' at '%s'".formatted(token, currentState));
                    }
                    break;
                case FieldValue:
                    int slotIndex = currContext.getSlotDef().getIndex();
                    var slotDef = currContext.getSlotDef();
                    Instance<?> currObject = currContext.getCurrObject();
                    currentState = ParseState.FieldName;    // next field
                    Slots slots = currObject.getSlots();
                    switch (token){
                        case VALUE_NUMBER_INT:
                        case VALUE_NUMBER_FLOAT:
                            switch (slotDef.getTypeCode().value){
                                case INT_VALUE:
                                    slots.setInt(slotIndex, p.getIntValue());
                                    break;
                                case LONG_VALUE:
                                    slots.setLong(slotIndex, p.getLongValue());
                                    break;
                                case FLOAT_VALUE:
                                    slots.setFloat(slotIndex, p.getFloatValue());
                                    break;
                                case DOUBLE_VALUE:
                                    slots.setDouble(slotIndex, p.getDoubleValue());
                                    break;
                                case SHORT_VALUE:
                                    slots.setShort(slotIndex, p.getShortValue());
                                    break;
                                case BYTE_VALUE:
                                    slots.setByte(slotIndex, p.getByteValue());
                                    break;
                                case OBJECT_VALUE:
                                    if(boxTypes.isBoxType(slotDef.getAgoClass())){
                                        var type = boxTypes.getUnboxType(slotDef.getAgoClass());
                                        var boxedValue = switch (type.getValue()){
                                            case INT_VALUE ->
                                                agoEngine.getBoxer().boxInt(callFrame, slotDef.getAgoClass(), p.getIntValue());
                                            case LONG_VALUE ->
                                                agoEngine.getBoxer().boxLong(callFrame, slotDef.getAgoClass(), p.getLongValue());
                                            case FLOAT_VALUE ->
                                                agoEngine.getBoxer().boxFloat(callFrame, slotDef.getAgoClass(), p.getFloatValue());
                                            case DOUBLE_VALUE ->
                                                agoEngine.getBoxer().boxDouble(callFrame, slotDef.getAgoClass(), p.getDoubleValue());
                                            case SHORT_VALUE ->
                                                agoEngine.getBoxer().boxShort(callFrame, slotDef.getAgoClass(), p.getShortValue());
                                            case BYTE_VALUE ->
                                                agoEngine.getBoxer().boxByte(callFrame, slotDef.getAgoClass(), p.getByteValue());
                                            case STRING_VALUE ->
                                                agoEngine.getBoxer().boxString(callFrame, slotDef.getAgoClass(), p.getValueAsString());
                                            default ->
                                                throw new JsonParseException(p, "unexpected number value for %s".formatted(slotDef.getTypeCode()));
                                        };
                                        slots.setObject(slotIndex, boxedValue);
                                    } else {
                                        parseContextStack.push(currContext);
                                        currContext = new ParseContext(ParseState.None, slotDef.getAgoClass());
                                    }
                                    break;
                                case CLASS_REF_VALUE:
                                case BOOLEAN_VALUE:
                                case STRING_VALUE:
                                case CHAR_VALUE:
                                    throw new JsonParseException(p, "unexpected number value for %s".formatted(slotDef.getTypeCode()));
                            }
                            break;
                        case VALUE_NULL:
                            if(slotDef.getTypeCode() == OBJECT){
                                slots.setObject(slotIndex, null);
                            } else {
                                throw new JsonParseException(p, "unexpected null value for %s".formatted(slotDef.getTypeCode()));
                            }
                            break;
                        case VALUE_FALSE:
                        case VALUE_TRUE:
                            if(slotDef.getTypeCode() == BOOLEAN){
                                slots.setBoolean(slotIndex,token == JsonToken.VALUE_TRUE);
                                break;
                            } else if(slotDef.getTypeCode() == OBJECT){
                                if(boxTypes.getUnboxType(slotDef.getAgoClass()) == BOOLEAN){
                                    slots.setObject(slotIndex,agoEngine.getBoxer().boxBoolean(callFrame,slotDef.getAgoClass(),token == JsonToken.VALUE_TRUE));
                                    break;
                                }
                            }
                            throw new JsonParseException(p, "unexpected boolean value for %s".formatted(slotDef.getTypeCode()));
                        case VALUE_STRING:
                            if (slotDef.getTypeCode() == STRING) {
                                slots.setString(slotIndex, p.getValueAsString());
                                break;
                            } else if (slotDef.getTypeCode() == OBJECT) {
                                if (boxTypes.getUnboxType(slotDef.getAgoClass()) == STRING) {
                                    slots.setObject(slotIndex, agoEngine.getBoxer().boxString(callFrame, slotDef.getAgoClass(), p.getValueAsString()));
                                    break;
                                }
                            }
                            throw new JsonParseException(p, "unexpected string value for %s".formatted(slotDef.getTypeCode()));
                        case START_OBJECT:
                            currContext.setParseState(ParseState.FieldValue);   // set back
                            parseContextStack.push(currContext);
                            currContext = new ParseContext(ParseState.FieldName, slotDef.getAgoClass());
                            break;
                        case START_ARRAY:
                            currentState = ParseState.CollectionElement;
                            resolveElementType(currContext, p);
                            break;
                        default:
                            throw new JsonParseException(p, "unexpected token '%s' for %s".formatted(token, slotDef.getTypeCode()));
                    }
                    break;

                case CollectionElement:
                    List<Object> collection = currContext.collection;
                    var elementType = currContext.elementType;
                    switch (token) {
                        case VALUE_NUMBER_INT:
                        case VALUE_NUMBER_FLOAT:
                            switch (elementType.getTypeCode().value) {
                                case INT_VALUE:
                                    collection.add(p.getIntValue());
                                    break;
                                case LONG_VALUE:
                                    collection.add(p.getLongValue());
                                    break;
                                case FLOAT_VALUE:
                                    collection.add(p.getFloatValue());
                                    break;
                                case DOUBLE_VALUE:
                                    collection.add(p.getDoubleValue());
                                    break;
                                case SHORT_VALUE:
                                    collection.add(p.getShortValue());
                                    break;
                                case BYTE_VALUE:
                                    collection.add(p.getByteValue());
                                    break;
                                case OBJECT_VALUE:
                                    if (boxTypes.isBoxType(elementType.getAgoClass())) {
                                        var type = boxTypes.getUnboxType(elementType.getAgoClass());
                                        var boxedValue = switch (type.getValue()) {
                                            case INT_VALUE ->
                                                    agoEngine.getBoxer().boxInt(callFrame, elementType.getAgoClass(), p.getIntValue());
                                            case LONG_VALUE ->
                                                    agoEngine.getBoxer().boxLong(callFrame, elementType.getAgoClass(), p.getLongValue());
                                            case FLOAT_VALUE ->
                                                    agoEngine.getBoxer().boxFloat(callFrame, elementType.getAgoClass(), p.getFloatValue());
                                            case DOUBLE_VALUE ->
                                                    agoEngine.getBoxer().boxDouble(callFrame, elementType.getAgoClass(), p.getDoubleValue());
                                            case SHORT_VALUE ->
                                                    agoEngine.getBoxer().boxShort(callFrame, elementType.getAgoClass(), p.getShortValue());
                                            case BYTE_VALUE ->
                                                    agoEngine.getBoxer().boxByte(callFrame, elementType.getAgoClass(), p.getByteValue());
                                            case STRING_VALUE ->
                                                    agoEngine.getBoxer().boxString(callFrame, elementType.getAgoClass(), p.getValueAsString());
                                            default ->
                                                    throw new JsonParseException(p, "unexpected number value for %s".formatted(elementType.getTypeCode()));
                                        };
                                        collection.add(boxedValue);
                                    } else {
                                        parseContextStack.push(currContext);
                                        currContext = new ParseContext(ParseState.None, elementType.getAgoClass());
                                    }
                                    break;
                                case CLASS_REF_VALUE:
                                case BOOLEAN_VALUE:
                                case STRING_VALUE:
                                case CHAR_VALUE:
                                    throw new JsonParseException(p, "unexpected number value for %s".formatted(elementType.getTypeCode()));
                            }
                        case VALUE_NULL:
                            if (elementType.getTypeCode() == OBJECT) {
                                collection.add(null);
                            } else {
                                throw new JsonParseException(p, "unexpected null value for %s".formatted(elementType.getTypeCode()));
                            }
                            break;
                        case VALUE_FALSE:
                        case VALUE_TRUE:
                            if (elementType.getTypeCode() == BOOLEAN) {
                                collection.add(token == JsonToken.VALUE_TRUE);
                                break;
                            } else if (elementType.getTypeCode() == OBJECT) {
                                if (boxTypes.getUnboxType(elementType.getAgoClass()) == BOOLEAN) {
                                    collection.add(agoEngine.getBoxer().boxBoolean(callFrame, elementType.getAgoClass(), token == JsonToken.VALUE_TRUE));
                                    break;
                                }
                            }
                            throw new JsonParseException(p, "unexpected boolean value for %s".formatted(elementType.getTypeCode()));
                        case VALUE_STRING:
                            if (elementType.getTypeCode() == STRING) {
                                collection.add(p.getValueAsString());
                                break;
                            } else if (elementType.getTypeCode() == OBJECT) {
                                if (boxTypes.getUnboxType(elementType.getAgoClass()) == STRING) {
                                    collection.add(agoEngine.getBoxer().boxString(callFrame, elementType.getAgoClass(), p.getValueAsString()));
                                    break;
                                }
                            }
                            throw new JsonParseException(p, "unexpected string value for %s".formatted(elementType.getTypeCode()));
                        case START_OBJECT:
                            parseContextStack.push(currContext);
                            currContext = new ParseContext(ParseState.FieldName, elementType.getAgoClass());
                            break;
                        case START_ARRAY:
                            parseContextStack.push(currContext);
                            currContext = new ParseContext(ParseState.CollectionElement, elementType.getAgoClass());
                            break;
                        case END_ARRAY:
                            acceptArray(currContext, p);
                            Instance<?> currArray = currContext.currObject;
                            if (parseContextStack.isEmpty()) {
                                return acceptObject(currArray);
                            } else {
                                currContext = parseContextStack.pop();
                                acceptObject(currContext, currArray);
                                break;
                            }
                        default:
                            throw new JsonParseException(p, "unexpected token '%s' for %s".formatted(token, elementType.getTypeCode()));
                    }
                    break;

            }
            p.nextToken();
        }
        return null;
    }

    protected void readObjectId(Instance<?> currObject, JsonParser p, CallFrame<?> callFrame) throws IOException {

    }

    private void acceptArray(ParseContext parseContext, JsonParser p) throws JsonParseException {
        AgoClass arrayClass = parseContext.currClass;
        List<Object> list = parseContext.collection;
        if(arrayClass.getConcreteTypeInfo() instanceof ArrayInfo) {
            AgoArrayInstance arrayInstance = switch (parseContext.elementType.getTypeCode().value) {
                case INT_VALUE ->   new IntArrayInstance(arrayClass.createSlots(), arrayClass, list.size());
                case LONG_VALUE -> new LongArrayInstance(arrayClass.createSlots(), arrayClass, list.size());
                case DOUBLE_VALUE -> new DoubleArrayInstance(arrayClass.createSlots(), arrayClass, list.size());
                case BOOLEAN_VALUE -> new BooleanArrayInstance(arrayClass.createSlots(), arrayClass, list.size());
                case STRING_VALUE -> new StringArrayInstance(arrayClass.createSlots(), arrayClass, list.size());
                case CHAR_VALUE -> new CharArrayInstance(arrayClass.createSlots(), arrayClass, list.size());
                case SHORT_VALUE -> new ShortArrayInstance(arrayClass.createSlots(), arrayClass, list.size());
                case BYTE_VALUE -> new ByteArrayInstance(arrayClass.createSlots(), arrayClass, list.size());
                case FLOAT_VALUE -> new FloatArrayInstance(arrayClass.createSlots(), arrayClass, list.size());
                case OBJECT_VALUE -> new ObjectArrayInstance(arrayClass.createSlots(), arrayClass, list.size());
                default ->
                        throw new JsonParseException(p, "Unknown array type: " + arrayClass);
            };
            arrayInstance.fill(list);
            parseContext.currObject = arrayInstance;
        } else {
            AgoClass currClass = parseContext.currClass;
            //TODO
        }
    }

    private void acceptObject(ParseContext currContext, Instance<?> currObject) {
        acceptObject(currObject);
        if(currContext.elementType != null){
            currContext.collection.add(currObject);
        } else {
            currContext.currObject.getSlots().setObject(currContext.slotDef.getIndex(), currObject);
        }
    }

    protected Instance<?> acceptObject(Instance<?> instance){
        return instance;
    }

    private void resolveElementType(ParseContext currContext, JsonParser p) throws JsonParseException {
        if(currContext.currClass.getConcreteTypeInfo() instanceof ArrayInfo arrayInfo){
            currContext.elementType = arrayInfo.getElementType();
        } else {
            AgoClass currClass = currContext.currClass;
            //TODO
        }
    }
}
