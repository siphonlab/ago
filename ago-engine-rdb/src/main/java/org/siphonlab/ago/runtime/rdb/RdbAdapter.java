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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.AgoArrayInstance;
import org.siphonlab.ago.runtime.ObjectArrayInstance;
import org.siphonlab.ago.runtime.db.CallFrameWithRunningState;
import org.siphonlab.ago.runtime.db.DbAdapter;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.IdGenerator;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import static org.apache.commons.dbcp2.Utils.closeQuietly;
import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.OBJECT_VALUE;

public abstract class RdbAdapter<Id> implements DbAdapter<Id> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdbAdapter.class);

    protected final BoxTypes boxTypes;
    protected ClassManager classManager;

    protected final TypeCode idType;
    protected final IdGenerator<Id> idGenerator;
    protected final TypeMapping typeMapping;

    protected final DataSource dataSource;

    protected Map<AgoClass, RdbTable> tablesByClass;
    protected Map<String, RdbTable> tablesByClassName;
    private RdbType idRdbType;
    private LangClasses langClasses;

    public RdbAdapter(ClassManager classManager, TypeCode idType, IdGenerator<Id> idGenerator, BoxTypes boxTypes, TypeMapping typeMapping, DataSource dataSource){
        this.boxTypes = boxTypes;
        this.idType = idType;
        this.idGenerator = idGenerator;
        this.typeMapping = typeMapping;
        typeMapping.setIdRdbType(this.idRdbType());
        this.dataSource = dataSource;
        this.classManager = classManager;
        this.typeMapping.initTypeMap(classManager);
    }

    public void setClassManager(ClassManager classManager) {
        this.classManager = classManager;
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

    public void loadTableMap(InputStream tableMapYaml){
        this.tablesByClass = RdbTable.load(tableMapYaml, this.classManager, this);
        Map<String, RdbTable> tables = new HashMap<>();
        for (Map.Entry<AgoClass, RdbTable> entry : tablesByClass.entrySet()) {
            tables.put(entry.getKey().getFullname(),entry.getValue());
        }
        this.tablesByClassName = tables;
    }

    public Id nextId(){
        return this.idGenerator.nextId();
    }

    public RdbType idRdbType() {
        if (this.idRdbType == null){
            this.idRdbType = typeMapping.mapType(idType, null);
        }
        return this.idRdbType;
    }

    public LangClasses getLangClasses() {
        if(classManager instanceof AgoEngine agoEngine){
            return agoEngine.getLangClasses();
        }
        if(this.langClasses != null) return this.langClasses;
        return this.langClasses = new LangClasses(classManager);
    }

    public ColumnDesc composeColumnDesc(AgoSlotDef slotDef, Set<String> usedNames) {
        boolean nullable = false;
        AgoClass agoClass = slotDef.getAgoClass();
        TypeCode typeCode = slotDef.getTypeCode();
        if(slotDef.getTypeCode() == UNION){
            if(agoClass.getConcreteTypeInfo() instanceof NullableTypeInfo nullableTypeInfo){
                agoClass = nullableTypeInfo.getBaseClass();
                typeCode = agoClass.getTypeCode();
                nullable = true;
            } else {
                nullable = true;
            }
        }
        return composeColumnDesc(slotDef, typeCode, agoClass, nullable, usedNames);
    }

    protected ColumnDesc composeColumnDesc(AgoSlotDef slotDef, TypeCode typeCode, AgoClass agoClass, boolean nullable, Set<String> usedNames){
        var type = typeMapping.mapType(typeCode, agoClass);
        assert type != null;
        var columnDesc = new ColumnDesc();
        columnDesc.setRdbType(type);
        columnDesc.setName(columnName(slotDef, usedNames));
        columnDesc.setSlotDef(slotDef);
        columnDesc.setNotNull(!nullable);

        if (columnDesc.getAdditional() != null) {
            RdbType additional = type.getAdditional();
            assert additional.getTypeCode() == STRING;      // now it only class name behind object id
            var additionColumn = new ColumnDesc();
            additionColumn.setRdbType(additional);
            additionColumn.setName(columnClassName(slotDef, usedNames));
            additionColumn.setSlotDef(slotDef);
            additionColumn.setNotNull(!nullable);
            columnDesc.setAdditional(additionColumn);
        }
        return columnDesc;
    }


    protected String columnName(AgoSlotDef slotDef, Set<String> usedNames) {
        var name = slotDef.getName() + "_" + slotDef.getIndex();
        assert !usedNames.contains(name);
        usedNames.add(name);
        return transformName(name);
    }

    protected String columnClassName(AgoSlotDef slotDef, Set<String> usedNames) {
        var name = slotDef.getName() + "_" + slotDef.getIndex() + "_class";
        assert !usedNames.contains(name);
        usedNames.add(name);
        return transformName(name);
    }

    protected String transformName(String name) {
        return '"' + StringEscapeUtils.escapeCsv(name) + '"';
    }

    public ColumnDesc composeField(String name, TypeCode typeCode, Set<String> usedNames) {
        var rdbType = typeMapping.mapType(typeCode, null);
        var columnDesc = new ColumnDesc();
        columnDesc.setRdbType(rdbType);
        columnDesc.setName(name);
        return columnDesc;
    }

    public ColumnDesc composeObjectField(String fieldName, Set<String> usedNames) {
        var columnDesc = new ColumnDesc();
        columnDesc.setName("@" + fieldName);    // @agoClass, @parentScope
        columnDesc.setRdbType(idRdbType());
        usedNames.add(columnDesc.getName());

        ColumnDesc className = new ColumnDesc();
        className.setName("@" + fieldName + "_class");      // @parentScope_class
        className.setRdbType(typeMapping.mapType(STRING, null));
        columnDesc.setAdditional(className);
        usedNames.add(className.getName());

        return columnDesc;
    }

    public String tableName(AgoClass agoClass) {
        return transformName(agoClass.getFullname());
    }

    public String primaryKeyName(AgoClass agoClass) {
        return transformName("PK_" + agoClass.getFullname());
    }

    public String tableName(String className) {
        return tablesByClassName.get(className).tableName();
    }

    public int fillParameter(PreparedStatement preparedStatement, int parameterIndex, AgoSlotDef slotDef, RdbType rdbType, Slots slots, int index) throws SQLException {
        switch (slotDef.getTypeCode().value) {
            case INT_VALUE:
                preparedStatement.setInt(parameterIndex, slots.getInt(index));
                break;
            case STRING_VALUE:
                preparedStatement.setString(parameterIndex, slots.getString(index));
                break;
            case LONG_VALUE:
                preparedStatement.setLong(parameterIndex, slots.getLong(index));
                break;
            case BOOLEAN_VALUE:
                preparedStatement.setBoolean(parameterIndex, slots.getBoolean(index));
                break;
            case DOUBLE_VALUE:
                preparedStatement.setDouble(parameterIndex, slots.getDouble(index));
                break;
            case DECIMAL_VALUE:
                preparedStatement.setBigDecimal(parameterIndex, slots.getDecimal(index));
                break;
            case BYTE_VALUE:
                preparedStatement.setByte(parameterIndex, slots.getByte(index));
                break;
            case FLOAT_VALUE:
                preparedStatement.setFloat(parameterIndex, slots.getFloat(index));
                break;
            case CHAR_VALUE:
                preparedStatement.setString(parameterIndex, String.valueOf(slots.getChar(index)));
                break;
            case SHORT_VALUE:
                preparedStatement.setShort(parameterIndex, slots.getShort(index));
                break;
            case CLASS_REF_VALUE: {
                AgoClass agoClass = classManager.getClass(slots.getClassRef(index));
                return fillClassRefParameter(preparedStatement, parameterIndex, agoClass);
            }
            case OBJECT_VALUE: {
                Integer parameterIndex1 = fillObjectParameter(preparedStatement, parameterIndex, slotDef, rdbType, slots, index);
                if (parameterIndex1 != null) return parameterIndex1;
            }
            break;
        }
        return parameterIndex + 1;
    }

    private @Nullable Integer fillObjectParameter(PreparedStatement preparedStatement, int parameterIndex, AgoSlotDef slotDef, RdbType rdbType, Slots slots, int index) throws SQLException {
        Instance<?> object = slots.getObject(index);
        if (slotDef.getAgoClass() instanceof MetaClass) {
            if (object == null) {
                preparedStatement.setNull(parameterIndex, rdbType.getSqlType());
            } else {
                preparedStatement.setString(parameterIndex, ((AgoClass) object).getFullname());
            }
            return parameterIndex + 1;
        }
        if (object == null) {
            if (rdbType.getAdditional() == null) {      // box type
                // set to null for box type
                preparedStatement.setNull(parameterIndex, rdbType.getSqlType());
            } else {
                return fillObjectParameter(preparedStatement, parameterIndex, slotDef, rdbType, null);
            }
        } else {
            if (rdbType.getAdditional() == null) {
                // rdbType already transformed to match primitive type
                return fillParameter(preparedStatement, parameterIndex, slotDef.getAgoClass().getSlotDefs()[0], rdbType, object.getSlots(), 0);
            } else {
                return fillObjectParameter(preparedStatement, parameterIndex, slotDef, rdbType, object);
            }
        }
        return null;
    }

    protected int fillObjectParameter(PreparedStatement preparedStatement, int parameterIndex, AgoSlotDef slotDef, RdbType rdbType, Instance<?> object) throws SQLException {
        if (slotDef.getAgoClass() instanceof MetaClass) {
            if (object == null) {
                preparedStatement.setString(parameterIndex, null);
            } else {
                preparedStatement.setString(parameterIndex, ((AgoClass) object).getFullname());
            }
            return parameterIndex + 1;
        }
        if (object == null || object instanceof NativeInstance) {        // TODO
            preparedStatement.setNull(parameterIndex, rdbType.getSqlType());
            preparedStatement.setString(parameterIndex + 1, null);
        } else {
            Object id = ((DbSlots<?>) object.getSlots()).getObjectRef().id();
            if(id instanceof Long l) {
                preparedStatement.setLong(parameterIndex, l);
            } else if(id instanceof String s){
                preparedStatement.setString(parameterIndex, s);
            } else if(id instanceof Integer i){
                preparedStatement.setInt(parameterIndex, i);
            } else {
                throw new IllegalStateException("unknown supported id type" + id.getClass());
            }
            preparedStatement.setString(parameterIndex + 1, slotDef.getAgoClass().getFullname());
        }
        return parameterIndex + 2;
    }

    protected int fillClassRefParameter(PreparedStatement preparedStatement, int parameterIndex, AgoClass agoClass) throws SQLException {
        preparedStatement.setString(parameterIndex, agoClass.getFullname());
        return parameterIndex + 1;
    }

    public int fillId(PreparedStatement ps, int parameterIndex, Id id) throws SQLException {
        ps.setLong(parameterIndex, (Long) id);
        return parameterIndex + 1;
    }

    protected void saveInstance(Instance<?> instance, Set<Instance<?>> saved) {
        saved.add(instance);

        if (boxTypes.isBoxType(instance.getAgoClass())) {
            return;
        }
        if (instance instanceof MetaClass && ((MetaClass) instance).getName().equals("<Meta>"))
            return;
        if (instance instanceof AgoArrayInstance aryInstance) {
            // save for ObjectArray
            if (aryInstance instanceof ObjectArrayInstance arr) {
                this.saveObjectArrayInstance(arr, saved);
            }
            return ;
        }

        if (getLangClasses().getListClass() != null && getLangClasses().getListClass().isThatOrSuperOfThat(instance.getAgoClass())) {
            var linkElementType = instance.getAgoClass().getConcreteTypeInfoAsGenericArguments().getArguments()[0];
            // save for LinkList
            if (linkElementType.getTypeCode().getValue() == OBJECT_VALUE) {
                this.saveObjectListInstance(instance, saved);
            }

            return ;
        }

        if (instance.getSlots() instanceof DbSlots<?> slots) {
            var dbSlots = (DbSlots<Id>) slots;

            if (dbSlots.getUsingInstances() != null) {
                dbSlots.getUsingInstances().removeIf(
                        value -> boxTypes.isBoxType(value.getAgoClass())
//                                || value instanceof AgoArrayInstance
                                || value instanceof MetaClass m && m.getName().equals("<Meta>"));
            }

            switch (dbSlots.getRowState()) {
                case RowState.Added:
                    dbSlots.setRowState(RowState.Saving);
                    this.insert(instance, dbSlots, instance.getAgoClass());
                    break;
                case RowState.Modified:
                    dbSlots.setRowState(RowState.Saving);
                    update(instance, dbSlots, instance.getAgoClass());       // need load ID
                    break;
//                    case RowState.Deleted:
//                        delete(rdbSlots);
//                        break;
                default:
                    if (instance instanceof CallFrameWithRunningState<?> callFrameWithRunningState) {
                        update(instance, dbSlots, instance.getAgoClass());
                    }
            }
            dbSlots.setRowState(RowState.Unchanged);
            dbSlots.clearDetachedInstances();

            if (dbSlots.getUsingInstances() != null) {
                for (Instance<?> usingInstance : dbSlots.getUsingInstances()) {
                    if (!saved.contains(usingInstance))
                        saveInstance(usingInstance, saved);
                }
            }
        }
    }

    private void saveObjectArrayInstance(ObjectArrayInstance arrayInstance, Set<Instance<?>> saved) {
        for (var valueInstance : arrayInstance.value) {
            this.saveInstance(valueInstance, saved);
        }
    }

    private void saveObjectListInstance(Instance<?> listInstance, Set<Instance<?>> saved) {
        var ls = (java.util.List<Instance<?>>) listInstance.getNativePayload();
        if (ls == null) {
            return ;
        }
        for (var item : ls) {
            this.saveInstance(item, saved);
        }
    }

    public void saveInstance(Instance<?> instance) {
        this.saveInstance(instance, new HashSet<>());
    }

    protected void insert(Instance<?> instance, DbSlots<Id> dbSlots, AgoClass agoClass) {
        var tableOfClass = tablesByClass.get(agoClass);
        StringBuilder sql = new StringBuilder("INSERT INTO " + tableOfClass.tableName()).append("(");

        sql.append("id,");

        int parameterCount = 1;
        var columns = tableOfClass.columns();

        // save related Added items at first
        for (ColumnDesc column : columns) {
            var slotDef = column.getSlotDef();
            saveUsingNewObject(dbSlots, slotDef);
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

        try(var conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int parameterIndex = 1;
                parameterIndex = this.fillId(ps, parameterIndex, dbSlots.getObjectRef().id());
                for (ColumnDesc column : columns) {
                    var slotDef = column.getSlotDef();
                    parameterIndex = this.fillParameter(ps, parameterIndex, slotDef, column.getRdbType(), dbSlots, slotDef.getIndex());
                }

                if (LOGGER.isDebugEnabled()) LOGGER.debug("EXECUTE INSERT %s : ".formatted(dbSlots.getObjectRef()) + sql);

                ps.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveUsingNewObject(DbSlots<Id> dbSlots, AgoSlotDef slotDef) {
        if (slotDef.getTypeCode() == OBJECT) {
            if (!boxTypes.isBoxTypeOrWithin(slotDef.getAgoClass()) && !(slotDef.getAgoClass() instanceof MetaClass)) {
                Instance<?> object = dbSlots.getObject(slotDef.getIndex());
                if (object != null && object.getSlots() instanceof DbSlots<?> objectSlots && objectSlots.getRowState() == RowState.Added) {
                    // saveInstance(object);
                }
            }
        }
    }

    protected void update(Instance<?> instance, DbSlots<Id> dbSlots, AgoClass agoClass) {
        RdbTable rdbTable = tablesByClass.get(agoClass);
        var columns = rdbTable.columns();

        for (var index : dbSlots.getChangedSlots()) {
            var column = rdbTable.columnDescOfSlot(index);
            var slotDef = column.getSlotDef();
            saveUsingNewObject(dbSlots, slotDef);
        }


        StringBuilder updateSql = new StringBuilder("UPDATE " + rdbTable.tableName()).append(" SET ");

        for (var index : dbSlots.getChangedSlots()) {
            var column = rdbTable.columnDescOfSlot(index);
            updateSql.append(column.getName()).append("= ?,");
            if (column.getAdditional() != null) {
                updateSql.append(column.getAdditional().getName()).append("= ?,");
            }
        }
        updateSql.setCharAt(updateSql.length() - 1, ' ');
        updateSql.append("WHERE id = ?");

        try(var conn = dataSource.getConnection()) {
            try (var ps = conn.prepareStatement(updateSql.toString())) {
                int parameterIndex = 1;
                for (var index : dbSlots.getChangedSlots()) {
                    var column = rdbTable.columnDescOfSlot(index);
                    var slotDef = column.getSlotDef();
                    parameterIndex = this.fillParameter(ps, parameterIndex, slotDef, column.getRdbType(), dbSlots, slotDef.getIndex());
                }
                this.fillId(ps, parameterIndex, dbSlots.getObjectRef().id());

                if (LOGGER.isDebugEnabled()) LOGGER.debug("{}{}", "EXECUTE UPDATE %s : ".formatted(dbSlots.getObjectRef()), updateSql);

                ps.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected RdbTable getTableOfClass(AgoClass agoClass) {
        var tableOfClass = tablesByClass.get(agoClass);
        if (tableOfClass == null)
            throw new NullPointerException("table for '%s' not found".formatted(agoClass.getFullname()));
        return tableOfClass;
    }

    public Instance<?> getById(ObjectRef<Id> objectRef, RunSpace runSpace) {
        AgoClass agoClass = classManager.getClass(objectRef.className());
        var tableOfClass = getTableOfClass(agoClass);
        StringBuilder sql = composeSelectFrom(tableOfClass);
        sql.append(" WHERE id=?");

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(sql.toString());

            this.fillId(ps, 1, objectRef.id());

            PreparedStatement finalPs = ps;
            resultSet = finalPs.executeQuery();
            var resultMapper = new ResultSetMapper(resultSet, agoClass, tableOfClass, boxTypes, runSpace);
            resultMapper.setAgoEngine((AgoEngine) classManager);
            if (resultMapper.hasNext()) {
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

    protected StringBuilder composeSelectFrom(RdbTable rdbTable) {
        StringBuilder sql = new StringBuilder("SELECT id,");
        for (ColumnDesc column : rdbTable.columns()) {
            sql.append(column.getName()).append(',');
        }
        sql.setCharAt(sql.length() - 1, ' ');
        sql.append("FROM ").append(rdbTable.tableName());
        return sql;
    }

    public ColumnDesc getColumnDesc(String className, int slot) {
        return tablesByClassName.get(className).columnDescOfSlot(slot);
    }

    public void fillPrimitiveParameter(PreparedStatement ps, int index, TypeCode typeCode, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(index, typeMapping.mapType(typeCode, null).getSqlType());
            return;
        }
        switch (typeCode.getValue()) {
            case INT_VALUE:
                ps.setInt(index, (Integer) value);
                break;
            case LONG_VALUE:
                ps.setLong(index, (Long) value);
                break;
            case SHORT_VALUE:
                ps.setShort(index, (Short) value);
                break;
            case BYTE_VALUE:
                ps.setByte(index, (Byte) value);
                break;
            case DOUBLE_VALUE:
                ps.setDouble(index, (Double) value);
                break;
            case DECIMAL_VALUE:
                ps.setBigDecimal(index, (BigDecimal) value);
                break;
            case FLOAT_VALUE:
                ps.setFloat(index, (Float) value);
                break;
            case BOOLEAN_VALUE:
                ps.setBoolean(index, (Boolean) value);
                break;
            case STRING_VALUE:
                ps.setString(index, (String) value);
                break;
            case CHAR_VALUE:
                ps.setString(index, String.valueOf((Character) value));
                break;
            case CLASS_REF_VALUE:
                ps.setString(index, (String) value);
                break;       // class name
            default:
                throw new SQLException("Unsupported primitive type: " + typeCode);
        }
    }

    @Override
    public void commitTransaction() throws SQLException {
        TransactionBoundDataSource transactionBoundDataSource = (TransactionBoundDataSource) this.getDataSource();
        transactionBoundDataSource.commit();
    }

    @Override
    public void rollbackTransaction() throws SQLException {
        TransactionBoundDataSource transactionBoundDataSource = (TransactionBoundDataSource) this.getDataSource();
        transactionBoundDataSource.rollback();
    }

    @Override
    public void close() throws SQLException {
        TransactionBoundDataSource transactionBoundDataSource = (TransactionBoundDataSource) this.getDataSource();
        transactionBoundDataSource.close();
    }
}
