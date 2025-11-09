package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.agrona.collections.IntArrayList;
import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.*;

import java.io.IOException;
import java.util.*;

import static org.siphonlab.ago.TypeCode.*;

public class InstanceJsonDeserializer extends JsonDeserializer<Instance<?>> {
    protected final AgoEngine agoEngine;

    enum ParseState {
        None,
        FieldName,
        FieldValue,

        Class,                      // {"@class":"class name", [@id: ], [scope: ]}
        CollectionElements,         // {"@collection": "List<int> or int[]", "@elements" : [1,2, 3]}, or `[1,2,3]` if type known
        ClassRef,                   // {"@classref": [classname, id, [parentScope]]}
        ObjectRef,                  // {"@objectref": [classname, id]}
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

    public InstanceJsonDeserializer(AgoEngine agoEngine) {
        this.agoEngine = agoEngine;
    }

    @Override
    public Instance<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        AgoJsonParser ajp = (AgoJsonParser) p;
        CallFrame<?> creator = ajp.getCallFrame();

        AgoClass agoClassOfSlots = (AgoClass) ctxt.getAttribute("class");       // to parse slots
        if(agoClassOfSlots != null){
            Map<String, AgoSlotDef> map = new HashMap<>();
            for (AgoSlotDef slotDef : agoClassOfSlots.getSlotDefs())
                map.put(slotDef.getName() + '_' + slotDef.getIndex(), slotDef);
            deserializeSlots(ajp, ctxt, creator, agoClassOfSlots, map);
            return null;
        }

        ajp.nextToken();
        return deserializeAny(ajp, ctxt, ajp.initialClass, creator, null, null);
    }

    protected Instance<?> deserializeAny(AgoJsonParser ajp, DeserializationContext ctxt, AgoClass expectedClass, CallFrame<?> creator, Instance<?> host, AgoSlotDef slotDef) throws IOException {
        // host and slotDef only for parse primitive values
        // for object value, an `slots.set(, deserializeUnknown())` will within deserializeObject
        JsonToken token = ajp.currentToken();
        switch (token){
            case START_OBJECT: {
                token = ajp.nextToken();
                if (token == JsonToken.FIELD_NAME){
                    String fieldName = ajp.getValueAsString();
                    if(fieldName.equals("@objectref")){
                        return deserializeObjectRef(ajp, ctxt);
                    } else if(fieldName.equals("@classref")){
                        return deserializeClassRef(ajp, ctxt, creator);
                    } else if(fieldName.equals("@collection")){
                        AgoClass agoClass = deserializeClass(ajp, ctxt, creator);
                        assert ajp.nextToken() == JsonToken.FIELD_NAME;
                        assert "@elements".equals(ajp.getValueAsString());
                        assert ajp.nextToken() == JsonToken.START_ARRAY;

                        return deserializeCollection(ajp, ctxt, agoClass, creator);
                    } else if(fieldName.equals("@class")){
                        String className = ajp.getValueAsString();
                        return deserializeClass(agoEngine.getClass(className), ajp, ctxt, creator);
                    } else {
                        if(fieldName.equals("@type")){
                            AgoClass agoClass = deserializeClass(ajp,ctxt, creator);
                            ajp.nextToken();
                            return deserializeObject(ajp, ctxt, agoClass, creator);
                        } else {
                            return deserializeObject(ajp, ctxt, expectedClass, creator);
                        }
                    }
                } else {
                    return deserializeObject(ajp, ctxt, expectedClass, creator);
                }
            }

            case START_ARRAY:
                assert expectedClass != null;
                return deserializeCollection(ajp,ctxt,expectedClass, creator);

            default:
                if(host != null){
                    Slots slots = host.getSlots();
                    readPrimitiveSlot(ajp, token, slots, slotDef);
                    return host;
                } else {
                    return deserializeBoxedValue(ajp, token, expectedClass, creator);
                }
        }

    }

