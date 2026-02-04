package org.siphonlab.ago.runtime.rdb.reactive.json;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;

public class ReactiveJsonAgoEngine extends PersistentRdbEngine {

    public ReactiveJsonAgoEngine(ReactiveJsonPGAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(rdbAdapter, runSpaceHost);
    }

    public ReactiveJsonAgoEngine(ReactiveJsonPGAdapter rdbAdapter) {
        super(rdbAdapter);
    }

    @Override
    protected AgoRunSpace createRunSpace(RunSpaceHost runSpaceHost) {
        return new AgoRunSpace(this, runSpaceHost);
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator) {
        var inst = new ReactiveJsonCallFrame(agoFunction.createSlots(), agoFunction,this);
        if(parentScope != null) inst.setParentScope(parentScope);
        saveInstance(inst);
        return inst;
    }

    @Override
    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        return super.createNativeInstance(parentScope, agoClass, creator);
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        var inst = super.createInstance(parentScope, agoClass, creator);
        return inst;
    }

    @Override
    public Instance<?> createInstanceFromScopedClass(AgoClass scopedClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createInstanceFromScopedClass(scopedClass, creator, runSpace);
        return inst;
    }
}
