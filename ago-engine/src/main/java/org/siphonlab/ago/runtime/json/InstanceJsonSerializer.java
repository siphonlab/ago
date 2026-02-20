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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.NotImplementedException;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.AgoArrayInstance;

import java.io.IOException;
import java.util.ArrayList;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.NULL_VALUE;
import static org.siphonlab.ago.TypeCode.OBJECT_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;

public class InstanceJsonSerializer extends JsonSerializer<Instance> {
    protected final AgoEngine agoEngine;

    public InstanceJsonSerializer(AgoEngine agoEngine) {
        this.agoEngine = agoEngine;
    }

    public void writeObjectId(Instance<?> instance, JsonGenerator gen) throws IOException {
        throw new NotImplementedException();
    }

    public void writeObjectAsReference(Instance<?> instance, JsonGenerator gen) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void serialize(Instance instance, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        AgoClass agoClass = instance.getAgoClass();

        AgoJsonGenerator ag = (AgoJsonGenerator) gen;
        AgoJsonConfig config = ag.getConfig();

        boolean writeType = ag.isWriteType();
        boolean writeId = config.isWriteId();
        boolean writeObjectAsReference = ag.currWriteObjectAsReference();

        BoxTypes boxTypes = agoEngine.getBoxTypes();
        if(boxTypes.isBoxType(agoClass)) {
            if (agoClass instanceof AgoEnum agoEnum) {
                // {@type: "enum class", value: number if writeSlots else string}
                //  or
                // number if writeSlots else string
                if (config.getWriteType() == AgoJsonConfig.WriteTypeMode.OnDemand) {
                    // for depth>0,depends on agoClass == slotDef.agoClass
                    if (ag.getDepth() == 0) {
                        writeType = true;
                    }
                } else if (config.getWriteType() == AgoJsonConfig.WriteTypeMode.Inner) {
                    if (!writeType) writeType = ag.getDepth() > 0;
                }
                if (config.isWriteSlots()) {
                    TypeCode typeCode = boxTypes.getUnboxType(agoClass);
                    serializeUnboxed(instance, typeCode, gen, serializerProvider, writeType);
                } else {
                    if (writeType) {
                        gen.writeStartObject();
                        gen.writeStringField("@box_type", agoClass.getFullname());
                        gen.writeStringField("value", instance.getSlots().getString(1));
                        gen.writeEndObject();
                    } else {
                        gen.writeString(instance.getSlots().getString(1));
                    }
                }
                return;
            }


            if(config.getWriteType() == AgoJsonConfig.WriteTypeMode.OnDemand) {
                // for depth>0,depends on agoClass == slotDef.agoClass
                if (ag.getDepth() == 0 && !agoEngine.getBoxer().isNarrowBoxType(agoClass)) {
                    writeType = true;
                }
            } else if(config.getWriteType() == AgoJsonConfig.WriteTypeMode.Inner){
                if(!writeType) writeType = !agoEngine.getBoxer().isNarrowBoxType(agoClass);
            }
            if(!agoEngine.getBoxer().isNarrowBoxType(agoClass) && agoClass.getSlotDefs().length > 1) {
                assert writeType;
                serializeComplexUnboxed(instance, agoClass, ag, serializerProvider);
            } else {
                TypeCode typeCode = boxTypes.getUnboxType(agoClass);
                serializeUnboxed(instance, typeCode, gen, serializerProvider, writeType);
            }
            return;
        }
        if(instance instanceof AgoClass classInst){     // output an AgoClass
            // class of AgoClass often be MetaClass, cannot find backward to itself, so we always
            if(!writeType){
                switch (config.getWriteType()) {
                    case OnDemand:
                    case Inner:
                        writeType = true;
                }
            }
            if (writeType || writeId) {     // if writeId preferred, writeType too
                gen.writeStartObject();
                gen.writeFieldName("@class");
                if(classInst instanceof MetaClass && classInst.getFullname().equals("<Meta>")){
                    gen.writeString(classInst.getFullname());
                    gen.writeEndObject();
                    return;
                }
                // jsonb doesn't preserver filed order,
                // so make the structure as
                //  {@class : classname}
                // or
                //  {@class : [classname, {"@id:": id}, {"scope":scope}]]
                if(writeId || agoClass.getParentScope() != null) {
                    gen.writeStartArray();
                    gen.writeString(classInst.getFullname());
                    if (writeId) {
                        gen.writeStartObject();
                        writeObjectId(classInst, gen);
                        gen.writeEndObject();
                    }
                    if (agoClass.getParentScope() != null) {
                        gen.writeStartObject();
                        gen.writeObjectField("scope", agoClass.getParentScope());
                        gen.writeEndObject();
                    }
                    gen.writeEndArray();
                } else {
                    gen.writeString(classInst.getFullname());
                }
                gen.writeEndObject();
            } else {
                gen.writeString(classInst.getFullname());
            }
            return;
        }

        if(instance instanceof AgoArrayInstance arrayInstance){
            if(config.getWriteType() == AgoJsonConfig.WriteTypeMode.OnDemand && ag.getDepth() == 0){
                writeType = true;
            }
            // writeId not apply on array
            if (writeType) {
                gen.writeStartObject();

                writeClass(gen, "@collection", instance.getAgoClass());

                gen.writeFieldName("@elements");
                gen.writeObject(arrayInstance.getArray());

                gen.writeEndObject();
            } else {
                gen.writeObject(arrayInstance.getArray());
            }
            return;
        }
        //TODO iterable/iterator

        if (writeObjectAsReference) {
            writeObjectAsReference(instance, gen);
            return;
        }

        gen.writeStartObject();
        if(writeType){
            writeClass(gen, "@type", instance.getAgoClass());
        }
        if(writeId) writeObjectId(instance, gen);
        var slots = instance.getSlots();
        AgoSlotDef[] slotDefs = agoClass.getSlotDefs();
        if(slotDefs == null) {
            gen.writeEndObject();
            return;
        }
        if(!config.isWriteSlots()){
            var ls = new ArrayList<AgoSlotDef>();
            for (AgoField field : agoClass.getFields()) {
                if((field.getModifiers() & AgoClass.PUBLIC) == AgoClass.PUBLIC) {
                    ls.add(slotDefs[field.getSlotIndex()]);
                }
            }
            slotDefs = ls.toArray(AgoSlotDef[]::new);
        }
        writeSlots(agoEngine,ag,slotDefs,config,slots);
    }

