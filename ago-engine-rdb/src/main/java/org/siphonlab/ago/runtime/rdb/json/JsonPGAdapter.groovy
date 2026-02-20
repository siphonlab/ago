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
import org.agrona.collections.Int2ObjectHashMap
import org.agrona.concurrent.IdGenerator
import org.postgresql.jdbc.PgArray
import org.postgresql.util.PGobject
import org.siphonlab.ago.*
import org.siphonlab.ago.native_.AgoNativeFunction
import org.siphonlab.ago.native_.NativeFrame
import org.siphonlab.ago.native_.NativeInstance
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner
import org.siphonlab.ago.runtime.rdb.RdbAdapter
import org.siphonlab.ago.runtime.rdb.RdbRunSpace
import org.siphonlab.ago.runtime.rdb.RdbEngine
import org.siphonlab.ago.runtime.rdb.RdbSlots
import org.siphonlab.ago.runtime.rdb.RdbType
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.RowState
import org.siphonlab.ago.runtime.rdb.RunSpaceDesc
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceObject
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableCallFrame
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame
import org.siphonlab.ago.runtime.rdb.reactive.json.ReactiveJsonCallFrame
import org.siphonlab.ago.runtime.rdb.reactive.json.ReactiveJsonAgoEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException
import java.sql.Types

@CompileStatic
public abstract class JsonPGAdapter extends RdbAdapter {

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
    public RdbType idType() {
        return mapType(TypeCode.LONG, null);
    }

    @Override
    protected void initTypeMap(Int2ObjectHashMap<RdbType> typeMap, Map<AgoClass, RdbType> standardDbTypes, ClassManager rdbEngine) {
        typeMap.put(TypeCode.INT_VALUE, new RdbType(TypeCode.INT, Types.INTEGER, "integer"));
        typeMap.put(TypeCode.LONG_VALUE, new RdbType(TypeCode.LONG, Types.BIGINT, "bigint"));
        typeMap.put(TypeCode.FLOAT_VALUE, new RdbType(TypeCode.FLOAT, Types.FLOAT, "float"));
        typeMap.put(TypeCode.DOUBLE_VALUE, new RdbType(TypeCode.DOUBLE, Types.DOUBLE, "double"));
        typeMap.put(TypeCode.BOOLEAN_VALUE, new RdbType(TypeCode.BOOLEAN, Types.BOOLEAN, "boolean"));
        typeMap.put(TypeCode.STRING_VALUE, new RdbType(TypeCode.STRING, Types.VARCHAR, "varchar"));
        typeMap.put(TypeCode.BYTE_VALUE, new RdbType(TypeCode.BYTE, Types.TINYINT, "smallint"));    // no byte type in PG
        typeMap.put(TypeCode.SHORT_VALUE, new RdbType(TypeCode.SHORT, Types.SMALLINT, "smallint"));
        typeMap.put(TypeCode.CHAR_VALUE, new RdbType(TypeCode.CHAR, Types.CHAR, "char"));
        typeMap.put(TypeCode.CLASS_REF_VALUE, new RdbType(TypeCode.CLASS_REF, Types.VARCHAR, "varchar(1024)"));

        typeMap.put(TypeCode.OBJECT_VALUE, new RdbType(TypeCode.OBJECT, Types.JAVA_OBJECT, "jsonb"));

        AgoClass agoClass = classManager.getClass("VarChar");
        if (agoClass != null) standardDbTypes.put(agoClass, new RdbType(TypeCode.STRING, Types.VARCHAR, "varchar", agoClass));

        agoClass = classManager.getClass("BigInt");
        if (agoClass != null)
            standardDbTypes.put(agoClass, new RdbType(TypeCode.LONG, Types.BIGINT, "bigint", agoClass));
    }

