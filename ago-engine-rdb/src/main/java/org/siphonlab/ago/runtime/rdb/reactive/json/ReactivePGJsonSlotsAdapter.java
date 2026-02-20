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
package org.siphonlab.ago.runtime.rdb.reactive.json;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import org.postgresql.util.PGobject;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.reactive.SlotsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import static org.apache.commons.dbcp2.Utils.closeQuietly;
import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;

public class ReactivePGJsonSlotsAdapter implements SlotsAdapter<ReactiveJsonRefSlotsWithCallFrame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactivePGJsonSlotsAdapter.class);

    final ReactiveJsonPGAdapter adapter;

    public ReactivePGJsonSlotsAdapter(ReactiveJsonPGAdapter adapter) {
        this.adapter = adapter;
    }

    protected Object getSlotValue(CallFrame<?> callFrame, ObjectRef objectRef, String jsonFiledName, String typeName, int slot, TypeCode typeCode) {
        String sql;
        if (typeCode == TypeCode.STRING || typeCode == TypeCode.OBJECT || typeCode == TypeCode.CLASS_REF) {
            sql = "SELECT nullif((slots->>'%s'), 'null')::%s FROM %s WHERE id=?".formatted(
                    jsonFiledName, typeName, adapter.getTableName(objectRef.className())
            );
        } else {
            sql = "SELECT (slots->>'%s')::%s FROM %s WHERE id=?".formatted(
                    jsonFiledName, typeName, adapter.getTableName(objectRef.className())
            );
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            connection = adapter.getDataSource().getConnection();
            ps = connection.prepareStatement(sql);

            adapter.fillId(ps, 1, objectRef.id());

            resultSet = ps.executeQuery();
            if(resultSet.next()) {
                return switch (typeCode.getValue()) {
                    case TypeCode.INT_VALUE -> resultSet.getInt(1);
                    case TypeCode.LONG_VALUE -> resultSet.getLong(1);
                    case TypeCode.SHORT_VALUE -> resultSet.getShort(1);
                    case TypeCode.BYTE_VALUE -> resultSet.getByte(1);
                    case TypeCode.BOOLEAN_VALUE -> resultSet.getBoolean(1);
                    case TypeCode.FLOAT_VALUE -> resultSet.getFloat(1);
                    case TypeCode.DOUBLE_VALUE -> resultSet.getDouble(1);
                    case TypeCode.STRING_VALUE -> resultSet.getString(1);
                    case TypeCode.OBJECT_VALUE -> {
                        PGobject obj = (PGobject) resultSet.getObject(1);
                        if(obj == null) yield null;
                        String json = obj.getValue();
                        Map<String, Object> r = (Map<String, Object>) new JsonSlurper().parseText(json);
                        ObjectRef ref = new ObjectRef((String) r.get("@type"), (Long)r.get("@id"));
                        yield adapter.restoreInstance(connection, ref);
                    }
                    case TypeCode.CLASS_REF_VALUE -> {
                        String className = resultSet.getString(1);
                        if (className == null) {
                            yield null;
                        }
                        yield adapter.getClassByName(className).getClassId();
                    }
                    default -> throw new IllegalArgumentException("unknown type code" + typeCode);
                };
            } else {
                return TypeCode.defaultValue(typeCode);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(ps);
            closeQuietly(resultSet);
            closeQuietly(connection);
        }
    }

    protected Instance<?> getObjectSlotValue(CallFrame<?> callFrame, ObjectRef objectRef, String jsonFiledName, String typeName, int slot, AgoSlotDef slotDef) {
        var unboxType = adapter.getBoxTypes().getUnboxType(slotDef.getAgoClass());
        if (unboxType != null) {
            var v = getSlotValue(callFrame, objectRef, jsonFiledName, typeName, slot, unboxType);
            final Boxer boxer = callFrame.getAgoEngine().getBoxer();
            return switch (unboxType.getValue()) {
                case INT_VALUE -> boxer.boxInt(callFrame, slotDef.getAgoClass(), (Integer)v);
                case LONG_VALUE -> boxer.boxLong(callFrame, slotDef.getAgoClass(), (Long) v);
                case FLOAT_VALUE -> boxer.boxFloat(callFrame, slotDef.getAgoClass(), (Float) v);
                case DOUBLE_VALUE -> boxer.boxDouble(callFrame, slotDef.getAgoClass(), (Double) v);
                case SHORT_VALUE -> boxer.boxShort(callFrame, slotDef.getAgoClass(), (Short) v);
                case BYTE_VALUE -> boxer.boxByte(callFrame, slotDef.getAgoClass(), (Byte) v);
                default -> throw new IllegalArgumentException("unknown unbox type: " + unboxType);
            };
        } else if (slotDef.getAgoClass() instanceof MetaClass) {
            var v = getSlotValue(callFrame, objectRef, jsonFiledName, typeName, slot, STRING);
            return callFrame.getAgoEngine().getClass((String) v);
        }
        String sql = "SELECT nullif((slots->'%s'), 'null') FROM %s WHERE id=?".formatted(
                jsonFiledName, adapter.getTableName(objectRef.className())
        );

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            connection = adapter.getDataSource().getConnection();
            ps = connection.prepareStatement(sql);

            adapter.fillId(ps, 1, objectRef.id());

            resultSet = ps.executeQuery();
            if (resultSet.next()) {
                PGobject obj = (PGobject) resultSet.getObject(1);
                if (obj == null) return null;
                String json = obj.getValue();
                Map<String, Object> r = (Map<String, Object>) new JsonSlurper().parseText(json);
                ObjectRef ref = new ObjectRef((String) r.get("@type"), (Long) r.get("@id"));
                return adapter.restoreInstance(connection, ref);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(ps);
            closeQuietly(resultSet);
            closeQuietly(connection);
        }
    }

    protected void updateSlotValue(ObjectRef objectRef, String jsonFieldName, String typeName, int slot, TypeCode typeCode, Object value) {
        String updateSql = "UPDATE %s SET slots['%s'] = to_jsonb(?::%s) WHERE id=?".formatted(
                adapter.getTableName(objectRef.className()), jsonFieldName, typeName
        );

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = adapter.getDataSource().getConnection();
            ps = connection.prepareStatement(updateSql.toString());

            if(typeCode != TypeCode.OBJECT) {
                adapter.fillPrimitiveParameter(ps, 1, typeCode, value);
            }

            adapter.fillId(ps, 2, objectRef.id());

            if (LOGGER.isDebugEnabled()) LOGGER.debug("EXECUTE UPDATE %s %s".formatted(objectRef, ps));

            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(ps);
            closeQuietly(connection);
        }
    }


    protected void updateObjectSlotValue(CallFrame<?> callFrame, ObjectRef objectRef, String jsonFieldName, String typeName, int slot, AgoSlotDef slotDef, Object value) {
        if(value instanceof AgoClass agoClass) {
            updateSlotValue(objectRef,jsonFieldName,typeName,slot,STRING, agoClass.getFullname());
            return;
        }

        String updateSql = "UPDATE %s SET slots['%s'] = ? WHERE id=?".formatted(
                adapter.getTableName(objectRef.className()), jsonFieldName
        );

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = adapter.getDataSource().getConnection();
            ps = connection.prepareStatement(updateSql);

            if (value == null) {
                ps.setNull(1, Types.NULL);
            } else {
                Instance<?> instance = (Instance<?>) value;
                ReactiveJsonRefSlots slots = (ReactiveJsonRefSlots) instance.getSlots();
                PGobject obj = new PGobject();
                obj.setType("jsonb");
                obj.setValue(JsonOutput.toJson(Map.of("@id", slots.getObjectRef().id(), "@type", slots.getObjectRef().className())));
                ps.setObject(1, obj);
            }

            adapter.fillId(ps, 2, objectRef.id());

            if (LOGGER.isDebugEnabled()) LOGGER.debug("EXECUTE UPDATE %s %s".formatted(objectRef, updateSql));

            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(ps);
            closeQuietly(connection);
        }
    }

    protected void incSlotValue(ObjectRef objectRef, String jsonFieldName, String typeName, int slot, TypeCode typeCode, Object value) {
        String updateSql = "UPDATE %s SET slots=jsonb_set(slots, '{%s}', slots->'%s'::%s + ?) WHERE id=?".formatted(
                adapter.getTableName(objectRef.className()), jsonFieldName, jsonFieldName, typeName
        );

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = adapter.getDataSource().getConnection();
            ps = connection.prepareStatement(updateSql);

            adapter.fillPrimitiveParameter(ps, 1, typeCode, value);

            adapter.fillId(ps, 2, objectRef.id());

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
    public int getInt(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        return (Integer) getSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.INT);
    }

    @Override
    public void setInt(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, int value) {
        updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.INT, value);
    }

    @Override
    public int getClassRef(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        var classRef = (Integer) getSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.CLASS_REF);
        if(classRef == null) return -1;
        return classRef;
    }

    @Override
    public void setClassRef(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, int value) {
        if(value != -1){
            updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.CLASS_REF, adapter.getClassById(value).getFullname());
        } else {
            updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.CLASS_REF, null);
        }
    }

    @Override
    public long getLong(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        return (Long)getSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.LONG);
    }

    @Override
    public void setLong(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, long value) {
        updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.LONG, value);
    }

    @Override
    public float getFloat(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        return (Float) getSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.FLOAT);
    }

    @Override
    public void setFloat(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, float value) {
        updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.FLOAT, value);
    }

    @Override
    public double getDouble(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        return (Double) getSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.DOUBLE);
    }

    @Override
    public void setDouble(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, double value) {
        updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.DOUBLE, value);
    }

    @Override
    public byte getByte(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        Object slotValue = getSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, BYTE);
        return (Byte) slotValue;
    }

    @Override
    public void setByte(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, byte value) {
        updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.BYTE, value);
    }

    @Override
    public short getShort(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        return (Short)getSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.SHORT);
    }

    @Override
    public void setShort(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, short value) {
        updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.SHORT, value);
    }

    @Override
    public char getChar(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        return (Character) getSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.CHAR);
    }

    @Override
    public void setChar(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, char value) {
        updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.CHAR, value);
    }

    @Override
    public boolean getBoolean(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        return (Boolean) getSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.BOOLEAN);
    }

    @Override
    public void setBoolean(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, boolean value) {
        updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.BOOLEAN, value);
    }

    @Override
    public String getString(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        return (String)getSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.STRING);
    }

    @Override
    public void setString(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, String value) {
        updateSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.STRING, value);
    }

    @Override
    public Instance<?> getObject(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot) {
        return getObjectSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, slots.getSlotDef(slot));
    }

    @Override
    public void setObject(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, Instance<?> value) {
        updateObjectSlotValue(slots.getCallFrame(), objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, slots.getSlotDef(slot), value);
    }

    @Override
    public void incInt(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, int value) {
        incSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.INT, value);
    }

    @Override
    public void incFloat(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, float value) {
        incSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.FLOAT, value);
    }

    @Override
    public void incDouble(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, double value) {
        incSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.DOUBLE, value);
    }

    @Override
    public void incByte(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, byte value) {
        incSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.BYTE, value);
    }

    @Override
    public void incShort(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, short value) {
        incSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.SHORT, value);
    }

    @Override
    public void incLong(ReactiveJsonRefSlotsWithCallFrame slots, ObjectRef objectRef, int slot, long value) {
        incSlotValue(objectRef, slots.getFieldName(slot), slots.getDataType(slot), slot, TypeCode.LONG, value);
    }

    @Override
    public String mapType(TypeCode typeCode, AgoClass agoClass) {
        return adapter.mapType(typeCode, agoClass).getTypeName();
    }

}