    private void readPrimitiveSlot(AgoJsonParser ajp, JsonToken token, Slots slots, AgoSlotDef slotDef) throws IOException {
        int index = slotDef.getIndex();
        switch (token){
            case VALUE_STRING:
                String string = ajp.getValueAsString();
                switch (slotDef.getTypeCode().getValue()) {
                    case STRING_VALUE:  slots.setString(index, string); break;
                    case CLASS_REF_VALUE: slots.setClassRef(index, agoEngine.getClass(string).getClassId()); break;
                    case CHAR_VALUE: slots.setChar(index, string.charAt(0));
                }
                break;
            case VALUE_NULL:
                switch (slotDef.getTypeCode().getValue()){
                    case STRING_VALUE : slots.setString(index, null); break;
                    case OBJECT_VALUE: slots.setObject(index, null); break;
                    case VOID_VALUE: slots.setVoid(index,null); break;
                }
                break;
            case VALUE_NUMBER_INT: {
                int value = ajp.getIntValue();
                switch (slotDef.getTypeCode().getValue()) {
                    case INT_VALUE: slots.setInt(index, value); break;
                    case DOUBLE_VALUE: slots.setDouble(index, value); break;
                    case LONG_VALUE: slots.setLong(index, value); break;
                    case FLOAT_VALUE: slots.setFloat(index, value); break;
                    case BYTE_VALUE: slots.setByte(index, (byte) value); break;
                    case SHORT_VALUE: slots.setInt(index, (short)value); break;
                }
                break;
            }
            case VALUE_NUMBER_FLOAT:{
                double value = ajp.getDoubleValue();
                switch (slotDef.getTypeCode().getValue()) {
                    case INT_VALUE: slots.setInt(index, (int) value); break;
                    case DOUBLE_VALUE: slots.setDouble(index, value); break;
                    case LONG_VALUE: slots.setLong(index, (long) value); break;
                    case FLOAT_VALUE: slots.setFloat(index, (float) value); break;
                    case BYTE_VALUE: slots.setByte(index, (byte) value); break;
                    case SHORT_VALUE: slots.setInt(index, (short)value); break;
                }
                break;
            }

            case VALUE_TRUE:
                slots.setBoolean(index, true);
                break;
            case VALUE_FALSE:
                slots.setBoolean(index,false);
                break;

        }
    }

    private Instance<?> deserializeBoxedValue(AgoJsonParser ajp, JsonToken token, AgoClass expectedClass, CallFrame<?> creator) throws IOException {
        //TODO should avoid save to db
        var instance = agoEngine.createInstance(expectedClass, creator);
        readPrimitiveSlot(ajp, token, instance.getSlots(), expectedClass.getSlotDefs()[0]);
        return instance;
    }

    private Instance<?> deserializeObject(AgoJsonParser ajp, DeserializationContext ctxt, AgoClass agoClass, CallFrame<?> creator) throws IOException {
        // curr token already pass '{'
        Map<String, AgoSlotDef> map = new HashMap<>();
        AgoSlotDef[] slotDefs = agoClass.getSlotDefs();
        if(!ajp.isSerializeSlots()){
            for (AgoField field : agoClass.getFields()) {
                if ((field.getModifiers() & AgoClass.PUBLIC) == AgoClass.PUBLIC) {
                    var slotDef = slotDefs[field.getSlotIndex()];
                    map.put(field.getName(), slotDef);
                }
            }
        } else {
            for (AgoSlotDef slotDef : slotDefs)
                map.put(slotDef.getName() + '_' + slotDef.getIndex(), slotDef);
        }
        var instance = agoEngine.createInstance(agoClass,creator);
        deserializeSlots(ajp, ctxt, creator, instance, map);
        return instance;
    }

    protected void deserializeSlots(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator, Instance<?> instance, Map<String, AgoSlotDef> map) throws IOException {
        JsonToken token;
        for(token = ajp.currentToken(); token != JsonToken.END_OBJECT; token = ajp.nextToken()){
            assert ajp.currentToken() == JsonToken.FIELD_NAME;
            String fieldName = ajp.getValueAsString();
            if(fieldName.equals("@id")){
                readObjectId(instance, ajp, ctxt, creator);
            } else {
                AgoSlotDef agoSlotDef = map.get(fieldName);
                if (agoSlotDef == null) throw new NullPointerException("'%s' not exists".formatted(fieldName));
                deserializeAny(ajp, ctxt, agoSlotDef.getAgoClass(), creator, instance, agoSlotDef);
            }
        }
    }

    protected void readObjectId(Instance<?> instance, AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        throw new UnsupportedOperationException();
    }

    // classname or {"@class":"classname", [@id: ], [scope: ]}
    private AgoClass deserializeClass(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        JsonToken token = ajp.nextToken();
        if(token == JsonToken.VALUE_STRING){
            String className = ajp.getValueAsString();
            return agoEngine.getClass(className);
        } else {
            token = ajp.nextToken();
            assert token == JsonToken.FIELD_NAME;
            String s = ajp.getValueAsString();
            assert s.equals("@class");
            String className = ajp.getValueAsString();
            return deserializeClass(agoEngine.getClass(className), ajp, ctxt, creator);
        }
    }