    @Override
    void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource)
        this.sql = new Sql(this.getDataSource());
    }

    Instance restoreInstance(Connection connection, ObjectRef objectRef){
        // TODO improve AgoClass performance, let's don't load row
        var row = new Sql(connection).firstRow("SELECT * FROM " + getTableName(objectRef.className()) + " WHERE id = ?", [objectRef.id() as Object])
        String className = row["ago_class"]
        String parentScopeTable = row['parent_scope_class']
        Instance parentScope = null;
        if(parentScopeTable != null){
            long parent_scope_id = row["parent_scope_id"] as long
            parentScope = restoreInstance(connection, new ObjectRef(parentScopeTable, parent_scope_id))
        }

        AgoClass agoClass = this.getClassByName(className);
        if(agoClass instanceof MetaClass){
            return this.getClassByName(row["fullname"] as String);
        } else if(agoClass instanceof AgoFunction){
            // TODO new NativeFrame...
            var frame = new ReactiveJsonCallFrame(restoreSlots(objectRef, row, agoClass), agoClass as AgoFunction, this.classManager as ReactiveJsonAgoEngine)
            frame.parentScope = parentScope
            frame.pc = row['pc'] as int
            frame.suspended = row['suspended']
            return frame
        } else {
            var slots = restoreSlots(objectRef, row, agoClass)
            var inst = new Instance(slots, agoClass)
            inst.parentScope = parentScope;
            return inst
        }
    }

    abstract Slots restoreSlots(ObjectRef objectRef, Map<String, Object> dbRow, AgoClass agoClass);
