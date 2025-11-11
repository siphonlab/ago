package org.siphonlab.ago.runtime.rdb.json.lazy

import groovy.json.JsonSlurper;
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.agrona.concurrent.IdGenerator
import org.apache.commons.lang3.NotImplementedException
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*
import org.siphonlab.ago.native_.NativeFrame
import org.siphonlab.ago.runtime.rdb.CallFrameWithRunningState;
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.RdbEngine
import org.siphonlab.ago.runtime.rdb.RdbSlots
import org.siphonlab.ago.runtime.rdb.RowState
import org.siphonlab.ago.runtime.rdb.lazy.DereferenceAdapter
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstance
import org.siphonlab.ago.runtime.rdb.lazy.RdbRefSlots
import org.siphonlab.ago.runtime.rdb.json.JsonRefSlots;
import org.siphonlab.ago.runtime.rdb.json.JsonPGAdapter
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import java.sql.Connection;

@CompileStatic
public class LazyJsonPGAdapter extends JsonPGAdapter implements DereferenceAdapter{

    private final static Logger logger = LoggerFactory.getLogger(LazyJsonPGAdapter)

    public LazyJsonPGAdapter(BoxTypes boxTypes, ClassManager classManager, int applicationId, IdGenerator idGenerator) {
        super(boxTypes, classManager, applicationId, idGenerator);
    }

    @Override
    public Instance restoreInstance(Connection connection, ObjectRef objectRef) {
        return restoreInstance(objectRef)
    }

    Instance restoreInstance(ObjectRef objectRef){
        if(objectRef == null) return null;
        //TODO the class may has scope
        AgoClass agoClass = classManager.getClass(objectRef.className())
        Instance r;
        if (agoClass instanceof AgoFunction) {
            r = new ObjectRefCallFrame(agoClass, objectRef, this, RowState.Unchanged)
        } else {
            r = new ObjectRefInstance(agoClass, objectRef, this)
        }
        return r
    }

    @Override
    Slots restoreSlots(ObjectRef objectRef, Map<String, Object> dbRow, AgoClass agoClass) {
        throw new UnsupportedOperationException("result is ObjectRefInstance, no DbRefSlots provided");
    }

    @Override
    def void insert(Instance<?> instance, RdbSlots rdbSlots, AgoClass agoClass) {
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
            if(instance instanceof AgoFrame) {
                sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots, suspended = :suspended, pc = :pc WHERE id = :id"
                arguments["pc"] = instance.pc
                arguments["suspended"] = instance.suspended
            } else {
                sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots, suspended = :suspended WHERE id = :id"
                arguments["suspended"] = instance.suspended
            }
        } else {
            sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots WHERE id = :id"
        }

        this.sql.executeUpdate(sql, arguments)

    }

    @Override
    Instance<?> dereference(ObjectRef objectRef) {
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
                } else if(frame instanceof NativeFrame){
                    if(row['payload']) frame.setPayload(new JsonSlurper().parseText(((PGobject)row['payload']).value))
                }
                if (logger.isDebugEnabled()) logger.debug("%s deference to %s".formatted(objectRef, frame))
                if(row['runspace']) {
                    PersistentRdbEngine persistentRdbEngine = (PersistentRdbEngine) this.classManager;
                    frame.runSpace = persistentRdbEngine.getRunSpace(row['runspace'] as Long)
                }

                if(row['is_entrance']){
                    return new EntranceCallFrame<>(frame)
                } else if(row['is_async_entrance']){
                    return new AsyncEntranceCallFrame<>(frame)
                }

                return frame
            } else {
                LazyJsonAgoEngine engine = this.classManager as LazyJsonAgoEngine;
                var inst = engine.createInstance(parentScope, agoClass, creator, slots -> {
                    getAgoEngine().restoreSlots(slots as LazyJsonRefSlots, objectRef.id(), agoClass, (String) ((row['slots'] as PGobject).value));
                })
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
