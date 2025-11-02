package org.siphonlab.ago.runtime.rdb.semischema.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbEngine;
import org.siphonlab.ago.runtime.rdb.lazy.DereferenceAdapter;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstanceTrait;
import org.siphonlab.ago.runtime.rdb.lazy.ReferenceableInstance;
import org.siphonlab.ago.runtime.stateful.StatefulNativeFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static org.siphonlab.ago.runtime.rdb.semischema.lazy.JsonAgoEngine.toObjectRefCallFrame;

public class DeferenceNativeFrame extends StatefulNativeFrame implements ReferenceableInstance, ObjectRefOwner {

    private static final Logger logger = LoggerFactory.getLogger(DeferenceNativeFrame.class);

    private final RdbAdapter adapter;

    public DeferenceNativeFrame(JsonRefSlots slots, AgoNativeFunction agoFunction, RdbEngine engine) {
        super(engine, slots, agoFunction, new RunningStateStoreViaAdapter(engine.getRdbAdapter()));
        this.adapter = engine.getRdbAdapter();
    }

    @Override
    public ObjectRefInstanceTrait toObjectRefInstance() {
        if (logger.isDebugEnabled()) logger.debug("%s convert to objectref instance %s".formatted(this, ((JsonRefSlots) this.slots).getObjectRef()));
        return new ObjectRefCallFrame<AgoNativeFunction>(this.agoClass, ((JsonRefSlots) this.slots).getObjectRef(), (DereferenceAdapter) this.adapter);
    }

    @Override
    public void setCaller(CallFrame<?> caller) {
        if (!Objects.equals(caller, this.caller)) {
            var c = toObjectRefCallFrame(caller, this);
            super.setCaller(c);
            this.runningStateStore.saveState(this);
        } else if(caller instanceof ObjectRefInstanceTrait){
            super.setCaller(caller);
        }
    }

    public ObjectRef getObjectRef() {
        return ((JsonRefSlots) this.slots).getObjectRef();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeferenceAgoFrame deferenceAgoFrame) {
            return this.getObjectRef().equals(deferenceAgoFrame.getObjectRef());
        } else if (obj instanceof ObjectRefInstanceTrait objectRefInstanceTrait) {
            return this.getObjectRef().equals(objectRefInstanceTrait.getObjectRef());
        } else {
            return false;
        }
    }
}
