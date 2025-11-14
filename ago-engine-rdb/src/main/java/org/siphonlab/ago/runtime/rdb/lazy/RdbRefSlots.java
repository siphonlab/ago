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

}
