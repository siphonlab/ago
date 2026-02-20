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
package org.siphonlab.ago.runtime.rdb.reactive.json;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoSlotDef;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.runtime.rdb.JsonSlotMapper;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.reactive.*;
import org.siphonlab.ago.runtime.rdb.json.JsonRefSlots;

/**
 * must call with `reactiveJsonRefSlots.withFrame()`
 * this `Slots` don't store value in memory, the getters always read value from db
 */
public class ReactiveJsonRefSlots extends RdbRefSlots implements JsonRefSlots {

    private final JsonSlotMapper mapper;

    public ReactiveJsonRefSlots(ObjectRef objectRef, SlotsAdapter slotsAdapter, AgoSlotDef[] slotDefs) {
        super(objectRef, slotsAdapter, slotDefs);

        this.mapper = new JsonSlotMapper(slotDefs){
            public String mapType(TypeCode typeCode, AgoClass agoClass){
                return slotsAdapter.mapType(typeCode,agoClass);
            }
        };
    }

    public JsonSlotMapper getJsonSlotMapper() {
        return mapper;
    }

    private String composeFieldName(AgoSlotDef slotDef) {
        return slotDef.getName() + "_" + slotDef.getIndex();
    }

    public String getDataType(int slot){
        return mapper.getDataType(slot);
    }

    public String getFieldName(int slot) {
        return mapper.getFieldName(slot);
    }

    @Override
    public ObjectRef getObjectRef() {
        return super.getObjectRef();
    }

    public AgoSlotDef[] getSlotDefs() {
        return slotDefs;
    }

    @Override
    public String toString() {
        return "(JsonRefSlots " + objectRef.className() + " " + objectRef.id() + ")";
    }
}