package org.siphonlab.ago.runtime.rdb;

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.AgoArrayInstance;
import org.siphonlab.ago.runtime.ObjectArrayInstance;
import org.siphonlab.ago.runtime.db.CallFrameWithRunningState;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.IdGenerator;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.dbcp2.Utils.closeQuietly;
import static org.siphonlab.ago.TypeCode.*;

public abstract class EntityDbAdapter<Id> extends DbAdapter<Id> {

    private final static Logger LOGGER = LoggerFactory.getLogger(EntityDbAdapter.class);

    public EntityDbAdapter(ClassManager classManager, TypeCode idType, IdGenerator<Id> idGenerator, BoxTypes boxTypes, TypeMapping typeMapping, DataSource dataSource) {
        super(classManager, idType, idGenerator, boxTypes, typeMapping, dataSource);
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

    public int fillId(PreparedStatement ps, int parameterIndex, Object id) throws SQLException {
        ps.setLong(parameterIndex, (Long) id);
        return parameterIndex + 1;
    }

    protected void saveInstance(@Nonnull Connection conn, Instance<?> instance, Set<Instance<?>> saved) {
        saved.add(instance);

        if (boxTypes.isBoxType(instance.getAgoClass())) {
            return;
        }
        if(instance instanceof AgoArrayInstance){
            if(instance instanceof ObjectArrayInstance arrayInstance){
                for (Instance<?> el : arrayInstance.value) {
                    saveInstance(conn, el, saved);
                }
            }
            return;
        }
        if (instance instanceof MetaClass && ((MetaClass) instance).getName().equals("<Meta>"))
            return;

        if (instance.getSlots() instanceof DbSlots<?> dbSlots) {
            if (dbSlots.getUsingInstances() != null) {
                dbSlots.getUsingInstances().removeIf(
                        value -> boxTypes.isBoxType(value.getAgoClass())
                                || value instanceof AgoArrayInstance
                                || value instanceof MetaClass m && m.getName().equals("<Meta>"));
            }

            switch (dbSlots.getRowState()) {
                case RowState.Added:
                    dbSlots.setRowState(RowState.Saving);
                    this.insert(conn, instance, (DbSlots<Id>) dbSlots, instance.getAgoClass());
                    break;
                case RowState.Modified:
                    dbSlots.setRowState(RowState.Saving);
                    update(conn, instance, dbSlots, instance.getAgoClass());       // need load ID
                    break;
//                    case RowState.Deleted:
//                        delete(rdbSlots);
//                        break;
                default:
                    if (instance instanceof CallFrameWithRunningState<?> callFrameWithRunningState) {
                        update(conn, instance, dbSlots, instance.getAgoClass());
                    }
            }
            dbSlots.setRowState(RowState.Unchanged);
            dbSlots.clearDetachedInstances();

            if (dbSlots.getUsingInstances() != null) {
                for (Instance<?> usingInstance : dbSlots.getUsingInstances()) {
                    if (!saved.contains(usingInstance))
                        saveInstance(conn, usingInstance, saved);
                }
            }
        }
    }

    // save an instance using exists connection.
    public void saveWithConn(@Nonnull Connection conn, Instance<?> instance) {
        this.saveInstance(conn, instance, new HashSet<>());
    }

    public void saveInstance(Instance<?> instance) {
        try (var conn = this.dataSource.getConnection()) {
            this.saveWithConn(conn, instance);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void insert(@Nonnull Connection conn, Instance<?> instance, DbSlots<Id> dbSlots, AgoClass agoClass) {
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

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            parameterIndex = this.fillId(ps, parameterIndex, dbSlots.getObjectRef());
            for (ColumnDesc column : columns) {
                var slotDef = column.getSlotDef();
                parameterIndex = this.fillParameter(ps, parameterIndex, slotDef, column.getRdbType(), dbSlots, slotDef.getIndex());
            }

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("EXECUTE INSERT %d : ".formatted(dbSlots.getObjectRef()) + sql);
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveUsingNewObject(DbSlots dbSlots, AgoSlotDef slotDef) {
        if (slotDef.getTypeCode() == OBJECT) {
            if (!boxTypes.isBoxTypeOrWithin(slotDef.getAgoClass()) && !(slotDef.getAgoClass() instanceof MetaClass)) {
                Instance<?> object = dbSlots.getObject(slotDef.getIndex());
                if (object != null && object.getSlots() instanceof DbSlots objectSlots && objectSlots.getRowState() == RowState.Added) {
                    // saveInstance(object);
                }
            }
        }
    }

    protected void update(
            @Nonnull Connection conn,
            Instance<?> instance,
            DbSlots dbSlots,
            AgoClass agoClass
    ) {
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

        try (var ps = conn.prepareStatement(updateSql.toString())) {
            int parameterIndex = 1;
            for (var index : dbSlots.getChangedSlots()) {
                var column = rdbTable.columnDescOfSlot(index);
                var slotDef = column.getSlotDef();
                parameterIndex = this.fillParameter(ps, parameterIndex, slotDef, column.getRdbType(), dbSlots, slotDef.getIndex());
            }
            this.fillId(ps, parameterIndex, dbSlots.getObjectRef());

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("{}{}", "EXECUTE INSERT %d : ".formatted(dbSlots.getObjectRef()), updateSql);

            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
            return new ResultSetMapper(finalPs.executeQuery(), tableOfClass, boxTypes) {
                public void close() {
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

    private RdbTable getTableOfClass(AgoClass agoClass) {
        var tableOfClass = tablesByClass.get(agoClass);
        if (tableOfClass == null)
            throw new NullPointerException("table for '%s' not found".formatted(agoClass.getFullname()));
        return tableOfClass;
    }

    public Instance<?> getById(ObjectRef<Id> objectRef) {
        var tableOfClass = getTableOfClass(classManager.getClass(objectRef.className()));
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
            var resultMapper = new ResultSetMapper(resultSet, tableOfClass, boxTypes);
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

}
