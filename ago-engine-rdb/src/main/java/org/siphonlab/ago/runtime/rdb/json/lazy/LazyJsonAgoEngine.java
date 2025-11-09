package org.siphonlab.ago.runtime.rdb.json.lazy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.agrona.collections.Long2ObjectHashMap;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.runtime.json.InstanceJsonSerializer;
import org.siphonlab.ago.runtime.json.ResultSlotsDeserializer;
import org.siphonlab.ago.runtime.json.ResultSlotsSerializer;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbAgoRunSpace;
import org.siphonlab.ago.runtime.rdb.RunSpaceDesc;
import org.siphonlab.ago.runtime.rdb.json.InstanceJsonSerializerWithObjectId;
import org.siphonlab.ago.runtime.rdb.json.InstanceJsonDeserializerWithObjectId;
import org.siphonlab.ago.runtime.rdb.json.SlotsIndicator;
import org.siphonlab.ago.runtime.rdb.json.SlotsJsonSerializer;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstanceTrait;
import org.siphonlab.ago.runtime.rdb.lazy.ReferenceableInstance;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;

import java.util.List;
import java.util.function.Consumer;

/**
 * an AgoEngine create ObjectRefInstance, which allocate no slots in default,
 * and deference slots when requested
 */
public class LazyJsonAgoEngine extends PersistentRdbEngine {

    public LazyJsonAgoEngine(RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(rdbAdapter, runSpaceHost);
        rdbAdapter.setClassManager(this);
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator) {
        return createFunctionInstance(agoFunction,parentScope, caller, creator, null);
    }

    @Override
    protected ObjectMapper createDefaultObjectMapper() {
        var r = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        InstanceJsonSerializer jsonSerializer = new InstanceJsonSerializerWithObjectId(this);
        module.addSerializer(Instance.class, jsonSerializer);
        module.addSerializer(SlotsIndicator.class, new SlotsJsonSerializer(this));
        module.addSerializer(ResultSlots.class, new ResultSlotsSerializer());

        module.addDeserializer(Instance.class, new InstanceJsonDeserializerWithObjectId(this));
        module.addDeserializer(ResultSlots.class, new ResultSlotsDeserializer(this));

        //        module.addSerializer(ResultSetMapper.class, new ResultSetMapper.JsonSerializer(this.getJsonObjectMapper()));
        r.registerModule(module);
        return r;
    }

    public CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, CallFrame<?> caller, CallFrame<?> creator, Consumer<Slots> slotsInitializer) {
        LazyJsonRefSlots slots = (LazyJsonRefSlots) agoFunction.createSlots();
        if(slotsInitializer != null) slotsInitializer.accept(slots);
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
        inst.setCaller(toObjectRefCallFrame(caller));
        inst.setCreator(toObjectRefCallFrame(creator));
        saveInstance(inst);
        return inst;
    }

    public static CallFrame<?> toObjectRefCallFrame(CallFrame<?> callFrame){
        if (callFrame instanceof ReferenceableInstance) {
            ObjectRefInstanceTrait r = ((ReferenceableInstance) callFrame).toObjectRefInstance();
            return (CallFrame<?>) r;
        }
        return callFrame;
    }

    @Override
    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        return super.createNativeInstance(parentScope, agoClass, creator);
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        return createInstance(parentScope,agoClass, creator, null);
    }

    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator, Consumer<Slots> slotsInitializer) {
        var inst = super.createInstance(parentScope,agoClass, creator);
        if(inst.getSlots() instanceof LazyJsonRefSlots lazyJsonRefSlots && slotsInitializer != null){
            slotsInitializer.accept(lazyJsonRefSlots);
        }
        return inst;
    }


    @Override
    public Instance<?> createInstanceFromScopedClass(AgoClass scopedClass, CallFrame<?> creator, AgoRunSpace runSpace) {
        var inst = super.createInstanceFromScopedClass(scopedClass, creator, runSpace);
        return inst;
    }

    public void resume(){
        var resumeFrame = this.createFunctionInstance(null, (AgoFunction) this.getClass("@resume#"), null, null);

        LazyJsonPGAdapter adapter = (LazyJsonPGAdapter) this.getRdbAdapter();
        List<RunSpaceDesc> runSpaceDescs = adapter.loadResumableRunSpaces(resumeFrame);

        Long2ObjectHashMap<RdbAgoRunSpace> runspaces = new Long2ObjectHashMap<>();
        for (RunSpaceDesc runSpaceDesc : runSpaceDescs) {
            var r  = new RdbAgoRunSpace(this, adapter, this.runSpaceHost, runSpaceDesc.getId()); //TODO multiple runSpaceHost
            runspaces.put(runSpaceDesc.getId(),r);
        }
        this.runspaces.putAll(runspaces);

        resumeFrame.setRunSpace(this.getRunSpace());
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

//        var resumeFrame = this.createFunctionInstance(null, (AgoFunction) this.getClass("@resume#"), null, null, this.getRunSpace());
//        CallFrame<?>[] callFrames = this.getRdbAdapter().loadResumableCallFrames(resumeFrame);
//        for (CallFrame<?> callFrame : callFrames) {
//            callFrame.getRunSpace().spawn(callFrame);   //TODO resume in original runspace
//        }
    }
}