    // {"@class":"classname", [@id: ], [scope: ]}
    // override this to implement your readObjectId
    protected AgoClass deserializeClass(AgoClass baseClass, AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        var token = ajp.nextToken();
        AgoClass result = null;
        while((token = ajp.nextToken()) != JsonToken.END_OBJECT) {
            if(token == JsonToken.FIELD_NAME) {
                if (ajp.getValueAsString().equals("@id")) {
                    if (result == null) {
                        result = deserializeClassRef(baseClass, ajp.getValueAsLong());
                    }
                } else if(ajp.getValueAsString().equals("scope")){
                    var scope = deserializeAny(ajp,ctxt,null,creator, null,null);
                    if(result == null) {
                        result = agoEngine.createScopedClass(creator, baseClass.getClassId(), scope);
                    }
                }
            }
        }
        return result == null ? baseClass : result;
    }

    //{"@classref": [classname, id, [parentScope]]}
    private Instance<?> deserializeClassRef(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        assert ajp.nextToken() == JsonToken.START_ARRAY;
        ajp.nextToken();
        String classname = ajp.getValueAsString();
        AgoClass baseClass = agoEngine.getClass(classname);
        JsonToken token;
        AgoClass result = null;
        while((token = ajp.nextToken()) != JsonToken.END_ARRAY) {
            if (token == JsonToken.VALUE_NUMBER_INT) {      // id
                if(result == null)
                    result = deserializeClassRef(result, ajp.getValueAsLong());
            } else if(token == JsonToken.START_OBJECT){
                var scope = deserializeAny(ajp, ctxt, null, creator, null, null);
                if(result == null)
                    result = agoEngine.createScopedClass(creator, baseClass.getClassId(), scope);
            }
        }
        assert ajp.nextToken() == JsonToken.END_OBJECT;
        return result == null ? baseClass : result;
    }

    protected AgoClass deserializeClassRef(AgoClass baseClass, long id) {
        throw new UnsupportedOperationException();
    }

    private Instance<?> deserializeCollection(AgoJsonParser ajp, DeserializationContext ctxt, AgoClass collectionClass, CallFrame<?> creator) throws IOException {
        // curr token is `[`
        if (collectionClass.getConcreteTypeInfo() instanceof ArrayInfo arrayInfo){
            TypeInfo elementType = arrayInfo.getElementType();
            var collection = switch (elementType.getTypeCode().getValue()) {
                case INT_VALUE -> {
                    IntArrayList list = new IntArrayList();
                    for (var token = ajp.currentToken(); token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.addInt(ajp.getIntValue());
                    }
                    var r = new IntArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    list.toIntArray(r.value);
                    yield r;
                }
                case DOUBLE_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getDoubleValue());
                    }
                    var r = new DoubleArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                case BOOLEAN_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getValueAsBoolean());
                    }
                    var r = new BooleanArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                case STRING_VALUE -> {
                    List<String> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getValueAsString());
                    }
                    var r = new StringArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    list.toArray(r.value);
                    yield r;
                }
                case CHAR_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getValueAsString().charAt(0));
                    }
                    var r = new CharArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                case SHORT_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getIntValue());
                    }
                    var r = new ShortArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                case BYTE_VALUE -> {
                    IoBuffer buffer = IoBuffer.allocate(128).setAutoExpand(true);
                    for (var token = ajp.currentToken(); token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        buffer.put((byte) ajp.getIntValue());   //TODO write as base64
                    }
                    var r = new ByteArrayInstance(collectionClass.createSlots(), collectionClass, buffer.position());
                    buffer.flip().get(r.value);
                    yield r;
                }
                case FLOAT_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getFloatValue());
                    }
                    var r = new FloatArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                case OBJECT_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(deserializeAny(ajp, ctxt, null, creator, null, null));
                    }
                    var r = new ObjectArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                default -> throw new IllegalStateException("Unexpected value: " + elementType.getTypeCode().getValue());
            };
            collection.setCreator(creator);
            return collection;
        }
        throw new RuntimeException("bad exit");
    }

    protected Instance<?> deserializeObjectRef(AgoJsonParser ajp, DeserializationContext ctxt) throws IOException {
        throw new UnsupportedOperationException();
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
