/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
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

        AgoClass agoClassOfSlots = (AgoClass) ctxt.getAttribute("slots_class");       // to parse slots
        if(agoClassOfSlots != null && agoClassOfSlots.getSlotDefs() != null){
            Map<String, AgoSlotDef> map = new HashMap<>();
            for (AgoSlotDef slotDef : agoClassOfSlots.getSlotDefs())
                map.put(slotDef.getName() + '_' + slotDef.getIndex(), slotDef);
            deserializeSlots(ajp, ctxt, creator, (Slots) ctxt.getAttribute("slots"), map);
            return null;
        }

        ajp.nextToken();
        return deserializeAny(ajp, ctxt, ajp.initialClass, creator, null, null);
    }

    protected Instance<?> deserializeAny(AgoJsonParser ajp, DeserializationContext ctxt, AgoClass expectedClass, CallFrame<?> creator, Slots hostSlots, AgoSlotDef slotDef) throws IOException {
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
                    } else if(fieldName.equals("@collection") || fieldName.equals("@elements")) {
                        AgoClass collectionType = expectedClass;
                        if (fieldName.equals("@collection")) {
                            collectionType = deserializeClass(ajp, ctxt, creator);

                            assert ajp.currentToken() == JsonToken.FIELD_NAME;
                            fieldName = ajp.getValueAsString();
                        }
                        if(fieldName.equals("@elements")){
                            ajp.nextToken();    // enter [
                            var r = deserializeCollection(ajp, ctxt, collectionType, creator);
                            ajp.nextToken();   // pass END_OBJECT
                            return r;
                        }
                        throw new IllegalStateException("@elements not found");

                    } else if(fieldName.equals("@class")){
                        return deserializeComplexClass(ajp, ctxt, creator);
                    } else {
                        if(fieldName.equals("@type")) {
                            AgoClass agoClass = deserializeClass(ajp, ctxt, creator);
                            ajp.nextToken();
                            return deserializeObject(ajp, ctxt, agoClass, creator);
                        } else if(fieldName.equals("@box_type")) {
                            AgoClass agoClass = deserializeClass(ajp, ctxt, creator);
                            ajp.nextToken();    // value:

                            Instance<?> r;
                            if (agoClass instanceof AgoEnum agoEnum) {
                                r = deserializeEnumValue(ajp, agoEnum);
                            } else {
                                if (!agoEngine.getBoxer().isNarrowBoxType(agoClass) && agoClass.getSlotDefs().length > 1) {
                                    r = deserializeComplexBoxedValue(ajp, agoClass, creator, ctxt);
                                } else {
                                    r = deserializeBoxedValue(ajp, ajp.currentToken(), agoClass, creator);
                                }
                            }
                            ajp.nextToken();    // pass END_OBJECT
                            return r;
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
                if(hostSlots != null){
                    readPrimitiveSlot(ajp, token, hostSlots, slotDef);
                    return null;
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
        ajp.nextToken();
    }

    private Instance<?> deserializeBoxedValue(AgoJsonParser ajp, JsonToken token, AgoClass expectedClass, CallFrame<?> creator) throws IOException {
        if(expectedClass instanceof AgoEnum agoEnum){
            return deserializeEnumValue(ajp, agoEnum);
        } else {
            var instance = agoEngine.createInstance(expectedClass, creator);
            readPrimitiveSlot(ajp, token, instance.getSlots(), expectedClass.getSlotDefs()[0]);
            if (expectedClass == agoEngine.getLangClasses().getClassRefClass()) {
                instance.getSlots().setObject(1, agoEngine.getClass(instance.getSlots().getClassRef(0)));
            }
            return instance;
        }
    }

    private static Instance<?> deserializeEnumValue(AgoJsonParser ajp, AgoEnum agoEnum) throws IOException {
        if (ajp.isSerializeSlots()) {
            var r = switch (agoEnum.getBasePrimitiveType().value) {
                case INT_VALUE -> agoEnum.findMember(ajp.getValueAsInt());
                case LONG_VALUE -> agoEnum.findMember(ajp.getValueAsLong());
                case BYTE_VALUE ->  agoEnum.findMember((byte) ajp.getValueAsInt());
                case SHORT_VALUE -> agoEnum.findMember((short) ajp.getValueAsInt());
                default -> throw new IllegalArgumentException("bad enum type " + agoEnum.getBasePrimitiveType());
            };
            ajp.nextToken();
            return r;
        } else {
            var r = agoEnum.findMember(ajp.getValueAsString());
            ajp.nextToken();
            return r;
        }
    }

    private Instance<?> deserializeComplexBoxedValue(AgoJsonParser ajp, AgoClass expectedClass, CallFrame<?> creator, DeserializationContext ctxt) throws IOException {
        var instance = agoEngine.createInstance(expectedClass, creator);

        Map<String, AgoSlotDef> map = new HashMap<>();
        AgoSlotDef[] slotDefs = expectedClass.getSlotDefs();
        for (AgoSlotDef slotDef : slotDefs)
            map.put(slotDef.getName() + '_' + slotDef.getIndex(), slotDef);

        deserializeSlots(ajp,ctxt,creator, instance.getSlots(), map);
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
        var instance = agoEngine.createInstance(agoClass, creator);
        deserializeSlots(ajp, ctxt, creator, instance.getSlots(), map);
        return instance;
    }


    protected void deserializeSlots(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator, Slots hostSlots, Map<String, AgoSlotDef> map) throws IOException {
        JsonToken token;
        if(ajp.currentToken() == JsonToken.START_OBJECT) ajp.nextToken();
        for(token = ajp.currentToken(); token != JsonToken.END_OBJECT; token = ajp.currentToken()){
            assert ajp.currentToken() == JsonToken.FIELD_NAME;
            String fieldName = ajp.getValueAsString();
            ajp.nextToken();
            if(fieldName.equals("@id")){
                readObjectId(hostSlots, ajp, ctxt, creator);
            } else {
                AgoSlotDef agoSlotDef = map.get(fieldName);
                if (agoSlotDef == null) throw new NullPointerException("'%s' not exists".formatted(fieldName));
                var r = deserializeAny(ajp, ctxt, agoSlotDef.getAgoClass(), creator, hostSlots, agoSlotDef);
                if(agoSlotDef.getTypeCode().getValue() == OBJECT_VALUE){
                    hostSlots.setObject(agoSlotDef.getIndex(), r);
                }
            }
        }
        ajp.nextToken();    // pass END_OBJECT
    }

    protected void readObjectId(Slots slots, AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        throw new UnsupportedOperationException();
    }

    // classname or {"@class": classname} or {"@class": [classname, {@id: }, {scope: }]}
    private AgoClass deserializeClass(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        JsonToken token = ajp.nextToken();
        if(token == JsonToken.VALUE_STRING){
            String className = ajp.getValueAsString();
            ajp.nextToken();
            return agoEngine.getClass(className);
        } else {
            token = ajp.nextToken();    // enter {
            assert token == JsonToken.FIELD_NAME;
            String s = ajp.getValueAsString();
            assert s.equals("@class");
            return deserializeComplexClass(ajp, ctxt, creator);
        }
    }

    // {"@class":[classname, {@id: }, {scope: }]}       // token at "@class"
    //   or
    // {"@class": classname}
    // override this to implement your readObjectId
    protected AgoClass deserializeComplexClass(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        var token = ajp.nextToken();
        if(token == JsonToken.VALUE_STRING){
            String className = ajp.getValueAsString();
            ajp.nextToken();
            return agoEngine.getClass(className);
        }
        ajp.nextToken();       // [

        String className = ajp.getValueAsString();
        AgoClass baseClass = agoEngine.getClass(className);

        AgoClass result = null;
        while((token = ajp.nextToken()) != JsonToken.END_ARRAY) {
            ajp.nextToken();    // {
            if (token == JsonToken.FIELD_NAME) {
                if (ajp.getValueAsString().equals("@id")) {
                    if (result == null) {
                        result = deserializeClassRef(baseClass, ajp.getValueAsLong());
                    }
                } else if (ajp.getValueAsString().equals("scope")) {
                    var scope = deserializeAny(ajp, ctxt, null, creator, null, null);
                    if (result == null) {
                        result = agoEngine.createScopedClass(creator, baseClass.getClassId(), scope);
                    }
                }
            }
            ajp.nextToken();    // }
        }
        ajp.nextToken();
        return result == null ? baseClass : result;
    }

    //{"@classref": [classname, id, [parentScope]]}
    protected Instance<?> deserializeClassRef(AgoJsonParser ajp, DeserializationContext ctxt, CallFrame<?> creator) throws IOException {
        ajp.nextToken();
        assert ajp.currentToken() == JsonToken.START_ARRAY;
        ajp.nextToken();
        if(ajp.currentToken() == JsonToken.VALUE_NULL){
            ajp.nextToken();
            assert ajp.currentToken() == JsonToken.END_ARRAY;
            ajp.nextToken();
            assert ajp.currentToken() == JsonToken.END_OBJECT;
            ajp.nextToken();
            return null;
        }
        String classname = ajp.getValueAsString();
        AgoClass baseClass = agoEngine.getClass(classname);
        JsonToken token;
        AgoClass result = null;
        while((token = ajp.nextToken()) != JsonToken.END_ARRAY) {
            if (token == JsonToken.VALUE_NUMBER_INT) {      // id
                if(result == null)
                    result = deserializeClassRef(baseClass, ajp.getValueAsLong());
            } else if(token == JsonToken.START_OBJECT){
                var scope = deserializeAny(ajp, ctxt, null, creator, null, null);
                if(result == null)
                    result = agoEngine.createScopedClass(creator, baseClass.getClassId(), scope);
            }
        }
        ajp.nextToken();    // END_ARRAY
        ajp.nextToken();
        assert ajp.currentToken() == JsonToken.END_OBJECT;
        ajp.nextToken();
        return result == null ? baseClass : result;
    }

    protected AgoClass deserializeClassRef(AgoClass baseClass, long id) {
        throw new UnsupportedOperationException();
    }

    private Instance<?> deserializeCollection(AgoJsonParser ajp, DeserializationContext ctxt, AgoClass collectionClass, CallFrame<?> creator) throws IOException {
        // curr token is `[`
        if(ajp.currentToken() == JsonToken.START_ARRAY){
            ajp.nextToken();
        }
        if (collectionClass.getConcreteTypeInfo() instanceof ArrayInfo arrayInfo){
            TypeInfo elementType = arrayInfo.getElementType();
            var collection = switch (elementType.getTypeCode().getValue()) {
                case INT_VALUE -> {
                    IntArrayList list = new IntArrayList();
                    for (var token = ajp.currentToken(); token != null && token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.addInt(ajp.getIntValue());
                    }
                    ajp.nextToken();
                    var r = new IntArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    list.toIntArray(r.value);
                    yield r;
                }
                case DOUBLE_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != null && token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getDoubleValue());
                    }
                    ajp.nextToken();
                    var r = new DoubleArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                case BOOLEAN_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != null && token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getValueAsBoolean());
                    }
                    ajp.nextToken();
                    var r = new BooleanArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                case STRING_VALUE -> {
                    List<String> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != null && token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getValueAsString());
                    }
                    ajp.nextToken();
                    var r = new StringArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    list.toArray(r.value);
                    yield r;
                }
                case CHAR_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != null && token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getValueAsString().charAt(0));
                    }
                    ajp.nextToken();
                    var r = new CharArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                case SHORT_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != null && token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getIntValue());
                    }
                    ajp.nextToken();
                    var r = new ShortArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                case BYTE_VALUE -> {
                    IoBuffer buffer = IoBuffer.allocate(128).setAutoExpand(true);
                    for (var token = ajp.currentToken(); token != null && token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        buffer.put((byte) ajp.getIntValue());   //TODO write as base64
                    }
                    ajp.nextToken();
                    var r = new ByteArrayInstance(collectionClass.createSlots(), collectionClass, buffer.position());
                    buffer.flip().get(r.value);
                    yield r;
                }
                case FLOAT_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != null && token != JsonToken.END_ARRAY; token = ajp.nextToken()) {
                        list.add(ajp.getFloatValue());
                    }
                    ajp.nextToken();
                    var r = new FloatArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                case OBJECT_VALUE -> {
                    List<Object> list = new ArrayList<>();
                    for (var token = ajp.currentToken(); token != null && token != JsonToken.END_ARRAY; token = ajp.currentToken()) { // getInt stay at original pos, need nextToken to advance, but `deserializeAny` moves the pos
                        list.add(deserializeAny(ajp, ctxt, null, creator, null, null));
                    }
                    ajp.nextToken();
                    var r = new ObjectArrayInstance(collectionClass.createSlots(), collectionClass, list.size());
                    r.fill(list);
                    yield r;
                }
                default -> throw new IllegalStateException("Unexpected value: " + elementType.getTypeCode().getValue());
            };
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
