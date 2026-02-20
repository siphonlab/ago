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
package org.siphonlab.ago.runtime.rdb.json.lazy

import groovy.json.JsonSlurper;
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.agrona.concurrent.IdGenerator
import org.apache.commons.lang3.mutable.MutableObject
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*
import org.siphonlab.ago.native_.NativeFrame
import org.siphonlab.ago.native_.NativeInstance
import org.siphonlab.ago.runtime.AgoArrayInstance
import org.siphonlab.ago.runtime.rdb.CallFrameWithRunningState;
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner
import org.siphonlab.ago.runtime.rdb.RdbRunSpace
import org.siphonlab.ago.runtime.rdb.RdbSlots
import org.siphonlab.ago.runtime.rdb.ReferenceCounter
import org.siphonlab.ago.runtime.rdb.RowState
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject
import org.siphonlab.ago.runtime.rdb.lazy.DereferenceAdapter
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableObject
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstance
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject
import org.siphonlab.ago.runtime.rdb.lazy.RdbRefSlots
import org.siphonlab.ago.runtime.rdb.json.JsonPGAdapter
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

@CompileStatic
public class LazyJsonPGAdapter extends JsonPGAdapter implements DereferenceAdapter{

    private final static Logger logger = LoggerFactory.getLogger(LazyJsonPGAdapter)

    private Map<ObjectRef, Instance> objectReferenceInstancesPool = new ConcurrentHashMap<>();

    public LazyJsonPGAdapter(BoxTypes boxTypes, ClassManager classManager, int applicationId, IdGenerator idGenerator) {
        super(boxTypes, classManager, applicationId, idGenerator);
    }

    @Override
    public Instance restoreInstance(Connection connection, ObjectRef objectRef) {
        return restoreInstance(objectRef)
    }

    Instance restoreInstance(ObjectRef objectRef) {
        return restoreInstance(objectRef, RowState.Unchanged)
    }

    Instance restoreInstance(ObjectRef objectRef, RowState rowState){
        if(objectRef == null) return null;

        return objectReferenceInstancesPool.computeIfAbsent(objectRef, {
            AgoClass agoClass = classManager.getClass(objectRef.className())
            ObjectRefObject r;
            if (agoClass instanceof AgoFunction) {
                r = new ObjectRefCallFrame(agoClass, objectRef, this, rowState)
            } else if(agoClass instanceof AgoClass){
                r = new ObjectRefInstance(agoClass, objectRef, this)
            } else {
                if (r == null && objectRef.className() == "<Meta>"){
                    return classManager.getTheMeta();
                }
                throw new IllegalArgumentException("unknown class " + objectRef.className());
            }
            return r
        }) as Instance;

    }

    @Override
    void release(ObjectRef objectRef) {
        logger.debug("RELEASE ${objectRef} FROM POOL")
        objectReferenceInstancesPool.remove(objectRef);
    }

    @Override
    void repair(ObjectRef objectRef, ObjectRefObject objectRefObject) {
        logger.debug("REPAIR ${objectRef}, put it back to pool")
        objectReferenceInstancesPool.put(objectRef, (Instance)objectRefObject);
    }

    @Override
    Slots restoreSlots(ObjectRef objectRef, Map<String, Object> dbRow, AgoClass agoClass) {
        throw new UnsupportedOperationException("result is ObjectRefInstance, no DbRefSlots provided");
    }

    @Override
    protected void saveInstance(Instance<?> instance, Set<Instance<?>> saved) {
        if (boxTypes.isBoxType((AgoClass)instance.getAgoClass()) || instance instanceof AgoArrayInstance)
            return;

        if(instance instanceof ObjectRefObject){
            if(instance.getDeferencedInstance() != null){
                var d = instance.getDeferencedInstance()
                if(!saved.contains(d)) {
                    saveInstance(d, saved)
                }
            }
            return;     // for folded Instance, it must be already saved or never touch its Slots, needn't save
        } else if(instance instanceof ExpandableObject){
            if(instance.isExpanded()){
                saved.add((Instance) instance);
                saveInstance(instance.getExpandedInstance(), saved)
            }
            return      // ignore folded Instance
        }
        if(logger.isDebugEnabled()) logger.debug("save instance " + instance)
        super.saveInstance(instance, saved)
        if(instance instanceof DeferenceObject){
            if(instance.isSaveRequired()){
                if(instance instanceof CallFrame) {
                    updateCallFrameRunningState(instance, (byte) -1)
                } else {
                    this.update((Instance)instance, (RdbSlots)null, instance.getAgoClass() as AgoClass);
                }
            }
        }
    }

