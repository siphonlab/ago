package org.siphonlab.ago.runtime.rdb.json

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.agrona.collections.Int2ObjectHashMap
import org.agrona.concurrent.IdGenerator
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.lang3.NotImplementedException
import org.postgresql.jdbc.PgArray
import org.postgresql.util.PGobject
import org.siphonlab.ago.*
import org.siphonlab.ago.native_.AgoNativeFunction
import org.siphonlab.ago.native_.NativeFrame
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner
import org.siphonlab.ago.runtime.rdb.RdbAdapter
import org.siphonlab.ago.runtime.rdb.RdbType
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.stateful.RunningState
import org.siphonlab.ago.runtime.rdb.reactive.json.ReactiveJsonCallFrame
import org.siphonlab.ago.runtime.rdb.reactive.json.ReactiveJsonAgoEngine
import org.siphonlab.ago.runtime.stateful.StatefulAgoFrame
import org.siphonlab.ago.runtime.stateful.StatefulCallFrame
import org.siphonlab.ago.runtime.stateful.StatefulNativeFrame

import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException
import java.sql.Types

@CompileStatic
public abstract class JsonPGAdapter extends RdbAdapter {

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

    Instance restoreInstance(Connection connection, ObjectRef objectRef, CallFrame<?> callFrame){
        // TODO improve AgoClass performance, let's don't load row
        var row = new Sql(connection).firstRow("SELECT * FROM " + getTableName(objectRef.className()) + " WHERE id = ?", [objectRef.id() as Object])
        String className = row["ago_class"]
        String parentScopeTable = row['parent_scope_class']
        Instance parentScope = null;
        if(parentScopeTable != null){
            long parent_scope_id = row["parent_scope_id"] as long
            parentScope = restoreInstance(connection, new ObjectRef(parentScopeTable, parent_scope_id), callFrame)
        }

        AgoClass agoClass = this.getClassByName(className);
        if(agoClass instanceof MetaClass){
            return this.getClassByName(row["fullname"] as String);
        } else if(agoClass instanceof AgoFunction){
            def runSpace = callFrame.getRunSpace()
            // TODO new NativeFrame...
            var frame = new ReactiveJsonCallFrame(restoreSlots(objectRef, row, agoClass), agoClass as AgoFunction, runSpace.getAgoEngine() as ReactiveJsonAgoEngine)
            frame.parentScope = parentScope
            frame.pc = row['pc'] as int
            frame.runningState = RunningState.fromCode((Integer)row["state"])
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

    void saveAgoFrame(StatefulAgoFrame agoFrame) {
        var slots = agoFrame.slots as JsonRefSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(agoFrame.parentScope);
        ObjectRef callerObjectRef = ObjectRefOwner.extractObjectRef(agoFrame.caller);
        ObjectRef creatorObjectRef = ObjectRefOwner.extractObjectRef(agoFrame.creator);

        var defaultSlots = defaultSlots(agoFrame.agoClass, slots.jsonSlotMapper.jsonFiledNames)

        sql.executeInsert("""INSERT INTO ago_frame
                                    (id, application, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, 
                                                caller_id, caller_class, pc, state, exception_id, exception_class, runspace, slots)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                                """,
                [slots.objectRef.id() as Object, applicationId, agoFrame.agoClass.fullname, parentScope?.id(), parentScope?.className(),
                 creatorObjectRef?.id(), creatorObjectRef ?.className(),
                 callerObjectRef ?.id(), callerObjectRef ?.className(),
                 0, agoFrame.runningState.code, null, null, null,
                 new PGobject().with { it.type = "jsonb"; it.value = JsonOutput.toJson(defaultSlots); it}]
        )
    }

    void saveNativeFrame(StatefulNativeFrame agoFrame) {
        var slots = agoFrame.slots as JsonRefSlots;
        var parentScope = ObjectRefOwner.extractObjectRef(agoFrame.parentScope);
        ObjectRef callerObjectRef = ObjectRefOwner.extractObjectRef(agoFrame.caller);
        ObjectRef creatorObjectRef = ObjectRefOwner.extractObjectRef(agoFrame.creator);

        var defaultSlots = defaultSlots(agoFrame.agoClass, slots.jsonSlotMapper.jsonFiledNames)

        sql.executeInsert("""INSERT INTO ago_frame
                                    (id, application, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, 
                                                caller_id, caller_class, pc, state, exception_id, exception_class, runspace, slots)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                                """,
                [slots.objectRef.id() as Object, applicationId, agoFrame.agoClass.fullname, parentScope?.id(), parentScope?.className(),
                 creatorObjectRef?.id(), creatorObjectRef?.className(), callerObjectRef?.id(), callerObjectRef?.className(),
                 0, agoFrame.runningState.code, null, null, null,
                 new PGobject().with { it.type = "jsonb"; it.value = JsonOutput.toJson(defaultSlots); it }]
        )
    }

    void saveCallFrameState(StatefulCallFrame statefulCallFrame) {
        var ds = (BasicDataSource)this.dataSource
        int a = ds.getNumActive()
        if(statefulCallFrame instanceof StatefulAgoFrame) {
            String sql = """UPDATE ago_frame SET pc = ?, state = ? WHERE id = ?"""

            JsonRefSlots slots = statefulCallFrame.slots as JsonRefSlots

            this.sql.execute(sql, [statefulCallFrame.pc as Object, statefulCallFrame.getRunningState().code, slots.objectRef.id()])
        } else if(statefulCallFrame instanceof NativeFrame){
            String sql = """UPDATE ago_frame SET state = ? WHERE id = ?"""

            JsonRefSlots slots = statefulCallFrame.slots as JsonRefSlots

            this.sql.execute(sql, [statefulCallFrame.getRunningState().code as Object, slots.objectRef.id()])
        } else {
            throw new UnsupportedOperationException("unsupported frame type " + statefulCallFrame)
        }
    }

    void saveAgoClass(AgoClass agoClass) {
        var slots = agoClass.slots as JsonRefSlots;
        System.err.println("INSERT CLASS " + slots.objectRef.id())

        sql.executeInsert("INSERT INTO ago_class (id, application, class_id, class_type, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, slots, fullname, modifiers, super_class, " +
                                'interfaces, children, methods, parent, permit_class, parameterized_base_class, "name", fields, slotdefs, concrete_type_info, source_location, has_slots_creator) ' +
                            "VALUES(:id, :application, :class_id, :class_type, :ago_class, :parent_scope_id, :parent_scope_class, :creator_id, :creator_class, :slots, :fullname, :modifiers, :super_class, " +
                                ":interfaces, :children, :methods, :parent, :permit_class, :parameterized_base_class, :name, :fields, :slotdefs, :concrete_type_info, :source_location, :has_slots_creator)",
                toMap(agoClass, applicationId)
        )
    }

    @Override
    void saveStrings(List<String> strings) {
        this.sql.withBatch("INSERT INTO ago_string(id, application, index, value) VALUES(?, ?, ?, ?)", {
            for (i in 0..<strings.size()) {
                it.addBatch([this.nextId() as Object, 1, i, strings[i]])
            }
        })
    }

    @Override
    void saveBlobs(List<byte[]> blobs) {
        this.sql.withBatch("INSERT INTO ago_blob(id, application, index, data) VALUES(?, ?, ?, ?)", {
            for (i in 0..<blobs.size()) {
                it.addBatch([this.nextId() as Object, 1, i, blobs[i]])
            }
        })
    }

    @Override
    CallFrame[] loadResumableCallFrames(CallFrame<?> resumeFrame) {
        throw new NotImplementedException("not implemented yet")
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
        if(r instanceof AgoField){
            r["ownerClass"] = r.getOwnerClass()?.getFullname();
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
            return ["type": "GenericArgumentsInfo" as Object, "templateClass": concreteTypeInfo.templateClass.fullname, "arguments": concreteTypeInfo.arguments.collect {toMap(it)}.toArray()]
        } else if(concreteTypeInfo instanceof GenericTypeParametersInfo){
            return ["type": "GenericTypeParametersInfo" as Object, "genericParameters": concreteTypeInfo.genericParameters.collect { toMap(it) }.toArray()]
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
        return ["begin": tryCatchItem.begin as Object, "end": tryCatchItem.end, "handler": tryCatchItem.handler, "exceptionClasses": tryCatchItem.exceptionClasses.collect { it.fullname }.toArray()];
    }

    static Map<String, Object> toMap(SourceMapEntry sourceMapEntry) {
        return ["codeOffset": sourceMapEntry.codeOffset(), "sourceLocation": toMap(sourceMapEntry.sourceLocation())]
    }

    void saveAgoFunction(AgoFunction agoFunction) {
        var slots = agoFunction.slots as JsonRefSlots;
        System.err.println("INSERT Function " + slots.objectRef.id())

        var m = toMap(agoFunction, applicationId);

        m["result_type"] = toJsonb(toMap(new TypeInfo(agoFunction.getResultTypeCode(), agoFunction.getResultClass())));
        m["code"] = agoFunction.code
        m["variables"] = toJsonbArray(agoFunction.variables?.collect {toMap(it)}?.toArray());
        m["parameters"] = toJsonbArray(agoFunction.parameters?.collect{toMap(it)}?.toArray());
        m["switch_tables"] = toJsonbArray(agoFunction.switchTables?.collect {toMap(it)}?.toArray());
        m["try_catch_items"] = toJsonbArray(agoFunction.tryCatchItems?.collect{toMap(it)}?.toArray());
        m["source_map_entries"] = toJsonbArray(agoFunction.sourceMap?.collect{toMap(it)}?.toArray());
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

    static Map<String, Object> toMap(AgoClass agoClass, int applicationId){
        var slots = agoClass.slots as JsonRefSlots;
        var parentScope = agoClass.parentScope?.slots as JsonRefSlots;
        var creator = agoClass.creator?.slots as JsonRefSlots;

        var defaultSlots = defaultSlots(agoClass.agoClass, slots.jsonSlotMapper.jsonFiledNames)

        return [
                id                      : slots.objectRef.id() as Object,
                application             : applicationId,
                class_id                : agoClass.classId,
                ago_class               : agoClass.agoClass.fullname,
                parent_scope_id         : parentScope?.objectRef?.id(),

                parent_scope_class      : parentScope?.objectRef?.className(),
                creator_id              : creator?.objectRef?.id(),
                creator_class           : creator?.objectRef?.className(),
                slots                   : toJsonb(defaultSlots),
                fullname                : agoClass.getFullname(),

                modifiers               : agoClass.getModifiers(),
                super_class             : agoClass.getSuperClass()?.getFullname(),
                interfaces              : agoClass.getInterfaces()?.collect { it.fullname }?.toArray(String[]::new),
                children                : agoClass.getChildren()?.collect { it.fullname }?.toArray(String[]::new),
                methods                 : agoClass.methods?.collect { it?.fullname }?.toArray(String[]::new),

                parent                  : agoClass.parent?.fullname,
                permit_class            : agoClass.permitClass?.fullname,
                parameterized_base_class: agoClass.parameterizedBaseClass?.fullname,
                name                    : agoClass.name,
                fields                  : toJsonbArray(agoClass.fields?.collect { toMap(it) }?.toArray()),

                slotdefs                : toJsonbArray(agoClass.slotDefs?.collect { toMap(it) }?.toArray()),
                concrete_type_info      : toJsonb(toMap(agoClass.concreteTypeInfo)),
                source_location         : toJsonb(agoClass.sourceLocation),
                class_type              : agoClass.getType() as int,
                has_slots_creator       : agoClass.slotsCreator != null
        ]
    }

    static PGobject toJsonb(Object object){
        if(object == null) return null;

        var obj = new PGobject();
        obj.type = "jsonb";
        obj.value = JsonOutput.toJson(object);
        return obj;
    }

    PGobject toJsonb(Instance instance) {
        if (instance == null) return null;

        var s = ((AgoEngine) this.classManager).jsonStringify(instance, true);
        var obj = new PGobject();
        obj.type = "jsonb";
        obj.value = s
        return obj;
    }

    static PGobject[] toJsonbArray(Object[] objects) {        // needn't connection.createArrayOf
        if (objects == null) return null;
        return objects.collect {toJsonb(it)}.toArray(PGobject[]::new);
    }


    static Map<String, Object> defaultSlots(AgoClass agoClass, String[] fieldNames){
        var map = new HashMap()
        if (agoClass.slotDefs == null) {
            return map;
        }
        for (var i=0; i<agoClass.slotDefs.length; i++) {
            map[fieldNames[i]] = TypeCode.defaultValueForDb(agoClass.slotDefs[i].typeCode)
        }
        return map;
    }

    void saveAgoInstance(Instance instance) {
        var slots = instance.slots as JsonRefSlots;
        var parentScope = instance.parentScope?.slots as JsonRefSlots;
        var creator = instance.creator?.slots as JsonRefSlots;

        var defaultSlots = defaultSlots(instance.agoClass, slots.jsonSlotMapper.jsonFiledNames)

        System.err.println("INSERT Instance " + slots.objectRef.id())

        sql.executeInsert("""INSERT INTO ago_instance
                                    (id, application, ago_class, parent_scope_id, parent_scope_class, creator_id, creator_class, slots)
                                VALUES(?, ?, ?, ?, ?, ?, ?, ?)""",
                [slots.objectRef.id() as Object, applicationId, instance.agoClass.fullname, parentScope?.objectRef?.id(), parentScope?.objectRef?.className(),
                 creator?.objectRef?.id(), creator?.objectRef?.className(),
                 toJsonb(defaultSlots)]
        )
    }

    @Override
    String tableName(AgoClass agoClass) {
        if(agoClass instanceof MetaClass ){
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

}
