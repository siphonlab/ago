package org.siphonlab.ago.runtime.rdb.json;

import com.fasterxml.jackson.core.JsonGenerator;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.json.InstanceJsonSerializer;
import org.siphonlab.ago.runtime.rdb.RdbSlots;

import java.io.IOException;

public class InstanceJsonSerializerWithObjectId extends InstanceJsonSerializer {

    public InstanceJsonSerializerWithObjectId(AgoEngine agoEngine) {
        super(agoEngine);
    }

    @Override
    public void writeObjectId(Instance<?> instance, JsonGenerator gen) throws IOException {
        RdbSlots slots = (RdbSlots) instance.getSlots();
        gen.writeNumberField("@id", slots.getId());
    }

    @Override
    public void writeObjectAsReference(Instance<?> instance, JsonGenerator gen) throws IOException {
        gen.writeStartObject();
        if (instance instanceof AgoClass classInst) {
            gen.writeFieldName("@classref");
            gen.writeStartArray();
            gen.writeString(classInst.getFullname());

            RdbSlots slots = (RdbSlots) instance.getSlots();
            gen.writeNumber(slots.getId());

            if(classInst.getParentScope() != null){
                gen.writeObject(classInst.getParentScope());
            }
            gen.writeEndArray();
        } else {
            gen.writeFieldName("@objectref");
            gen.writeStartArray();
            gen.writeString(instance.getAgoClass().getFullname());

            RdbSlots slots = (RdbSlots) instance.getSlots();
            gen.writeNumber(slots.getId());

            gen.writeEndArray();
        }
        gen.writeEndObject();
    }
}