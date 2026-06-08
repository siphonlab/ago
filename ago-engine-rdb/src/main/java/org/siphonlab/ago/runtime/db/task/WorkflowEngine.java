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
import org.apache.mina.util.IdentityHashSet;
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.classloader.ClassRefValue;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.db.*;
import org.siphonlab.ago.runtime.db.lazy.*;
import org.siphonlab.ago.runtime.db.sdk.ForkEntityRunSpace;
import org.siphonlab.ago.runtime.db.sdk.ForkEntityWorkflowRunSpace;
import org.siphonlab.ago.runtime.db.sdk.ForkWorkflowRunSpace;
import org.siphonlab.ago.runtime.json.*;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.json.*;
import org.siphonlab.ago.runtime.db.lazy.DeferenceObject;
import org.siphonlab.ago.runtime.db.lazy.ObjectRefCallFrame;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


/**
 * full persistent engine.
 * all objects store in db, only function instances and runspace live in memory, and the `setObject` result (by load_xx, move_o_xx), stay in memory.
 * however in memory they are just a `(table-name, id)` key, instead of real data
 * and for `setInt`, `setDouble`, they are all stay in db, and all commands works as sql, i.e. `jump_if_i` transforms to `update function_inst set pc = x where cond = `
 */
public class WorkflowEngine<Id> extends DbEngine<Id> {

    protected final WorkflowAdapter<Id> workflowAdapter;
    private EntityAdapter<Id> entityAdapter;

    protected Map<Id, RunSpace> runspaces = new ConcurrentHashMap<>();

    public WorkflowEngine(WorkflowAdapter<Id> workflowAdapter, EntityAdapter<Id> entityAdapter, RunSpaceHost runSpaceHost, Id sampleId) {
        super(workflowAdapter, runSpaceHost, sampleId);
        this.workflowAdapter = workflowAdapter;
        this.entityAdapter = entityAdapter;
    }

    public WorkflowEngine(WorkflowAdapter<Id> dbAdapter, EntityAdapter<Id> entityAdapter, Id sampleId){
        super(dbAdapter, sampleId);
        this.workflowAdapter = dbAdapter;
        this.entityAdapter = entityAdapter;
    }

    protected RunSpace createDefaultRunSpace() {
        return new WorkflowRunSpace<Id>(this, workflowAdapter, runSpaceHost);
    }

