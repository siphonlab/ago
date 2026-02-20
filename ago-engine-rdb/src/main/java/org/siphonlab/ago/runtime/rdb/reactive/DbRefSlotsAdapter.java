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
package org.siphonlab.ago.runtime.rdb.reactive;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.runtime.rdb.ColumnDesc;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.apache.commons.dbcp2.Utils.closeQuietly;

// a SlotsAdapter for static schema mode
public class DbRefSlotsAdapter implements SlotsAdapter<RdbRefSlots> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlotsAdapter.class);

    protected final RdbAdapter rdbAdapter;

    public DbRefSlotsAdapter(RdbAdapter rdbAdapter) {
        this.rdbAdapter = rdbAdapter;
    }

    @Override
    public int getInt(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return (Integer) getSlotValue(objectRef, slot, TypeCode.INT);
    }

    @Override
    public void setInt(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, int value) {
        updateSlotValue(objectRef, slot, TypeCode.INT, value);
    }

    protected Object getSlotValue(ObjectRef objectRef, int slot, TypeCode typeCode) {
        ColumnDesc column = rdbAdapter.getColumnDesc(objectRef.className(), slot);
        StringBuilder sql = new StringBuilder("SELECT ").append(column.getName());
        for(var additional = column.getAdditional(); additional != null; additional = additional.getAdditional()){
            sql.append(",").append(additional.getName());
        }
        sql.append(" FROM ").append(rdbAdapter.getTableName(objectRef.className())).append(" WHERE id=?");

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            connection = rdbAdapter.getDataSource().getConnection();
            ps = connection.prepareStatement(sql.toString());

            rdbAdapter.fillId(ps, 1, objectRef.id());

            PreparedStatement finalPs = ps;
            resultSet = finalPs.executeQuery();
            return switch (typeCode.getValue()) {
                case TypeCode.INT_VALUE -> resultSet.getInt(1);
                case TypeCode.LONG_VALUE -> resultSet.getLong(1);
                case TypeCode.SHORT_VALUE -> resultSet.getShort(1);
                case TypeCode.BYTE_VALUE -> resultSet.getByte(1);
                case TypeCode.BOOLEAN_VALUE -> resultSet.getBoolean(1);
                case TypeCode.FLOAT_VALUE -> resultSet.getFloat(1);
                case TypeCode.DOUBLE_VALUE -> resultSet.getDouble(1);
                case TypeCode.STRING_VALUE -> resultSet.getString(1);
                case TypeCode.CLASS_REF_VALUE -> {
                    String className = resultSet.getString(1);
                    if (className == null) {
                        yield null;
                    }
                    yield rdbAdapter.getClassByName(className).getClassId();
                }
                default -> {
                    Object value = resultSet.getObject(1);
                    yield value;
                }
            };
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(ps);
            closeQuietly(resultSet);
            closeQuietly(connection);
        }
    }

    protected void updateSlotValue(ObjectRef objectRef, int slot, TypeCode typeCode, Object value) {
        StringBuilder updateSql = new StringBuilder("UPDATE " + rdbAdapter.getTableName(objectRef.className())).append(" SET ");

        ColumnDesc column = rdbAdapter.getColumnDesc(objectRef.className(), slot);
        updateSql.append(column.getName()).append("= ?");

        updateSql.append("WHERE id = ?");

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = rdbAdapter.getDataSource().getConnection();
            ps = connection.prepareStatement(updateSql.toString());

            rdbAdapter.fillPrimitiveParameter(ps,1, typeCode, value);

            rdbAdapter.fillId(ps, 2, objectRef.id());

            if (LOGGER.isDebugEnabled()) LOGGER.debug("EXECUTE UPDATE %s %s".formatted(objectRef, updateSql));

            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(ps);
            closeQuietly(connection);
        }
    }

    protected void incSlotValue(ObjectRef objectRef, int slot, TypeCode typeCode, Object value) {
        StringBuilder updateSql = new StringBuilder("UPDATE " + rdbAdapter.getTableName(objectRef.className())).append(" SET ");

        ColumnDesc column = rdbAdapter.getColumnDesc(objectRef.className(), slot);
        updateSql.append(column.getName()).append("=").append(column.getName()).append(" + ?");

        updateSql.append("WHERE id = ?");

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = rdbAdapter.getDataSource().getConnection();
            ps = connection.prepareStatement(updateSql.toString());

            rdbAdapter.fillPrimitiveParameter(ps,1, typeCode, value);

            rdbAdapter.fillId(ps, 2, objectRef.id());

            if (LOGGER.isDebugEnabled()) LOGGER.debug("EXECUTE UPDATE %s %s".formatted(objectRef, updateSql));

            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(ps);
            closeQuietly(connection);
        }
    }


    @Override
    public int getClassRef(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return (Integer) getSlotValue(objectRef, slot, TypeCode.CLASS_REF);
    }

    @Override
    public void setClassRef(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, int value) {
        updateSlotValue(objectRef, slot, TypeCode.CLASS_REF, rdbAdapter.getClassById(value).getFullname());
    }

    @Override
    public long getLong(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return (Long) getSlotValue(objectRef, slot, TypeCode.LONG);
    }

    @Override
    public void setLong(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, long value) {
        updateSlotValue(objectRef, slot, TypeCode.LONG, value);
    }

    @Override
    public float getFloat(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return (Float) getSlotValue(objectRef, slot, TypeCode.FLOAT);
    }

    @Override
    public void setFloat(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, float value) {
        updateSlotValue(objectRef, slot, TypeCode.FLOAT, value);
    }

    @Override
    public double getDouble(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return (Double) getSlotValue(objectRef, slot, TypeCode.DOUBLE);
    }

    @Override
    public void setDouble(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, double value) {
        updateSlotValue(objectRef, slot, TypeCode.DOUBLE, value);
    }

    @Override
    public byte getByte(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return (Byte) getSlotValue(objectRef, slot, TypeCode.BYTE);
    }

    @Override
    public void setByte(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, byte value) {
        updateSlotValue(objectRef, slot, TypeCode.BYTE, value);
    }

    @Override
    public short getShort(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return (Short) getSlotValue(objectRef, slot, TypeCode.SHORT);
    }

    @Override
    public void setShort(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, short value) {
        updateSlotValue(objectRef, slot, TypeCode.SHORT, value);
    }

    @Override
    public char getChar(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return (Character) getSlotValue(objectRef, slot, TypeCode.CHAR);
    }

    @Override
    public void setChar(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, char value) {
        updateSlotValue(objectRef, slot, TypeCode.CHAR, value);
    }

    @Override
    public boolean getBoolean(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return (Boolean) getSlotValue(objectRef, slot, TypeCode.BOOLEAN);
    }

    @Override
    public void setBoolean(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, boolean value) {
        updateSlotValue(objectRef, slot, TypeCode.BOOLEAN, value);
    }

    @Override
    public String getString(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return (String) getSlotValue(objectRef, slot, TypeCode.STRING);
    }

    @Override
    public void setString(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, String value) {
        updateSlotValue(objectRef, slot, TypeCode.STRING, value);
    }

    @Override
    public Instance<?> getObject(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot) {
        return null;
    }

    @Override
    public void setObject(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, Instance<?> value) {
        updateSlotValue(objectRef, slot, TypeCode.OBJECT, value);
    }

    @Override
    public void incInt(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, int value) {
        incSlotValue(objectRef, slot, TypeCode.INT, value);
    }

    @Override
    public void incFloat(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, float value) {
        incSlotValue(objectRef, slot, TypeCode.FLOAT, value);
    }

    @Override
    public void incDouble(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, double value) {
        incSlotValue(objectRef,slot, TypeCode.DOUBLE, value);
    }

    @Override
    public void incByte(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, byte value) {
        incSlotValue(objectRef, slot, TypeCode.BYTE, value);
    }

    @Override
    public void incShort(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, short value) {
        incSlotValue(objectRef, slot, TypeCode.SHORT, value);
    }

    @Override
    public void incLong(RdbRefSlots rdbRefSlots, ObjectRef objectRef, int slot, long value) {
        incSlotValue(objectRef, slot, TypeCode.LONG, value);
    }

    @Override
    public String mapType(TypeCode typeCode, AgoClass agoClass) {
        return rdbAdapter.mapType(typeCode,agoClass).getTypeName();
    }
}
