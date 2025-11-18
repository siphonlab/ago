package org.siphonlab.ago.runtime.rdb.json.lazy

import groovy.transform.CompileStatic
import org.siphonlab.ago.AgoClass
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner
import org.siphonlab.ago.runtime.rdb.RdbAdapter
import org.siphonlab.ago.runtime.rdb.RdbEngine;
import org.siphonlab.ago.runtime.rdb.ReferenceCounter
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstance
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger

import static org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine.toObjectRefCallFrame;
import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.Reason;
@CompileStatic
public class DeferenceInstance extends Instance implements DeferenceObject, ObjectRefOwner {
    private static final Logger logger = LoggerFactory.getLogger(DeferenceAgoFrame)

    private final RdbAdapter adapter;

    private final AtomicInteger referenceCounter = new AtomicInteger();
    private final ObjectRefInstance objectRefInstance

    private boolean saveRequired;

    public DeferenceInstance(LazyJsonRefSlots slots, AgoClass agoClass, RdbEngine engine) {
        super(slots, agoClass);

        slots.setOwner(this);
        this.adapter = engine.getRdbAdapter();

        ObjectRefInstance inst = adapter.restoreInstance(objectRef) as ObjectRefInstance;
        inst.setDeferencedInstance(this)
        this.objectRefInstance = inst
    }

    @Override
    ObjectRef getObjectRef() {
        return ((LazyJsonRefSlots) this.slots).objectRef
    }

    @Override
    void setCreator(CallFrame creator) {
        if (!Objects.equals(creator, this.creator)) {
            CallFrame c = toObjectRefCallFrame(creator)
            super.setCreator(c)
        } else {
            super.setCreator((CallFrame) creator)
        }
        ReferenceCounter.increaseRef(this.creator, Reason.SetCreatorInstall, this)
        saveRequired = true;
    }

    @Override
    void setParentScope(Instance parentScope) {
        super.setParentScope(parentScope)
        ReferenceCounter.increaseRef(parentScope, Reason.SetParentInstall, this);
        saveRequired = true;
    }

    @Override
    String toString() {
        return "(DeferenceInstance %s)".formatted(this.objectRef)
    }

    @Override
    ObjectRefObject toObjectRefInstance() {
        assert (this.objectRefInstance != null);
        return objectRefInstance
    }

    boolean isSaveRequired() {
        return saveRequired;
    }

    void markSaved() {
        saveRequired = false
    }


    boolean equals(Object obj) {
        if (obj instanceof DeferenceInstance) {
            return this.getObjectRef().equals(obj.getObjectRef())
        } else if (obj instanceof ObjectRefObject) {
            return this.getObjectRef().equals(obj.getObjectRef())
        } else {
            return false
        }
    }

    public void releaseSlotsDeference(Reason reason) {
        releaseSlotsDeference((LazyJsonRefSlots) this.slots, reason);
    }

    public void increaseSlotsDeference(Reason reason) {
        increaseSlotsDeference((LazyJsonRefSlots) this.slots, reason);
    }

}
