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
package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.RdbSlots;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;

import java.util.Objects;

public class RdbRefSlots extends RdbSlots implements ObjectRefOwner {

    private ObjectRef objectRef;

    /**
     *
     * @param baseSlots
     * @param objectRef
     */
    public RdbRefSlots(Slots baseSlots, ObjectRef objectRef) {
        super(baseSlots);
        this.objectRef = objectRef;
    }

    public ObjectRef getObjectRef() {
        return objectRef;
    }

    public void restoreId(long id){
        this.objectRef = new ObjectRef(objectRef.className(), id);
    }

    @Override
    public String toString() {
        return "(RdbRefSlots %s)".formatted(objectRef);
    }
}
