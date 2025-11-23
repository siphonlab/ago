package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject;
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableObject;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstance;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.Reason;

public class DeferenceNativeInstance extends NativeInstance implements DeferenceObject, ObjectRefOwner {
    private static final Logger logger = LoggerFactory.getLogger(DeferenceNativeInstance.class);

    private final RdbAdapter adapter;

    private final DeferenceObjectState state;

    public DeferenceNativeInstance(LazyJsonRefSlots slots, AgoClass agoClass, RdbEngine engine) {
        super(slots, agoClass);

        slots.setOwner(this);
        this.adapter = engine.getRdbAdapter();

        ObjectRefInstance inst = (ObjectRefInstance) adapter.restoreInstance(getObjectRef());
        inst.setDeferencedInstance(this);
        this.state = new DeferenceObjectState(inst);
    }

    @Override
    public ObjectRef getObjectRef() {
        return ((LazyJsonRefSlots) this.slots).getObjectRef();
    }

    @Override
    public void setParentScope(Instance parentScope) {
        super.setParentScope(parentScope);
        ReferenceCounter.increaseRef(parentScope, Reason.SetParentInstall, this);
        state.setSaveRequired();
    }

    @Override
    public String toString() {
        return "(DeferenceInstance %s)".formatted(this.getObjectRef());
    }

    @Override
    public ObjectRefObject toObjectRefInstance() {
        return state.getObjectRefInstance();
    }

    public boolean isSaveRequired() {
        return state.isSaveRequired();
    }

    public void markSaved() {
        state.markSaved();
    }

    @Override
    public void setNativePayload(Object nativePayload) {
        super.setNativePayload(nativePayload);
        state.setSaveRequired();
    }

    public boolean equals(Object obj) {
        return ObjectRefOwner.equals(this, (Instance<?>) obj);
    }

    public void releaseSlotsDeference(Reason reason) {
        releaseSlotsDeference((LazyJsonRefSlots) this.slots, reason);
    }

    public void increaseSlotsDeference(Reason reason) {
        increaseSlotsDeference((LazyJsonRefSlots) this.slots, reason);
    }

    @Override
    public DeferenceObjectState getDeferenceObjectState() {
        return state;
    }

}
