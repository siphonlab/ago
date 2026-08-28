package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.*;
import org.siphonlab.ago.runtime.db.sdk.ForkEntityRunSpace;
import org.siphonlab.ago.runtime.rdb.DbEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static org.siphonlab.ago.TypeCode.*;

/**
 * a runspace collecting changed, and flush when complete
 *
 */
public class EntityRunSpace<Id> extends RunSpace implements CreateInstanceRunSpace<Id>{

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityRunSpace.class);

    private final EntityAdapter<Id> entityAdapter;

    public EntityRunSpace(DbEngine<Id> dbEngine, EntityAdapter<Id> entityAdapter, RunSpaceHost runSpaceHost) {
        super(dbEngine, runSpaceHost);
        this.entityAdapter = entityAdapter.beginTransaction();      // each entity runspace start a transaction
    }

    public EntityAdapter<Id> getEntityAdapter() {
        return entityAdapter;
    }

    @Override
    public void run() {
        try {
            super.run();
        } catch (Exception ex) {
            LOGGER.error("%s failed: %s".formatted(this, ex.getMessage()), ex);
            if(this.currCallFrame != null){
                currCallFrame.raiseJavaException(currCallFrame, ex, false);
            } else {
                try {
                    this.entityAdapter.rollbackTransaction();
                } catch (Exception e) {
                    //
                }
                throw ex;
            }
        }
    }

    @Override
    protected boolean tryComplete() {
        var b = super.tryComplete();
        if(b){
            entityAdapter.flush(this);
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

    @Override
    public Instance<?> createArrayInstance(AgoClass arrayType, int length, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer) {
        Slots slots = arrayType.createSlots();
        if(slotsInitializer != null) slotsInitializer.accept(slots);

        AgoClass elementType = arrayType.getElementClassOfArray();
        int typeCodeValue = elementType.getTypeCode().value;

        switch (typeCodeValue) {
            case INT_VALUE: return new IntArrayInstance(slots, arrayType, length);
            case BYTE_VALUE: return new ByteArrayInstance(slots, arrayType, length);
            case BOOLEAN_VALUE: return new BooleanArrayInstance(slots, arrayType, length);
            case CHAR_VALUE: return new CharArrayInstance(slots, arrayType, length);
            case DOUBLE_VALUE: return new DoubleArrayInstance(slots, arrayType, length);
            case FLOAT_VALUE: return new FloatArrayInstance(slots, arrayType, length);
            case LONG_VALUE: return new LongArrayInstance(slots, arrayType, length);
            case OBJECT_VALUE: return new ObjectArrayInstance(slots, arrayType, length);
            case UNION_VALUE: return new UnionArrayInstance(slots, arrayType, length);
            case DECIMAL_VALUE: return new DecimalArrayInstance(slots, arrayType, length);
            case SHORT_VALUE: return new ShortArrayInstance(slots, arrayType, length);
            case STRING_VALUE: return new StringArrayInstance(slots, arrayType, length);
            case CLASS_REF_VALUE: return new IntArrayInstance(slots, arrayType, 0);
            default: throw new IllegalArgumentException("Unknown element type: " + elementType.getTypeCode());
        }
    }

    public static <Id> EntityAdapter<Id> retrieveEntityAdapter(RunSpace runSpace) {
        if(runSpace instanceof EntityRunSpace<?> entityRunSpace){
            return (EntityAdapter<Id>) entityRunSpace.getEntityAdapter();
        }
        if(runSpace instanceof EntityWorkflowRunSpace<?> entityWorkflowRunSpace){
            return (EntityAdapter<Id>) entityWorkflowRunSpace.getEntityAdapter();
        }
        return null;
    }

}
