package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.AgoJsonGenerator;
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
    private final AgoEngine agoEngine;
    private final BoxTypes boxTypes;
    private final boolean writeType;
    private final boolean serializeSlots;

    public InstanceJsonSerializer(AgoEngine agoEngine, BoxTypes boxTypes, boolean writeType, boolean serializeSlots) {
        this.agoEngine = agoEngine;
        this.boxTypes = boxTypes;
        this.writeType = writeType;
        this.serializeSlots = serializeSlots;
    }

    public void writeObjectId(Instance<?> instance, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {

    }

    @Override
    public void serialize(Instance instance, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        AgoClass agoClass = instance.getAgoClass();

        AgoJsonGenerator agoJsonGenerator = (AgoJsonGenerator) gen;
        boolean writeType = agoJsonGenerator.isWriteType();
        boolean writeId = agoJsonGenerator.isWriteId();
        boolean serializeObjectAsReference = agoJsonGenerator.currSerializeObjectAsReference();

        if(boxTypes.isBoxType(agoClass)) {
            TypeCode typeCode = boxTypes.getUnboxType(agoClass);
            serializeUnboxed(instance, typeCode, gen, serializerProvider);
            return;
        }
        if(serializeObjectAsReference){
            gen.writeStartObject();
            gen.writeStringField("@type", instance.getAgoClass().getFullname());
            writeObjectId(instance, gen, serializerProvider);
            gen.writeEndObject();
            return;
        }
        if(instance instanceof AgoArrayInstance arrayInstance){
            if (writeType || writeId) {
                gen.writeStartObject();

                if(writeType) gen.writeStringField("@type", instance.getAgoClass().getFullname());
                if(writeId) writeObjectId(instance, gen, serializerProvider);

                gen.writeArrayFieldStart("@elements");
                gen.writeObject(arrayInstance.getArray());
                gen.writeEndArray();

                gen.writeEndObject();
            } else {
                gen.writeObject(arrayInstance.getArray());
            }
            return;
        }
        // iterable/iterator

        gen.writeStartObject();
        if(writeType){
            gen.writeStringField("@type", instance.getAgoClass().getFullname());
        }
        if(writeId) writeObjectId(instance, gen, serializerProvider);
        var slots = instance.getSlots();
        AgoSlotDef[] slotDefs = agoClass.getSlotDefs();
        if(!serializeSlots){
            var ls = new ArrayList<AgoSlotDef>();
            for (AgoField field : agoClass.getFields()) {
                if((field.getModifiers() & AgoClass.PUBLIC) == AgoClass.PUBLIC) {
                    ls.add(slotDefs[field.getSlotIndex()]);
                }
            }
            slotDefs = ls.toArray(AgoSlotDef[]::new);
        }
        for (AgoSlotDef slotDef : slotDefs) {
            int slotDefIndex = slotDef.getIndex();
            String slotDefName =  serializeSlots ? slotDef.getName() + "_" + slotDefIndex : slotDef.getName();
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
                        if(!this.writeType) {
                            if(slotDef.getAgoClass() != object.getAgoClass()){
                                agoJsonGenerator.setWriteType(true);     // write type for inner objects if the type mismatch with slotDef
                            }
                        }
                        gen.writeObjectField(slotDefName, object);
                    }
                    break;
                case NULL_VALUE:
                    gen.writeNullField(slotDefName);
                    break;
                case CLASS_REF_VALUE:
                    int classRef = slots.getClassRef(slotDefIndex);
                    gen.writeStringField(slotDefName, agoEngine.getClass(classRef).getFullname());
                    break;
            }
        }
        gen.writeEndObject();
    }

    private void serializeUnboxed(Instance<?> instance, TypeCode typeCode, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        Slots slots = instance.getSlots();
        switch (typeCode.value){
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
                serialize(object,gen,serializerProvider);
                break;
            case NULL_VALUE:
                gen.writeNull();
                break;
            case CLASS_REF_VALUE:
                int classRef = slots.getClassRef(0);
                gen.writeString(agoEngine.getClass(classRef).getFullname());
                break;
        }
    }
}
