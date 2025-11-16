package org.siphonlab.ago.runtime.rdb.json.lazy

import groovy.json.JsonSlurper;
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.agrona.concurrent.IdGenerator
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*
import org.siphonlab.ago.native_.NativeFrame
import org.siphonlab.ago.runtime.rdb.CallFrameWithRunningState;
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.RdbAgoRunSpace
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

    private Map<ObjectRef, ObjectRefObject> objectReferenceInstancesPool = new ConcurrentHashMap<>();

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
            } else {
                r = new ObjectRefInstance(agoClass, objectRef, this)
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
        objectReferenceInstancesPool.put(objectRef, objectRefObject);
    }

    @Override
    Slots restoreSlots(ObjectRef objectRef, Map<String, Object> dbRow, AgoClass agoClass) {
        throw new UnsupportedOperationException("result is ObjectRefInstance, no DbRefSlots provided");
    }

    @Override
    protected void saveInstance(Instance<?> instance, Set<Instance<?>> saved) {
        if(instance instanceof ObjectRefObject){
            if(instance.getDeferencedInstance() != null){
                saveInstance(instance.getDeferencedInstance(), saved)
            }
            return;     // for folded Instance, it must be already saved or never touch its Slots, needn't save
        } else if(instance instanceof ExpandableObject){
            if(instance.isExpanded()){
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
                    throw new UnsupportedOperationException("not for DeferenceInstance");
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
    def void update(Instance<?> instance, RdbSlots rdbSlots, AgoClass agoClass) {
        if (instance instanceof CallFrameWithRunningState) {
            updateCallFrameRunningState(instance.unwrap(), instance.getRunningState());
            return
        }

        Map<String, Object> arguments = new HashMap<>();
        ObjectRef ref = ((RdbRefSlots) rdbSlots).objectRef
        logger.info("UPDATE " + ref)

        arguments["id"] = ref.id()
        arguments["slots"] = toJsonb(this.getAgoEngine().jsonStringifySlots(instance))

        String sql
        if(instance instanceof CallFrame){
            arguments["runspace"] = (instance.runSpace as RdbAgoRunSpace)?.id
            if(instance instanceof AgoFrame) {
                sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots, runspace = :runspace, suspended = :suspended, pc = :pc WHERE id = :id"
                arguments["pc"] = instance.pc
                arguments["suspended"] = instance.suspended
            } else {
                sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots, runspace = :runspace, suspended = :suspended WHERE id = :id"
                arguments["suspended"] = instance.suspended
            }
        } else {
            sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots WHERE id = :id"
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
            var existed = objrefInstance.getDeferencedInstance()
            if(existed)
                return existed
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

            CallFrame creator;
            if (row["creator_id"] != null) {
                creator = restoreInstance(new ObjectRef(row["creator_class"] as String, row["creator_id"] as Long)) as CallFrame
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
                var frame = engine.createFunctionInstance(agoClass as AgoFunction, parentScope, caller, creator, slots -> {
                    getAgoEngine().restoreSlots(slots as LazyJsonRefSlots, objectRef.id(), agoClass, (String) ((row['slots'] as PGobject).value));
                })
                if (frame instanceof DeferenceAgoFrame) {
                    frame.pc = row['pc'] as int
                    frame.isEntrance = row['is_entrance']
                    frame.isAsyncEntrance = row['is_async_entrance']
                } else if(frame instanceof DeferenceNativeFrame){        //DeferenceNativeFrame
                    if(row['payload']) frame.setPayload(new JsonSlurper().parseText(((PGobject)row['payload']).value))
                    frame.isEntrance = row['is_entrance']
                    frame.isAsyncEntrance = row['is_async_entrance']
                } else {
                    throw new RuntimeException("not deference type");
                }
                if (logger.isDebugEnabled()) logger.debug("%s deference to %s".formatted(objectRef, frame))
                if(row['runspace']) {
                    PersistentRdbEngine persistentRdbEngine = (PersistentRdbEngine) this.classManager;
                    frame.runSpace = persistentRdbEngine.getRunSpace(row['runspace'] as Long)
                }

                ReferenceCounter.increaseDeferenceSlotsForRestoreInstance(frame);

                if (frame instanceof DeferenceObject) frame.markSaved()

                return frame
            } else {
                LazyJsonAgoEngine engine = this.classManager as LazyJsonAgoEngine;
                LazyJsonRefSlots slots = agoClass.createSlots() as LazyJsonRefSlots
                getAgoEngine().restoreSlots(slots, objectRef.id(), agoClass, (String) ((row['slots'] as PGobject).value));
                var inst = new DeferenceInstance(slots, agoClass, engine);
                inst.parentScope = parentScope
                inst.creator = creator
                if(inst instanceof DeferenceObject) inst.markSaved()

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
        var row = sql.firstRow("SELECT parent_scope_class, parent_scope_id, creator_class, creator_id, slots FROM ago_class WHERE id =?", [id])
        var parentScopeId = row["parent_scope_id"]
        if(parentScopeId != null) {
            Instance scope = restoreInstance(new ObjectRef((String) row["parent_scope_class"], (Long) parentScopeId));
            var scoped = baseClass.withScope(scope)
            Object creatorId = row["creator_id"];
            if (parentScopeId != null) {
                scoped.setCreator((CallFrame) this.restoreInstance(new ObjectRef((String) row["creator_class"], (Long) creatorId)));
            }
            getAgoEngine().restoreSlots(scoped.getSlots() as LazyJsonRefSlots,id,baseClass.getAgoClass(),row['slots'] as String)
            return scoped;
        }
        return baseClass
    }


}
