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

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.ObjectRef;

public class ReactiveJsonSlotsCreatorFactory implements SlotsCreatorFactory {

    private ReactiveJsonPGAdapter adapter;

    public ReactiveJsonSlotsCreatorFactory(ReactiveJsonPGAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public SlotsCreator generateSlotsCreator(AgoClass agoClass) {
        return new SlotsCreator() {
            @Override
            public Slots create() {
                long id = adapter.nextId();
                ObjectRef objectRef = new ObjectRef(agoClass.getFullname(), id);
                var r = new ReactiveJsonRefSlots(objectRef, adapter.getSlotsAdapter(), agoClass.getSlotDefs());
                r.setSaved(false);
                return r;
            }

            @Override
            public Class<?> getSlotType(int slotIndex) {
                return DefaultSlotsCreatorFactory.typeOf(agoClass.getSlotDefs()[slotIndex].getTypeCode());
            }
        };
    }

    public void setAdapter(ReactiveJsonPGAdapter adapter) {
        this.adapter = adapter;
    }
}