//    {
//        return null;        //TODO restoreInstance need this, however, JsonRefSlots need SlotsAdapter
////        return new JsonRefSlots(dbRef, this.getSlotsAdapter(), agoClass.getSlotDefs());       // don't restore value, only a ref
////        for(var slotDef : agoClass.getSlotDefs()){
////            var v = map[slots.getFieldName(slotDef.index)]
////            switch (slotDef.typeCode.value){
////                case TypeCode.INT_VALUE -> slots.setInt(slotDef.index, v as int)
////                case TypeCode.LONG_VALUE -> slots.setLong(slotDef.index, v as long)
////                case TypeCode.SHORT_VALUE -> slots.setShort(slotDef.index, v as short)
////                case TypeCode.BYTE_VALUE -> slots.setByte(slotDef.index, v as byte)
////                case TypeCode.FLOAT_VALUE -> slots.setFloat(slotDef.index, v as float)
////                case TypeCode.DOUBLE_VALUE -> slots.setDouble(slotDef.index, v as double)
////                case TypeCode.BOOLEAN_VALUE -> slots.setBoolean(slotDef.index, v as boolean)
////                case TypeCode.STRING_VALUE -> slots.setString(slotDef.index, v as String)
////                case TypeCode.CHAR_VALUE -> slots.setChar(slotDef.index, v as char)
////                case TypeCode.CLASS_REF_VALUE -> slots.setClassRef(slotDef.index, this.getClassByName(v as String).classId)
////                case TypeCode.OBJECT_VALUE -> restoreInstance()
////            }
////        }
//    }

    void saveAgoFrame(AgoFrame agoFrame) {
        var slots = agoFrame.slots as JsonRefSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(agoFrame.parentScope);
        ObjectRef creatorObjectRef = ObjectRefOwner.extractCreator(agoFrame);

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
                runspace          : (agoFrame.getRunSpace() as RdbRunSpace)?.id,
                slots             : toJsonb((classManager as RdbEngine).jsonStringifySlots(agoFrame))
        ]

        sql.executeInsert("""
            INSERT INTO ago_frame
                (id, application, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, 
                 caller_id, caller_class, pc, state, suspended, exception_id, exception_class, runspace, slots)
            VALUES (:id, :application, :ago_class, :parent_scope_id, :parent_scope_class, :creator_id, :creator_class, 
                    :caller_id, :caller_class, :pc, :state, :suspended, :exception_id, :exception_class, :runspace, :slots);
        """, params)

    }

    void saveNativeFrame(NativeFrame agoFrame) {
        var slots = agoFrame.slots as JsonRefSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(agoFrame.parentScope);
        ObjectRef creatorObjectRef = ObjectRefOwner.extractCreator(agoFrame);

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
                runspace          : (agoFrame.getRunSpace() as RdbRunSpace)?.id,
                slots             : toJsonb((classManager as RdbEngine).jsonStringifySlots(agoFrame))
        ]

        sql.executeInsert("""INSERT INTO ago_frame
                (id, application, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, 
                 caller_id, caller_class, pc, state, suspended, exception_id, exception_class, runspace, slots)
            VALUES (:id, :application, :ago_class, :parent_scope_id, :parent_scope_class, :creator_id, :creator_class, 
                    :caller_id, :caller_class, :pc, :state, :suspended, :exception_id, :exception_class, :runspace, :slots)""", params)
    }

    def updateRow(String table, Map<String, Object> data, String pkName = 'id') {
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


    void updateCallFrameRunningState(CallFrame callFrame, byte runningState) {
        var objectRef = ObjectRefOwner.extractObjectRef(callFrame);
        logger.info("UPDATE " + objectRef)
        var map = [
                "id"       : objectRef.id() as Object,
                "suspended": callFrame.suspended,
                "runspace" : (callFrame.runSpace as RdbRunSpace)?.id,
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
            }
            else if (callFrame instanceof ExpandableCallFrame) {
                callFrame = callFrame.getExpandedInstance();
            } else {
                break
            }
        }
        if(saveSlots){
            var slots = callFrame.slots as JsonRefSlots
            if(slots instanceof RdbSlots && (slots.rowState == RowState.Saving || slots.rowState == RowState.Modified))
                map['slots'] = toJsonb(this.getAgoEngine().jsonStringifySlots(callFrame))
        }
        ObjectRef callerObjectRef = ObjectRefOwner.extractObjectRef(callFrame.caller);
        if(callerObjectRef){
            map['caller_id'] = callerObjectRef.id()
            map['caller_class'] = callerObjectRef.className()
        }

        if(callFrame instanceof AgoFrame) {
            map["pc"] = callFrame.pc
        } else if(callFrame instanceof NativeFrame) {
            map["payload"] = callFrame.payload ? toJsonb(callFrame.payload) : null;
        } else {
            throw new UnsupportedOperationException("unsupported frame type " + callFrame)
        }
        updateRow("ago_frame", map, "id")
        if(callFrame instanceof DeferenceObject){
            callFrame.markSaved()
        }
    }

    void saveAgoClass(AgoClass agoClass) {
        var slots = agoClass.slots as JsonRefSlots;
        if(logger.isDebugEnabled()) logger.debug("INSERT CLASS " + slots.objectRef.id())

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
        if(concreteTypeInfo instanceof ArrayInfo){
            return ["type": "ArrayInfo", "elementType" : toMap(concreteTypeInfo.elementType)]
        } else if(concreteTypeInfo instanceof GenericArgumentsInfo){
            return ["type": "GenericArgumentsInfo" as Object, "templateClass": concreteTypeInfo.templateClass.fullname, "arguments": concreteTypeInfo.arguments.collect({toMap(it as TypeInfo)}).toArray()]
        } else if(concreteTypeInfo instanceof GenericTypeParametersInfo){
            return ["type": "GenericTypeParametersInfo" as Object, "genericParameters": concreteTypeInfo.genericParameters.collect { toMap(it as TypeInfo) }.toArray()]
        } else if(concreteTypeInfo instanceof ParameterizedClassInfo){
            return ["type": "ParameterizedClassInfo" as Object, "parameterizedBaseClass": concreteTypeInfo.parameterizedBaseClass.fullname, "parameterizedConstructor": concreteTypeInfo.parameterizedConstructor.fullname, "arguments": concreteTypeInfo.arguments]
        } else {
            throw new IllegalArgumentException("unknown concrete type " + concreteTypeInfo);
        }
    }

    static Map<String, Object> toMap(TypeInfo typeInfo) {
        return ["typeCode": toMap(typeInfo.typeCode) as Object, "agoClass": typeInfo?.agoClass?.fullname]
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
        var slots = agoFunction.slots as JsonRefSlots;
        if (logger.isDebugEnabled()) logger.debug("INSERT Function " + slots.objectRef.id())

        var m = toMap(agoFunction, applicationId);

        m["result_type"] = toJsonb(toMap(new TypeInfo(agoFunction.getResultTypeCode(), agoFunction.getResultClass())));
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
        var slots = agoClass.slots as JsonRefSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(agoClass.parentScope);
        var creator = ObjectRefOwner.extractCreator(agoClass);

        return [
                id                      : slots.objectRef.id() as Object,
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

    RdbEngine getAgoEngine(){
        return this.classManager as RdbEngine;
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

        var s = ((RdbEngine) this.classManager).dumpJson(instance);
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
                [slots.objectRef.id() as Object, applicationId,
                 instance.agoClass.fullname, parentScope?.id(), parentScope?.className(),
                 creator?.id(), creator?.className(),
                 toJsonb((classManager as RdbEngine).jsonStringifySlots(instance)),
                 payload
                ]
        )
    }

    void saveRunSpace(RdbRunSpace runSpace){
        sql.executeInsert(toMap(runSpace),
            """insert into ago_runspace (
                    id, application, native_host_class, curr_frame_table, curr_frame_id, result_slots, running_state, exception_id, pausing_parents, forked_runspaces, parent_runspace
                )
                values (
                    :id,:application,:native_host_class,:curr_frame_table,:curr_frame_id,:result_slots,:running_state,:exception_id,:pausing_parents,:forked_runspaces,:parent_runspace
                )
                """);
    }

    void updateRunSpace(RdbRunSpace runSpace){
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

    Map<String, Object> toMap(RdbRunSpace runSpace){
        RdbEngine rdbEngine = this.classManager as RdbEngine
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
                "pausing_parents"  : runSpace.pausingParents.collect { ((RdbRunSpace) it).id }.toArray(new Long[0]),
                "forked_runspaces" : runSpace.forkedSpaces.collect { ((RdbRunSpace) it).id }.toArray(new Long[0]), "parent": ((RdbRunSpace) runSpace.getParent())?.id
        ];      // UnhandledException
    }

    Map<String, Object> toUpdateMap(RdbRunSpace runSpace) {
        RdbEngine rdbEngine = this.classManager as RdbEngine

        ObjectRef currFrameRef = ObjectRefOwner.extractObjectRef(runSpace.getCurrentCallFrame());
        return [
                "id"               : (Object) runSpace.id,
                "curr_frame_table" : currFrameRef?.className(),
                "curr_frame_id"    : currFrameRef?.id(),
                "result_slots"     : toJsonb(rdbEngine.dumpJson(runSpace.resultSlots)),
                "running_state"    : runSpace.runningState,
                "exception_id"     : ObjectRefOwner.extractObjectRef(runSpace.getException())?.id(),
                "pausing_parents"  : runSpace.pausingParents.collect { ((RdbRunSpace) it).id}.toArray(new Long[0]),
                "forked_runspaces" : runSpace.forkedSpaces.collect { ((RdbRunSpace) it).id}.toArray(new Long[0]),
        ];
    }

    private ResultSlots parseResultSlots(PGobject json) {
        return ((RdbEngine)agoEngine).getDumpingObjectMapper().readValue(json.value, ResultSlots);
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

    @Override
    String getTableName(String className) {
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

    static int[] loadPgLongArray(PgArray array) throws SQLException {
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
            r.forkedRunSpaces = row['forked_runspaces'] == null ? null : loadPgLongArray(row['forked_runspaces'] as PgArray).collect { runspaceDescById[it as Long] }.toList()
            r.parentRunSpace = row['parent_runspace'] == null ? null : runspaceDescById[row['parent_runspace'] as Long]
        }

        return ls
    }


}
