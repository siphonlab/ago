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
package org.siphonlab.ago.runtime.rdb;

import org.agrona.concurrent.SnowflakeIdGenerator;
import org.siphonlab.ago.*;

public class RdbSlotsCreatorFactory implements SlotsCreatorFactory {

    private final DefaultSlotsCreatorFactory baseSlotFactory;
    private final RdbAdapter rdbAdapter;

    public RdbSlotsCreatorFactory(RdbAdapter rdbAdapter){
        this.rdbAdapter = rdbAdapter;
        this.baseSlotFactory = new DefaultSlotsCreatorFactory();
    }

    @Override
    public SlotsCreator generateSlotsCreator(AgoClass agoClass) {
        var creator = baseSlotFactory.generateSlotsCreator(agoClass);
        return new SlotsCreator() {
            @Override
            public Slots create() {
                var baseSlots = (creator == null)? new AgoClass.TraceOwnerSlots(agoClass) : creator.create();
                var slots = new RdbSlots(baseSlots);
                if (agoClass.getSlotDefs() != null) {
                    slots.allocateObjectSlots(agoClass.getSlotDefs().length);
                }
                slots.setId(rdbAdapter.idGenerator.nextId());
                return slots;
            }

            @Override
            public Class<?> getSlotType(int slotIndex) {
                return creator.getSlotType(slotIndex);
            }
        };
    }

}
