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
package org.siphonlab.ago.runtime.rdb.json

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
import groovy.transform.CompileStatic
import io.netty.util.collection.LongObjectHashMap
import org.agrona.concurrent.IdGenerator
import org.apache.commons.lang3.mutable.MutableObject
import org.postgresql.jdbc.PgArray
import org.postgresql.util.PGobject
import org.siphonlab.ago.*
import org.siphonlab.ago.native_.AgoNativeFunction
import org.siphonlab.ago.native_.NativeFrame
import org.siphonlab.ago.native_.NativeInstance
import org.siphonlab.ago.runtime.AgoArrayInstance
import org.siphonlab.ago.runtime.db.WorkflowAdapter
import org.siphonlab.ago.runtime.db.CallFrameWithRunningState
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner
import org.siphonlab.ago.runtime.rdb.DbAdapter
import org.siphonlab.ago.runtime.db.TaskRunSpace
import org.siphonlab.ago.runtime.rdb.DbEngine
import org.siphonlab.ago.runtime.db.DbSlots
import org.siphonlab.ago.runtime.db.ObjectRef
import org.siphonlab.ago.runtime.rdb.RowState
import org.siphonlab.ago.runtime.rdb.RunSpaceDesc
import org.siphonlab.ago.runtime.rdb.SavableRunSpace
import org.siphonlab.ago.runtime.db.lazy.DeferenceAgoFrame
import org.siphonlab.ago.runtime.db.lazy.DeferenceInstance
import org.siphonlab.ago.runtime.db.lazy.DeferenceNativeFrame
import org.siphonlab.ago.runtime.db.lazy.DeferenceNativeInstance
import org.siphonlab.ago.runtime.db.lazy.DeferenceObject
import org.siphonlab.ago.runtime.db.lazy.DereferenceAdapter
import org.siphonlab.ago.runtime.db.lazy.ObjectRefCallFrame
import org.siphonlab.ago.runtime.db.lazy.ObjectRefInstance
import org.siphonlab.ago.runtime.db.lazy.ObjectRefObject
import org.siphonlab.ago.runtime.rdb.reactive.PersistentDbEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Nonnull
import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException

@CompileStatic
public abstract class JsonPGAdapter extends DbAdapter implements DereferenceAdapter, WorkflowAdapter{

    private  final Logger logger = LoggerFactory.getLogger(JsonPGAdapter)

    protected Sql sql
    protected int applicationId

    public JsonPGAdapter(BoxTypes boxTypes, ClassManager classManager, int applicationId, IdGenerator idGenerator) {
        super(boxTypes, classManager, idGenerator);
        this.applicationId = applicationId;
    }

    static Map<String, Object> toMap(TypeCode typeCode) {
        return typeCode.value < TypeCode.GENERIC_TYPE_START ? ["code": typeCode.value as Object] : ["code": typeCode.value as Object, "description": typeCode.toShortString()];
    }

