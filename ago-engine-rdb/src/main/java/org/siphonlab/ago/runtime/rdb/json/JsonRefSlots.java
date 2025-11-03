package org.siphonlab.ago.runtime.rdb.json;

import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.rdb.JsonSlotMapper;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;

public interface JsonRefSlots extends Slots, ObjectRefOwner {
    JsonSlotMapper getJsonSlotMapper();
    ObjectRef getObjectRef();

}
