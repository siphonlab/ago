package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbEngine;
import org.siphonlab.ago.runtime.rdb.lazy.DereferenceAdapter;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstanceTrait;
import org.siphonlab.ago.runtime.rdb.lazy.ReferenceableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine.toObjectRefCallFrame;

public class DeferenceNativeFrame extends NativeFrame implements ReferenceableInstance, ObjectRefOwner {

    private static final Logger logger = LoggerFactory.getLogger(DeferenceNativeFrame.class);

    private final RdbAdapter adapter;

    public DeferenceNativeFrame(LazyJsonRefSlots slots, AgoNativeFunction agoFunction, RdbEngine engine) {
        super(engine, slots, agoFunction);
        this.adapter = engine.getRdbAdapter();
    }

    @Override
    public ObjectRefInstanceTrait toObjectRefInstance() {
        LazyJsonRefSlots slots = (LazyJsonRefSlots) this.slots;
        if (logger.isDebugEnabled()) logger.debug("%s convert to objectref instance %s".formatted(this, slots.getObjectRef()));
        return new ObjectRefCallFrame<>(this.agoClass, slots.getObjectRef(), (DereferenceAdapter) this.adapter, slots.getRowState());
    }

    @Override
    public void setCaller(CallFrame<?> caller) {
        if (!Objects.equals(caller, this.caller)) {
            var c = toObjectRefCallFrame(caller);
            super.setCaller(c);
        } else if(caller instanceof ObjectRefInstanceTrait){
            super.setCaller(caller);
        }
    }

    public ObjectRef getObjectRef() {
        return ((LazyJsonRefSlots) this.slots).getObjectRef();
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

    @Override
    public void setPayload(Object payload) {
        super.setPayload(payload);
        ((RdbEngine)this.engine).getRdbAdapter().saveInstance(this);
    }
}
