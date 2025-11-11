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
import org.siphonlab.ago.runtime.rdb.RowState;

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
class ObjectRefInstance extends Instance<AgoClass> implements ObjectRefInstanceTrait, ObjectRefOwner{

    ObjectRefInstance(AgoClass agoClass, ObjectRef objectRef, DereferenceAdapter dereferenceAdapter) {
        super(agoClass)
        init(agoClass, objectRef, dereferenceAdapter)
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
    public CallFrame<?> getCreator() {
        return deference().getCreator();
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

}

@CompileStatic
class ObjectRefCallFrame<F extends AgoFunction> extends CallFrame<F> implements ObjectRefInstanceTrait, ObjectRefOwner{

    ObjectRefCallFrame(F agoClass, ObjectRef objectRef, DereferenceAdapter dereferenceAdapter, RowState rowState) {
        super(agoClass.createSlots().with {if(it instanceof RdbSlots){
            it.rowState = rowState;
            it.setId(objectRef.id())
        }; it}, agoClass)
        init(agoClass, objectRef, dereferenceAdapter)
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
    public CallFrame<?> getCreator() {
        return deference().getCreator();
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
    AgoRunSpace getRunSpace() {
        var r =  super.getRunSpace()
        if(r != null) return r;
        return recomposeAsCallFrame().getRunSpace()
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
}