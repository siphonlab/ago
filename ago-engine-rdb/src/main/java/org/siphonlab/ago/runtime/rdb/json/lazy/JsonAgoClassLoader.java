package org.siphonlab.ago.runtime.rdb.json.lazy;

import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.SourceMapEntry;
import org.siphonlab.ago.native_.AgoNativeFunction;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

import static org.siphonlab.ago.AgoClass.NATIVE;
import static org.siphonlab.ago.runtime.rdb.json.JsonPGAdapter.*;

public class JsonAgoClassLoader extends AgoClassLoader {

    private Map<String, GroovyRowResult> rowsByClassName;

    public JsonAgoClassLoader(PGJsonSlotsCreatorFactory slotsCreatorFactory) {
        super(slotsCreatorFactory);
    }

    public JsonAgoClassLoader() {
        super();
    }

    public void loadClasses(DataSource dataSource, int applicationId) throws SQLException {
        var sql = new Sql(dataSource);
        Map<String, GroovyRowResult> rowsByClassName = new LinkedHashMap<>();

        List<GroovyRowResult> classRows = sql.rows("SELECT * FROM ago_class WHERE application=? AND parent_scope_id IS NULL ORDER BY class_id", List.of(applicationId));
        List<GroovyRowResult> functionRows = sql.rows("SELECT * FROM ago_function WHERE application=? AND parent_scope_id IS NULL ORDER BY class_id", List.of(applicationId));
        List<GroovyRowResult> allRows = ListUtils.union(classRows, functionRows).stream().sorted(Comparator.comparingInt(o -> (Integer) o.get("class_id"))).toList();
        // blobs, strings
        var strings = sql.rows("SELECT index, value FROM ago_string WHERE application=? ORDER BY index", List.of(applicationId));
        for (GroovyRowResult r : strings) {
            var id = this.idOfString((String)r.get("value"));
            assert id == (Integer) r.get("index");
        }
        var blobs = sql.rows("SELECT index, data FROM ago_blob WHERE application=? ORDER BY index", List.of(applicationId));
        for (GroovyRowResult r : blobs) {
            assert this.blobs.size() == (Integer) r.get("index");
            this.blobs.add((byte[]) r.get("data"));
        }
        this.classes = new ArrayList<>();
        this.classByName.put(this.getTheMeta().getFullname(), this.getTheMeta());

        // stage 1, names
        for(var row : allRows){
            String fullname = (String) row.get("fullname");
            rowsByClassName.put(fullname, row);
            this.classes.add(null);     // will call set(index, loaded)
        }

        for (GroovyRowResult row : allRows) {
            loadAgoClass(row, rowsByClassName);
        }
        // stage2, resolve hierarchy
        for (AgoClass agoClass : classes) {
            GroovyRowResult row = rowsByClassName.get(agoClass.getFullname());
            loadAgoClass(agoClass, row);
        }

        // parentScope, creator, slots not apply here
        this.rowsByClassName = rowsByClassName;
    }

    public Map<String, GroovyRowResult> getRowsByClassName() {
        return rowsByClassName;
    }

