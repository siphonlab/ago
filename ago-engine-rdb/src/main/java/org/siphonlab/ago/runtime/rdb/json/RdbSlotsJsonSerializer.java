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
package org.siphonlab.ago.runtime.rdb.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.AgoArrayInstance;
import org.siphonlab.ago.runtime.json.AgoJsonConfig;
import org.siphonlab.ago.runtime.json.AgoJsonGenerator;
import org.siphonlab.ago.runtime.rdb.RdbEngine;
import org.siphonlab.ago.runtime.rdb.RdbSlots;
import org.siphonlab.ago.runtime.rdb.RowState;

import java.io.IOException;

import static org.siphonlab.ago.runtime.json.InstanceJsonSerializer.writeSlots;

// bind attribute `slots_class` to transfer SlotDefs
// slots_instance is optional
public class RdbSlotsJsonSerializer extends JsonSerializer<RdbSlots> {

    private final RdbEngine rdbEngine;

    public RdbSlotsJsonSerializer(RdbEngine rdbEngine) {
        this.rdbEngine = rdbEngine;
    }

    @Override
    public void serialize(RdbSlots value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("usingInstances", value.getUsingInstances());

        // box instance scope, i.e. (1|Integer).toString()
        Instance<?> instance = (Instance<?>) serializers.getAttribute("slots_instance");
        if(instance != null && instance.getParentScope() != null){
            if(rdbEngine.getBoxTypes().isBoxType(instance.getParentScope().getAgoClass())){
                gen.writeObjectField("scope",instance.getParentScope());
            }
        }

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