    @Override
    void insert(Instance<?> instance, RdbSlots rdbSlots, AgoClass agoClass) {
        if (instance instanceof AgoFrame) {
            saveAgoFrame(instance)
        } else if(instance instanceof NativeFrame){
            saveNativeFrame(instance)
        } else if (instance instanceof AgoFunction) {
            saveAgoFunction((AgoFunction) instance)
        } else if (instance instanceof AgoClass) {
            saveAgoClass((AgoClass) instance)
        } else {
            saveAgoInstance(instance)
        }

        if (instance instanceof DeferenceObject) {
            instance.markSaved()
        }
    }

    @Override
    void update(Instance<?> instance, RdbSlots rdbSlots, AgoClass agoClass) {
        if (instance instanceof CallFrameWithRunningState) {
            updateCallFrameRunningState(instance.unwrap(), instance.getRunningState());
            return
        }

        Map<String, Object> arguments = new HashMap<>();
        ObjectRef ref = ((RdbRefSlots) rdbSlots).objectRef
        logger.info("UPDATE " + ref)

        arguments["id"] = ref.id()
        if(rdbSlots != null && rdbSlots.getRowState() != RowState.Unchanged) {
            arguments["slots"] = toJsonb(this.getAgoEngine().jsonStringifySlots(instance))
        }

        boolean hasPayload = false;
        if(instance instanceof NativeInstance){
            hasPayload = true;
            if(instance.nativePayload != null) {
                arguments['payload'] = toJsonb(instance.nativePayload)
            } else {
                arguments['payload'] = null
            }
        } else if(instance instanceof NativeFrame){
            hasPayload = true;
            if (instance.payload != null) {
                arguments['payload'] = toJsonb(instance.payload)
            } else {
                arguments['payload'] = null
            }
        }

        String sql
        if(instance instanceof CallFrame){
            arguments["runspace"] = (instance.runSpace as RdbRunSpace)?.id
            if(instance instanceof AgoFrame) {
                sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots, ${hasPayload ? 'payload = :payload,' : ''} runspace = :runspace, suspended = :suspended, pc = :pc WHERE id = :id"
                arguments["pc"] = instance.pc
                arguments["suspended"] = instance.suspended
            } else {
                sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots, ${hasPayload ? 'payload = :payload,' : ''}  runspace = :runspace, suspended = :suspended WHERE id = :id"
                arguments["suspended"] = instance.suspended
            }
            ObjectRef callerObjectRef = ObjectRefOwner.extractObjectRef(instance.caller);
            if(callerObjectRef){
                arguments['caller_id'] = callerObjectRef.id()
                arguments['caller_class'] = callerObjectRef.className()
            }
        } else {
            sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots ${hasPayload ? ',payload = :payload' : ''}  WHERE id = :id"
        }

        if(instance instanceof DeferenceObject){
            instance.markSaved()
        }

        this.sql.executeUpdate(arguments, sql)

    }

