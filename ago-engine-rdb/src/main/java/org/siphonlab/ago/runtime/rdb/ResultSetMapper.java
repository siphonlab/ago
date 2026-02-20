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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.siphonlab.ago.*;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.apache.commons.dbcp2.Utils.closeQuietly;
import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.NULL_VALUE;
import static org.siphonlab.ago.TypeCode.OBJECT_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;

public class ResultSetMapper {

    private final ResultSet resultSet;
    private final TableOfClass tableOfClass;
    private final BoxTypes boxTypes;

    private RdbEngine agoEngine;
    private boolean closed = false;
    private CallFrame<?> creator;

    public ResultSetMapper(ResultSet resultSet, TableOfClass tableOfClass, BoxTypes boxTypes){
        this.resultSet = resultSet;
        this.tableOfClass = tableOfClass;
        this.boxTypes = boxTypes;
    }

    public void close(){
        this.closed = true;
        closeQuietly(resultSet);
    }

    public boolean hasNext() throws SQLException {
        if(closed) return false;
        return resultSet.next();
    }

    public Instance<?> next() throws SQLException {
        var instance = agoEngine.createInstance(tableOfClass.agoClass(), creator);
        RdbSlots slots = (RdbSlots) instance.getSlots();
        slots.setId(resultSet.getLong("id"));
        for (ColumnDesc column : tableOfClass.columns()) {
            AgoSlotDef slotDef = column.getSlotDef();
            int slotIndex = slotDef.getIndex();
            int columnIndex = slotIndex + 1 + 1;        // skip id
            switch (slotDef.getTypeCode().value){
                case INT_VALUE:
                    slots.setInt(slotIndex, resultSet.getInt(columnIndex));
                    break;
                case LONG_VALUE:
                    slots.setLong(slotIndex, resultSet.getLong(columnIndex));
                    break;
                case FLOAT_VALUE:
                    slots.setFloat(slotIndex, resultSet.getFloat(columnIndex));
                    break;
                case DOUBLE_VALUE:
                    slots.setDouble(slotIndex, resultSet.getDouble(columnIndex));
                    break;
                case BOOLEAN_VALUE:
                    slots.setBoolean(slotIndex, resultSet.getBoolean(columnIndex));
                    break;
                case STRING_VALUE:
                    slots.setString(slotIndex, resultSet.getString(columnIndex));
                    break;
                case SHORT_VALUE:
                    slots.setShort(slotIndex, resultSet.getShort(columnIndex));
                    break;
                case BYTE_VALUE:
                    slots.setByte(slotIndex, resultSet.getByte(columnIndex));
                    break;
                case CHAR_VALUE:
                    slots.setChar(slotIndex, resultSet.getString(columnIndex).charAt(0));
                    break;
                case OBJECT_VALUE:
                    if(slotDef.getAgoClass() instanceof MetaClass){
                        String cls = resultSet.getString(columnIndex);
                        if(cls != null){
                            slots.setObject(slotIndex, agoEngine.getClass(cls));
                        }
                    } else {
                        if(column.getAdditional() == null){     // box type
                            slots.setObject(slotIndex, box(column.getRdbType().getTypeCode(), slotDef.getAgoClass(), resultSet, columnIndex));
                        } else {
                            throw new RuntimeException("TODO");
//                            slots.setObject();
                        }
                    }
                    break;
                case NULL_VALUE:
                    throw new UnsupportedOperationException("null??");
                case CLASS_REF_VALUE:
                    var classname = resultSet.getString(columnIndex);
                    var classId = agoEngine.getClass(classname).getClassId();
                    slots.setClassRef(slotIndex, classId);
                    break;
            }
        }
        slots.setRowState(RowState.Unchanged);
        return instance;
    }

    private Instance<?> box(TypeCode typeCode, AgoClass agoClass, ResultSet resultSet, int columnIndex) throws SQLException {
        if(agoClass == boxTypes.getBoxType(typeCode)) {
            Boxer boxer = agoEngine.getBoxer();
            switch (typeCode.value) {
                case INT_VALUE:
                    return boxer.boxInt(resultSet.getInt(columnIndex));
                case LONG_VALUE:
                    return boxer.boxLong(resultSet.getLong(columnIndex));
                case FLOAT_VALUE:
                    return boxer.boxFloat(resultSet.getFloat(columnIndex));
                case DOUBLE_VALUE:
                    return boxer.boxDouble(resultSet.getDouble(columnIndex));
                case BOOLEAN_VALUE:
                    return boxer.boxBoolean(resultSet.getBoolean(columnIndex));
                case STRING_VALUE:
                    return boxer.boxString(resultSet.getString(columnIndex));
                case SHORT_VALUE:
                    return boxer.boxShort(resultSet.getShort(columnIndex));
                case BYTE_VALUE:
                    return boxer.boxByte(resultSet.getByte(columnIndex));
                case CHAR_VALUE:
                    return boxer.boxChar(resultSet.getString(columnIndex).charAt(0));
            }
        } else {
            Instance<?> instance = agoEngine.createInstance(agoClass,creator);
            Slots slots = instance.getSlots();
            switch (typeCode.value) {
                case INT_VALUE:
                    slots.setInt(0, resultSet.getInt(columnIndex));
                    break;
                case LONG_VALUE:
                    slots.setLong(0, resultSet.getLong(columnIndex));
                    break;
                case FLOAT_VALUE:
                    slots.setFloat(0, resultSet.getFloat(columnIndex));
                    break;
                case DOUBLE_VALUE:
                    slots.setDouble(0, resultSet.getDouble(columnIndex));
                    break;
                case BOOLEAN_VALUE:
                    slots.setBoolean(0, resultSet.getBoolean(columnIndex));
                    break;
                case STRING_VALUE:
                    slots.setString(0, resultSet.getString(columnIndex));
                    break;
                case SHORT_VALUE:
                    slots.setShort(0, resultSet.getShort(columnIndex));
                    break;
                case BYTE_VALUE:
                    slots.setByte(0, resultSet.getByte(columnIndex));
                    break;
                case CHAR_VALUE:
                    slots.setChar(0, resultSet.getString(columnIndex).charAt(0));
                    break;
            }
            return instance;
        }
        throw new UnsupportedOperationException("unknown type code " + typeCode);
    }

    public RdbEngine getAgoEngine() {
        return agoEngine;
    }

    public void setAgoEngine(RdbEngine agoEngine) {
        this.agoEngine = agoEngine;
    }

    public void setCreator(CallFrame<?> creator) {
        this.creator = creator;
    }

    public CallFrame<?> getCreator() {
        return creator;
    }

    public static class JsonSerializer extends com.fasterxml.jackson.databind.JsonSerializer<ResultSetMapper> {
        private final ObjectMapper jsonObjectMapper;

        public JsonSerializer(ObjectMapper jsonObjectMapper) {
            this.jsonObjectMapper = jsonObjectMapper;
        }

        @Override
        public void serialize(ResultSetMapper value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartArray();
            try {
                while (value.hasNext()) {
                    gen.writeObject(value.next());
                }
            } catch (SQLException e){
                throw new IOException(e);
            } finally {
                try {
                    value.close();
                } catch (Exception e){

                }
            }
            gen.writeEndArray();
        }
    }
}
