package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbEngine;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstance;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine.toObjectRefCallFrame;
import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.Reason;

public class DeferenceInstance extends Instance implements DeferenceObject, ObjectRefOwner {
    private static final Logger logger = LoggerFactory.getLogger(DeferenceAgoFrame.class);

    private final RdbAdapter adapter;

    private final DeferenceObjectState state;

    public DeferenceInstance(LazyJsonRefSlots slots, AgoClass agoClass, RdbEngine engine) {
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


    public boolean equals(Object obj) {
        if (obj instanceof DeferenceInstance deferenceInstance) {
            return this.getObjectRef().equals(deferenceInstance.getObjectRef());
        } else if (obj instanceof ObjectRefObject objectRefObject) {
            return this.getObjectRef().equals(objectRefObject.getObjectRef());
        } else {
            return false;
        }
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
