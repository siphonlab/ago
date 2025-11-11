package org.siphonlab.ago.runtime.rdb.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.AgoArrayInstance;
import org.siphonlab.ago.runtime.json.AgoJsonConfig;
import org.siphonlab.ago.runtime.json.AgoJsonGenerator;
import org.siphonlab.ago.runtime.rdb.RdbSlots;
import org.siphonlab.ago.runtime.rdb.RowState;

import java.io.IOException;

import static org.siphonlab.ago.runtime.json.InstanceJsonSerializer.writeSlots;

// bind attribute `slots_class` to transfer SlotDefs
// slots_instance is optional
public class RdbSlotsJsonSerializer extends JsonSerializer<RdbSlots> {

    @Override
    public void serialize(RdbSlots value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("usingInstances", value.getUsingInstances());
        RowState rowState = value.getRowState();
        if(rowState != RowState.Saving) {
            gen.writeObjectField("rowState", rowState);
        } else {
            gen.writeObjectField("rowState", RowState.Unchanged);
        }
        // changedSlots and detachedInstances needn't save, for after saving

        gen.writeFieldName("slots");
        serializers.findValueSerializer(Slots.class)
                .serialize(value.getBaseSlots(), gen, serializers);

        gen.writeEndObject();
    }
}
