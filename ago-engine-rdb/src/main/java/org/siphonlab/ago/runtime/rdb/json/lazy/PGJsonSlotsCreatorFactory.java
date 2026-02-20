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
package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.JsonSlotMapper;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;

import java.util.function.Supplier;

public class PGJsonSlotsCreatorFactory implements SlotsCreatorFactory {

    private LazyJsonPGAdapter adapter;

    private final DefaultSlotsCreatorFactory baseSlotFactory;
    private PersistentRdbEngine engine;

    public PGJsonSlotsCreatorFactory() {
        this.baseSlotFactory = new DefaultSlotsCreatorFactory();
    }

    @Override
    public SlotsCreator generateSlotsCreator(AgoClass agoClass) {
        var creator = baseSlotFactory.generateSlotsCreator(agoClass);
        return new JsonRefSlotsCreator(creator, agoClass);
    }

    public void setAdapter(LazyJsonPGAdapter adapter) {
        this.adapter = adapter;
    }

    public void setEngine(PersistentRdbEngine engine) {
        this.engine = engine;
    }

    public PersistentRdbEngine getEngine() {
        return engine;
    }


    public class JsonRefSlotsCreator implements SlotsCreator {
        private final SlotsCreator creator;
        private final AgoClass agoClass;

        public JsonRefSlotsCreator(SlotsCreator creator, AgoClass agoClass) {
            this.creator = creator;
            this.agoClass = agoClass;
        }

        @Override
        public Slots create() {
            if(engine.getBoxTypes() != null && engine.getBoxTypes().isBoxType(agoClass)){
                return creator.create();
            } else {
                LangClasses langClasses = engine.getLangClasses();
                if(langClasses != null && langClasses.getArrayClass() != null && agoClass.isThatOrDerivedFrom(engine.getLangClasses().getArrayClass())){
                    return creator.create();
                }
            }
            long id = adapter.nextId();
            ObjectRef objectRef = new ObjectRef(agoClass.getFullname(), id);
            return create(objectRef);
        }

        public Slots create(ObjectRef objectRef) {
            Slots baseSlots = creator == null ? new AgoClass.TraceOwnerSlots(agoClass) : creator.create();
            var r = new LazyJsonRefSlots(baseSlots, objectRef, new JsonSlotMapper(agoClass.getSlotDefs()) {
                @Override
                public String mapType(TypeCode typeCode, AgoClass agoClass) {
                    return adapter.mapType(typeCode, agoClass).getTypeName();
                }
            });
            if(agoClass.getSlotDefs() != null) {
                r.allocateObjectSlots(agoClass.getSlotDefs().length);
            }
            return r;
        }

        @Override
        public Class<?> getSlotType(int slotIndex) {
            return DefaultSlotsCreatorFactory.typeOf(agoClass.getSlotDefs()[slotIndex].getTypeCode());
        }
    }
}
