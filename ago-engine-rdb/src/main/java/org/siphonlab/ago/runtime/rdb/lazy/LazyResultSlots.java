package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.ResultSlots;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
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