    @Override
    void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource)
        this.sql = new Sql(this.getDataSource());
    }

    void saveAgoFrame(@Nonnull Connection conn, AgoFrame agoFrame) {
        var slots = agoFrame.slots as JsonRefSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(agoFrame.parentScope);
        ObjectRef creatorObjectRef = ObjectRefOwner.extractCreator(agoFrame);

//        var defaultSlots = defaultSlots(agoFrame.agoClass, slots.jsonSlotMapper.jsonFiledNames)

        def params = [
                id                : slots.objectRef.setObjectRef() as Object,
                application       : applicationId,
                ago_class         : agoFrame.agoClass.fullname,
                parent_scope_id   : parentScope?.id(),
                parent_scope_class: parentScope?.className(),
                creator_id        : creatorObjectRef?.id(),
                creator_class     : creatorObjectRef?.className(),
                pc                : 0,
                state             : RunSpace.RunningState.PENDING,
                suspended         : false,
                exception_id      : null,
                exception_class   : null,
                runspace          : (agoFrame.getRunSpace() as TaskRunSpace)?.id,
                slots             : toJsonb((classManager as DbEngine).jsonStringifySlots(agoFrame))
        ]

        var sql = new Sql(conn);
        sql.executeInsert("""
            INSERT INTO ago_frame
                (id, application, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, 
                 caller_id, caller_class, pc, state, suspended, exception_id, exception_class, runspace, slots)
            VALUES (:id, :application, :ago_class, :parent_scope_id, :parent_scope_class, :creator_id, :creator_class, 
                    :caller_id, :caller_class, :pc, :state, :suspended, :exception_id, :exception_class, :runspace, :slots);
        """, params)

    }

    void saveNativeFrame(@Nonnull Connection conn, NativeFrame agoFrame) {
        var slots = agoFrame.slots as JsonRefSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(agoFrame.parentScope);
        ObjectRef creatorObjectRef = ObjectRefOwner.extractCreator(agoFrame);

        def params = [
                id                : slots.objectRef.setObjectRef() as Object,
                application       : applicationId,
                ago_class         : agoFrame.agoClass.fullname,
                parent_scope_id   : parentScope?.id(),
                parent_scope_class: parentScope?.className(),
                creator_id        : creatorObjectRef?.id(),
                creator_class     : creatorObjectRef?.className(),
                pc                : 0,
                state             : RunSpace.RunningState.PENDING,
                suspended         : false,
                exception_id      : null,
                exception_class   : null,
                runspace          : (agoFrame.getRunSpace() as TaskRunSpace)?.id,
                slots             : toJsonb((classManager as DbEngine).jsonStringifySlots(agoFrame))
        ]

        var sql = new Sql(conn);
        sql.executeInsert("""INSERT INTO ago_frame
                (id, application, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, 
                 caller_id, caller_class, pc, state, suspended, exception_id, exception_class, runspace, slots)
            VALUES (:id, :application, :ago_class, :parent_scope_id, :parent_scope_class, :creator_id, :creator_class, 
                    :caller_id, :caller_class, :pc, :state, :suspended, :exception_id, :exception_class, :runspace, :slots)""", params)
    }

    def updateRow(@Nonnull Connection conn, String table, Map<String, Object> data, String pkName = 'id') {
        def pkValue = data[pkName]
        if (pkValue == null)
            throw new IllegalArgumentException("Missing primary key '${pkName}'")

        def columns = data.findAll { k, v -> k != pkName }
        if (columns.isEmpty())
            return 0

        def setClause = columns.collect { k, v -> "$k = :$k" }.join(', ')
        def sqlText = "UPDATE $table SET $setClause WHERE $pkName = :$pkName"

        var sql = new Sql(conn)
        return sql.executeUpdate(data, sqlText)
    }


    void updateCallFrameRunningState(@Nonnull Connection conn, CallFrame callFrame, byte runningState, int pc = -1) {
        var objectRef = ObjectRefOwner.extractObjectRef(callFrame)
        logger.info("UPDATE " + objectRef)
        var map = [
                "id"       : objectRef.id() as Object,
                "suspended": callFrame.suspended,
                "runspace" : (callFrame.runSpace as SavableRunSpace)?.id,
        ]
        if(runningState != (byte)-1) map["state"] = runningState

        boolean saveSlots = true;
        while(true) {
            if (callFrame instanceof ObjectRefCallFrame) {
                if (callFrame.deferencedInstance == null) {
                    return
                } else {
                    callFrame = callFrame.deferencedInstance as CallFrame;

                    if (callFrame instanceof AsyncEntranceCallFrame) {
                        map["is_async_entrance"] = true
                        callFrame = callFrame.inner
                    } else if (callFrame instanceof EntranceCallFrame) {
                        map["is_entrance"] = true
                        callFrame = callFrame.inner
                    }
                }
            } else if (callFrame instanceof AsyncEntranceCallFrame) {
                map["is_async_entrance"] = true
                callFrame = callFrame.inner
            } else if (callFrame instanceof EntranceCallFrame) {
                map["is_entrance"] = true
                callFrame = callFrame.inner
            } else {
                break
            }
        }
        if(saveSlots){
            var slots = callFrame.slots as JsonRefSlots
            if(slots instanceof DbSlots && (slots.rowState == RowState.Saving || slots.rowState == RowState.Modified))
                map['slots'] = toJsonb(this.getAgoEngine().jsonStringifySlots(callFrame))
        }
        ObjectRef callerObjectRef = ObjectRefOwner.extractObjectRef(callFrame.caller);
        if(callerObjectRef){
            map['caller_id'] = callerObjectRef.id()
            map['caller_class'] = callerObjectRef.className()
        }

        if(callFrame instanceof AgoFrame) {
            var savedPc = pc;
            if (pc == -1) {
                savedPc = callFrame.pc;
            }
            map["pc"] = savedPc;
        } else if(callFrame instanceof NativeFrame) {
            map["payload"] = callFrame.nativePayload ? toJsonb(callFrame.nativePayload) : null;
        } else {
            throw new UnsupportedOperationException("unsupported frame type " + callFrame)
        }
        updateRow(conn, "ago_frame", map, "id")
        if(callFrame instanceof DeferenceObject){
            callFrame.markSaved()
        }
    }

    void saveAgoClass(@Nonnull Connection conn, AgoClass agoClass) {
        var slots = agoClass.slots as JsonRefSlots;
        if(logger.isDebugEnabled()) logger.debug("INSERT CLASS " + slots.objectRef.setObjectRef())

        sql.executeInsert(toMap(agoClass, applicationId),
            """INSERT INTO ago_class (id, application, class_id, class_type, ago_class, parent_scope_id, parent_scope_class, 
                        creator_id, creator_class, slots, fullname, modifiers, super_class, interfaces, children, methods, parent,
                         permit_class, parameterized_base_class, \"name\", fields, slotdefs, concrete_type_info, source_location,
                          has_slots_creator) 
                    VALUES(:id, :application, :class_id, :class_type, :ago_class, :parent_scope_id, :parent_scope_class, :creator_id, 
                            :creator_class, :slots, :fullname, :modifiers, :super_class, :interfaces, :children, :methods, :parent, :permit_class,
                            :parameterized_base_class, :name, :fields, :slotdefs, :concrete_type_info, :source_location, :has_slots_creator)"""
        )
    }

    @Override
    void saveStrings(List<String> strings) {
        this.sql.withBatch("INSERT INTO ago_string(id, application, index, value) VALUES(?, ?, ?, ?)", {
            for (i in 0..<strings.size()) {
                it.addBatch([this.nextId() as Object, applicationId, i, strings[i]])
            }
        })
    }

    @Override
    void saveBlobs(List<byte[]> blobs) {
        this.sql.withBatch("INSERT INTO ago_blob(id, application, index, data) VALUES(?, ?, ?, ?)", {
            for (i in 0..<blobs.size()) {
                it.addBatch([this.nextId() as Object, applicationId, i, blobs[i]])
            }
        })
    }

    static Map<String, Object> toMap(AgoField agoField){
        return ["name": agoField.name as Object,
                "modifiers": agoField.modifiers,
                "typeCode": toMap(agoField.typeCode),
                "agoClass": agoField.agoClass?.fullname,
                "slotIndex": agoField.slotIndex,
                "constLiteralValue": agoField.constLiteralValue,
                "sourceLocation": toMap(agoField.sourceLocation),
                "ownerClass": agoField.ownerClass?.fullname]
    }

    static Map<String, Object> toMap(AgoVariable variable) {
        var r = ["name"             : variable.name as Object,
                "modifiers"        : variable.modifiers,
                "typeCode"         : toMap(variable.typeCode),
                "agoClass"         : variable.agoClass?.fullname,
                "slotIndex"        : variable.slotIndex,
                "constLiteralValue": variable.constLiteralValue,
                "sourceLocation"   : toMap(variable.sourceLocation)]
        if(variable instanceof AgoField){
            r["ownerClass"] = variable.getOwnerClass()?.getFullname();
        }
        return r;
    }

    static Map<String, Object> toMap(SourceLocation sourceLocation) {
        if(sourceLocation == null) return null
        return ["end": sourceLocation.end as Object, "start": sourceLocation.start,
                "filename": sourceLocation.filename, "line": sourceLocation.line,
                "column": sourceLocation.column, "length": sourceLocation.length]
    }

    static Map<String, Object> toMap(AgoSlotDef slotDef) {
        return ["index": slotDef.index as Object, "name": slotDef.name, "typeCode": toMap(slotDef.typeCode), "agoClass": slotDef.agoClass?.fullname]
    }

    static Map<String, Object> toMap(ConcreteTypeInfo concreteTypeInfo) {
        if(concreteTypeInfo == null) return null;
        if(concreteTypeInfo instanceof ArrayInfo) {
            return ["type": "ArrayInfo", "elementType": concreteTypeInfo.elementType.fullname as Object]
        } else if(concreteTypeInfo instanceof NullableTypeInfo){
            return ["type": "NullableInfo", "baseType" : concreteTypeInfo.baseClass.fullname as Object]
        } else if(concreteTypeInfo instanceof GenericArgumentsInfo){
            return ["type": "GenericArgumentsInfo" as Object, "templateClass": concreteTypeInfo.templateClass.fullname, "arguments": concreteTypeInfo.arguments.collect( { ((AgoClass)it).fullname }).toArray()]
        } else if(concreteTypeInfo instanceof GenericTypeParametersInfo){
            return ["type": "GenericTypeParametersInfo" as Object,
                    "genericParameters": concreteTypeInfo.genericParameters.collect { ((AgoClass)it).fullname } .toArray()]
        } else if(concreteTypeInfo instanceof ParameterizedClassInfo){
            var arguments = []
            for (arg in concreteTypeInfo.arguments) {
                if (arg instanceof AgoClass klass) {
                    arguments.add(klass.getFullname());
                }
                else {
                    arguments.add(arg);
                }
            }
            return ["type": "ParameterizedClassInfo" as Object,
                    "parameterizedBaseClass": concreteTypeInfo.parameterizedBaseClass.fullname,
                    "parameterizedConstructor": concreteTypeInfo.parameterizedConstructor.fullname,
                    "arguments": arguments]
        } else {
            throw new IllegalArgumentException("unknown concrete type " + concreteTypeInfo);
        }
    }

    static Map<String, Object> toMap(SwitchTable switchTable) {
        if(switchTable instanceof DenseSwitchTable){
            return ["type": "DenseSwitchTable" as Object, "data": switchTable.data];
        } else {
            SparseSwitchTable sparseSwitchTable = switchTable as SparseSwitchTable;
            return ["type": "SparseSwitchTable" as Object, "data": sparseSwitchTable.getMap()]
        }
    }

    static Map<String, Object> toMap(TryCatchItem tryCatchItem) {
        return ["begin": tryCatchItem.begin as Object, "end": tryCatchItem.end, "handler": tryCatchItem.handler, "exceptionClasses": tryCatchItem.exceptionClasses.collect { (it as AgoClass).fullname }.toArray()];
    }

    static Map<String, Object> toMap(SourceMapEntry sourceMapEntry) {
        return ["codeOffset": sourceMapEntry.codeOffset(), "sourceLocation": toMap(sourceMapEntry.sourceLocation())]
    }

    void saveAgoFunction(@Nonnull Connection conn, AgoFunction agoFunction) {
        var slots = agoFunction.slots as JsonRefSlots;
        if (logger.isDebugEnabled()) logger.debug("INSERT Function " + slots.objectRef.setObjectRef())

        var m = toMap(agoFunction, applicationId);

        m["result_type"] = agoFunction.getResultClass().getFullname()
        m["code"] = agoFunction.code
        m["variables"] = toJsonbArray(agoFunction.variables?.collect {toMap(it as AgoVariable)}?.toArray());
        m["parameters"] = toJsonbArray(agoFunction.parameters?.collect{toMap(it as AgoParameter)}?.toArray());
        m["switch_tables"] = toJsonbArray(agoFunction.switchTables?.collect {toMap(it as SwitchTable)}?.toArray());
        m["try_catch_items"] = toJsonbArray(agoFunction.tryCatchItems?.collect{toMap(it as TryCatchItem)}?.toArray());
        m["source_map_entries"] = toJsonbArray(agoFunction.sourceMap?.collect{toMap(it as SourceMapEntry)}?.toArray());
        m["native_function_entrance"] = agoFunction instanceof AgoNativeFunction? agoFunction.nativeEntrance : null
        m["native_function_result_slot"] = agoFunction instanceof AgoNativeFunction ? agoFunction.resultSlot : null

        var sql = new Sql(conn);
        sql.executeInsert("INSERT INTO ago_function (id, application, class_id, class_type, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, slots, fullname, modifiers, super_class, " +
                        'interfaces, children, methods, parent, permit_class, parameterized_base_class, "name", fields, slotdefs, concrete_type_info, source_location, ' +
                        'variables,parameters,switch_tables,try_catch_items,source_map_entries, native_function_entrance, native_function_result_slot, has_slots_creator, code, result_type) ' +
                        "VALUES(:id, :application, :class_id, :class_type, :ago_class, :parent_scope_id, :parent_scope_class, :creator_id, :creator_class, :slots, :fullname, :modifiers, :super_class, " +
                        ":interfaces, :children, :methods, :parent, :permit_class, :parameterized_base_class, :name, :fields, :slotdefs, :concrete_type_info, :source_location," +
                        ":variables,:parameters,:switch_tables,:try_catch_items,:source_map_entries, :native_function_entrance, :native_function_result_slot, :has_slots_creator, :code, :result_type)",

                m
        )
    }

    Map<String, Object> toMap(AgoClass agoClass, int applicationId){
        var slots = agoClass.slots as JsonRefSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(agoClass.parentScope);
        var creator = ObjectRefOwner.extractCreator(agoClass);

        return [
                id                      : slots.objectRef.setObjectRef() as Object,
                application             : applicationId,
                class_id                : agoClass.classId,
                ago_class               : agoClass.agoClass.fullname,
                parent_scope_id         : parentScope?.id(),
                parent_scope_class      : parentScope?.className(),

                creator_id              : creator?.id(),
                creator_class           : creator?.className(),
                slots                   : toJsonb(getAgoEngine().jsonStringifySlots(slots, agoClass.agoClass)),
                fullname                : agoClass.getFullname(),

                modifiers               : agoClass.getModifiers(),
                super_class             : agoClass.getSuperClass()?.getFullname(),
                interfaces              : agoClass.getInterfaces()?.collect { (it as AgoClass).fullname }?.toArray(String[]::new), // trait is not interface
                children                : agoClass.getChildren()?.collect { (it as AgoClass).fullname }?.toArray(String[]::new),
                methods                 : agoClass.methods?.collect { (it as AgoFunction)?.fullname }?.toArray(String[]::new),

                parent                  : agoClass.parent?.fullname,
                permit_class            : agoClass.permitClass?.fullname,
                parameterized_base_class: agoClass.parameterizedBaseClass?.fullname,
                name                    : agoClass.name,
                fields                  : toJsonbArray(agoClass.fields?.collect { toMap(it as AgoField) }?.toArray()),

                slotdefs                : toJsonbArray(agoClass.slotDefs?.collect { toMap(it as AgoSlotDef) }?.toArray()),
                concrete_type_info      : toJsonb(toMap(agoClass.concreteTypeInfo)),
                source_location         : toJsonb(agoClass.sourceLocation),
                class_type              : agoClass.getType() as int,
                has_slots_creator       : agoClass.slotsCreator != null
        ]
    }

    DbEngine getAgoEngine(){
        return this.classManager as DbEngine;
    }

    PGobject toJsonb(Object object){
        if(object == null) return null;

        var obj = new PGobject();
        obj.type = "json";
        obj.value = JsonOutput.toJson(object);
        return obj;
    }

    PGobject toJsonb(String string) {
        if (string == null) return null;

        var obj = new PGobject();
        obj.type = "json";
        obj.value = string;
        return obj;
    }


    PGobject toJsonb(Instance instance) {
        if (instance == null) return null;

        var s = ((DbEngine) this.classManager).dumpJson(instance);
        var obj = new PGobject();
        obj.type = "json";
        obj.value = s
        return obj;
    }

    PGobject[] toJsonbArray(Object[] objects) {        // needn't connection.createArrayOf
        if (objects == null) return null;
        return objects.collect {toJsonb(it)}.toArray(PGobject[]::new);
    }


    void saveAgoInstance(Instance instance) {
        var slots = instance.slots as JsonRefSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(instance.parentScope);
        var creator = ObjectRefOwner.extractCreator(instance);

        if (logger.isDebugEnabled()) logger.debug("INSERT Instance " + slots.objectRef)

        // always put some payload for NativeInstance
        Object payload;
        if (instance instanceof NativeInstance && instance.nativePayload != null) {
            payload = toJsonb(instance.nativePayload)
        } else {
            payload = null;
        }

        sql.executeInsert("""INSERT INTO ago_instance
                                    (id, application, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, slots, payload)
                                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                [slots.objectRef.setObjectRef() as Object, applicationId,
                 instance.agoClass.fullname, parentScope?.id(), parentScope?.className(),
                 creator?.id(), creator?.className(),
                 toJsonb((classManager as DbEngine).jsonStringifySlots(instance)),
                 payload
                ]
        )
    }

    void saveRunSpace(SavableRunSpace runSpace){
        sql.executeInsert(toMap(runSpace),
            """insert into ago_runspace (
                    id, application, native_host_class, curr_frame_table, curr_frame_id, result_slots, running_state, exception_id, pausing_parents, forked_runspaces, parent_runspace
                )
                values (
                    :id,:application,:native_host_class,:curr_frame_table,:curr_frame_id,:result_slots,:running_state,:exception_id,:pausing_parents,:forked_runspaces,:parent_runspace
                )
                """);
    }

    void updateRunSpace(SavableRunSpace runSpace) {
        sql.executeUpdate(toUpdateMap(runSpace),
            """UPDATE ago_runspace
                SET
                    curr_frame_table = :curr_frame_table,
                    curr_frame_id = :curr_frame_id,
                    result_slots = :result_slots,
                    running_state = :running_state,
                    exception_id = :exception_id,
                    pausing_parents = :pausing_parents,
                    forked_runspaces = :forked_runspaces
                WHERE id = :id""")
    }

    Map<String, Object> toMap(SavableRunSpace runSpace){
        DbEngine rdbEngine = this.classManager as DbEngine
        ObjectRef currFrameRef = ObjectRefOwner.extractObjectRef(runSpace.getCurrentCallFrame());
        return [
                "id"               : (Object) runSpace.id,
                "application"      : applicationId,
                "native_host_class": runSpace.getRunSpaceHost().getClass().getName(),
                "curr_frame_table" : currFrameRef?.className(),
                "curr_frame_id"    : currFrameRef?.id(),
                "result_slots"     : toJsonb(rdbEngine.dumpJson(runSpace.resultSlots)),
                "running_state"    : runSpace.runningState,
                "exception_id"     : ObjectRefOwner.extractObjectRef(runSpace.getException())?.id(),
                "pausing_parents"  : runSpace.pausingParents.collect { ((TaskRunSpace) it).id }.toArray(new Long[0]),
                "forked_runspaces" : runSpace.forkedSpaces.collect { ((TaskRunSpace) it).id }.toArray(new Long[0]), "parent": ((TaskRunSpace) runSpace.getParent())?.id
        ];      // UnhandledException
    }

    Map<String, Object> toUpdateMap(SavableRunSpace runSpace) {
        DbEngine rdbEngine = this.classManager as DbEngine

        ObjectRef currFrameRef = ObjectRefOwner.extractObjectRef(runSpace.getCurrentCallFrame());
        return [
                "id"               : (Object) runSpace.id,
                "curr_frame_table" : currFrameRef?.className(),
                "curr_frame_id"    : currFrameRef?.id(),
                "result_slots"     : toJsonb(rdbEngine.dumpJson(runSpace.resultSlots)),
                "running_state"    : runSpace.runningState,
                "exception_id"     : ObjectRefOwner.extractObjectRef(runSpace.getException())?.id(),
                "pausing_parents"  : runSpace.pausingParents.collect { ((SavableRunSpace) it).id}.toArray(new Long[0]),
                "forked_runspaces" : runSpace.forkedSpaces.collect { ((SavableRunSpace) it).id}.toArray(new Long[0]),
        ];
    }

    private ResultSlots parseResultSlots(PGobject json) {
        return ((DbEngine)agoEngine).getDumpingObjectMapper().readValue(json.value, ResultSlots);
    }


    @Override
    String tableName(AgoClass agoClass) {
        if(agoClass instanceof MetaClass ){
            if(agoClass.getName() == "<Meta>"){
                throw new IllegalArgumentException("<Meta> shouldn't save");
            }
            var m = agoClass as MetaClass
            if(m.getInstanceClass() instanceof AgoFunction){
                return "ago_function";
            } else {
                return "ago_class";
            }
        } else if(agoClass instanceof AgoFunction){
            return "ago_frame"
        } else {
            return "ago_instance"
        }
    }

    String tableName(String className) {
        return tableName(classManager.getClass(className))
    }

    public static String[] loadPgStringArray(PgArray array) throws SQLException {
        if (array == null) return null;
        return (String[]) array.getArray();
    }

    public static Map<String, Object> loadPgJsonAsMap(PGobject json) throws SQLException {
        if (json == null) return null;
        String s = json.getValue();
        return (Map<String, Object>) new JsonSlurper().parseText(s);
    }

    public static Map<String, Object>[] loadPgJsonArrayAsList(PgArray array) throws SQLException {
        if (array == null) return null;
        String[] items = (String[]) array.getArray();
        Map<String, Object>[] result = new Map[items.length];
        for (int i = 0; i < items.length; i++) {
            String s = items[i];
            result[i] = (Map<String, Object>) new JsonSlurper().parseText(s);
        }
        return result;
    }

    static int[] loadPgIntArray(PgArray array) throws SQLException {
        if (array == null) return null;
        Integer[] integers = (Integer[]) array.getArray();
        int[] r = new int[integers.length];
        for (int i = 0; i < integers.length; i++) {
            Integer integer = integers[i];
            r[i] = integer;
        }
        return r;
    }

    static long[] loadPgLongArray(PgArray array) throws SQLException {
        if (array == null) return null;
        Long[] integers = (Long[]) array.getArray();
        long[] r = new long[integers.length];
        for (int i = 0; i < integers.length; i++) {
            Long integer = integers[i];
            r[i] = integer;
        }
        return r;
    }

    @Override
    List<RunSpaceDesc> loadResumableRunSpaces() {
        var rows = this.sql.rows("SELECT * FROM ago_runspace WHERE application=? AND running_state < 16", [applicationId as Object])
        LongObjectHashMap<RunSpaceDesc> runspaceDescById = new LongObjectHashMap<>()

        List<RunSpaceDesc> ls = new ArrayList<>(rows.size())
        for(var row : rows){
            ls.add(new RunSpaceDesc().with {
                it.id = row["id"] as Long
                it.runSpaceHostClass = row['native_host_class'] as String
                it.currFrame = row['curr_frame_id'] != null ? new ObjectRef(row['curr_frame_table'] as String, row['curr_frame_id'] as Long) : null
                it.resultSlots =  parseResultSlots(row['result_slots'] as PGobject)
                it.runningState = (byte) (row['running_state'] as int)
                it.exception = row['exception_id'] == null ? null : new ObjectRef("ago_instance", row['exception_id'] as Long);

                runspaceDescById[it.id] = it

                it
            })
        }

        for(var row : rows){
            var r = runspaceDescById[row['id'] as Long]
            r.pausingParents = row['pausing_parents'] == null ? null : loadPgLongArray(row['pausing_parents'] as PgArray).collect { runspaceDescById[it as Long] }.toList()

            r.forkedRunSpaces = null
            var forkedRunspaces = row['forked_runspaces']
            if (forkedRunspaces != null) {
                var ids = loadPgLongArray(forkedRunspaces as PgArray)
                var rs = ids.collect {
                    runspaceDescById[it as Long]
                }.toList()
                r.forkedRunSpaces = rs
            }

            r.parentRunSpace = row['parent_runspace'] == null ? null : runspaceDescById[row['parent_runspace'] as Long]
        }

        return ls
    }


    Instance restoreInstance(ObjectRef objectRef){
        if(objectRef == null) return null;

        return objectReferenceInstancesPool.computeIfAbsent(objectRef, {
            AgoClass agoClass = classManager.getClass(objectRef.className())
            ObjectRefObject r;
            if (agoClass instanceof AgoFunction) {
                r = new ObjectRefCallFrame(agoClass, objectRef, this, RowState.Unchanged)
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
    protected void saveInstance(@Nonnull Connection conn, Instance<?> instance, Set<Instance<?>> saved) {
        if (boxTypes.isBoxType((AgoClass)instance.getAgoClass()) || instance instanceof AgoArrayInstance)
            return;

        if(instance instanceof ObjectRefObject){
            if(instance.getDeferencedInstance() != null){
                var d = instance.getDeferencedInstance()
                if(!saved.contains(d)) {
                    saveInstance(conn, d, saved)
                }
            }
            return;     // for folded Instance, it must be already saved or never touch its Slots, needn't save
        } else if(instance instanceof ExpandableObject){
            if(instance.isExpanded()){
                saved.add((Instance) instance);
                saveInstance(conn, instance.getExpandedInstance(), saved)
            }
            return      // ignore folded Instance
        }
        if(logger.isDebugEnabled()) logger.debug("save instance " + instance)
        super.saveInstance(conn, instance, saved)
        if(instance instanceof DeferenceObject){
            if(instance.isSaveRequired()){
                if(instance instanceof CallFrame) {
                    updateCallFrameRunningState(conn, instance, (byte) -1)
                } else {
                    this.update(conn, (Instance)instance, (DbSlots)null, instance.getAgoClass() as AgoClass);
                }
            }
        }
    }

    @Override
    void insert(@Nonnull Connection conn, Instance<?> instance, DbSlots rdbSlots, AgoClass agoClass) {
        if (instance instanceof AgoFrame) {
            saveAgoFrame(conn, instance)
        } else if(instance instanceof NativeFrame){
            saveNativeFrame(conn, instance)
        } else if (instance instanceof AgoFunction) {
            saveAgoFunction(conn, (AgoFunction) instance)
        } else if (instance instanceof AgoClass) {
            saveAgoClass(conn, (AgoClass) instance)
        } else {
            saveAgoInstance(instance)
        }

        if (instance instanceof DeferenceObject) {
            instance.markSaved()
        }
    }

    @Override
    void update(@Nonnull Connection conn, Instance<?> instance, DbSlots rdbSlots, AgoClass agoClass) {
        if (instance instanceof CallFrameWithRunningState) {
            updateCallFrameRunningState(conn, instance.unwrap(), instance.getRunningState());
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
            if (instance.nativePayload != null) {
                arguments['payload'] = toJsonb(instance.nativePayload)
            } else {
                arguments['payload'] = null
            }
        }

        String sql
        if(instance instanceof CallFrame){
            arguments["runspace"] = (instance.runSpace as SavableRunSpace)?.id
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

        var sqlx = new Sql(conn);
        sqlx.executeUpdate(arguments, sql)

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
            var row = new Sql(connection).firstRow("SELECT * FROM " + tableName(objectRef.className()) + " WHERE id = ?", [objectRef.id() as Object])
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
                PersistentDbEngine engine = this.classManager as PersistentDbEngine;
                MutableObject<Instance> boxInstanceScope = new MutableObject<>();
                var frame = engine.createFunctionInstance(agoClass as AgoFunction, parentScope, null, slots -> {
                    getAgoEngine().jsonDeserializeSlots(slots, objectRef.id(), agoClass, (String) ((row['slots'] as PGobject).value), boxInstanceScope);
                })
                if (frame instanceof DeferenceAgoFrame) {
                    frame.pc = row['pc'] as int
                    frame.getDeferenceFrameState().entrance = row['is_entrance']
                    frame.getDeferenceFrameState().asyncEntrance = row['is_async_entrance']
                    frame.getDeferenceObjectState().setCreator(creator)
                } else if(frame instanceof DeferenceNativeFrame){        //DeferenceNativeFrame
                    if(row['payload']) frame.setNativePayload(new JsonSlurper().parseText(((PGobject)row['payload']).value))
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
                    PersistentDbEngine persistentRdbEngine = (PersistentDbEngine) this.classManager;
                    frame.runSpace = persistentRdbEngine.getRunSpace(row['runspace'] as Long)
                }
                frame.setCaller(caller)

                if (frame instanceof DeferenceObject) frame.markSaved()

                return frame
            } else {
                LazyJsonAgoEngine engine = this.classManager as LazyJsonAgoEngine;
                LazyJsonRefSlots slots = agoClass.createSlots() as LazyJsonRefSlots
                getAgoEngine().jsonDeserializeSlots(slots, objectRef.id(), agoClass, (String) ((row['slots'] as PGobject).value), null);

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

                if (logger.isDebugEnabled()) logger.debug("%s deference to %s".formatted(objectRef, inst))
                return inst
            }
        }
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
            var slots = scoped.getSlots() as DbSlots;
            if(baseClass.getAgoClass() != agoEngine.getTheMeta()) {
                getAgoEngine().jsonDeserializeSlots(slots, id, baseClass.getAgoClass(), row['slots'] as String, null)
            } else {
                slots.setId(id)
            }
            slots.setRowState(RowState.Unchanged);
            return scoped;
        }
        return baseClass
    }
}
