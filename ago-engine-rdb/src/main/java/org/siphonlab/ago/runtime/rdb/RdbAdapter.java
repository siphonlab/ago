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
package org.siphonlab.ago.runtime.rdb;

import io.ebeaninternal.dbmigration.migration.Column;
import io.ebeaninternal.dbmigration.migration.CreateTable;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.concurrent.IdGenerator;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.AgoArrayInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

import static org.apache.commons.dbcp2.Utils.closeQuietly;
import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;

public abstract class RdbAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdbAdapter.class);

    protected final BoxTypes boxTypes;
    protected ClassManager classManager;

    // primitive typecode -> RdbType
    protected Int2ObjectHashMap<RdbType> typeMap = new Int2ObjectHashMap<>();

    // more detailed db types, i..e VarChar::(length), BigInt ...
    // maybe need multiple columns, but now not found
    protected Map<AgoClass, RdbType> standardDbTypes = new HashMap<>();

    protected Map<AgoClass, RdbType> cache = new HashMap<>();
    private DataSource dataSource;

    protected final IdGenerator idGenerator;

    public RdbAdapter(BoxTypes boxTypes, ClassManager classManager, IdGenerator idGenerator){
        this.boxTypes = boxTypes;
        this.classManager = classManager;
        this.idGenerator = idGenerator;
        initTypeMap(typeMap, standardDbTypes, classManager);
    }

    public void setClassManager(ClassManager classManager) {
        this.classManager = classManager;
    }

    public abstract RdbType idType();

    protected abstract void initTypeMap(Int2ObjectHashMap<RdbType> typeMap, Map<AgoClass, RdbType> standardDbTypes, ClassManager rdbEngine);

    // return at least one typename for one type, allow multi types, for maybe need multi columns for one object field
    public RdbType mapType(TypeCode typeCode, AgoClass agoClass){
        if(typeCode == OBJECT){
            RdbType types = cache.get(agoClass);
            if(types != null) return types;
            var r =  mapObjectType(agoClass);
            cache.put(agoClass, r);
            return r;
        } else {
            return typeMap.get(typeCode.value);
        }
    }

    protected RdbType mapObjectType(AgoClass agoClass) {
        var existed = standardDbTypes.get(agoClass);
        if(existed != null) return existed;
        for (AgoClass k : standardDbTypes.keySet()) {
            if(agoClass.isThatOrDerivedFrom(k)){
                return mapStandardType(standardDbTypes.get(k), agoClass);
            }
        }

        if(agoClass instanceof MetaClass){
            // if slot is MetaClass, it means the value is Class, we store it with STRING
            return mapType(STRING, null);
        }
        if(boxTypes.isBoxType(agoClass)){
            TypeCode unboxType = boxTypes.getUnboxType(agoClass);
            return mapType(unboxType,null);
        } else {
            var idType = idType().clone();
            var classNameType = mapType(STRING,null).clone();
            return idType.chain(classNameType);
        }
    }

    public BoxTypes getBoxTypes() {
        return boxTypes;
    }

    // standardType is parameterizable
    protected RdbType mapStandardType(RdbType standardType, AgoClass agoClass) {
        if(standardType.getTypeCode() == STRING && agoClass.getParameterizedBaseClass() == standardType.getAgoClass()){
            ParameterizedClassInfo parameterizedClassInfo = (ParameterizedClassInfo) agoClass.getConcreteTypeInfo();
            Object length = parameterizedClassInfo.getArguments()[0];
            String typename = "varchar(%s)".formatted(length);
            return new RdbType(standardType.getTypeCode(),standardType.getSqlType(),typename);
        }
        return standardType;
    }

    public ColumnDesc composeIdColumn(Set<String> usedNames){
        ColumnDesc column = new ColumnDesc();
        column.setRdbType(idType());
        column.setName("id");
        usedNames.add("id");
        column.setPrimaryKey(true);
        return column;
    }

    /**
     * instance columns, i.e. `parentScope`, `caller`
     *
     * @param createTable
     * @param agoClass
     * @param usedNames
     */
    public void composeInstanceColumns(CreateTable createTable, AgoClass agoClass, Set<String> usedNames) {

    }

    public ColumnDesc composeColumnDesc(AgoSlotDef slotDef, Set<String> usedNames){
        var type = mapType(slotDef.getTypeCode(), slotDef.getAgoClass());
        assert type != null;
        var columnDesc = new ColumnDesc();
        columnDesc.setRdbType(type);
        columnDesc.setName(columnName(slotDef, usedNames));
        columnDesc.setSlotDef(slotDef);
        if(slotDef.getTypeCode() == OBJECT){
            if(columnDesc.getAdditional() != null) {
                RdbType additional = type.getAdditional();
                assert additional.getTypeCode() == STRING;      // now it only class name behind object id
                var additionColumn = new ColumnDesc();
                additionColumn.setRdbType(additional);
                additionColumn.setName(columnClassName(slotDef, usedNames));
                additionColumn.setSlotDef(slotDef);
                columnDesc.setAdditional(additionColumn);
            }
        }
        return columnDesc;
    }

    public Column toColumn(ColumnDesc columnDesc) {
        Column column = new Column();
        column.setName(columnDesc.getName());
        column.setType(columnDesc.getRdbType().getTypeName());
        return column;
    }

    protected String columnName(AgoSlotDef slotDef, Set<String> usedNames) {
        var name = slotDef.getName();
        if (usedNames.contains(name)) {
            name += "_" + slotDef.getIndex();
        }
        assert !usedNames.contains(name);
        usedNames.add(name);
        return transformName(name);
    }

    protected String columnClassName(AgoSlotDef slotDef, Set<String> usedNames) {
        String name = slotDef.getName();
        if (usedNames.contains(name)) {
            name += "_" + slotDef.getIndex();
        }
        name += "_class";
        assert !usedNames.contains(name);
        usedNames.add(name);
        return transformName(name);
    }

    protected String transformName(String name) {
        return '"' + StringEscapeUtils.escapeCsv(name) + '"';
    }

    public ColumnDesc composeField(String name, TypeCode typeCode, Set<String> usedNames) {
        var rdbType = mapType(typeCode, null);
        var columnDesc = new ColumnDesc();
        columnDesc.setRdbType(rdbType);
        columnDesc.setName(name);
        return columnDesc;
    }

    public ColumnDesc composeObjectField(String fieldName, Set<String> usedNames) {
        var columnDesc = new ColumnDesc();
        columnDesc.setName("@" + fieldName);    // @agoClass, @parentScope
        columnDesc.setRdbType(idType());
        usedNames.add(columnDesc.getName());

        ColumnDesc className = new ColumnDesc();
        className.setName("@" + fieldName + "_class");      // @parentScope_class
        className.setRdbType(mapType(STRING, null));
        columnDesc.setAdditional(className);
        usedNames.add(className.getName());

        return columnDesc;
    }

    public int fillParameter(PreparedStatement preparedStatement, int parameterIndex, AgoSlotDef slotDef, RdbType rdbType, Slots slots, int index) throws SQLException {
        switch (slotDef.getTypeCode().value){
            case INT_VALUE:         preparedStatement.setInt(parameterIndex, slots.getInt(index)); break;
            case STRING_VALUE:      preparedStatement.setString(parameterIndex, slots.getString(index)); break;
            case LONG_VALUE:        preparedStatement.setLong(parameterIndex, slots.getLong(index)); break;
            case BOOLEAN_VALUE:     preparedStatement.setBoolean(parameterIndex, slots.getBoolean(index)); break;
            case DOUBLE_VALUE:      preparedStatement.setDouble(parameterIndex, slots.getDouble(index)); break;
            case BYTE_VALUE:        preparedStatement.setByte(parameterIndex, slots.getByte(index)); break;
            case FLOAT_VALUE:       preparedStatement.setFloat(parameterIndex, slots.getFloat(index)); break;
            case CHAR_VALUE:        preparedStatement.setString(parameterIndex, String.valueOf(slots.getChar(index))); break;
            case SHORT_VALUE:       preparedStatement.setShort(parameterIndex, slots.getShort(index)); break;
            case CLASS_REF_VALUE:   {
                AgoClass agoClass = classManager.getClass(slots.getClassRef(index));
                return fillClassRefParameter(preparedStatement, parameterIndex, agoClass);
            }
            case OBJECT_VALUE:      {
                Instance<?> object = slots.getObject(index);
                if(slotDef.getAgoClass() instanceof MetaClass){
                    if(object == null){
                        preparedStatement.setNull(parameterIndex,rdbType.getSqlType());
                    } else {
                        preparedStatement.setString(parameterIndex,((AgoClass)object).getFullname());
                    }
                    return parameterIndex + 1;
                }
                if(object == null){
                    if (rdbType.getAdditional() == null) {      // box type
                        // set to null for box type
                        preparedStatement.setNull(parameterIndex, rdbType.getSqlType());
                    } else {
                        return fillObjectParameter(preparedStatement, parameterIndex, slotDef, rdbType,null);
                    }
                } else {
                    if (rdbType.getAdditional() == null) {
                        // rdbType already transformed to match primitive type
                        return fillParameter(preparedStatement, parameterIndex, slotDef.getAgoClass().getSlotDefs()[0], rdbType, object.getSlots(), 0);
                    } else {
                        return fillObjectParameter(preparedStatement,parameterIndex, slotDef, rdbType, object);
                    }
                }
            } break;
        }
        return parameterIndex + 1;
    }

    protected int fillObjectParameter(PreparedStatement preparedStatement, int parameterIndex, AgoSlotDef slotDef, RdbType rdbType, Instance<?> object) throws SQLException {
        if(slotDef.getAgoClass() instanceof MetaClass){
            if(object == null) {
                preparedStatement.setString(parameterIndex, null);
            } else {
                preparedStatement.setString(parameterIndex, ((AgoClass)object).getFullname());
            }
            return parameterIndex + 1;
        }
        if(object == null || object instanceof NativeInstance) {        // TODO
            preparedStatement.setNull(parameterIndex, rdbType.getSqlType());
            preparedStatement.setString(parameterIndex + 1, null);
        } else {
            preparedStatement.setLong(parameterIndex, ((RdbSlots) object.getSlots()).getId());
            preparedStatement.setString(parameterIndex + 1, slotDef.getAgoClass().getFullname());
        }
        return parameterIndex + 2;
    }

    protected int fillClassRefParameter(PreparedStatement preparedStatement, int parameterIndex, AgoClass agoClass) throws SQLException {
        preparedStatement.setString(parameterIndex, agoClass.getFullname());
        return parameterIndex + 1;
    }

    public String tableName(AgoClass agoClass) {
        return transformName(agoClass.getFullname());
    }

    public String primaryKeyName(AgoClass agoClass) {
        return transformName("PK_" + agoClass.getFullname());
    }

    public int fillId(PreparedStatement ps, int parameterIndex, Object id) throws SQLException {
        ps.setLong(parameterIndex, (Long)id);
        return parameterIndex + 1;
    }

    protected void saveInstance(Instance<?> instance, Set<Instance<?>> saved){
        saved.add(instance);

        if (boxTypes.isBoxType(instance.getAgoClass()) || instance instanceof AgoArrayInstance)
            return;
        if(instance instanceof MetaClass && ((MetaClass) instance).getName().equals("<Meta>"))
            return;

        if (instance.getSlots() instanceof RdbSlots rdbSlots) {
            if(rdbSlots.getUsingInstances() != null) {
                rdbSlots.getUsingInstances().removeIf(
                value -> boxTypes.isBoxType(value.getAgoClass())
                            || value instanceof AgoArrayInstance
                            || value instanceof MetaClass m && m.getName().equals("<Meta>"));
            }

            switch (rdbSlots.getRowState()) {
                case RowState.Added:
                    rdbSlots.setRowState(RowState.Saving);
                    insert(instance, rdbSlots, instance.getAgoClass());
                    break;
                case RowState.Modified:
                    rdbSlots.setRowState(RowState.Saving);
                    update(instance, rdbSlots, instance.getAgoClass());       // need load ID
                    break;
//                    case RowState.Deleted:
//                        delete(rdbSlots);
//                        break;
                default:
                    if (instance instanceof CallFrameWithRunningState<?> callFrameWithRunningState) {
                        update(instance, rdbSlots, instance.getAgoClass());
                    }
            }
            rdbSlots.setRowState(RowState.Unchanged);
            rdbSlots.clearDetachedInstances();

            if (rdbSlots.getUsingInstances() != null) {
                for (Instance<?> usingInstance : rdbSlots.getUsingInstances()) {
                    if (!saved.contains(usingInstance))
                        saveInstance(usingInstance, saved);
                }
            }
        }
    }
    public void saveInstance(Instance<?> instance) {
        saveInstance(instance, new HashSet<>());
    }

    public void saveRunSpace(RdbRunSpace runSpace) {
        throw new NotImplementedException("not implemented yet");
    }

    public void updateRunSpace(RdbRunSpace runSpace) {
        throw new NotImplementedException("not implemented yet");
    }

    public void updateCallFrameRunningState(CallFrame<?> statefulCallFrame, byte runningState) {
        throw new NotImplementedException();
    }

    protected void insert(Instance<?> instance, RdbSlots rdbSlots, AgoClass agoClass) {
        var tableOfClass = tableOfClassMap.get(agoClass);
        StringBuilder sql = new StringBuilder("INSERT INTO " + tableOfClass.tableName()).append("(");

        sql.append("id,");

        int parameterCount = 1;
        var columns = tableOfClass.columns();

        // save related Added items at first
        for (ColumnDesc column : columns) {
            var slotDef = column.getSlotDef();
            saveUsingNewObject(rdbSlots, slotDef);
        }

        for (ColumnDesc column : columns) {
            sql.append(column.getName()).append(',');
            parameterCount++;
            if (column.getAdditional() != null) {
                sql.append(column.getAdditional().getName()).append(',');
                parameterCount++;
            }
        }
        sql.setCharAt(sql.length() - 1, ')');
        sql.append(" VALUES (").append(StringUtils.repeat("?", ",", parameterCount)).append(')');

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(sql.toString());

            int parameterIndex = 1;
            parameterIndex = this.fillId(ps, parameterIndex, rdbSlots.getId());
            for (ColumnDesc column : columns) {
                var slotDef = column.getSlotDef();
                parameterIndex = this.fillParameter(ps, parameterIndex, slotDef, column.getRdbType(), rdbSlots, slotDef.getIndex());
            }

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("EXECUTE INSERT %d : ".formatted(rdbSlots.getId()) + sql);
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(ps);
            closeQuietly(connection);
        }
    }

    private void saveUsingNewObject(RdbSlots rdbSlots, AgoSlotDef slotDef) {
        if(slotDef.getTypeCode() == OBJECT){
            if (!boxTypes.isBoxTypeOrWithin(slotDef.getAgoClass()) && !(slotDef.getAgoClass() instanceof MetaClass)) {
                Instance<?> object = rdbSlots.getObject(slotDef.getIndex());
                if(object != null && object.getSlots() instanceof RdbSlots objectSlots && objectSlots.getRowState() == RowState.Added){
                    saveInstance(object);
                }
            }
        }
    }

    protected void update(Instance<?> instance, RdbSlots rdbSlots, AgoClass agoClass) {
        TableOfClass tableOfClass = tableOfClassMap.get(agoClass);
        var columns = tableOfClass.columns();

        for (var index : rdbSlots.getChangedSlots()) {
            var column = tableOfClass.columnDescOfSlot(index);
            var slotDef = column.getSlotDef();
            saveUsingNewObject(rdbSlots, slotDef);
        }


        StringBuilder updateSql = new StringBuilder("UPDATE " + tableOfClass.tableName()).append(" SET ");

        for (var index : rdbSlots.getChangedSlots()) {
            var column = tableOfClass.columnDescOfSlot(index);
            updateSql.append(column.getName()).append("= ?,");
            if (column.getAdditional() != null) {
                updateSql.append(column.getAdditional().getName()).append("= ?,");
            }
        }
        updateSql.setCharAt(updateSql.length() - 1, ' ');
        updateSql.append("WHERE id = ?");

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(updateSql.toString());

            int parameterIndex = 1;
            for (var index : rdbSlots.getChangedSlots()) {
                var column = tableOfClass.columnDescOfSlot(index);
                var slotDef = column.getSlotDef();
                parameterIndex = this.fillParameter(ps, parameterIndex, slotDef, column.getRdbType(), rdbSlots, slotDef.getIndex());
            }
            this.fillId(ps, parameterIndex, rdbSlots.getId());

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("EXECUTE INSERT %d : ".formatted(rdbSlots.getId()) + updateSql);

            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(ps);
            closeQuietly(connection);
        }
    }

    public ResultSetMapper fetchAll(AgoClass agoClass) {
        var tableOfClass = getTableOfClass(agoClass);

        StringBuilder sql = composeSelectFrom(tableOfClass);

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(sql.toString());

            PreparedStatement finalPs = ps;
            Connection finalConnection = connection;
            return new ResultSetMapper(finalPs.executeQuery(), tableOfClass, boxTypes){
                public void close(){
                    super.close();
                    closeQuietly(finalPs);
                    closeQuietly(finalConnection);
                }
            };
        } catch (SQLException e) {
            closeQuietly(ps);
            closeQuietly(connection);
            throw new RuntimeException(e);
        }
    }

    private TableOfClass getTableOfClass(AgoClass agoClass) {
        var tableOfClass = tableOfClassMap.get(agoClass);
        if(tableOfClass == null) throw new NullPointerException("table for '%s' not found".formatted(agoClass.getFullname()));
        return tableOfClass;
    }

    public Instance<?> getById(AgoClass agoClass, RdbEngine rdbEngine, Object id) {
        var tableOfClass = getTableOfClass(agoClass);
        StringBuilder sql = composeSelectFrom(tableOfClass);
        sql.append(" WHERE id=?");

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(sql.toString());

            this.fillId(ps, 1, id);

            PreparedStatement finalPs = ps;
            resultSet = finalPs.executeQuery();
            var resultMapper = new ResultSetMapper(resultSet, tableOfClass, boxTypes);
            resultMapper.setAgoEngine(rdbEngine);
            if(resultMapper.hasNext()){
                return resultMapper.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(ps);
            closeQuietly(resultSet);
            closeQuietly(connection);
        }
        return null;
    }


    protected StringBuilder composeSelectFrom(TableOfClass tableOfClass) {
        StringBuilder sql = new StringBuilder("SELECT id,");
        for (ColumnDesc column : tableOfClass.columns()) {
            sql.append(column.getName()).append(',');
        }
        sql.setCharAt(sql.length() -1,' ');
        sql.append("FROM ").append(tableOfClass.tableName());
        return sql;
    }

    protected Map<AgoClass, TableOfClass> tableOfClassMap;
    protected Map<String, TableOfClass> tables;


    public void loadTableMap(InputStream tableMapYaml){
        this.tableOfClassMap = TableOfClass.load(tableMapYaml, this.classManager, this);
        Map<String, TableOfClass> tables = new HashMap<>();
        for (Map.Entry<AgoClass, TableOfClass> entry : tableOfClassMap.entrySet()) {
            tables.put(entry.getKey().getFullname(),entry.getValue());
        }
        this.tables = tables;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void executeDDL(String ddl) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            statement.execute(ddl);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public ColumnDesc getColumnDesc(String className, int slot) {
        return tables.get(className).columnDescOfSlot(slot);
    }

    public String getTableName(String className){
        return tables.get(className).tableName();
    }

    public void fillPrimitiveParameter(PreparedStatement ps, int index, TypeCode typeCode, Object value) throws SQLException {
        switch (typeCode.getValue()) {
            case INT_VALUE:     ps.setInt(index, (Integer) value); break;
            case LONG_VALUE:    ps.setLong(index, (Long) value); break;
            case SHORT_VALUE:   ps.setShort(index, (Short) value); break;
            case BYTE_VALUE:    ps.setByte(index, (Byte) value); break;
            case DOUBLE_VALUE:  ps.setDouble(index, (Double) value); break;
            case FLOAT_VALUE:   ps.setFloat(index, (Float) value); break;
            case BOOLEAN_VALUE: ps.setBoolean(index, (Boolean) value); break;
            case STRING_VALUE:  ps.setString(index, (String) value); break;
            case CHAR_VALUE:    ps.setString(index, String.valueOf((Character) value)); break;
            case CLASS_REF_VALUE: ps.setString(index, (String) value); break;       // class name
            default: throw new SQLException("Unsupported primitive type: " + typeCode);
        }
    }


    public AgoClass getClassById(int value) {
        return this.classManager.getClass(value);
    }

    public AgoClass getClassByName(String className) {
        return this.classManager.getClass(className);
    }

    public abstract void saveStrings(List<String> strings);

    public abstract void saveBlobs(List<byte[]> blobs);

    public long nextId(){
        return this.idGenerator.nextId();
    }

    public List<RunSpaceDesc> loadResumableRunSpaces() {
        throw new NotImplementedException();
    }

    public AgoClass loadScopedAgoClass(AgoClass baseClass, long id) {
        throw new NotImplementedException();
    }

    public Instance<?> restoreInstance(ObjectRef objectRef) {
        RdbEngine rdbEngine = (RdbEngine) this.classManager;
        return getById(rdbEngine.getClass(objectRef.className()),rdbEngine, objectRef.id());
    }
}
