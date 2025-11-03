package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.Instance;

public interface ObjectRefOwner {
    ObjectRef getObjectRef();

    static ObjectRef extractObjectRef(Instance<?> instance) {
        if (instance == null) return null;
        if (instance instanceof ObjectRefOwner) {
            return ((ObjectRefOwner) instance).getObjectRef();
        } else if(instance.getSlots() instanceof ObjectRefOwner slots){
            return slots.getObjectRef();
        }
        throw new IllegalArgumentException("ObjectRefOwner expected");
    }

}
