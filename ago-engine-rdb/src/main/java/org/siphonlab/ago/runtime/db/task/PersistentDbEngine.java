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
package org.siphonlab.ago.runtime.db.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import groovy.sql.GroovyRowResult;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.mina.util.IdentityHashSet;
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.classloader.ClassRefValue;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.db.WorkflowRunSpace;
import org.siphonlab.ago.runtime.db.WorkflowAdapter;
import org.siphonlab.ago.runtime.db.lazy.*;
import org.siphonlab.ago.runtime.json.*;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.json.*;
import org.siphonlab.ago.runtime.db.lazy.DeferenceObject;
import org.siphonlab.ago.runtime.db.lazy.ObjectRefCallFrame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


/**
 * full persistent engine.
 * all objects store in db, only function instances and runspace live in memory, and the `setObject` result (by load_xx, move_o_xx), stay in memory.
 * however in memory they are just a `(table-name, id)` key, instead of real data
 * and for `setInt`, `setDouble`, they are all stay in db, and all commands works as sql, i.e. `jump_if_i` transforms to `update function_inst set pc = x where cond = `
 */
public abstract class PersistentDbEngine<Id> extends DbEngine<Id> {

    protected final WorkflowAdapter<Id> workflowAdapter;

    protected Map<Id, RunSpace> runspaces = new ConcurrentHashMap<>();

    public PersistentDbEngine(WorkflowAdapter<Id> dbAdapter, RunSpaceHost runSpaceHost, Id sampleId) {
        super(dbAdapter, runSpaceHost, sampleId);
        this.workflowAdapter = dbAdapter;
    }

    public PersistentDbEngine(WorkflowAdapter<Id> dbAdapter, Id sampleId){
        super(dbAdapter, sampleId);
        this.workflowAdapter = dbAdapter;
    }

    @Override
    public void load(AgoClassLoader classLoader) {
        boolean loadFromDb = (classLoader instanceof JsonAgoClassLoader);
        if(!loadFromDb) {
            workflowAdapter.saveStrings(classLoader.getStrings());
            workflowAdapter.saveBlobs(classLoader.getBlobs());
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
                getDbAdapter().saveInstance(agoClass);
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

        module.addDeserializer(Instance.class, new InstanceJsonDeserializerWithObjectId<>(this, sampleId));
        module.addDeserializer(ResultSlots.class, new ResultSlotsDeserializer(this));

        //        module.addSerializer(ResultSetMapper.class, new ResultSetMapper.JsonSerializer(this.getJsonObjectMapper()));
        r.registerModule(module);
        return r;
    }

    protected void restoreClassStates(AgoClass agoClass, GroovyRowResult row) throws JsonProcessingException {
        PGobject slots = (PGobject) row.get("slots");

        jsonDeserializeSlots((DbSlots<Id>) agoClass.getSlots(), (Id)row.get("id"), agoClass.getAgoClass(), slots.getValue(), null);

        Object parentScopeId = row.get("parent_scope_id");
        if(parentScopeId != null) {
            agoClass.setParentScope(workflowAdapter.getById(ObjectRef.create((String)row.get("parent_scope_class"), (Id)parentScopeId)));
        }

        if (agoClass instanceof AgoEnum enumClass) {
            Map<String, Object> enumValues = new HashMap<>();
            for (AgoField field : enumClass.getAgoClass().getFields()) {
                if(field.getAgoClass() == enumClass){
                    var enumValue = createInstance(enumClass,null);
                    Slots enumValueSlots = enumValue.getSlots();
                    enumValueSlots.setString( 1, field.getName());
                    switch (enumClass.getBasePrimitiveType().value) {
                        case TypeCode.INT_VALUE:
                            enumValueSlots.setInt(0, (Integer) field.getConstLiteralValue());
                            enumValues.put(enumValueSlots.getString(1),enumValueSlots.getInt(0));
                            break;
                        case TypeCode.BYTE_VALUE:
                            enumValueSlots.setByte(0, (Byte) field.getConstLiteralValue());
                            enumValues.put(enumValueSlots.getString(1),enumValueSlots.getByte(0));
                            break;
                        case TypeCode.SHORT_VALUE:
                            enumValueSlots.setShort(0, (Short) field.getConstLiteralValue());
                            enumValues.put(enumValueSlots.getString(1),enumValueSlots.getShort(0));
                            break;
                        case TypeCode.LONG_VALUE:
                            enumValueSlots.setLong(0, (Long) field.getConstLiteralValue());
                            enumValues.put(enumValueSlots.getString(1),enumValueSlots.getLong(0));
                            break;
                    }
                }
            }
            enumClass.setEnumValues(enumValues);
        }
    }

    public void jsonDeserializeSlots(DbSlots<Id> jsonRefSlots, Id id, AgoClass agoClass,
                             String json, MutableObject<Instance<?>> boxInstanceScope) throws JsonProcessingException {
        jsonRefSlots.setObjectRef(ObjectRef.create(jsonRefSlots.getObjectRef().className(), id));
        super.jsonDeserializeSlots(jsonRefSlots, agoClass, json, boxInstanceScope);
    }


    @Override
    protected RunSpace createRunSpace(RunSpaceHost runSpaceHost) {
        var r = super.createRunSpace(runSpaceHost);
        if(r instanceof WorkflowRunSpace workflowRunSpace){
            this.runspaces.put((Id)workflowRunSpace.getId(), workflowRunSpace);
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
        var inst = createFunctionInstance(agoFunction,parentScope, null);
        if(inst instanceof DeferenceObject) {
            getDbAdapter().saveInstance(inst);
            return (CallFrame<?>) ((DeferenceObject)inst).toObjectRefInstance();
        } else {
            return inst;
        }
    }

    public CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, Consumer<Slots> slotsInitializer) {
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
        ((DeferenceObject) inst).markSaved();       // avoid instance marked as saveRequired
        return inst;
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        var inst = createInstance(parentScope,agoClass, (Consumer<Slots>) null);
        getDbAdapter().saveInstance(inst);
        return inst;
    }

    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass,
                                      Consumer<Slots> slotsInitializer) {
        if (agoClass instanceof AgoFunction fun) {
            return createFunctionInstance(fun, parentScope, slotsInitializer);
        }

        var slots = agoClass.createSlots();
        if(slotsInitializer != null) slotsInitializer.accept(slots);

        if(!(slots instanceof DbSlots<?>)){   // box types use default slots
            return new Instance<>(slots, agoClass);
        }

        Instance<?> inst;
        if(agoClass.isNative()){
            inst = new DeferenceNativeInstance((DbSlots) slots, agoClass, this);
        } else {
            inst = new DeferenceInstance((DbSlots) slots, agoClass, this);
        }
        if (parentScope != null) inst.setParentScope(parentScope);

        DeferenceObject deferenceObject = (DeferenceObject) inst;
        deferenceObject.markSaved();

        return inst;
    }

