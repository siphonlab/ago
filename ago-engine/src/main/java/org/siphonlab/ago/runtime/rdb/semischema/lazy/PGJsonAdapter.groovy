package org.siphonlab.ago.runtime.rdb.semischema.lazy;

import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.agrona.concurrent.IdGenerator
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.RdbSlots
import org.siphonlab.ago.runtime.rdb.RowState
import org.siphonlab.ago.runtime.rdb.lazy.BoxValueInstance
import org.siphonlab.ago.runtime.rdb.lazy.DereferenceAdapter
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstance
import org.siphonlab.ago.runtime.rdb.lazy.RdbRefSlots
import org.siphonlab.ago.runtime.stateful.StatefulAgoFrame
import org.siphonlab.ago.runtime.rdb.semischema.SemiSchemaJsonRefSlots;
import org.siphonlab.ago.runtime.rdb.semischema.SemiSchemaPGAdapter
import org.siphonlab.ago.runtime.stateful.StatefulCallFrame
import org.siphonlab.ago.runtime.stateful.StatefulNativeFrame
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import java.sql.Connection;

@CompileStatic
public class PGJsonAdapter extends SemiSchemaPGAdapter implements DereferenceAdapter{

    private final static Logger logger = LoggerFactory.getLogger(PGJsonAdapter)

    public PGJsonAdapter(BoxTypes boxTypes, ClassManager classManager, int applicationId, IdGenerator idGenerator) {
        super(boxTypes, classManager, applicationId, idGenerator);
    }

    @Override
    public Instance restoreInstance(Connection connection, ObjectRef objectRef, CallFrame<?> callFrame) {
        return restoreInstance(objectRef, callFrame)
    }

    Instance restoreInstance(ObjectRef objectRef, CallFrame<?> callFrame){
        AgoClass agoClass = classManager.getClass(objectRef.className())
        Instance r;
        if (agoClass instanceof AgoFunction) {
            r = new ObjectRefCallFrame(agoClass, objectRef, this)
        } else {
            r = new ObjectRefInstance(agoClass, objectRef, this)
        }
        r.bindCallFrame(callFrame);
        return r
    }

    @Override
    Slots restoreSlots(ObjectRef objectRef, Map<String, Object> dbRow, AgoClass agoClass) {
        throw new UnsupportedOperationException("result is ObjectRefInstance, no slots provided");
    }

    @Override
    void saveInstance(Instance<?> instance) {
        super.saveInstance(instance)
    }