    @Override
    public void load(AgoClassLoader classLoader) {
        this.langClasses = classLoader.getLangClasses();

        boolean loadFromDb = (classLoader instanceof JsonAgoClassLoader);
        if (!loadFromDb) {
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
            if(!loadFromDb) agoClass.initSlots();
        }

        // here is parentScope, creator, slots of class
        super.load(classLoader);

        if (!loadFromDb) {
            for (AgoClass agoClass : classLoader.getClasses()) {
                // assert agoClass.getSlots() != null && !(agoClass.getSlots() instanceof AgoClass.TraceOwnerSlots);
                workflowAdapter.saveInstance(agoClass);
            }
        }

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

        var dbSlots = DbSlotsCreator.create(agoClass.getAgoClass(), ObjectRef.create(agoClass.getAgoClass().getFullname(), (Id)row.get("id")));
        agoClass.initSlots(dbSlots);

        jsonDeserializeSlots(dbSlots, agoClass.getAgoClass(), slots.getValue(), null);

        Object parentScopeId = row.get("parent_scope_id");
        if(parentScopeId != null) {
            agoClass.setParentScope(createObjectRefInstance(ObjectRef.create((String)row.get("parent_scope_class"), (Id)parentScopeId), getDefaultRunSpace()));
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

//    public void jsonDeserializeSlots(DbSlots<Id> jsonRefSlots, Id id, AgoClass agoClass,
//                             String json, MutableObject<Instance<?>> boxInstanceScope) throws JsonProcessingException {
//        jsonRefSlots.setObjectRef(ObjectRef.create(jsonRefSlots.getObjectRef().className(), id));
//        super.jsonDeserializeSlots(jsonRefSlots, agoClass, json, boxInstanceScope);
//    }

    protected RunSpace createRunSpaceInner(RunSpaceHost host, ForkContext forkContext) {
        RunSpace r;
        if(forkContext == null){
            r = new RunSpace(this, host);
        } else if(forkContext instanceof ForkEntityRunSpace){
            r = new EntityRunSpace<Id>(this, entityAdapter, host);
        } else if(forkContext instanceof ForkWorkflowRunSpace){
            WorkflowRunSpace<Id> workflowRunSpace = new WorkflowRunSpace<Id>(this, workflowAdapter, host);
            this.runspaces.put(workflowRunSpace.getId(), workflowRunSpace);
            r = workflowRunSpace;
        } else if(forkContext instanceof ForkEntityWorkflowRunSpace) {
            EntityWorkflowRunSpace<Id> workflowRunSpace = new EntityWorkflowRunSpace<>(this, workflowAdapter, entityAdapter, host);
            this.runspaces.put(workflowRunSpace.getId(), workflowRunSpace);
            r = workflowRunSpace;
        } else {
            throw new IllegalArgumentException("unsupport fork context " + forkContext);
        }
        return r;
    }


    public void releaseRunSpace(Id id) {
        this.runspaces.remove(id);
    }

    public RunSpace getRunSpace(Id id) {
        return this.runspaces.get(id);
    }

    Set<RunSpace> getRunSpaces(){
        return new IdentityHashSet<>(this.runspaces.values());
    }

    @Override
    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass, RunSpace runSpace) {
        var inst = createInstance(parentScope,agoClass, runSpace, null, (Consumer<Slots>) null);
        this.saveCreatedInstance(inst, agoClass, runSpace);
        return inst;
    }

    public Instance<?> createInstance(Instance<?> parentScope, AgoClass agoClass,
                                      RunSpace runSpace, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer) {

        if (agoClass instanceof AgoFunction fun) {
            return createFunctionInstance(fun, parentScope,runSpace, objectRef, slotsInitializer);
        }
        if(runSpace instanceof CreateInstanceRunSpace<?>) {
            return ((CreateInstanceRunSpace<Id>) runSpace).createInstance(parentScope, agoClass, objectRef, slotsInitializer);
        } else {
            Instance<?> instance;
            var slots = DbSlotsCreator.create(agoClass, objectRef);
            if(slotsInitializer != null) slotsInitializer.accept(slots);
            if(agoClass.isNative()){
                instance = new NativeInstance(slots, agoClass);
            } else {
                instance = new Instance<>(slots, agoClass);
            }
            if(parentScope != null) instance.setParentScope(parentScope);
            return instance;
        }
    }

    public CallFrame<?> createFunctionInstance(Instance<?> parentScope, AgoFunction agoFunction, RunSpace runSpace) {
        if(getBoxTypes().isBoxTypeOrWithin(agoFunction)){       // isWithinBoxType
            return super.createFunctionInstance(parentScope, agoFunction, runSpace);
        }
        var inst = createFunctionInstance(agoFunction, parentScope, runSpace,null, null);
        if(inst instanceof DeferenceObject) {
//            workflowAdapter.saveInstance(inst);
            return (CallFrame<?>) ((DeferenceObject)inst).toObjectRefInstance();
        } else {
            return inst;
        }
    }

    public CallFrame<?> createFunctionInstance(AgoFunction agoFunction, Instance<?> parentScope, RunSpace runSpace, ObjectRef<Id> objectRef, Consumer<Slots> slotsInitializer) {
        if(runSpace instanceof CreateInstanceRunSpace createInstanceRunSpace){
            return createInstanceRunSpace.createFunctionInstance(agoFunction, parentScope, objectRef, slotsInitializer);
        }
        CallFrame<?> instance;
        var slots = DbSlotsCreator.create(agoFunction, objectRef);
        if(slotsInitializer != null) slotsInitializer.accept(slots);
        if(agoFunction instanceof AgoNativeFunction agoNativeFunction){
            instance = new NativeFrame(this, slots, agoNativeFunction);
        } else {
            instance = new AgoFrame(slots, agoFunction, this);
        }
        if(parentScope != null) instance.setParentScope(parentScope);
        return instance;
    }

    @Override
    public Instance<?> createNativeInstance(Instance<?> parentScope, AgoClass agoClass, RunSpace runSpace) {
        var inst = createInstance(parentScope, agoClass, runSpace, null, (Consumer<Slots>) null);
        this.saveCreatedInstance(inst, agoClass, runSpace);
        return inst;
    }

    private void saveCreatedInstance(Instance<?> inst, AgoClass agoClass, RunSpace runSpace) {
        if(runSpace instanceof  EntityRunSpace<?> entityRunSpace){
            var entityAdapter = entityRunSpace.getEntityAdapter();
            if(entityAdapter != null && entityAdapter.isEntityClass(agoClass) && inst.getSlots() instanceof DbSlots) {
                entityAdapter.saveInstance(inst);
            }
        } else if(runSpace instanceof EntityWorkflowRunSpace<?> entityWorkflowRunSpace){
            if(entityAdapter != null && entityAdapter.isEntityClass(agoClass) && inst.getSlots() instanceof DbSlots) {
                entityWorkflowRunSpace.getEntityAdapter().saveInstance(inst);
            } else {
                //workflowAdapter.saveInstance(inst);
            }
        } else {
            //workflowAdapter.saveInstance(inst);
        }
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
            var runSpace = runspaces.get(runSpaceDesc.getId());
            CallFrame<?> currCallFrame = (CallFrame<?>) createObjectRefInstance(runSpaceDesc.getCurrFrame(), runSpace);
            if(currCallFrame instanceof ObjectRefCallFrame objectRefCallFrame){
                currCallFrame = objectRefCallFrame.deference();
            }
            List<RunSpace> forkedRunspaces = runSpaceDesc.getForkedRunSpaces() == null ? null : runSpaceDesc.getForkedRunSpaces().stream().map(d -> (RunSpace) runspaces.get(d.getId())).toList();
            RunSpace parent = runSpaceDesc.getParentRunSpace() == null ? null : runspaces.get(runSpaceDesc.getParentRunSpace().getId());
            List<RunSpace> pausingParents = runSpaceDesc.getPausingParents() == null ? null : runSpaceDesc.getPausingParents().stream().map(d -> (RunSpace)runspaces.get(d.getId())).toList();
            byte runningState = runSpaceDesc.getRunningState();
            Instance<?> exception = createObjectRefInstance(runSpaceDesc.getException(), runSpace);
            runSpace.restore(runningState, currCallFrame, parent, forkedRunspaces, pausingParents, exception, runSpaceDesc.getResultSlots());
        }

        for (WorkflowRunSpace<Id> runSpace : runspaces.values()) {
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

    public Instance<?> createObjectRefInstance(ObjectRef<Id> objectRef, RunSpace runSpace) {
        AgoClass agoClass = getClass(objectRef.className());
        var dereferenceAdapter = entityAdapter != null && entityAdapter.isEntityClass(agoClass) ? entityAdapter : workflowAdapter;
        if (agoClass instanceof AgoFunction agoFunction) {
            return new ObjectRefCallFrame(agoFunction, objectRef, dereferenceAdapter, runSpace, RowState.Unchanged);
        } else if(agoClass instanceof AgoClass){
            return new ObjectRefInstance(agoClass, objectRef, dereferenceAdapter, runSpace);
        } else {
            if (Objects.equals(objectRef.className(), "<Meta>")){
                return getTheMeta();
            }
            throw new IllegalArgumentException("unknown class " + objectRef.className());
        }
    }
}
