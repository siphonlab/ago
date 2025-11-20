package org.siphonlab.ago.runtime.rdb.json.lazy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import groovy.sql.GroovyRowResult;
import org.agrona.collections.Long2ObjectHashMap;
import org.apache.commons.lang3.mutable.MutableObject;
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.json.*;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.json.*;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefResultsRdbRunSpace;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;

import java.util.*;
import java.util.function.Consumer;

import static org.siphonlab.ago.runtime.rdb.ObjectRefOwner.extractObjectRef;

/**
 * an AgoEngine create ObjectRefInstance, which allocate no slots in default,
 * and deference slots when requested
 */
public class LazyJsonAgoEngine extends PersistentRdbEngine {

    public LazyJsonAgoEngine(RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(rdbAdapter, runSpaceHost);
        rdbAdapter.setClassManager(this);
    }

    @Override
    protected void restoreClassStates(AgoClass agoClass, GroovyRowResult row) throws JsonProcessingException {
        PGobject slots = (PGobject) row.get("slots");
        restoreSlots((LazyJsonRefSlots) agoClass.getSlots(), (Long)row.get("id"), agoClass.getAgoClass(), slots.getValue(), null);

        Object parentScopeId = row.get("parent_scope_id");
        if(parentScopeId != null) {
            agoClass.setParentScope(this.getRdbAdapter().restoreInstance(new ObjectRef((String)row.get("parent_scope_class"), (Long)parentScopeId)));
        }
    }

    @Override
    public LazyJsonPGAdapter getRdbAdapter() {
        return (LazyJsonPGAdapter) super.getRdbAdapter();
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator) {
        var inst = createFunctionInstance(agoFunction,parentScope, caller, creator, null);
        if(inst instanceof DeferenceObject) {
            saveInstance(inst);
            return (CallFrame<?>) ((DeferenceObject)inst).toObjectRefInstance();
        } else {
            return inst;
        }
    }

    @Override
    protected ObjectMapper createDefaultObjectMapper() {
        var r = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        InstanceJsonSerializer jsonSerializer = new InstanceJsonSerializerWithObjectId(this);
        module.addSerializer(Instance.class, jsonSerializer);
        module.addSerializer(Slots.class, new SlotsJsonSerializer(this));
        module.addSerializer(RdbSlots.class, new RdbSlotsJsonSerializer(this));
        module.addSerializer(LazyJsonRefSlots.class, new RdbSlotsJsonSerializer(this));
        module.addSerializer(ResultSlots.class, new ResultSlotsSerializer());

        module.addDeserializer(Instance.class, new InstanceJsonDeserializerWithObjectId(this));
        module.addDeserializer(ResultSlots.class, new ResultSlotsDeserializer(this));

        //        module.addSerializer(ResultSetMapper.class, new ResultSetMapper.JsonSerializer(this.getJsonObjectMapper()));
        r.registerModule(module);
        return r;
    }

    @Override
    protected void createDumpingObjectMapper() {
        var r = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        InstanceJsonSerializer jsonSerializer = new InstanceJsonSerializerWithObjectId(this);
        module.addSerializer(Instance.class, jsonSerializer);
        module.addSerializer(ResultSlots.class, new ResultSlotsSerializer());
        module.addSerializer(Slots.class, new SlotsJsonSerializer(this));
        module.addSerializer(RdbSlots.class, new RdbSlotsJsonSerializer(this));
        module.addSerializer(LazyJsonRefSlots.class, new RdbSlotsJsonSerializer(this));

        module.addDeserializer(Instance.class, new InstanceJsonDeserializerWithObjectId(this));
        module.addDeserializer(ResultSlots.class, new ResultSlotsDeserializer(this));

        //        module.addSerializer(ResultSetMapper.class, new ResultSetMapper.JsonSerializer(this.getJsonObjectMapper()));
        r.registerModule(module);

        //new AgoJsonConfig(AgoJsonConfig.WriteTypeMode.Always, true, AgoJsonConfig.ObjectAsReferenceMode.Always, true)
        this.dumpingObjectMapper = r.copyWith(new AgoJsonParserFactory(AgoJsonConfig.RPC_OBJECT_REF));
    }

    public void restoreSlots(LazyJsonRefSlots jsonRefSlots, long id, AgoClass agoClass,
                             String json, MutableObject<Instance<?>> boxInstanceScope) throws JsonProcessingException {
        jsonRefSlots.setId(id);
        super.restoreSlots(jsonRefSlots, agoClass, json, boxInstanceScope);
    }

//    @Override
//    protected RdbAgoRunSpace createRunSpaceInner(RunSpaceHost runSpaceHost) {
//        return new ObjectRefResultsRdbRunSpace(this,getRdbAdapter(),this.runSpaceHost);
//    }

