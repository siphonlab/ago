package org.siphonlab.ago.runtime.rdb.semischema;

import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.rdb.JsonSlotMapper;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;

public interface SemiSchemaJsonRefSlots extends Slots, ObjectRefOwner {
    JsonSlotMapper getJsonSlotMapper();
    ObjectRef getObjectRef();

}
