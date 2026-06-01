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
package org.siphonlab.ago.runtime.rdb.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import groovy.sql.GroovyRowResult;
import org.agrona.collections.Long2ObjectHashMap;
import org.apache.mina.util.IdentityHashSet;
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.classloader.ClassRefValue;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.db.TaskRunSpace;
import org.siphonlab.ago.runtime.db.lazy.*;
import org.siphonlab.ago.runtime.json.*;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.json.InstanceJsonDeserializerWithObjectId;
import org.siphonlab.ago.runtime.rdb.json.InstanceJsonSerializerWithObjectId;
import org.siphonlab.ago.runtime.rdb.json.RdbSlotsJsonSerializer;
import org.siphonlab.ago.runtime.rdb.json.SlotsJsonSerializer;
import org.siphonlab.ago.runtime.db.lazy.DeferenceObject;
import org.siphonlab.ago.runtime.db.lazy.ObjectRefCallFrame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * full persistent engine.
 * all objects store in db, only function instances and runspace live in memory, and the `setObject` result (by load_xx, move_o_xx), stay in memory.
 * however in memory they are just a `(table-name, id)` key, instead of real data
 * and for `setInt`, `setDouble`, they are all stay in db, and all commands works as sql, i.e. `jump_if_i` transforms to `update function_inst set pc = x where cond = `
 */
public class PersistentDbEngine extends DbEngine {

    protected Long2ObjectHashMap<RunSpace> runspaces = new Long2ObjectHashMap<>();

    public PersistentDbEngine(DbAdapter dbAdapter, RunSpaceHost runSpaceHost) {
        super(dbAdapter, runSpaceHost);
    }

    public PersistentDbEngine(DbAdapter dbAdapter){
        super(dbAdapter);
    }

    @Override
    public void load(AgoClassLoader classLoader) {
        boolean loadFromDb = (classLoader instanceof JsonAgoClassLoader);
        if(!loadFromDb) {
            getRdbAdapter().saveStrings(classLoader.getStrings());
            getRdbAdapter().saveBlobs(classLoader.getBlobs());
        }

        classLoader.getTheMeta().setSlotsCreator(classLoader.getSlotsCreatorFactory().generateSlotsCreator(classLoader.getTheMeta()));
        for (AgoClass agoClass : classLoader.getClasses()) {
            if (agoClass.getAgoClass() != null) {
                MetaClass metaClass = agoClass.getAgoClass();
                if (metaClass.getSlotsCreator() == null) {
                    metaClass.setSlotsCreator(classLoader.getSlotsCreatorFactory().generateSlotsCreator(classLoader.getTheMeta()));
                }
            }
            agoClass.initSlots();
            // assert agoClass.getSlots() != null && !(agoClass.getSlots() instanceof AgoClass.TraceOwnerSlots);
            if (!loadFromDb) {
                saveInstance(agoClass);
            }
        }

        // here is parentScope, creator, slots of class
        super.load(classLoader);

        if(loadFromDb){
            JsonAgoClassLoader jsonAgoClassLoader = (JsonAgoClassLoader) classLoader;
            for (Map.Entry<String, GroovyRowResult> entry : jsonAgoClassLoader.getRowsByClassName().entrySet()) {
                var agoClass = this.getClass(entry.getKey());
                try {
                    restoreClassStates(agoClass, entry.getValue());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    protected ObjectMapper createDefaultObjectMapper() {
        var r = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        InstanceJsonSerializer jsonSerializer = new InstanceJsonSerializerWithObjectId(this);
        module.addSerializer(Instance.class, jsonSerializer);
        module.addSerializer(Slots.class, new SlotsJsonSerializer(this));
        module.addSerializer(DbSlots.class, new RdbSlotsJsonSerializer(this));
        module.addSerializer(ResultSlots.class, new ResultSlotsSerializer());
        module.addSerializer(ClassRefValue.class, new ClassRefValueSerializer());

        module.addDeserializer(Instance.class, new InstanceJsonDeserializerWithObjectId(this));
        module.addDeserializer(ResultSlots.class, new ResultSlotsDeserializer(this));

        //        module.addSerializer(ResultSetMapper.class, new ResultSetMapper.JsonSerializer(this.getJsonObjectMapper()));
        r.registerModule(module);
        return r;
    }

    @Override
    protected void restoreClassStates(AgoClass agoClass, GroovyRowResult row) throws JsonProcessingException {
        PGobject slots = (PGobject) row.get("slots");

        jsonDeserializeSlots((LazyJsonRefSlots) agoClass.getSlots(), (Long)row.get("id"), agoClass.getAgoClass(), slots.getValue(), null);

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
    protected RunSpace createRunSpace(RunSpaceHost runSpaceHost) {
        var r = super.createRunSpace(runSpaceHost);
        if(r instanceof SavableRunSpace rdbAgoRunSpace){
            this.runspaces.put(rdbAgoRunSpace.id, rdbAgoRunSpace);
        }
        return r;
    }

    public void releaseRunSpace(Object id) {
        this.runspaces.remove(id);
    }

    public RunSpace getRunSpace(long id) {
        return this.runspaces.get(id);
    }

    Set<RunSpace> getRunSpaces(){
        return new IdentityHashSet<>(this.runspaces.values());
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



    public CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, CallFrame<?> creator, Consumer<Slots> slotsInitializer) {
        var slots = (DbSlots<?>) agoFunction.createSlots();
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

        Long2ObjectHashMap<TaskRunSpace> runspaces = new Long2ObjectHashMap<>();
        for (RunSpaceDesc runSpaceDesc : runSpaceDescs) {
            var r  = new TaskRunSpace(this, adapter, this.runSpaceHost, runSpaceDesc.getId()); //TODO multiple runSpaceHost
            runspaces.put(runSpaceDesc.getId(),r);
        }
        this.runspaces.putAll(runspaces);

        for (RunSpaceDesc runSpaceDesc : runSpaceDescs) {
            var r = runspaces.get(runSpaceDesc.getId());
            CallFrame<?> currCallFrame = (CallFrame<?>) adapter.restoreInstance(runSpaceDesc.getCurrFrame());
            if(currCallFrame instanceof ObjectRefCallFrame<?> objectRefCallFrame){
                currCallFrame = objectRefCallFrame.deference();
            }
            List<RunSpace> forkedRunspaces = runSpaceDesc.getForkedRunSpaces() == null ? null : runSpaceDesc.getForkedRunSpaces().stream().map(d -> (RunSpace) runspaces.get(d.getId())).toList();
            RunSpace parent = runSpaceDesc.getParentRunSpace() == null ? null : runspaces.get(runSpaceDesc.getParentRunSpace().getId());
            List<RunSpace> pausingParents = runSpaceDesc.getPausingParents() == null ? null : runSpaceDesc.getPausingParents().stream().map(d -> (RunSpace)runspaces.get(d.getId())).toList();
            byte runningState = runSpaceDesc.getRunningState();
            Instance<?> exception = adapter.restoreInstance(runSpaceDesc.getException());
            r.restore(runningState, currCallFrame, parent, forkedRunspaces, pausingParents, exception, runSpaceDesc.getResultSlots());
        }

        for (TaskRunSpace runSpace : runspaces.values()) {
            if(runSpace.getRunningState() == RunSpace.RunningState.RUNNING || runSpace.getRunningState() == RunSpace.RunningState.PENDING){
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
        return callFrame;
    }

}