    private void writeClass(JsonGenerator gen, String fieldName, AgoClass agoClass) throws IOException {
        if(agoClass.getParentScope() == null){
            gen.writeStringField(fieldName,agoClass.getFullname());
        } else {
            gen.writeFieldName(fieldName);
            writeObjectAsReference(agoClass, gen);
        }
    }

    public static void writeSlots(AgoEngine agoEngine, AgoJsonGenerator gen, AgoSlotDef[] slotDefs, AgoJsonConfig config, Slots slots) throws IOException {
        gen.writeStartObject();
        if (slotDefs == null || slotDefs.length == 0 || slots instanceof AgoClass.TraceOwnerSlots) {
            gen.writeEndObject();
            return;
        }
        for (AgoSlotDef slotDef : slotDefs) {
            int slotDefIndex = slotDef.getIndex();
            String slotDefName = config.isWriteSlots() ? slotDef.getName() + "_" + slotDefIndex : slotDef.getName();
            switch (slotDef.getTypeCode().value) {
                case INT_VALUE:
                    gen.writeNumberField(slotDefName, slots.getInt(slotDefIndex));
                    break;
                case LONG_VALUE:
                    gen.writeNumberField(slotDefName, slots.getLong(slotDefIndex));
                    break;
                case FLOAT_VALUE:
                    gen.writeNumberField(slotDefName, slots.getFloat(slotDefIndex));
                    break;
                case DOUBLE_VALUE:
                    gen.writeNumberField(slotDefName, slots.getDouble(slotDefIndex));
                    break;
                case BOOLEAN_VALUE:
                    gen.writeBooleanField(slotDefName, slots.getBoolean(slotDefIndex));
                    break;
                case STRING_VALUE:
                    gen.writeStringField(slotDefName, slots.getString(slotDefIndex));
                    break;
                case SHORT_VALUE:
                    gen.writeNumberField(slotDefName, slots.getShort(slotDefIndex));
                    break;
                case BYTE_VALUE:
                    gen.writeNumberField(slotDefName, slots.getByte(slotDefIndex));
                    break;
                case CHAR_VALUE:
                    gen.writeStringField(slotDefName, String.valueOf(slots.getChar(slotDefIndex)));
                    break;
                case OBJECT_VALUE:
                    Instance<?> object = slots.getObject(slotDefIndex);
                    if (object == null) {
                        gen.writeNullField(slotDefName);
                    } else {
                        boolean innerWriteType = switch (config.getWriteType()) {
                            case Always, Inner -> true;
                            case OnDemand -> (slotDef.getAgoClass() != object.getAgoClass());
                            case null, default -> false;
                        };
                        gen.setWriteType(innerWriteType);
                        gen.writeObjectField(slotDefName, object);
                    }
                    break;
                case NULL_VALUE:
                    gen.writeNullField(slotDefName);
                    break;
                case CLASS_REF_VALUE:
                    int classRef = slots.getClassRef(slotDefIndex);
                    if(classRef == 0){
                        gen.writeNullField(slotDefName);
                    } else {
                        gen.writeStringField(slotDefName, agoEngine.getClass(classRef).getFullname());
                    }
                    break;
            }
        }
        gen.writeEndObject();
    }

