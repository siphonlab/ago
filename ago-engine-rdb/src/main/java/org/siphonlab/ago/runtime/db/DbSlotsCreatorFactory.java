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
import org.siphonlab.ago.runtime.rdb.RowState;

public class DbSlotsCreatorFactory<Id> implements SlotsCreatorFactory {

    private final DefaultSlotsCreatorFactory baseSlotFactory;
    private IdGenerator<Id> idGenerator;

    public DbSlotsCreatorFactory(){
        this.baseSlotFactory = new DefaultSlotsCreatorFactory();
    }

    public void setIdGenerator(IdGenerator<Id> idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public SlotsCreator generateSlotsCreator(AgoClass agoClass) {
        var baseCreator = baseSlotFactory.generateSlotsCreator(agoClass);
        boolean isEntity =  agoClass.getClassLoader().getLangClasses().getEntityClass().isThatOrSuperOfThat(agoClass);
        return new DbSlotsCreator<Id>(){
            @Override
            public Slots create() {
                if(isEntity) {
                    return create(null);
                }
                return createDefaultSlots();
            }

            @Override
            public Class<?> getSlotType(int slotIndex) {
                return baseCreator.getSlotType(slotIndex);
            }

            // for restore with existed objectRef
            @Override
            public DbSlots<Id> create(ObjectRef<Id> objectRef) {
                var baseSlots = createDefaultSlots();

                if(objectRef == null) objectRef = ObjectRef.create(agoClass.getFullname(), idGenerator.nextId());

                var slots = new DbSlots<Id>(baseSlots, objectRef);
                if (agoClass.getSlotDefs() != null) {
                    slots.allocateObjectSlots(agoClass.getSlotDefs().length);
                }
                return slots;
            }

            // for default runspace
            public Slots createDefaultSlots() {
                return  (baseCreator == null)? new AgoClass.TraceOwnerSlots(agoClass) : baseCreator.create();
            }
        };
    }

}
