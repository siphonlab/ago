package org.siphonlab.ago.runtime.rdb.lazy

import groovy.transform.CompileStatic;
import org.siphonlab.ago.AgoClass
import org.siphonlab.ago.AgoFunction
import org.siphonlab.ago.AgoRunSpace;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance
import org.siphonlab.ago.Slots
import org.siphonlab.ago.SourceLocation
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner
import org.siphonlab.ago.runtime.rdb.RdbSlots
import org.siphonlab.ago.runtime.rdb.ReferenceCounter
import org.siphonlab.ago.runtime.rdb.RowState
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger;

/**
 * lazy instance
 */
@CompileStatic
public trait ObjectRefInstanceTrait extends ReferenceInstanceTrait{

    private ObjectRef objectRef;
    private DereferenceAdapter dereferenceAdapter;

    public init(AgoClass agoClass, ObjectRef objectRef, DereferenceAdapter dereferenceAdapter) {
        this.objectRef = objectRef;
        this.dereferenceAdapter = dereferenceAdapter;
    }

    public ObjectRef getObjectRef() {
        return objectRef;
    }

    Instance<?> doDeference() {
        return dereferenceAdapter.dereference(objectRef);
    }

}

@CompileStatic
class ObjectRefInstance extends Instance<AgoClass> implements ObjectRefInstanceTrait, ObjectRefOwner, ReferenceCounter{
    private final static Logger logger = LoggerFactory.getLogger(ObjectRefInstance)

    private AtomicInteger referenceCounter = new AtomicInteger(0);
    DereferenceAdapter dereferenceAdapter

    ObjectRefInstance(AgoClass agoClass, ObjectRef objectRef, DereferenceAdapter dereferenceAdapter) {
        super(agoClass)
        init(agoClass, objectRef, dereferenceAdapter)
        this.dereferenceAdapter = dereferenceAdapter;
    }

    @Override
    public Slots getSlots() {
        return deference().getSlots();
    }

    @Override
    public Instance<?> getParentScope() {
        return deference().getParentScope();
    }

    @Override
    void setParentScope(Instance parentScope) {
        deference().setParentScope(parentScope)
    }

    @Override
    public CallFrame<?> getCreator() {
        return deference().getCreator();
    }

    @Override
    void setCreator(CallFrame<?> creator) {
        deference().setCreator(creator)
    }


    @Override
    Object invokeMethod(CallFrame<?> caller, AgoFunction method, Object... arguments) {
        return deference().invokeMethod(caller, method, arguments)
    }

    @Override
    Object invokeMethod(CallFrame<?> caller, AgoRunSpace runSpace, AgoFunction method, Object... arguments) {
        return deference().invokeMethod(caller, runSpace, method, arguments)
    }

    @Override
    int hashCode() {
        return objectRef.hashCode()
    }

    @Override
    String toString() {
        return "(ObjectRefInstance %s)".formatted(this.objectRef)
    }

    @Override
    int getRefCount() {
        return referenceCounter.get()
    }

    @Override
    void increaseRef(Reason reason) {
        var cnt = referenceCounter.incrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("$this inc ref got $cnt for $reason")
    }

    @Override
    int releaseRef(Reason reason) {
        var r = referenceCounter.decrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("$this release ref got $r for $reason")
        if (r == 0) {
            this.cleanDeferencedInstance();     // again confirm
            dereferenceAdapter.release(this.getObjectRef())
        }
        return r
    }
}

@CompileStatic
class ObjectRefCallFrame<F extends AgoFunction> extends CallFrame<F> implements ObjectRefInstanceTrait, ObjectRefOwner, ReferenceCounter{

    private final static Logger logger = LoggerFactory.getLogger(ObjectRefCallFrame);

    private AtomicInteger referenceCounter = new AtomicInteger(0);
    DereferenceAdapter dereferenceAdapter

    ObjectRefCallFrame(F agoClass, ObjectRef objectRef, DereferenceAdapter dereferenceAdapter, RowState rowState) {
        super(agoClass.createSlots().with {if(it instanceof RdbSlots){
            it.rowState = rowState;
            it.setId(objectRef.id())
        }; it}, agoClass)
        init(agoClass, objectRef, dereferenceAdapter)
        this.dereferenceAdapter = dereferenceAdapter;
    }

    @Override
    public Slots getSlots() {
        return deference().getSlots();
    }

    @Override
    public Instance<?> getParentScope() {
        return deference().getParentScope();
    }

    @Override
    void setParentScope(Instance parentScope) {
        deference().setParentScope(parentScope)
    }

    @Override
    public CallFrame<?> getCreator() {
        return deference().getCreator();
    }

    @Override
    void setCreator(CallFrame<?> creator) {
        recomposeAsCallFrame().setCreator(creator)
    }

    @Override
    void setCaller(CallFrame<?> caller) {
        recomposeAsCallFrame().setCaller(caller)
    }

    public CallFrame recomposeAsCallFrame() {
        return deference() as CallFrame;
//        var r = deference() as CallFrame;
//        r.setRunSpace(this.runSpace)
//        r.setCaller(this.caller)
//        r.setParentScope(this.parentScope)
//        r.setCreator(this.creator)
//        return r;
    }

    @Override
    SourceLocation resolveSourceLocation() {
        return recomposeAsCallFrame().resolveSourceLocation()
    }

    @Override
    boolean handleException(Instance<?> exception) {
        return recomposeAsCallFrame().handleException(exception)
    }

    @Override
    void setRunSpace(AgoRunSpace runSpace) {
        recomposeAsCallFrame().setRunSpace(runSpace);
    }

    @Override
    AgoRunSpace getRunSpace() {
        var r =  super.getRunSpace()
        if(r != null) return r;
        return recomposeAsCallFrame().getRunSpace()
    }

    @Override
    String toString() {
        return "(ObjectRefCallFrame %s)".formatted(this.objectRef)
    }

    @Override
    void run() {
        recomposeAsCallFrame().run()
    }

    @Override
    void run(CallFrame<?> self) {
        CallFrame r = recomposeAsCallFrame()
        if(self == this){
            r.run(r)
        } else {
            r.run(self)
        }
    }

    @Override
    CallFrame<?> getCaller() {
        return recomposeAsCallFrame().getCaller()
    }

    @Override
    Object invokeMethod(CallFrame<?> caller, AgoFunction method, Object... arguments) {
        return deference().invokeMethod(caller, method, arguments)
    }

    @Override
    Object invokeMethod(CallFrame<?> caller, AgoRunSpace runSpace, AgoFunction method, Object... arguments) {
        return deference().invokeMethod(caller, runSpace, method, arguments)
    }

    @Override
    int hashCode() {
        return objectRef.hashCode()
    }

    @Override
    int getRefCount() {
        return referenceCounter.get()
    }

    @Override
    void increaseRef(Reason reason) {
        var r = referenceCounter.incrementAndGet();
        if (logger.isDebugEnabled()) logger.debug("$this inc ref got $r for $reason")
    }

    @Override
    int releaseRef(Reason reason) {
        var r= referenceCounter.decrementAndGet()
        if (logger.isDebugEnabled()) logger.debug("$this release ref got $r for $reason")
        if(r == 0){
            this.cleanDeferencedInstance();     // again confirm
            dereferenceAdapter.release(this.getObjectRef())
        }
        return r;
    }
}