    protected void serializeUnboxed(Instance<?> instance, TypeCode typeCode, JsonGenerator gen, SerializerProvider serializerProvider,
                                  boolean writeType) throws IOException {

        //TODO for number types, I am not sure OnDemand need `@type` or not yet
        if(writeType){
            gen.writeStartObject();
            gen.writeStringField("@box_type",instance.getAgoClass().getFullname());
            gen.writeFieldName("value");
        }
        Slots slots = instance.getSlots();
        switch (typeCode.value) {
            case INT_VALUE:
                gen.writeNumber(slots.getInt(0));
                break;
            case LONG_VALUE:
                gen.writeNumber(slots.getLong(0));
                break;
            case FLOAT_VALUE:
                gen.writeNumber(slots.getFloat(0));
                break;
            case DOUBLE_VALUE:
                gen.writeNumber(slots.getDouble(0));
                break;
            case BOOLEAN_VALUE:
                gen.writeBoolean(slots.getBoolean(0));
                break;
            case STRING_VALUE:
                gen.writeString(slots.getString(0));
                break;
            case SHORT_VALUE:
                gen.writeNumber(slots.getShort(0));
                break;
            case BYTE_VALUE:
                gen.writeNumber(slots.getByte(0));
                break;
            case CHAR_VALUE:
                gen.writeString(String.valueOf(slots.getChar(0)));
                break;
            case OBJECT_VALUE:
                Instance<?> object = slots.getObject(0);
                serialize(object, gen, serializerProvider);
                break;
            case NULL_VALUE:
                gen.writeNull();
                break;
            case CLASS_REF_VALUE:
                int classRef = slots.getClassRef(0);
                gen.writeString(agoEngine.getClass(classRef).getFullname());
                break;
        }
        if(writeType){
            gen.writeEndObject();
        }

    }

    protected void serializeComplexUnboxed(Instance<?> instance, AgoClass agoClass, AgoJsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("@box_type", instance.getAgoClass().getFullname());
        gen.writeFieldName("value");

        writeSlots(agoEngine, gen, agoClass.getSlotDefs(), gen.getConfig(), instance.getSlots());
        gen.writeEndObject();
    }
}
