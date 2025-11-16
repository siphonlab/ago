package org.siphonlab.ago.runtime.rdb.json.lazy

import groovy.transform.CompileStatic;
import org.siphonlab.ago.*
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbEngine
import org.siphonlab.ago.runtime.rdb.ReferenceCounter
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger

import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.increaseRef
import static org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine.toObjectRefCallFrame;
import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.Reason;

@CompileStatic
public class DeferenceAgoFrame extends AgoFrame implements DeferenceObject, ObjectRefOwner{

    private static final Logger logger = LoggerFactory.getLogger(DeferenceAgoFrame)

    private final RdbAdapter adapter;

    private final AtomicInteger referenceCounter = new AtomicInteger();
    private final ObjectRefCallFrame objectRefInstance

    private boolean saveRequired = false;

    // for objRefFrame.deference(), to deference to EntranceFrame
    public boolean isEntrance = false;
    public boolean isAsyncEntrance = false;

    public DeferenceAgoFrame(LazyJsonRefSlots slots, AgoFunction agoFunction, RdbEngine engine) {
        super(slots, agoFunction, engine);

        slots.setOwner(this);
        this.adapter = engine.getRdbAdapter();

        ObjectRefCallFrame inst = adapter.restoreInstance(objectRef) as ObjectRefCallFrame;
        inst.setDeferencedInstance(this)
        this.objectRefInstance = inst;
    }

    private void setSaveRequired(boolean value){
        this.saveRequired = value;
    }

    @Override
    void setRunSpace(AgoRunSpace runSpace) {
        super.setRunSpace(runSpace)
        this.setSaveRequired(true)
    }

    @Override
    void setCaller(CallFrame<?> caller) {
        CallFrame c = toObjectRefCallFrame(caller);
        if (ObjectRefOwner.equals(caller, this.caller)) return;

        if(this.caller != null){
            ReferenceCounter.releaseRef(this.caller, Reason.SetCallerDrop)
        }
        super.setCaller(c)
        ReferenceCounter.increaseRef(c, Reason.SetCallerInstall);
        this.setSaveRequired(true);
    }

    @Override
    void setParentScope(Instance parentScope) {
        super.setParentScope(parentScope)
        ReferenceCounter.increaseRef(parentScope, Reason.SetParentInstall);
        this.setSaveRequired(true);
    }

    boolean isSaveRequired(){
        return saveRequired;
    }

    void markSaved(){
        saveRequired = false
    }

    @Override
    void setCreator(CallFrame<?> creator) {
        if(ObjectRefOwner.equals(creator, this.creator)) return;

        CallFrame c = toObjectRefCallFrame(creator);

        super.setCreator(c)
        ReferenceCounter.increaseRef(c, Reason.SetCreatorInstall);
        saveRequired = true;
    }

    ObjectRef getObjectRef(){
        return ((LazyJsonRefSlots) this.slots).objectRef
    }

    @Override
    ObjectRefObject toObjectRefInstance() {
        assert this.objectRefInstance != null;
        return this.objectRefInstance;
    }

    @Override
    boolean equals(Object obj) {
        if(obj instanceof DeferenceAgoFrame){
            return this.getObjectRef().equals(obj.getObjectRef())
        } else if(obj instanceof ObjectRefObject){
            return this.getObjectRef().equals(obj.getObjectRef())
        } else {
            return false
        }
    }

    @Override
    String toString() {
        return "(DeferenceAgoFrame %s)".formatted(this.objectRef)
    }

    void releaseSlotsDeference(Reason reason) {
        releaseSlotsDeference(this.slots as LazyJsonRefSlots, reason)
    }

    void increaseSlotsDeference(Reason reason) {
        increaseSlotsDeference(this.slots as LazyJsonRefSlots, reason)
    }

}
