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
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.json.InstanceJsonSerializer;
import org.siphonlab.ago.runtime.db.DbSlots;

import java.io.IOException;

import static org.siphonlab.ago.runtime.rdb.ObjectRefOwner.extractObjectRef;

public class InstanceJsonSerializerWithObjectId extends InstanceJsonSerializer {

    public InstanceJsonSerializerWithObjectId(AgoEngine agoEngine) {
        super(agoEngine);
    }

    @Override
    public void writeObjectId(Instance<?> instance, JsonGenerator gen) throws IOException {
        DbSlots<?> slots = (DbSlots<?>) instance.getSlots();

        Object id = slots.getObjectRef().id();
        gen.writeObjectField("id", id);
//        gen.writeNumberField("@id", id);
    }

    @Override
    public void writeObjectAsReference(Instance<?> instance, JsonGenerator gen) throws IOException {
        gen.writeStartObject();
        if (instance instanceof AgoClass classInst) {
            gen.writeFieldName("@classref");
            gen.writeStartArray();
            gen.writeString(classInst.getFullname());

            DbSlots<?> slots = (DbSlots<?>) instance.getSlots();
            gen.writeObject(slots.getObjectRef().id());

            if(classInst.getParentScope() != null){
                gen.writeObject(classInst.getParentScope());
            }
            gen.writeEndArray();
        } else {
            gen.writeFieldName("@objectref");
            gen.writeStartArray();
            var objectRef = extractObjectRef(instance);
            gen.writeString(objectRef.className());
            gen.writeObject(objectRef.id());

            gen.writeEndArray();
        }
        gen.writeEndObject();
    }
}