    private void loadAgoClass(AgoClass agoClass, GroovyRowResult row) throws SQLException {
        agoClass.setSuperClass(getClass((String) row.get("super_class")));
        String[] interfaces = loadPgStringArray((PgArray) row.get("interfaces"));
        if(interfaces != null)
            agoClass.setInterfaces(Arrays.stream(interfaces).map(this::getClass).toArray(AgoClass[]::new));
        String permitClass = (String) row.get("permit_class");
        if(permitClass != null) agoClass.setPermitClass(getClass(permitClass));

        String[] children = loadPgStringArray((PgArray) row.get("children"));
        if(children != null)
            agoClass.setChildren(Arrays.stream(children).map(this::getClass).toArray(AgoClass[]::new));

        String[] methods = loadPgStringArray((PgArray) row.get("methods"));
        if(methods != null)
            agoClass.setMethods(Arrays.stream(methods).map(fullname -> (AgoFunction)getClass(fullname)).toArray(AgoFunction[]::new));

        String parent = (String) row.get("parent");
        if (parent != null) agoClass.setParent(getClass(parent));
        String parameterizedBaseClass = (String) row.get("parameterized_base_class");
        if (parameterizedBaseClass != null) agoClass.setParameterizedBaseClass(getClass(parameterizedBaseClass));

        Map<String,Object>[] slotDefs = loadPgJsonArrayAsList((PgArray) row.get("slotdefs"));
        if(slotDefs != null)
            agoClass.setSlotDefs(Arrays.stream(slotDefs).map(this::loadSlotDef).toArray(AgoSlotDef[]::new));

        Map<String, Object>[] fields = loadPgJsonArrayAsList((PgArray) row.get("fields"));
        if (fields != null)
            agoClass.setFields(Arrays.stream(fields).map(this::loadField).toArray(AgoField[]::new));

        Map<String,Object> concreteTypeInfo = loadPgJsonAsMap((PGobject) row.get("concrete_type_info"));
        if(concreteTypeInfo != null)
            agoClass.setConcreteTypeInfo(loadConcreteTypeInfo(concreteTypeInfo));

        if(agoClass instanceof AgoFunction agoFunction){
            Map<String, Object>[] variables = loadPgJsonArrayAsList((PgArray) row.get("variables"));
            TypeInfo resultType = loadTypeInfo(loadPgJsonAsMap((PGobject) row.get("result_type")));
            agoFunction.setResultType(resultType.getTypeCode(), resultType.getAgoClass());

            if (variables != null)
                agoFunction.setVariables(Arrays.stream(variables).map(this::loadVariable).toArray(AgoVariable[]::new));

            Map<String, Object>[] parameters = loadPgJsonArrayAsList((PgArray) row.get("parameters"));
            if (parameters != null)
                agoFunction.setParameters(Arrays.stream(parameters).map(this::loadParameter).toArray(AgoParameter[]::new));

            agoFunction.setCode(loadPgIntArray((PgArray) row.get("code")));

            Map<String, Object>[] switchTables = loadPgJsonArrayAsList((PgArray) row.get("switch_tables"));
            if(switchTables != null)
                agoFunction.setSwitchTables(Arrays.stream(switchTables).map(this::loadSwitchTable).toArray(SwitchTable[]::new));

            Map<String, Object>[] tryCatchItems = loadPgJsonArrayAsList((PgArray) row.get("try_catch_items"));
            if (tryCatchItems != null)
                agoFunction.setTryCatchItems(Arrays.stream(tryCatchItems).map(this::loadTryCatchItem).toArray(TryCatchItem[]::new));

            Map<String, Object>[] sourceMapEntries = loadPgJsonArrayAsList((PgArray) row.get("source_map_entries"));
            if (sourceMapEntries != null)
                agoFunction.setSourceMap(Arrays.stream(sourceMapEntries).map(this::loadSourceMapEntry).toArray(SourceMapEntry[]::new));

            if(agoFunction instanceof AgoNativeFunction nativeFunction){
                generateNativeCaller(nativeFunction);
            }
        }

        if ((Boolean) row.get("has_slots_creator")) {
            agoClass.setSlotsCreator(this.getSlotsCreatorFactory().generateSlotsCreator(agoClass));
        }
    }

    private AgoClass loadAgoClass(GroovyRowResult row, Map<String, GroovyRowResult> rowsByClassName) throws SQLException {
        String fullname = (String) row.get("fullname");
        var existed = classByName.get(fullname);
        if(existed != null) return existed;

        String metaClassName = (String) row.get("ago_class");
        AgoClass agoClass;
        MetaClass metaClass = (MetaClass) this.getClass(metaClassName);
        if(StringUtils.isNotEmpty(metaClassName) && metaClass == null){
            GroovyRowResult metaRow = rowsByClassName.get(metaClassName);
            assert metaRow != null;
            metaClass = (MetaClass) loadAgoClass(metaRow, rowsByClassName);
        }
        String name = (String) row.get("name");
        Integer modifiers = (Integer) row.get("modifiers");
        String[] interfaces = loadPgStringArray((PgArray) row.get("interfaces"));

        switch (((Integer) row.get("class_type")).byteValue()){
            case AgoClass.TYPE_CLASS:
            agoClass = new AgoClass(this, metaClass, fullname, name);
            break;
        case AgoClass.TYPE_METACLASS:
            agoClass = new MetaClass(this, metaClass, fullname);
            break;
        case AgoClass.TYPE_ENUM:
            agoClass = new AgoEnum(this, metaClass, fullname, name);
//                enumClass.setBasePrimitiveType(this.enumBasePrimitiveType);
//                enumClass.setEnumValues(this.enumValues);
            break;
        case AgoClass.TYPE_INTERFACE:
            agoClass = new AgoInterface(this, metaClass, fullname, name);
            break;
        case AgoClass.TYPE_TRAIT:
            agoClass = new AgoTrait(this, metaClass,fullname, name);
            break;
        case AgoClass.TYPE_FUNCTION:
            if ((modifiers & NATIVE) == NATIVE) {
                AgoNativeFunction n = new AgoNativeFunction(this, metaClass, fullname, name);
                n.setNativeEntrance((String) row.get("native_function_entrance"));
                n.setResultSlot((Integer) row.get("native_function_result_slot"));
                agoClass = n;
            } else {
                agoClass = new AgoFunction(this, metaClass, fullname, name);
            }
            break;
        default:
            throw new IllegalArgumentException("illegal type " + row.get("class_type"));
        }
        agoClass.setClassId((Integer) row.get("class_id"));
        agoClass.setModifiers(modifiers);
        agoClass.setSourceLocation(loadSourceLocation(loadPgJsonAsMap((PGobject) row.get("source_location"))));
        this.classes.set(agoClass.getClassId(), agoClass);
        this.classByName.put(agoClass.getFullname(),agoClass);
        return agoClass;
    }

    private SourceMapEntry loadSourceMapEntry(Map<String, Object> json) {
        return new SourceMapEntry((Integer) json.get("codeOffset"), loadSourceLocation((Map<String, Object>) json.get("sourceLocation")));
    }