    @Override
    Instance<?> dereference(ObjectRef objectRef) {
        var objrefInstance = objectReferenceInstancesPool.get(objectRef);
        if(objrefInstance != null){
            if(objrefInstance instanceof ObjectRefObject) {
                var existed = objrefInstance.getDeferencedInstance()
                if (existed)
                    return existed
            } else {
                return objrefInstance       // MetaClass
            }
        }
        try(var connection = getDataSource().getConnection()) {
            var row = new Sql(connection).firstRow("SELECT * FROM " + getTableName(objectRef.className()) + " WHERE id = ?", [objectRef.id() as Object])
            String className = row["ago_class"]
            String parentScopeTable = row['parent_scope_class']
            Instance parentScope = null;
            if (parentScopeTable != null) {
                long parent_scope_id = row["parent_scope_id"] as long
                parentScope = restoreInstance(connection, new ObjectRef(parentScopeTable, parent_scope_id))
            }

            ObjectRef creator;
            if (row["creator_id"] != null) {
                creator = new ObjectRef(row["creator_class"] as String, row["creator_id"] as Long)
            } else {
                creator = null
            }

            AgoClass agoClass = this.getClassByName(className);
            if (agoClass instanceof MetaClass) {
                return this.getClassByName(row["fullname"] as String);
            } else if (agoClass instanceof AgoFunction) {
                CallFrame caller;
                if (row["caller_id"] != null) {
                    caller = (CallFrame) restoreInstance(new ObjectRef(row["caller_class"] as String, row["caller_id"] as Long))
                } else {
                    caller = null
                }
                LazyJsonAgoEngine engine = this.classManager as LazyJsonAgoEngine;
                MutableObject<Instance> boxInstanceScope = new MutableObject<>();
                var frame = engine.createFunctionInstance(agoClass as AgoFunction, parentScope, null, slots -> {
                    getAgoEngine().restoreSlots(slots as LazyJsonRefSlots, objectRef.id(), agoClass, (String) ((row['slots'] as PGobject).value), boxInstanceScope);
                })
                if (frame instanceof DeferenceAgoFrame) {
                    frame.pc = row['pc'] as int
                    frame.getDeferenceFrameState().entrance = row['is_entrance']
                    frame.getDeferenceFrameState().asyncEntrance = row['is_async_entrance']
                    frame.getDeferenceObjectState().setCreator(creator)
                } else if(frame instanceof DeferenceNativeFrame){        //DeferenceNativeFrame
                    if(row['payload']) frame.setPayload(new JsonSlurper().parseText(((PGobject)row['payload']).value))
                    frame.getDeferenceFrameState().entrance = row['is_entrance']
                    frame.getDeferenceFrameState().asyncEntrance = row['is_async_entrance']
                    frame.getDeferenceObjectState().setCreator(creator)
                } else {
                    throw new RuntimeException("not deference type");
                }
                if(boxInstanceScope.get() != null){
                    frame.setParentScope(boxInstanceScope.get())
                }
                if (logger.isDebugEnabled()) logger.debug("%s deference to %s".formatted(objectRef, frame))
                if(row['runspace']) {
                    PersistentRdbEngine persistentRdbEngine = (PersistentRdbEngine) this.classManager;
                    frame.runSpace = persistentRdbEngine.getRunSpace(row['runspace'] as Long)
                }
                frame.setCaller(caller)

                ReferenceCounter.increaseDeferenceSlotsForRestoreInstance(frame);

                if (frame instanceof DeferenceObject) frame.markSaved()

                return frame
            } else {
                LazyJsonAgoEngine engine = this.classManager as LazyJsonAgoEngine;
                LazyJsonRefSlots slots = agoClass.createSlots() as LazyJsonRefSlots
                getAgoEngine().restoreSlots(slots, objectRef.id(), agoClass, (String) ((row['slots'] as PGobject).value), null);

                var inst = agoClass.isNative() ? new DeferenceNativeInstance(slots,agoClass, engine) : new DeferenceInstance(slots, agoClass, engine);
                inst.parentScope = parentScope
                inst.getDeferenceObjectState().setCreator(creator)
                if(inst instanceof DeferenceNativeInstance){
                    var payload = row['payload'];
                    if(payload){
                        inst.setNativePayload(new JsonSlurper().parseText(((PGobject)payload).value))
                    }
                }
                inst.markSaved()

                ReferenceCounter.increaseDeferenceSlotsForRestoreInstance(inst);

                if (logger.isDebugEnabled()) logger.debug("%s deference to %s".formatted(objectRef, inst))
                return inst
            }
        }
    }


    LazyJsonAgoEngine getAgoEngine() {
        return this.classManager as LazyJsonAgoEngine;
    }

    public AgoClass loadScopedAgoClass(AgoClass baseClass, long id) {
        var row = sql.firstRow("SELECT parent_scope_class, parent_scope_id, creator_class, creator_id, slots FROM ${baseClass instanceof AgoFunction ? "ago_function" : "ago_class"} WHERE id =?", [id])
        var parentScopeId = row["parent_scope_id"]
        if(parentScopeId != null) {
            Instance scope = restoreInstance(new ObjectRef((String) row["parent_scope_class"], (Long) parentScopeId));
            var scoped = baseClass.cloneWithScope(scope)
            Object creatorId = row["creator_id"];
            if (creatorId != null) {
                if(scoped instanceof DeferenceObject){
                    scoped.getDeferenceObjectState().setCreator(new ObjectRef((String) row["creator_class"], (Long) creatorId))
                }
            }
            var slots = scoped.getSlots() as LazyJsonRefSlots;
            if(baseClass.getAgoClass() != agoEngine.getTheMeta()) {
                getAgoEngine().restoreSlots(slots, id, baseClass.getAgoClass(), row['slots'] as String, null)
            } else {
                slots.setId(id)
            }
            slots.setRowState(RowState.Unchanged);
            return scoped;
        }
        return baseClass
    }


}
