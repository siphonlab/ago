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
import org.apache.commons.lang3.mutable.MutableObject
import org.postgresql.jdbc.PgArray
import org.postgresql.util.PGobject
import org.siphonlab.ago.*
import org.siphonlab.ago.native_.AgoNativeFunction
import org.siphonlab.ago.native_.NativeFrame
import org.siphonlab.ago.native_.NativeInstance
import org.siphonlab.ago.runtime.db.DbSlotsCreator
import org.siphonlab.ago.runtime.db.IdGenerator
import org.siphonlab.ago.runtime.db.WorkflowAdapter
import org.siphonlab.ago.runtime.db.CallFrameWithRunningState
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner
import org.siphonlab.ago.runtime.db.WorkflowRunSpace
import org.siphonlab.ago.runtime.rdb.DbEngine
import org.siphonlab.ago.runtime.db.DbSlots
import org.siphonlab.ago.runtime.db.ObjectRef
import org.siphonlab.ago.runtime.rdb.RdbAdapter
import org.siphonlab.ago.runtime.rdb.RowState
import org.siphonlab.ago.runtime.rdb.RunSpaceDesc
import org.siphonlab.ago.runtime.db.lazy.DeferenceAgoFrame
import org.siphonlab.ago.runtime.db.lazy.DeferenceInstance
import org.siphonlab.ago.runtime.db.lazy.DeferenceNativeFrame
import org.siphonlab.ago.runtime.db.lazy.DeferenceNativeInstance
import org.siphonlab.ago.runtime.db.lazy.DeferenceObject
import org.siphonlab.ago.runtime.db.lazy.DereferenceAdapter
import org.siphonlab.ago.runtime.db.lazy.ObjectRefCallFrame
import org.siphonlab.ago.runtime.db.lazy.ObjectRefInstance
import org.siphonlab.ago.runtime.db.lazy.ObjectRefObject
import org.siphonlab.ago.runtime.rdb.TransactionBoundDataSource
import org.siphonlab.ago.runtime.db.task.WorkflowEngine
import org.siphonlab.ago.runtime.rdb.pg.PGTypeMapping
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.sql.DataSource
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap

@CompileStatic
public class JsonPGAdapter<Id> extends RdbAdapter<Id> implements DereferenceAdapter<Id>, WorkflowAdapter<Id>{

    private  final Logger logger = LoggerFactory.getLogger(JsonPGAdapter)

    protected Sql sql
    protected int applicationId

    protected Map<ObjectRef, Instance> objectReferenceInstancesPool = new ConcurrentHashMap<>();

    JsonPGAdapter(ClassManager classManager, TypeCode idType, IdGenerator<Id> idGenerator, BoxTypes boxTypes, DataSource dataSource, int applicationId) {
        super(classManager, idType, idGenerator, boxTypes, new PGTypeMapping(boxTypes), dataSource)
        this.applicationId = applicationId
        this.sql = new Sql(dataSource);
    }

    static Map<String, Object> toMap(TypeCode typeCode) {
        return typeCode.value < TypeCode.GENERIC_TYPE_START ? ["code": typeCode.value as Object] : ["code": typeCode.value as Object, "description": typeCode.toShortString()];
    }

    void insertAgoFrame(AgoFrame agoFrame) {
        var slots = agoFrame.slots as DbSlots<Id>;
        var parentScope = ObjectRefOwner.extractObjectRef(agoFrame.parentScope);
        ObjectRef creatorObjectRef = ObjectRefOwner.extractObjectRef(agoFrame);

//        var defaultSlots = defaultSlots(agoFrame.agoClass, slots.jsonSlotMapper.jsonFiledNames)

        def params = [
                id                : slots.objectRef.id() as Object,
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
                runspace          : (agoFrame.getRunSpace() as WorkflowRunSpace)?.id,
                slots             : toJsonb(getAgoEngine().jsonStringifySlots(agoFrame))
        ]

        sql.executeInsert("""
            INSERT INTO ago_frame
                (id, application, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, 
                 caller_id, caller_class, pc, state, suspended, exception_id, exception_class, runspace, slots)
            VALUES (:id, :application, :ago_class, :parent_scope_id, :parent_scope_class, :creator_id, :creator_class, 
                    :caller_id, :caller_class, :pc, :state, :suspended, :exception_id, :exception_class, :runspace, :slots);
        """, params)

    }