    @Override
    def void insert(Instance<?> instance, RdbSlots rdbSlots, AgoClass agoClass) {
        if (instance instanceof AgoFrame) {
            saveAgoFrame((StatefulAgoFrame) instance)
        } else if(instance instanceof NativeFrame){
            saveNativeFrame((StatefulNativeFrame) instance)
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
        Map<String, Object> arguments = new HashMap<>();

        ObjectRef ref = ((RdbRefSlots) rdbSlots).objectRef
        arguments["id"] = ref.id()
        arguments["slots"] = toJsonb(instance)

        String sql
        if(instance instanceof CallFrame){
            if(instance instanceof NativeFrame) {
                sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots, state = :state WHERE id = :id"
                arguments["state"] = ((StatefulCallFrame) instance).runningState.code
            } else {
                sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots, state = :state, pc = :pc WHERE id = :id"
                arguments["state"] = ((StatefulAgoFrame) instance).runningState.code
                arguments["pc"] = ((StatefulAgoFrame) instance).pc
            }
        } else {
            sql = "UPDATE " + tableName(instance.getAgoClass() as AgoClass) + " SET slots = :slots WHERE id = :id"
        }

        this.sql.executeUpdate(sql, arguments)

    }

    @Override
    Instance<?> dereference(ObjectRef objectRef, CallFrame callFrame) {
        try(var connection = getDataSource().getConnection()) {
            var row = new Sql(connection).firstRow("SELECT * FROM " + getTableName(objectRef.className()) + " WHERE id = ?", [objectRef.id() as Object])
            String className = row["ago_class"]
            String parentScopeTable = row['parent_scope_class']
            Instance parentScope = null;
            if (parentScopeTable != null) {
                long parent_scope_id = row["parent_scope_id"] as long
                parentScope = restoreInstance(connection, new ObjectRef(parentScopeTable, parent_scope_id), callFrame)
            }

            CallFrame creator;
            if (row["creator_id"] != null) {
                creator = restoreInstance(new ObjectRef(row["creator_class"] as String, row["creator_id"] as Long), callFrame) as CallFrame
            } else {
                creator = null
            }

            AgoClass agoClass = this.getClassByName(className);
            if (agoClass instanceof MetaClass) {
                return this.getClassByName(row["fullname"] as String);
            } else if (agoClass instanceof AgoFunction) {
                CallFrame caller;
                if (row["caller_id"] != null) {
                    caller = (CallFrame) restoreInstance(new ObjectRef(row["caller_class"] as String, row["caller_id"] as Long), callFrame)
                } else {
                    caller = null
                }
                def runSpace = callFrame.getRunSpace()
                JsonAgoEngine engine = runSpace.getAgoEngine() as JsonAgoEngine;
                var frame = engine.createFunctionInstance(agoClass as AgoFunction, parentScope, caller, creator, slots -> {
                    dereferenceSlots(slots as JsonRefSlots, objectRef, row, agoClass, callFrame)
                })
                if (frame instanceof DeferenceAgoFrame) {
                    frame.pc = row['pc'] as int
                }
                if (logger.isDebugEnabled()) logger.debug("%s deference to %s".formatted(objectRef, frame))
                return frame
            } else {
                def runSpace = callFrame.getRunSpace()
                JsonAgoEngine engine = runSpace.getAgoEngine() as JsonAgoEngine;
                var inst = engine.createInstance(parentScope, agoClass, creator, runSpace, slots -> {
                    dereferenceSlots(slots as JsonRefSlots, objectRef, row, agoClass, callFrame)
                })
                if (logger.isDebugEnabled()) logger.debug("%s deference to %s".formatted(objectRef, inst))
                return inst
            }
        }
    }

    void dereferenceSlots(JsonRefSlots jsonRefSlots, ObjectRef objectRef, Map<String, Object> dbRow, AgoClass agoClass, CallFrame callFrame) {
        Map<String, Object> map = loadPgJsonAsMap((PGobject)dbRow["slots"]);
        jsonRefSlots.setId(objectRef.id())
        jsonRefSlots.setRowState(RowState.Unchanged)
        var slots = jsonRefSlots.getBaseSlots()     //TODO lost a generated id

        for(var slotDef : agoClass.getSlotDefs()){
            int slot = slotDef.index
            var v = map[jsonRefSlots.getJsonSlotMapper().getFieldName(slot)]
            switch (slotDef.typeCode.value){
                case TypeCode.INT_VALUE -> slots.setInt(slot, v as int)
                case TypeCode.LONG_VALUE -> slots.setLong(slot, v as long)
                case TypeCode.SHORT_VALUE -> slots.setShort(slot, v as short)
                case TypeCode.BYTE_VALUE -> slots.setByte(slot, v as byte)
                case TypeCode.FLOAT_VALUE -> slots.setFloat(slot, v as float)
                case TypeCode.DOUBLE_VALUE -> slots.setDouble(slot, v as double)
                case TypeCode.BOOLEAN_VALUE -> slots.setBoolean(slot, v as boolean)
                case TypeCode.STRING_VALUE -> slots.setString(slot, v as String)
                case TypeCode.CHAR_VALUE -> slots.setChar(slot, v as char)
                case TypeCode.CLASS_REF_VALUE -> slots.setClassRef(slot, this.getClassByName(v as String).classId)
                case TypeCode.OBJECT_VALUE -> {
                    if(v instanceof Map){
                        var r = new ObjectRef(v["@type"] as String, v["@id"] as Long)
                        slots.setObject(slot, restoreInstance(null,  r, callFrame));
                    } else if(v == null){
                        //
                    } else {
                        slots.setObject(slot, new BoxValueInstance(v, slotDef.agoClass));
                    }
                }
            }
        }
    }

    Map<String, Object> toMap(SemiSchemaJsonRefSlots slots, AgoClass agoClass) {
        Map<String, Object> map = [:]
        for (var slotDef : agoClass.getSlotDefs()) {
            int slot = slotDef.index
            String fldName = slots.jsonSlotMapper.getFieldName(slot)
            map[fldName]  = switch (slotDef.typeCode.value) {
                case TypeCode.INT_VALUE -> slots.getInt(slot)
                case TypeCode.LONG_VALUE -> slots.getLong(slot)
                case TypeCode.SHORT_VALUE -> slots.getShort(slot)
                case TypeCode.BYTE_VALUE -> slots.getByte(slot)
                case TypeCode.FLOAT_VALUE -> slots.getFloat(slot)
                case TypeCode.DOUBLE_VALUE -> slots.getDouble(slot)
                case TypeCode.BOOLEAN_VALUE -> slots.getBoolean(slot)
                case TypeCode.STRING_VALUE -> slots.getString(slot)
                case TypeCode.CHAR_VALUE -> slots.getChar(slot)
                case TypeCode.CLASS_REF_VALUE ->  classManager.getClass(slots.getClassRef(slot)).fullname
                case TypeCode.OBJECT_VALUE -> {
                    var v = slots.getObject(slot)
                    if(v == null) yield null

                    if(slotDef.getAgoClass() instanceof MetaClass){
                        yield ((AgoClass)v).fullname
                    }
                    if (v instanceof Instance) {
                        var t = mapObjectType(v.agoClass)
                        if(t.additional != null){
                            var r = v instanceof ObjectRefInstance ? v.objectRef:  (v.slots as SemiSchemaJsonRefSlots).objectRef
                            yield ["@type" : r.className(), "@id": r.id()]
                        } else {
                            //TODO boxType != slotDef.class
                            if(v instanceof BoxValueInstance){
                                yield v.value;
                            } else {
                                // unbox
                                switch (t.typeCode.value){
                                    case TypeCode.INT_VALUE:
                                        yield (slots.getInt(0));
                                    case TypeCode.LONG_VALUE:
                                        yield (slots.getLong(0));
                                    case TypeCode.FLOAT_VALUE:
                                        yield (slots.getFloat(0));
                                    case TypeCode.DOUBLE_VALUE:
                                        yield(slots.getDouble(0));
                                    case TypeCode.BOOLEAN_VALUE:
                                        yield(slots.getBoolean(0));
                                    case TypeCode.STRING_VALUE:
                                        yield(slots.getString(0));
                                    case TypeCode.SHORT_VALUE:
                                        yield(slots.getShort(0));
                                    case TypeCode.BYTE_VALUE:
                                        yield(slots.getByte(0));
                                    case TypeCode.CHAR_VALUE:
                                        yield(String.valueOf(slots.getChar(0)));
                                    case TypeCode.NULL_VALUE:
                                        yield null;
                                    case TypeCode.CLASS_REF_VALUE:
                                        int classRef = slots.getClassRef(0);
                                        yield(classManager.getClass(classRef).getFullname());
                                }
                            }
                        }
                    }
                }
            }
        }
        return map
    }

    @Override
    CallFrame[] loadResumableCallFrames(CallFrame<?> resumeFrame) {
        // PENDING, RUNNING
        var rows = sql.rows("SELECT id, ago_class FROM ago_frame WHERE application = ? AND state IN (0, 1)", [this.applicationId as Object])
        var refInstances = rows.collect({
            AgoClass agoClass = classManager.getClass(it["ago_class"] as String)
            return (ObjectRefCallFrame)restoreInstance(new ObjectRef(agoClass.fullname, (Long) it["id"]), resumeFrame)
        })
        return refInstances.collect {it.recomposeAsCallFrame()}.toArray(CallFrame[]::new)     //TODO need a CallFrame to deserialize json
    }
}
