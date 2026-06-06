package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.db.lazy.DeferenceAgoFrame;
import org.siphonlab.ago.runtime.db.lazy.DeferenceNativeFrame;
import org.siphonlab.ago.runtime.db.sdk.ForkEntityRunSpace;
import org.siphonlab.ago.runtime.rdb.DbEngine;

import java.util.function.Consumer;

/**
 * a runspace collecting changed, and flush when complete
 *
 */
public class EntityRunSpace<Id> extends RunSpace implements CreateInstanceRunSpace<Id>{

    private final EntityAdapter<Id> entityAdapter;

    public EntityRunSpace(DbEngine<Id> dbEngine, EntityAdapter<Id> entityAdapter, RunSpaceHost runSpaceHost) {
        super(dbEngine, runSpaceHost);
        this.entityAdapter = entityAdapter.beginTransaction();      // each entity runspace start a transaction
    }

    public EntityAdapter<Id> getEntityAdapter() {
        return entityAdapter;
    }

    @Override
    protected boolean tryComplete() {
        var b = super.tryComplete();
        if(b){
            entityAdapter.flush();
        }
        return b;
    }

    @Override
    public RunSpace createChildRunSpace(ForkContext forkContext) {
        return super.createChildRunSpace(forkContext == null ? new ForkEntityRunSpace() : forkContext);
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer) {
        if (agoClass instanceof AgoFunction fun) {
            return createFunctionInstance(fun, parentScope, objectRef, slotsInitializer);
        }
        Slots slots;
        if(entityAdapter.isEntityClass(agoClass)) {
            slots = DbSlotsCreator.create(agoClass, objectRef);
            if(slotsInitializer != null) slotsInitializer.accept(slots);
        } else {
            slots = agoClass.createSlots();
        }
        Instance<?> instance;
        if(agoClass.isNative()) {
            instance = new NativeInstance(slots, agoClass);
        } else {
            instance = new Instance<>(slots, agoClass);
        }
        if(parentScope != null) instance.setParentScope(parentScope);
        return instance;
    }

    @Override
    public CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer) {
        Slots slots;
        if(entityAdapter.isEntityClass(agoFunction)) {
            slots = DbSlotsCreator.create(agoFunction, objectRef);
            if(slotsInitializer != null) slotsInitializer.accept(slots);
        } else {
            slots = agoFunction.createSlots();
        }
        CallFrame<?> inst;
        if(agoFunction instanceof AgoNativeFunction agoNativeFunction) {
            inst = new NativeFrame(getAgoEngine(), slots, agoNativeFunction);
        } else {
            inst = new AgoFrame(slots, agoFunction, this.getAgoEngine());
        }
        if(parentScope != null) inst.setParentScope(parentScope);
        return inst;
    }
}