    void insertNativeFrame(NativeFrame agoFrame) {
        var slots = agoFrame.slots as DbSlots<Id>;
        var parentScope = ObjectRefOwner.extractObjectRef(agoFrame.parentScope);

        def params = [
                id                : slots.objectRef.id() as Object,
                application       : applicationId,
                ago_class         : agoFrame.agoClass.fullname,
                parent_scope_id   : parentScope?.id(),
                parent_scope_class: parentScope?.className(),
                pc                : 0,
                state             : RunSpace.RunningState.PENDING,
                suspended         : false,
                exception_id      : null,
                exception_class   : null,
                runspace          : (agoFrame.getRunSpace() as WorkflowRunSpace)?.id,
                slots             : toJsonb((classManager as DbEngine).jsonStringifySlots(agoFrame))
        ]

        sql.executeInsert("""INSERT INTO ago_frame
                (id, application, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, 
                 caller_id, caller_class, pc, state, suspended, exception_id, exception_class, runspace, slots)
            VALUES (:id, :application, :ago_class, :parent_scope_id, :parent_scope_class, :creator_id, :creator_class, 
                    :caller_id, :caller_class, :pc, :state, :suspended, :exception_id, :exception_class, :runspace, :slots)""", params)
    }

    int updateRow(String table, Map<String, Object> data, String pkName = 'id') {
        def pkValue = data[pkName]
        if (pkValue == null)
            throw new IllegalArgumentException("Missing primary key '${pkName}'")

        def columns = data.findAll { k, v -> k != pkName }
        if (columns.isEmpty())
            return 0

        def setClause = columns.collect { k, v -> "$k = :$k" }.join(', ')
        def sqlText = "UPDATE $table SET $setClause WHERE $pkName = :$pkName"

        return sql.executeUpdate(data, sqlText)
    }


