package org.siphonlab.ago.runtime.rdb.reactive.semischema;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;

public class SemiSchemaEngine extends PersistentRdbEngine {

    public SemiSchemaEngine(SemiSchemaPGAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(rdbAdapter, runSpaceHost);
    }

    public SemiSchemaEngine(SemiSchemaPGAdapter rdbAdapter) {
        super(rdbAdapter);
    }

    @Override
    protected AgoRunSpace createRunSpace(RunSpaceHost runSpaceHost) {
        return new AgoRunSpace(this, runSpaceHost);
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = new SemiSchemaCallFrame(agoFunction.createSlots(), agoFunction,this);
        if(parentScope != null) inst.setParentScope(parentScope);
        inst.setCaller(caller);
        inst.setCreator(creator);
        saveInstance(inst);
        return inst;
    }

    @Override
    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        return super.createNativeInstance(parentScope, agoClass, creator);
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createInstance(parentScope, agoClass, creator, runSpace);
        return inst;
    }

    @Override
    public Instance<?> createInstanceFromScopedClass(AgoClass scopedClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createInstanceFromScopedClass(scopedClass, creator, runSpace);
        return inst;
    }
}