    public CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, CallFrame<?> caller, CallFrame<?> creator, Consumer<Slots> slotsInitializer) {
        LazyJsonRefSlots slots = (LazyJsonRefSlots) agoFunction.createSlots();
        if(slotsInitializer != null) slotsInitializer.accept(slots);    // may change slots rowstate -> none
        CallFrame<?> inst;
        if(agoFunction instanceof AgoNativeFunction agoNativeFunction) {
            inst = new DeferenceNativeFrame(slots, agoNativeFunction, this);
        } else {
            inst = new DeferenceAgoFrame(slots, agoFunction, this);
        }
        if (parentScope != null)
            inst.setParentScope(parentScope);  // not sure parentScope need restore to ObjectRefInstance too
        // restore DeferenceInstance to ObjectRefInstance
        // it cut off caller chain so that only running CallFrame living in the memory
        CallFrame<?> callerRef = toObjectRefCallFrame(caller);
        inst.setCaller(callerRef);

        if(inst instanceof DeferenceObject deferenceObject){
            if(Objects.equals(caller, creator)){
                deferenceObject.getDeferenceObjectState().setCreator(ObjectRefOwner.extractObjectRef(callerRef));
            } else {
                deferenceObject.getDeferenceObjectState().setCreator(ObjectRefOwner.extractObjectRef(creator));
            }
        }
        ((DeferenceObject) inst).markSaved();       // avoid instance marked as saveRequired
        return inst;
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        var inst = createInstance(parentScope,agoClass, creator, null);
        saveInstance(inst);
        return inst;
    }

    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator, Consumer<Slots> slotsInitializer) {
        if (agoClass instanceof AgoFunction fun) {
            return createFunctionInstance(fun, parentScope, creator, creator, slotsInitializer);
        }

        var slots = agoClass.createSlots();
        if(slotsInitializer != null) slotsInitializer.accept(slots);

        if(!(slots instanceof LazyJsonRefSlots)){   // box types use default slots
            return new Instance<>(slots, agoClass);
        }

        var inst = new DeferenceInstance((LazyJsonRefSlots) slots,agoClass,this);
        if (parentScope != null) inst.setParentScope(parentScope);
        inst.getDeferenceObjectState().setCreator(ObjectRefOwner.extractObjectRef(creator));

        inst.markSaved();

        return inst;
    }

    @Override
    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        return super.createNativeInstance(parentScope, agoClass, creator);
    }

    @Override
    public Instance<?> createInstanceFromScopedClass(AgoClass scopedClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createInstanceFromScopedClass(scopedClass, creator, runSpace);
        return inst;
    }

    public void resume(){

        LazyJsonPGAdapter adapter = (LazyJsonPGAdapter) this.getRdbAdapter();
        List<RunSpaceDesc> runSpaceDescs = adapter.loadResumableRunSpaces();

        Long2ObjectHashMap<RdbAgoRunSpace> runspaces = new Long2ObjectHashMap<>();
        for (RunSpaceDesc runSpaceDesc : runSpaceDescs) {
            var r  = new RdbAgoRunSpace(this, adapter, this.runSpaceHost, runSpaceDesc.getId()); //TODO multiple runSpaceHost
            runspaces.put(runSpaceDesc.getId(),r);
        }
        this.runspaces.putAll(runspaces);

        for (RunSpaceDesc runSpaceDesc : runSpaceDescs) {
            var r = runspaces.get(runSpaceDesc.getId());
            CallFrame<?> currCallFrame = (CallFrame<?>) adapter.restoreInstance(runSpaceDesc.getCurrFrame());
            if(currCallFrame instanceof ObjectRefCallFrame<?> objectRefCallFrame){
                currCallFrame = (CallFrame<?>) objectRefCallFrame.recomposeAsCallFrame();
            }
            List<AgoRunSpace> forkedRunspaces = runSpaceDesc.getForkedRunSpaces() == null ? null : runSpaceDesc.getForkedRunSpaces().stream().map(d -> (AgoRunSpace) runspaces.get(d.getId())).toList();
            AgoRunSpace parent = runSpaceDesc.getParentRunSpace() == null ? null : runspaces.get(runSpaceDesc.getParentRunSpace().getId());
            List<AgoRunSpace> pausingParents = runSpaceDesc.getPausingParents() == null ? null : runSpaceDesc.getPausingParents().stream().map(d -> (AgoRunSpace)runspaces.get(d.getId())).toList();
            byte runningState = runSpaceDesc.getRunningState();
            Instance<?> exception = adapter.restoreInstance(runSpaceDesc.getException());
            r.restore(runningState, currCallFrame, parent, forkedRunspaces, pausingParents, exception, runSpaceDesc.getResultSlots());
        }

        for (RdbAgoRunSpace runSpace : runspaces.values()) {
            if(runSpace.getRunningState() == AgoRunSpace.RunningState.RUNNING){
                runSpace.resumeByRestore();
            }
        }
    }

    public static CallFrame<?> toObjectRefCallFrame(CallFrame<?> callFrame) {
        if(callFrame instanceof EntranceCallFrame<?> entranceCallFrame){
            callFrame =entranceCallFrame.getInner();
        }
        if (callFrame instanceof DeferenceObject) {
            ObjectRefObject r = ((DeferenceObject) callFrame).toObjectRefInstance();
            var cf = (CallFrame<?>) r;
            cf.setRunSpace(callFrame.getRunSpace());
            return cf;
        }
        return callFrame;
    }

}