    @Override
    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, CallFrame<?> creator) {
        var inst = createInstance(parentScope, agoClass, (Consumer<Slots>) null);
        getDbAdapter().saveInstance(inst);
        return inst;
    }

    public void resume(){

        List<RunSpaceDesc<Id>> runSpaceDescs = workflowAdapter.loadResumableRunSpaces();

        Map<Id, WorkflowRunSpace<Id>> runspaces = new HashMap<>();
        for (RunSpaceDesc<Id> runSpaceDesc : runSpaceDescs) {
            var r  = new WorkflowRunSpace<>(this, workflowAdapter, this.runSpaceHost, runSpaceDesc.getId()); //TODO multiple runSpaceHost
            runspaces.put(runSpaceDesc.getId(),r);
        }
        this.runspaces.putAll(runspaces);

        for (RunSpaceDesc<Id> runSpaceDesc : runSpaceDescs) {
            var r = runspaces.get(runSpaceDesc.getId());
            CallFrame<?> currCallFrame = (CallFrame<?>) workflowAdapter.getById(runSpaceDesc.getCurrFrame());
            if(currCallFrame instanceof ObjectRefCallFrame objectRefCallFrame){
                currCallFrame = objectRefCallFrame.deference();
            }
            List<RunSpace> forkedRunspaces = runSpaceDesc.getForkedRunSpaces() == null ? null : runSpaceDesc.getForkedRunSpaces().stream().map(d -> (RunSpace) runspaces.get(d.getId())).toList();
            RunSpace parent = runSpaceDesc.getParentRunSpace() == null ? null : runspaces.get(runSpaceDesc.getParentRunSpace().getId());
            List<RunSpace> pausingParents = runSpaceDesc.getPausingParents() == null ? null : runSpaceDesc.getPausingParents().stream().map(d -> (RunSpace)runspaces.get(d.getId())).toList();
            byte runningState = runSpaceDesc.getRunningState();
            Instance<?> exception = workflowAdapter.getById(runSpaceDesc.getException());
            r.restore(runningState, currCallFrame, parent, forkedRunspaces, pausingParents, exception, runSpaceDesc.getResultSlots());
        }

        for (WorkflowRunSpace runSpace : runspaces.values()) {
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
