/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago.runtime.rdb.json.lazy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import groovy.sql.GroovyRowResult;
import org.agrona.collections.Long2ObjectHashMap;
import org.apache.commons.lang3.mutable.MutableObject;
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.runtime.json.*;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.json.*;
import org.siphonlab.ago.runtime.rdb.lazy.*;
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

        if (agoClass instanceof AgoEnum enumClass) {
            Map<String, Object> enumValues = new HashMap<>();
            for (AgoField field : enumClass.getAgoClass().getFields()) {
                if(field.getAgoClass() == enumClass){
                    var enumValue = agoClass.getSlots().getObject(field.getSlotIndex());
                    Slots enumValueSlots = enumValue.getSlots();
                    switch (enumClass.getBasePrimitiveType().value){
                        case TypeCode.INT_VALUE:
                            enumValues.put(enumValueSlots.getString(1), enumValueSlots.getInt(0));
                            break;
                        case TypeCode.BYTE_VALUE:
                            enumValues.put(enumValueSlots.getString(1), enumValueSlots.getByte(0));
                            break;
                        case TypeCode.SHORT_VALUE:
                            enumValues.put(enumValueSlots.getString(1), enumValueSlots.getShort(0));
                            break;
                        case TypeCode.LONG_VALUE:
                            enumValues.put(enumValueSlots.getString(1), enumValueSlots.getLong(0));
                            break;
                    }
                }
            }
        }
    }

    @Override
    public LazyJsonPGAdapter getRdbAdapter() {
        return (LazyJsonPGAdapter) super.getRdbAdapter();
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, CallFrame<?> caller, CallFrame<?> creator) {
        if(getBoxTypes().isBoxTypeOrWithin(agoFunction)){       // isWithinBoxType
            return super.createFunctionInstance(parentScope, agoFunction, caller, creator);
        }
        var inst = createFunctionInstance(agoFunction,parentScope, creator, null);
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

    public CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, CallFrame<?> creator, Consumer<Slots> slotsInitializer) {
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
        DeferenceObject deferenceObject = (DeferenceObject) inst;
        deferenceObject.getDeferenceObjectState().setCreator(ObjectRefOwner.extractObjectRef(creator));
        ((DeferenceObject) inst).markSaved();       // avoid instance marked as saveRequired
        return inst;
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        var inst = createInstance(parentScope,agoClass, creator, null);
        saveInstance(inst);
        return inst;
    }

    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator,
                                      Consumer<Slots> slotsInitializer) {
        if (agoClass instanceof AgoFunction fun) {
            return createFunctionInstance(fun, parentScope, creator, slotsInitializer);
        }

        var slots = agoClass.createSlots();
        if(slotsInitializer != null) slotsInitializer.accept(slots);

        if(!(slots instanceof LazyJsonRefSlots)){   // box types use default slots
            return new Instance<>(slots, agoClass);
        }

        Instance<?> inst;
        if(agoClass.isNative()){
            inst = new DeferenceNativeInstance((LazyJsonRefSlots) slots, agoClass, this);
        } else {
            inst = new DeferenceInstance((LazyJsonRefSlots) slots, agoClass, this);
        }
        if (parentScope != null) inst.setParentScope(parentScope);

        DeferenceObject deferenceObject = (DeferenceObject) inst;
        deferenceObject.getDeferenceObjectState().setCreator(ObjectRefOwner.extractObjectRef(creator));
        deferenceObject.markSaved();

        return inst;
    }

    @Override
    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        var inst = createInstance(parentScope, agoClass, creator, null);
        saveInstance(inst);
        return inst;
    }

    public void resume(){

        LazyJsonPGAdapter adapter = (LazyJsonPGAdapter) this.getRdbAdapter();
        List<RunSpaceDesc> runSpaceDescs = adapter.loadResumableRunSpaces();

        Long2ObjectHashMap<RdbRunSpace> runspaces = new Long2ObjectHashMap<>();
        for (RunSpaceDesc runSpaceDesc : runSpaceDescs) {
            var r  = new RdbRunSpace(this, adapter, this.runSpaceHost, runSpaceDesc.getId()); //TODO multiple runSpaceHost
            runspaces.put(runSpaceDesc.getId(),r);
        }
        this.runspaces.putAll(runspaces);

        for (RunSpaceDesc runSpaceDesc : runSpaceDescs) {
            var r = runspaces.get(runSpaceDesc.getId());
            CallFrame<?> currCallFrame = (CallFrame<?>) adapter.restoreInstance(runSpaceDesc.getCurrFrame());
            if(currCallFrame instanceof ObjectRefCallFrame<?> objectRefCallFrame){
                currCallFrame = (CallFrame<?>) objectRefCallFrame.recomposeAsCallFrame();
            }
            List<RunSpace> forkedRunspaces = runSpaceDesc.getForkedRunSpaces() == null ? null : runSpaceDesc.getForkedRunSpaces().stream().map(d -> (RunSpace) runspaces.get(d.getId())).toList();
            RunSpace parent = runSpaceDesc.getParentRunSpace() == null ? null : runspaces.get(runSpaceDesc.getParentRunSpace().getId());
            List<RunSpace> pausingParents = runSpaceDesc.getPausingParents() == null ? null : runSpaceDesc.getPausingParents().stream().map(d -> (RunSpace)runspaces.get(d.getId())).toList();
            byte runningState = runSpaceDesc.getRunningState();
            Instance<?> exception = adapter.restoreInstance(runSpaceDesc.getException());
            r.restore(runningState, currCallFrame, parent, forkedRunspaces, pausingParents, exception, runSpaceDesc.getResultSlots());
        }

        for (RdbRunSpace runSpace : runspaces.values()) {
            if(runSpace.getRunningState() == RunSpace.RunningState.RUNNING){
                runSpace.resumeByRestore();
            }
        }
    }

    public static CallFrame<?> toObjectRefCallFrame(CallFrame<?> callFrame) {
        if(callFrame instanceof EntranceCallFrame<?> entranceCallFrame){
            callFrame =entranceCallFrame.getInner();
        }
        if (callFrame instanceof DeferenceObject) {
            return (CallFrame<?>) ((DeferenceObject) callFrame).toObjectRefInstance();
        }
        if(callFrame instanceof ExpandableCallFrame<?> expandableCallFrame){
            return expandableCallFrame.getObjectRefInstance();
        }
        return callFrame;
    }

}
