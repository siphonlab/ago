package org.siphonlab.ago.runtime.db.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.CreateInstanceRunSpace;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;

public class DereferenceContextNativeFrame<Id> extends AgoFrame implements ObjectRefOwner<Id> {

    public DereferenceContextNativeFrame(Slots slots, AgoFunction agoFunction, AgoEngine engine) {
        super(slots, agoFunction, engine);
    }

    @Override
    public void setRunSpace(RunSpace runSpace) {
        super.setRunSpace(runSpace);
        if(runSpace instanceof CreateInstanceRunSpace<?> createInstanceRunSpace){
            createInstanceRunSpace.updateDeferenceContext(this, (DereferenceContextSlots)getSlots());
        }
    }

    @Override
    public ObjectRef<Id> getObjectRef() {
        return ((DereferenceContextSlots<Id>)this.getSlots()).getObjectRef();
    }


}
