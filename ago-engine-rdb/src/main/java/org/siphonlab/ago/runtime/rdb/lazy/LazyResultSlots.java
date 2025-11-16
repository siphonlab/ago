package org.siphonlab.ago.runtime.rdb.lazy;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.ResultSlots;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;

// think about increase ref to keep instance in ResultSlots
// an idea yet
public class LazyResultSlots extends ResultSlots {

    @Override
    public void setObjectValue(Instance<?> value) {
        super.setObjectValue(value);
        ReferenceCounter.increaseRef(value, ReferenceCounter.Reason.SetSlotInstall);
    }

    @Override
    public Instance<?> getObjectValue() {
        return super.getObjectValue();
    }
}
