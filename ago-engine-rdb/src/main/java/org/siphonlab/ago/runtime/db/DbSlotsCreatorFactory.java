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
package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.*;

public class DbSlotsCreatorFactory<Id> implements SlotsCreatorFactory {

    private final DefaultSlotsCreatorFactory baseSlotFactory;
    private DbAdapter<Id> adapter;

    public DbSlotsCreatorFactory(){
        this.baseSlotFactory = new DefaultSlotsCreatorFactory();
    }

    public void setAdapter(DbAdapter<Id> adapter) {
        this.adapter = adapter;
    }

    @Override
    public SlotsCreator generateSlotsCreator(AgoClass agoClass) {
        var creator = baseSlotFactory.generateSlotsCreator(agoClass);
        return new DbSlotsCreator<Id>(){
            @Override
            public Slots create() {
                /*
                TODO handle value type
                    if(engine.getBoxTypes() != null && engine.getBoxTypes().isBoxType(agoClass)){
                        return creator.create();
                    } else {
                        LangClasses langClasses = engine.getLangClasses();
                        if(langClasses != null && langClasses.getArrayClass() != null && agoClass.isThatOrDerivedFrom(engine.getLangClasses().getArrayClass())){
                            return creator.create();
                        }
                    }
                 */
                var baseSlots = (creator == null)? new AgoClass.TraceOwnerSlots(agoClass) : creator.create();
                var objectRef = ObjectRef.create(agoClass.getFullname(), adapter.nextId());
                var slots = new DbSlots<Id>(baseSlots, objectRef);
                if (agoClass.getSlotDefs() != null) {
                    slots.allocateObjectSlots(agoClass.getSlotDefs().length);
                }
                return slots;
            }

            @Override
            public Class<?> getSlotType(int slotIndex) {
                return creator.getSlotType(slotIndex);
            }

            // for restore with existed objectRef
            @Override
            public DbSlots<Id> create(ObjectRef<Id> objectRef) {
                var baseSlots = (creator == null)? new AgoClass.TraceOwnerSlots(agoClass) : creator.create();
                var slots = new DbSlots<Id>(baseSlots, objectRef);
                if (agoClass.getSlotDefs() != null) {
                    slots.allocateObjectSlots(agoClass.getSlotDefs().length);
                }
                return slots;
            }
        };
    }

}
