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

public class DeferenceNativeFrame extends NativeFrame implements DeferenceObject, ObjectRefOwner, ReferenceCounter {

    private static final Logger logger = LoggerFactory.getLogger(DeferenceNativeFrame.class);

    private final RdbAdapter adapter;

    private final AtomicInteger referenceCounter = new AtomicInteger();
    private final ObjectRefCallFrame objectRefInstance;

    public DeferenceNativeFrame(LazyJsonRefSlots slots, AgoNativeFunction agoFunction, RdbEngine engine) {
        super(engine, slots, agoFunction);
        this.adapter = engine.getRdbAdapter();

        ObjectRefCallFrame inst = (ObjectRefCallFrame) adapter.restoreInstance(slots.getObjectRef());
        inst.increaseRef(Reason.CreateDeferenceFrame);
        assert ((ReferenceInstanceTrait)inst).getExistedDeferenced() == null;
        inst.setDeferenced(this);
        this.objectRefInstance = inst;
    }

    @Override
    public ObjectRefInstanceTrait toObjectRefInstance() {
        assert (this.objectRefInstance != null);
        return objectRefInstance;
//        LazyJsonRefSlots slots = (LazyJsonRefSlots) this.slots;
//        if (logger.isDebugEnabled()) logger.debug("%s convert to objectref instance %s".formatted(this, slots.getObjectRef()));
//        return (ObjectRefInstanceTrait) ((LazyJsonPGAdapter)adapter).restoreInstance(slots.getObjectRef(), slots.getRowState());
    }

    @Override
    public String toString() {
        return "(DeferenceNativeFrame %s)".formatted(this.getObjectRef());
    }

    @Override
    public void setCaller(CallFrame<?> caller) {
        if (ObjectRefOwner.equals(caller, this.caller)) return;
        if (this.caller != null) {
            ReferenceCounter.releaseRefOfCallFrame(this.caller, Reason.SetCallerDrop);
        }
        var c = toObjectRefCallFrame(caller);
        super.setCaller(c);
        ReferenceCounter.increaseRefOfCallFrame(c, Reason.SetCallerInstall);
    }

    @Override
    public void setCreator(CallFrame<?> creator) {
        if (ObjectRefOwner.equals(creator, this.creator)) return;

        CallFrame<?> c = toObjectRefCallFrame(creator);

        super.setCreator(c);
        ReferenceCounter.increaseRefOfCallFrame(c, Reason.SetCreatorInstall);
    }

    @Override
    public void setParentScope(Instance parentScope) {
        super.setParentScope(parentScope);
        if (parentScope instanceof ReferenceCounter rc) {
            rc.increaseRef(Reason.SetParentInstall);
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

    @Override
    public int getRefCount() {
        return referenceCounter.get();
    }

    @Override
    public void increaseRef(Reason reason) {
        var cnt = referenceCounter.incrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("%s inc ref got %d for %s".formatted(this, cnt, reason));
    }

    @Override
    public int releaseRef(Reason reason) {
        var cnt = referenceCounter.decrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("%s release ref got %d for %s".formatted(this, cnt, reason));
        if(cnt == 0){
            ReferenceCounter.releaseDeferenceSlotsAndContext(this);

            objectRefInstance.cleanDeferencedInstance();
            objectRefInstance.releaseRef(Reason.ReleaseRefForDeferenceInstanceFree);
        }
        return cnt;
    }

    public void releaseSlotsDeference(Reason reason) {
        releaseSlotsDeference((LazyJsonRefSlots)this.slots, reason);
    }

    public void increaseSlotsDeference(Reason reason) {
        increaseSlotsDeference((LazyJsonRefSlots) this.slots, reason);
    }

}