    private TryCatchItem loadTryCatchItem(Map<String, Object> json) {
        String[] exceptionClasses = (String[]) json.get("exceptionClasses");
        return new TryCatchItem((Integer) json.get("begin"), (Integer) json.get("end"), (Integer) json.get("handler"), Arrays.stream(exceptionClasses).map(this::getClass).toArray(AgoClass[]::new));
    }

    private SwitchTable loadSwitchTable(Map<String, Object> json) {
        var type = (String) json.get("type");
        if("DenseSwitchTable".equals(type)){
            return new DenseSwitchTable((int[])json.get("data"));
        } else {
            assert "SparseSwitchTable".equals(type);
            return new SparseSwitchTable((int[]) json.get("data"));
        }
    }

    private TypeInfo loadTypeInfo(Map<String, Object> json) {
        return new TypeInfo(loadTypeCode((Map<String, Object>) json.get("typeCode")),getClass((String)json.get("agoClass")));
    }

    private TypeCode loadTypeCode(Map<String, Object> json){
        Integer code = (Integer) json.get("code");
        if(code >= TypeCode.GENERIC_TYPE_START){
            return new TypeCode(code, (String) json.get("description"));
        } else {
            return TypeCode.of(code);
        }
    }

    private ConcreteTypeInfo loadConcreteTypeInfo(Map<String, Object> json) {
        var type = (String)json.get("type");
        if("ArrayInfo".equals(type)){
            return new ArrayInfo(loadTypeInfo((Map<String, Object>) json.get("elementType")));
        } else if("GenericArgumentsInfo".equals(type)){
            Map<String,Object>[] arguments = ((List<Object>)json.get("arguments")).stream().map(o ->(Map<String, Object>) o).toArray(Map[]::new);
            return new GenericArgumentsInfo(getClass((String)json.get("templateClass")), Arrays.stream(arguments).map(this::loadTypeInfo).toArray(TypeInfo[]::new));
        } else if("GenericTypeParametersInfo".equals(type)){
            Map<String, Object>[] genericParameters = ((List<Object>) json.get("genericParameters")).stream().map(o -> (Map<String, Object>) o).toArray(Map[]::new);
            return new GenericTypeParametersInfo(Arrays.stream(genericParameters).map(this::loadTypeInfo).toArray(TypeInfo[]::new));
        } else if("ParameterizedClassInfo".equals(type)){
            return new ParameterizedClassInfo(getClass((String) json.get("parameterizedBaseClass")), (AgoFunction) getClass((String) json.get("parameterizedConstructor")),
                    objectListToArray((List<Object>) json.get("arguments")));
        } else {
            throw new IllegalArgumentException("unknown concrete type info" + type);
        }
    }

    private Object[] objectListToArray(List<Object> ls) {
        if(ls == null) return null;
        return ls.toArray();
    }

    private AgoParameter loadParameter(Map<String, Object> json) {
        if (json == null) return null;
        //TODO json.get("constLiteralValue") to matchType
        var parameter = new AgoParameter((String) json.get("name"), (Integer) json.get("modifiers"), loadTypeCode((Map<String, Object>) json.get("typeCode")), getClass((String) json.get("agoClass")),
                (Integer) json.get("slotIndex"), (AgoFunction) getClass((String) json.get("ownerClass")), json.get("constLiteralValue"));
        parameter.setSourceLocation(loadSourceLocation((Map<String, Object>) json.get("sourceLocation")));
        return parameter;
    }

    private AgoField loadField(Map<String, Object> json) {
        if (json == null) return null;
        //TODO json.get("constLiteralValue") to matchType
        var field = new AgoField((String) json.get("name"), (Integer) json.get("modifiers"), loadTypeCode((Map<String, Object>) json.get("typeCode")), getClass((String) json.get("agoClass")),
                (Integer) json.get("slotIndex"), getClass((String) json.get("ownerClass")), json.get("constLiteralValue"));
        field.setSourceLocation(loadSourceLocation((Map<String, Object>) json.get("sourceLocation")));
        return field;
    }


    private AgoVariable loadVariable(Map<String, Object> json) {
        if (json == null) return null;
        //TODO json.get("constLiteralValue") to matchType
        var variable = new AgoVariable((String) json.get("name"), (Integer) json.get("modifiers"), loadTypeCode((Map<String, Object>) json.get("typeCode")), getClass((String) json.get("agoClass")),
                (Integer) json.get("slotIndex"),json.get("constLiteralValue"));
        variable.setSourceLocation(loadSourceLocation((Map<String, Object>) json.get("sourceLocation")));
        return variable;
    }

    private AgoSlotDef loadSlotDef(Map<String, Object> json) {
        if(json == null) return null;
        return new AgoSlotDef((Integer) json.get("index"), (String) json.get("name"), loadTypeCode((Map<String, Object>) json.get("typeCode")), getClass((String) json.get("agoClass")));
    }

    SourceLocation loadSourceLocation(Map<String, Object> json){
        if(json == null) return null;
        return new SourceLocation((String)json.get("filename"), (Integer)json.get("line"), (Integer)json.get("column"),
                (Integer) json.get("length"), (Integer) json.get("start"), (Integer) json.get("end"));
    }

}
