package org.siphonlab.ago.runtime.rdb.lazy

import groovy.transform.CompileStatic;
import org.siphonlab.ago.AgoClass
import org.siphonlab.ago.AgoFunction
import org.siphonlab.ago.AgoRunSpace;
import org.siphonlab.ago.CallFrame
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
public trait ObjectRefInstanceTrait {

    private final static Logger logger = LoggerFactory.getLogger(ObjectRefInstanceTrait)

    private ObjectRef objectRef;
    private DereferenceAdapter dereferenceAdapter;
    private Set<CallFrame> expanders = new HashSet<>()
    private Instance<?> deferencedInstance;

    public init(AgoClass agoClass, ObjectRef objectRef, DereferenceAdapter dereferenceAdapter) {
        this.objectRef = objectRef;
        this.dereferenceAdapter = dereferenceAdapter;
    }

    public ObjectRef getObjectRef() {
        return objectRef;
    }

    public Instance getDeferencedInstance(){
        return deferencedInstance
    }

    Instance<?> deference() {
        if(deferencedInstance != null) return deferencedInstance;
        if (logger.isDebugEnabled()) logger.debug("$objectRef expand deference")
        var r = dereferenceAdapter.dereference(objectRef);
        return deferencedInstance = r;
    }

    void foldBy(CallFrame<?> expander){
        expanders.remove(expander);
        if(logger.isDebugEnabled()) logger.debug("$expander quit, $objectRef has %s expanders".formatted(expanders.size()))
        tryFold()
    }

    void tryFold() {
        if (this.expanders.isEmpty()){
            if (logger.isDebugEnabled()) logger.debug("$objectRef fold")
            ReferenceCounter.releaseDeferenceSlotsAndContext(deferencedInstance);
            deferencedInstance = null
        }
    }

    abstract ExpandableObject expandFor(CallFrame expander, boolean alreadyDeferenced);

    Instance dereferenceForExpander(CallFrame expander){
        if (logger.isDebugEnabled()) logger.debug("$objectRef expand for $expander")
        expanders.add(expander);
        return deference()
    }

    void setDeferenceInstance(Instance inst) {
        this.deferencedInstance = inst;
    }
}

@CompileStatic
class ObjectRefInstance<T extends AgoClass> extends Instance<T> implements ObjectRefInstanceTrait, ObjectRefOwner, ReferenceCounter{
    private final static Logger logger = LoggerFactory.getLogger(ObjectRefInstance)

    private AtomicInteger referenceCounter = new AtomicInteger(0);
    DereferenceAdapter dereferenceAdapter

    ObjectRefInstance(T agoClass, ObjectRef objectRef, DereferenceAdapter dereferenceAdapter) {
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

    ExpandableInstance expandFor(CallFrame expander, boolean alreadyDeferenced){
        return new ExpandableInstance(this, expander, alreadyDeferenced);
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

    ExpandableCallFrame expandFor(CallFrame expander, boolean alreadyDeferenced) {
        return new ExpandableCallFrame(this, expander, alreadyDeferenced);
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
            dereferenceAdapter.release(this.getObjectRef())
        }
        return r;
    }

}