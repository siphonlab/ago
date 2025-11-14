package org.siphonlab.ago.runtime.rdb.json.lazy

import groovy.transform.CompileStatic;
import org.siphonlab.ago.*
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbEngine
import org.siphonlab.ago.runtime.rdb.ReferenceCounter
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstanceTrait
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger

import static org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine.toObjectRefCallFrame;

@CompileStatic
public class DeferenceAgoFrame extends AgoFrame implements DeferenceObject, ObjectRefOwner, ReferenceCounter{

    private static final Logger logger = LoggerFactory.getLogger(DeferenceAgoFrame)

    private final RdbAdapter adapter;

    private final AtomicInteger referenceCounter = new AtomicInteger();
    private final ObjectRefCallFrame objectRefInstance

    public DeferenceAgoFrame(LazyJsonRefSlots slots, AgoFunction agoFunction, RdbEngine engine) {
        super(slots, agoFunction, engine);

        slots.setOwner(this);
        this.adapter = engine.getRdbAdapter();

        ObjectRefCallFrame inst = adapter.restoreInstance(objectRef) as ObjectRefCallFrame;
        inst.increaseRef(Reason.CreateDeferenceFrame);
        assert inst.getExistedDeferenced() == null
        inst.setDeferenced(this)
        this.objectRefInstance = inst;
    }

//    @Override
//    protected CallFrame<?> getCallFrameAt(int slot) {
//        var inst = slots.getObject(slot);
//        if(inst instanceof ObjectRefCallFrame){
//            return (CallFrame<?>) inst.doDeference();
//        }
//        return (CallFrame<?>) inst;
//    }

    @Override
    void setCaller(CallFrame<?> caller) {
        CallFrame c = toObjectRefCallFrame(caller);
        if (ObjectRefOwner.equals(caller, this.caller)) return;

        if(this.caller != null){
            ReferenceCounter.releaseRefOfCallFrame(this.caller, Reason.SetCallerDrop)
        }
        super.setCaller(c)
        ReferenceCounter.increaseRefOfCallFrame(c, Reason.SetCallerInstall);
    }

    @Override
    void setParentScope(Instance parentScope) {
        super.setParentScope(parentScope)
        if (parentScope instanceof ReferenceCounter) {
            parentScope.increaseRef(Reason.SetParentInstall);
        }
    }

    @Override
    void setCreator(CallFrame<?> creator) {
        if(ObjectRefOwner.equals(creator, this.creator)) return;

        CallFrame c = toObjectRefCallFrame(creator);

        super.setCreator(c)
        ReferenceCounter.increaseRefOfCallFrame(c, Reason.SetCreatorInstall);
    }

    ObjectRef getObjectRef(){
        return ((LazyJsonRefSlots) this.slots).objectRef
    }

    @Override
    ObjectRefInstanceTrait toObjectRefInstance() {
        assert this.objectRefInstance != null;
        return this.objectRefInstance;
//        def slots = (LazyJsonRefSlots) this.slots
//        if(logger.isDebugEnabled())
//            logger.debug("%s convert to objectref instance %s".formatted(this, slots.objectRef))
//
//        if(this.objectRefInstance != null) return this.objectRefInstance;
//
//        return  ((LazyJsonPGAdapter) adapter).restoreInstance(slots.getObjectRef(), slots.getRowState()) as ObjectRefCallFrame;
    }

    @Override
    boolean equals(Object obj) {
        if(obj instanceof DeferenceAgoFrame){
            return this.getObjectRef().equals(obj.getObjectRef())
        } else if(obj instanceof ObjectRefInstanceTrait){
            return this.getObjectRef().equals(obj.getObjectRef())
        } else {
            return false
        }
    }

    @Override
    String toString() {
        return "(DeferenceAgoFrame %s)".formatted(this.objectRef)
    }

    @Override
    int getRefCount() {
        return referenceCounter.get()
    }

    @Override
    void increaseRef(Reason reason) {
        var cnt= referenceCounter.incrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("$this inc ref got $cnt for $reason")
    }

    @Override
    int releaseRef(Reason reason) {
        var cnt = referenceCounter.decrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("$this release ref got $cnt for $reason")
        if (cnt == 0) {
            ReferenceCounter.releaseDeferenceSlotsAndContext(this);

            objectRefInstance.cleanDeferencedInstance();
            objectRefInstance.releaseRef(Reason.ReleaseRefForDeferenceInstanceFree);
        }
        return cnt;
    }

    void releaseSlotsDeference(Reason reason) {
        releaseSlotsDeference(this.slots as LazyJsonRefSlots, reason)
    }

    void increaseSlotsDeference(Reason reason) {
        increaseSlotsDeference(this.slots as LazyJsonRefSlots, reason)
    }

}