    void updateCallFrameRunningState(CallFrame callFrame, byte runningState, int pc = -1) {
        var objectRef = ObjectRefOwner.extractObjectRef(callFrame)
        logger.info("UPDATE " + objectRef)
        var map = [
                "id"       : objectRef.id() as Object,
                "suspended": callFrame.suspended,
                "runspace" : (callFrame.runSpace as WorkflowRunSpace)?.id,
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
            var slots = callFrame.slots as DbSlots<Id>
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
        updateRow("ago_frame", map, "id")
        if(callFrame instanceof DeferenceObject){
            callFrame.markSaved()
        }
    }

    void saveAgoClass(AgoClass agoClass) {
        var slots = agoClass.slots as DbSlots<Id>;
        if(logger.isDebugEnabled()) logger.debug("INSERT CLASS " + slots.objectRef)

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

    @Override
    void updateCallFrameRunningState(CallFrameWithRunningState<?> callFrame) {
        updateCallFrameRunningState(callFrame.unwrap(), callFrame.getRunningState(), callFrame.pc)
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

    void saveAgoFunction(AgoFunction agoFunction) {
        var slots = agoFunction.slots as DbSlots;
        if (logger.isDebugEnabled()) logger.debug("INSERT Function " + slots.objectRef)

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
        var slots = agoClass.slots as DbSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(agoClass.parentScope);

        return [
                id                      : slots.objectRef.id() as Object,
                application             : applicationId,
                class_id                : agoClass.classId,
                ago_class               : agoClass.agoClass.fullname,
                parent_scope_id         : parentScope?.id(),
                parent_scope_class      : parentScope?.className(),

//                creator_id              : creator?.id(),
//                creator_class           : creator?.className(),
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

    WorkflowEngine<Id> getAgoEngine(){
        return this.classManager as WorkflowEngine<Id>;
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
        var slots = instance.slots as DbSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(instance.parentScope);

        if (logger.isDebugEnabled()) logger.debug("INSERT Instance " + slots.objectRef)

        // always put some payload for NativeInstance
        Object payload;
        if (instance instanceof NativeInstance && instance.nativePayload != null) {
            payload = toJsonb(instance.nativePayload)
        } else {
            payload = null;
        }

        sql.executeInsert("""INSERT INTO ago_instance
                                    (id, application, ago_class, parent_scope_id, parent_scope_class, slots, payload)
                                VALUES(?, ?, ?, ?, ?, ?, ?)""",
                [slots.objectRef.id() as Object, applicationId,
                 instance.agoClass.fullname, parentScope?.id(), parentScope?.className(),
                 toJsonb((classManager as DbEngine).jsonStringifySlots(instance)),
                 payload
                ]
        )
    }

    void insertRunSpace(WorkflowRunSpace runSpace){
        sql.executeInsert(toMap(runSpace, runSpace.getCurrentCallFrame()),
            """insert into ago_runspace (
                    id, application, native_host_class, curr_frame_table, curr_frame_id, result_slots, running_state, exception_id, pausing_parents, forked_runspaces, parent_runspace
                )
                values (
                    :id,:application,:native_host_class,:curr_frame_table,:curr_frame_id,:result_slots,:running_state,:exception_id,:pausing_parents,:forked_runspaces,:parent_runspace
                )
                """);
    }

    void updateRunSpace(WorkflowRunSpace runSpace) {
        sql.executeUpdate(toUpdateMap(runSpace, runSpace.getCurrentCallFrame()),
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

    @Override
    void updateRunSpace(WorkflowRunSpace<?> runSpace, CallFrame<?> currentCallFrame) {
        sql.executeUpdate(toUpdateMap(runSpace, currentCallFrame),
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

    Map<String, Object> toMap(WorkflowRunSpace runSpace, CallFrame<?> callFrame){
        DbEngine rdbEngine = this.classManager as DbEngine
        ObjectRef currFrameRef = ObjectRefOwner.extractObjectRef(callFrame);
        return [
                "id"               : (Object) runSpace.id,
                "application"      : applicationId,
                "native_host_class": runSpace.getRunSpaceHost().getClass().getName(),
                "curr_frame_table" : currFrameRef?.className(),
                "curr_frame_id"    : currFrameRef?.id(),
                "result_slots"     : toJsonb(rdbEngine.dumpJson(runSpace.resultSlots)),
                "running_state"    : runSpace.runningState,
                "exception_id"     : ObjectRefOwner.extractObjectRef(runSpace.getException())?.id(),
                "pausing_parents"  : runSpace.pausingParents.collect { ((WorkflowRunSpace) it).id }.toArray(new Long[0]),
                "forked_runspaces" : runSpace.forkedSpaces.collect { ((WorkflowRunSpace) it).id }.toArray(new Long[0]), "parent": ((WorkflowRunSpace) runSpace.getParent())?.id
        ];      // UnhandledException
    }

    Map<String, Object> toUpdateMap(WorkflowRunSpace runSpace, CallFrame<?> callFrame) {
        DbEngine rdbEngine = this.classManager as DbEngine

        ObjectRef currFrameRef = ObjectRefOwner.extractObjectRef(callFrame);
        return [
                "id"               : (Object) runSpace.id,
                "curr_frame_table" : currFrameRef?.className(),
                "curr_frame_id"    : currFrameRef?.id(),
                "result_slots"     : toJsonb(rdbEngine.dumpJson(runSpace.resultSlots)),
                "running_state"    : runSpace.runningState,
                "exception_id"     : ObjectRefOwner.extractObjectRef(runSpace.getException())?.id(),
                "pausing_parents"  : runSpace.pausingParents.collect { ((WorkflowRunSpace) it).id}.toArray(new Long[0]),
                "forked_runspaces" : runSpace.forkedSpaces.collect { ((WorkflowRunSpace) it).id}.toArray(new Long[0]),
        ];
    }

    private ResultSlots parseResultSlots(PGobject json) {
        return ((DbEngine)agoEngine).getDumpingObjectMapper().readValue(json.value, ResultSlots);
    }


    @Override
    String tableName(AgoClass agoClass) {
//        if(isEntityClass(agoClass)) {
//            return super.tableName(agoClass)
//        }

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
    List<RunSpaceDesc<Id>> loadResumableRunSpaces() {
        var rows = this.sql.rows("SELECT * FROM ago_runspace WHERE application=? AND running_state < 16", [applicationId as Id])
        Map<Id, RunSpaceDesc<Id>> runspaceDescById = new HashMap<>();

        List<RunSpaceDesc<Id>> ls = new ArrayList<>(rows.size())
        for(var row : rows){
            ls.add(new RunSpaceDesc<Id>().with {
                it.id = row["id"] as Id
                it.runSpaceHostClass = row['native_host_class'] as String
                it.currFrame = row['curr_frame_id'] != null ? ObjectRef.create(row['curr_frame_table'] as String, row['curr_frame_id'] as Id) : null
                it.resultSlots =  parseResultSlots(row['result_slots'] as PGobject)
                it.runningState = (byte) (row['running_state'] as int)
                it.exception = row['exception_id'] == null ? null : ObjectRef.create("ago_instance", row['exception_id'] as Id);

                runspaceDescById[it.id as Id] = it

                it
            })
        }

        for(var row : rows){
            var r = runspaceDescById[row['id'] as Id]
            //TODO support string id
            r.pausingParents = row['pausing_parents'] == null ? null : loadPgLongArray(row['pausing_parents'] as PgArray).collect { runspaceDescById[it as Id] }.toList()

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

    @Override
    Instance<?> getById(ObjectRef<Id> objectRef) {
        if(objectRef == null) return null;

        return objectReferenceInstancesPool.computeIfAbsent(objectRef, {
            return dereference(it)
        }) as Instance;

    }

    @Override
    protected void saveInstance(Instance<?> instance, Set<Instance<?>> saved) {
//        if (boxTypes.isBoxType((AgoClass)instance.getAgoClass()) || instance instanceof AgoArrayInstance)
//            return;

        if(instance instanceof ObjectRefObject){
            if(instance.getDeferencedInstance() != null){
                var d = instance.getDeferencedInstance()
                if(!saved.contains(d)) {
                    saveInstance(d, saved)
                }
            }
            return;     // for folded Instance, it must be already saved or never touch its Slots, needn't save
        }
        if(logger.isDebugEnabled()) logger.debug("save instance " + instance)
        super.saveInstance(instance, saved)
        if(instance instanceof DeferenceObject){
            if(instance.isSaveRequired()){
                if(instance instanceof CallFrame) {
                    updateCallFrameRunningState(instance, (byte) -1)
                } else {
                    this.update((Instance)instance, (DbSlots)null, instance.getAgoClass() as AgoClass);
                }
            }
        }
    }

    @Override
    void insert(Instance<?> instance, DbSlots<Id> dbSlots, AgoClass agoClass) {
//        if(entityClass != agoClass && entityClass.isThatOrSuperOfThat(agoClass)) {
//            super.insert(instance, dbSlots, agoClass);
//            return;
//        }
        if (instance instanceof AgoFrame) {
            insertAgoFrame(instance)
        } else if(instance instanceof NativeFrame){
            insertNativeFrame(instance)
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
    void update(Instance<?> instance, DbSlots<Id> dbSlots, AgoClass agoClass) {
//        if(entityClass != agoClass && entityClass.isThatOrSuperOfThat(agoClass)) {
//            super.update(instance, dbSlots, agoClass);
//            return;
//        }

        if (instance instanceof CallFrameWithRunningState) {
            updateCallFrameRunningState(instance.unwrap(), instance.getRunningState());
            return
        }

        Map<String, Object> arguments = new HashMap<>();
        ObjectRef ref = ((DbSlots<Id>) dbSlots).objectRef
        logger.info("UPDATE " + ref)

        arguments["id"] = ref.id()
        if(dbSlots != null && dbSlots.getRowState() != RowState.Unchanged) {
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
            arguments["runspace"] = (instance.runSpace as WorkflowRunSpace)?.id
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
    Instance<?> dereference(ObjectRef<Id> objectRef) {
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

        var row = this.sql.firstRow("SELECT * FROM " + tableName(objectRef.className()) + " WHERE id = ?", [objectRef.id() as Object])
        String className = row["ago_class"]
        String parentScopeTable = row['parent_scope_class']
        Instance parentScope = null;
        if (parentScopeTable != null) {
            var parent_scope_id = row["parent_scope_id"] as Id
            parentScope = agoEngine.createObjectRefInstance(ObjectRef.create(parentScopeTable, parent_scope_id))
        }

        AgoClass agoClass = this.classManager.getClass(className);
        if (agoClass instanceof MetaClass) {
            return this.classManager.getClass(row["fullname"] as String);
        } else if (agoClass instanceof AgoFunction) {
            CallFrame caller;
            if (row["caller_id"] != null) {
                caller = (CallFrame) agoEngine.createObjectRefInstance(ObjectRef.create(row["caller_class"] as String, row["caller_id"] as Id))
            } else {
                caller = null
            }
            WorkflowEngine engine = this.classManager as WorkflowEngine;
            MutableObject<Instance> boxInstanceScope = new MutableObject<>();
            var frame = engine.createFunctionInstance(agoClass as AgoFunction, parentScope, null, objectRef, slots -> {
                getAgoEngine().jsonDeserializeSlots((DbSlots<Id>)slots, agoClass, (String) ((row['slots'] as PGobject).value), boxInstanceScope);
            })
            if (frame instanceof DeferenceAgoFrame) {
                frame.pc = row['pc'] as int
                frame.getDeferenceFrameState().entrance = row['is_entrance']
                frame.getDeferenceFrameState().asyncEntrance = row['is_async_entrance']
            } else if(frame instanceof DeferenceNativeFrame){        //DeferenceNativeFrame
                if(row['payload']) frame.setNativePayload(new JsonSlurper().parseText(((PGobject)row['payload']).value))
                frame.getDeferenceFrameState().entrance = row['is_entrance']
                frame.getDeferenceFrameState().asyncEntrance = row['is_async_entrance']
            } else {
                throw new RuntimeException("not deference type");
            }
            if(boxInstanceScope.get() != null){
                frame.setParentScope(boxInstanceScope.get())
            }
            if (logger.isDebugEnabled()) logger.debug("%s deference to %s".formatted(objectRef, frame))
            if(row['runspace']) {
                WorkflowEngine persistentRdbEngine = (WorkflowEngine) this.classManager;
                frame.runSpace = persistentRdbEngine.getRunSpace(row['runspace'] as Id)
            }
            frame.setCaller(caller)

            if (frame instanceof DeferenceObject) frame.markSaved()

            return frame
        } else {
            var engine = this.getAgoEngine();
            DbSlots<Id> slots = DbSlotsCreator<Id>.create(agoClass, ObjectRef.create(agoClass.fullname, objectRef.id())) as DbSlots<Id>
            getAgoEngine().jsonDeserializeSlots(slots, agoClass, (String) ((row['slots'] as PGobject).value), null);

            var inst = agoClass.isNative() ? new DeferenceNativeInstance(slots,agoClass, this) : new DeferenceInstance(slots, agoClass, this);
            inst.parentScope = parentScope
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

    public AgoClass loadScopedAgoClass(AgoClass baseClass, Id id) {
        var row = sql.firstRow("SELECT parent_scope_class, parent_scope_id, creator_class, creator_id, slots FROM ${baseClass instanceof AgoFunction ? "ago_function" : "ago_class"} WHERE id =?", [id])
        var parentScopeId = row["parent_scope_id"]
        if(parentScopeId != null) {
            Instance scope = agoEngine.createObjectRefInstance(ObjectRef.create((String) row["parent_scope_class"], (Id) parentScopeId));
            var scoped = baseClass.cloneWithScope(scope)
            var slots = scoped.getSlots() as DbSlots<Id>;
            slots.setObjectRef(ObjectRef.create(slots.getObjectRef().className(), id));
            if(baseClass.getAgoClass() != agoEngine.getTheMeta()) {
                getAgoEngine().jsonDeserializeSlots(slots, baseClass.getAgoClass(), row['slots'] as String, null)
            }
            slots.setRowState(RowState.Unchanged);
            return scoped;
        }
        return baseClass
    }

    void saveCallChainIncludeCurrent(CallFrame<?> currentFrame) {
        var transactionAdapter = this.beginTransaction();
        for(var c = currentFrame.getCaller(); c != null; c = c.getCaller()){
            var callFrameWithRunningState = new CallFrameWithRunningState(c, c.getRunSpace().getRunningState())
            transactionAdapter.saveInstance(callFrameWithRunningState);
        }
        transactionAdapter.saveInstance(currentFrame);
        transactionAdapter.updateRunSpace((WorkflowRunSpace<Id>) currentFrame.getRunSpace());
        try {
            transactionAdapter.commitTransaction();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    void saveFrameAndRunspace(CallFrame<?> frame) {
        if (frame == null) {
            return ;
        }

        if (frame instanceof ObjectRefCallFrame<?,?> objectRefCallFrame) {
            frame = objectRefCallFrame.deference();
        }

        var transactionAdapter = this.beginTransaction();
        transactionAdapter.saveInstance(frame);
        transactionAdapter.updateRunSpace((WorkflowRunSpace<?>) frame.getRunSpace(), frame);
        try {
            transactionAdapter.commitTransaction();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    JsonPGAdapter<Id> beginTransaction() {
        var adapter = new JsonPGAdapter<Id>(classManager, idType, idGenerator, boxTypes, new TransactionBoundDataSource(dataSource, true), applicationId);
        adapter.tablesByClass = this.tablesByClass;
        adapter.tablesByClassName = this.tablesByClassName;
        return adapter;
    }

}
