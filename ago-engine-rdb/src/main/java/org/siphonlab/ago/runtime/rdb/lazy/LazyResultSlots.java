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
import org.siphonlab.ago.ResultSlots;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;

// think about increase ref to keep instance in ResultSlots
// an idea yet
public class LazyResultSlots extends ResultSlots {

    @Override
    public void setObjectValue(Instance<?> value) {
        if(this.objectValue != null){
            ReferenceCounter.releaseRef(objectValue, ReferenceCounter.Reason.SetResultSlotsDrop);
        }
        this.objectValue = value;
        if(value != null) {
            ReferenceCounter.increaseRef(value, ReferenceCounter.Reason.SetResultSlotsInstall);
        }
    }

    @Override
    public Instance<?> takeObjectValue() {
        if(objectValue == null) return null;

        var r = objectValue;
        ReferenceCounter.releaseRef(r, ReferenceCounter.Reason.TakeObjectValue);
        objectValue = null;
        return r;
    }

    public void cleanObjectResult() {
        if(this.getObjectValue() != null){
            ReferenceCounter.releaseRef(getObjectValue(), ReferenceCounter.Reason.SetResultSlotsDrop);
        }
    }
}
