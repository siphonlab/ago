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

import org.apache.commons.text.StringEscapeUtils;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

import static org.apache.commons.dbcp2.Utils.closeQuietly;
import static org.siphonlab.ago.TypeCode.OBJECT;
import static org.siphonlab.ago.TypeCode.STRING;

public abstract class DbAdapter<Id> implements org.siphonlab.ago.runtime.db.DbAdapter<Id> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbAdapter.class);

    protected final BoxTypes boxTypes;
    protected final ClassManager classManager;

    protected final TypeCode idType;
    protected final IdGenerator<Id> idGenerator;
    protected final TypeMapping typeMapping;

    protected final DataSource dataSource;

    protected Map<AgoClass, RdbTable> tablesByClass;
    protected Map<String, RdbTable> tablesByClassName;
    private RdbType idRdbType;

    public DbAdapter(ClassManager classManager, TypeCode idType, IdGenerator<Id> idGenerator, BoxTypes boxTypes, TypeMapping typeMapping, DataSource dataSource){
        this.boxTypes = boxTypes;
        this.idType = idType;
        this.idGenerator = idGenerator;
        this.typeMapping = typeMapping;
        typeMapping.setIdRdbType(this.idRdbType());
        this.dataSource = dataSource;
        this.classManager = classManager;
        this.typeMapping.initTypeMap(classManager);
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

    public ColumnDesc composeIdColumn(Set<String> usedNames) {
        ColumnDesc column = new ColumnDesc();
        column.setRdbType(idRdbType());
        column.setName("id");
        usedNames.add("id");
        column.setPrimaryKey(true);
        return column;
    }

    public ColumnDesc composeColumnDesc(AgoSlotDef slotDef, Set<String> usedNames) {
        var type = typeMapping.mapType(slotDef.getTypeCode(), slotDef.getAgoClass());
        assert type != null;
        var columnDesc = new ColumnDesc();
        columnDesc.setRdbType(type);
        columnDesc.setName(columnName(slotDef, usedNames));
        columnDesc.setSlotDef(slotDef);
        if (slotDef.getTypeCode() == OBJECT) {
            if (columnDesc.getAdditional() != null) {
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
}
