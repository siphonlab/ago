package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.lazy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine.toObjectRefCallFrame;
import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.Reason;

public class DeferenceNativeFrame extends NativeFrame implements DeferenceObject, ObjectRefOwner {

    private static final Logger logger = LoggerFactory.getLogger(DeferenceNativeFrame.class);

    private final RdbAdapter adapter;

    private final AtomicInteger referenceCounter = new AtomicInteger();
    private final ObjectRefCallFrame objectRefInstance;

    private boolean saveRequired = false;

    public boolean isEntrance = false;
    public boolean isAsyncEntrance = false;

    public DeferenceNativeFrame(LazyJsonRefSlots slots, AgoNativeFunction agoFunction, RdbEngine engine) {
        super(engine, slots, agoFunction);
        this.adapter = engine.getRdbAdapter();

        ObjectRefCallFrame inst = (ObjectRefCallFrame) adapter.restoreInstance(slots.getObjectRef());
        inst.setDeferencedInstance(this);
        this.objectRefInstance = inst;
    }

    @Override
    public ObjectRefObject toObjectRefInstance() {
        assert (this.objectRefInstance != null);
        return objectRefInstance;
    }

    @Override
    public boolean isSaveRequired() {
        return saveRequired;
    }

    @Override
    public void markSaved() {
        saveRequired = false;
    }

    @Override
    public String toString() {
        return "(DeferenceNativeFrame %s)".formatted(this.getObjectRef());
    }

    @Override
    public void setCaller(CallFrame<?> caller) {
        if (ObjectRefOwner.equals(caller, this.caller)) return;
        if (this.caller != null) {
            ReferenceCounter.releaseRef(this.caller, Reason.SetCallerDrop, this);
        }
        var c = toObjectRefCallFrame(caller);
        super.setCaller(c);
        ReferenceCounter.increaseRef(c, Reason.SetCallerInstall, this);
        saveRequired = true;
    }

    @Override
    public void setCreator(CallFrame<?> creator) {
        if (ObjectRefOwner.equals(creator, this.creator)) return;

        CallFrame<?> c = toObjectRefCallFrame(creator);

        super.setCreator(c);
        ReferenceCounter.increaseRef(c, Reason.SetCreatorInstall, this);
        saveRequired = true;
    }

    @Override
    public void setParentScope(Instance parentScope) {
        super.setParentScope(parentScope);
        ReferenceCounter.increaseRef(parentScope, Reason.SetParentInstall, this);
        saveRequired = true;
    }

    @Override
    public void setRunSpace(AgoRunSpace runSpace) {
        super.setRunSpace(runSpace);
        saveRequired = true;
    }

    public ObjectRef getObjectRef() {
        return ((LazyJsonRefSlots) this.slots).getObjectRef();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeferenceAgoFrame deferenceAgoFrame) {
            return this.getObjectRef().equals(deferenceAgoFrame.getObjectRef());
        } else if (obj instanceof ObjectRefObject objectRefObject) {
            return this.getObjectRef().equals(objectRefObject.getObjectRef());
        } else {
            return false;
        }
    }

    @Override
    public void setPayload(Object payload) {
        super.setPayload(payload);
        ((RdbEngine)this.engine).getRdbAdapter().saveInstance(this);
    }

    public void releaseSlotsDeference(Reason reason) {
        releaseSlotsDeference((LazyJsonRefSlots)this.slots, reason);
    }

    public void increaseSlotsDeference(Reason reason) {
        increaseSlotsDeference((LazyJsonRefSlots) this.slots, reason);
    }

}
