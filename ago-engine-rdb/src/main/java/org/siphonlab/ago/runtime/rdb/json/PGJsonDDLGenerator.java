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
package org.siphonlab.ago.runtime.rdb.json;

import io.ebean.config.dbplatform.DatabasePlatform;
import io.ebeaninternal.dbmigration.ddlgeneration.DdlWrite;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.BaseTableDdl;
import io.ebeaninternal.dbmigration.migration.Column;
import io.ebeaninternal.dbmigration.migration.CreateTable;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbDDLGenerator;
import org.siphonlab.ago.runtime.rdb.RdbType;

import java.util.List;

public class PGJsonDDLGenerator extends RdbDDLGenerator {

    public PGJsonDDLGenerator(AgoClassLoader classLoader, RdbAdapter rdbAdapter, DatabasePlatform databasePlatform) {
        super(classLoader, rdbAdapter, databasePlatform);
    }

    // we always generate 3 tables, ago_class, ago_instance, ago_callframe, however, they are all instances indeed
    @Override
    protected void generate(BaseTableDdl ddlGen, DdlWrite writer) {
        ddlGen.generate(writer, createStringsTable());
        ddlGen.generate(writer, createBlobsTable());

        ddlGen.generate(writer, createClassTable());
        ddlGen.generate(writer, createFunctionTable());

        ddlGen.generate(writer,createInstanceTable());
        ddlGen.generate(writer, createCallFrameTable());
    }

    CreateTable createInstanceTable() {
        CreateTable createTable = new CreateTable();
        createTable.setName("ago_instance");
        createTable.setPkName("pk_ago_instance");
        List<Column> columns = createTable.getColumn();
        RdbType idType = rdbAdapter.idType();

        createInstanceColumns(idType, columns);

        createCallFrameTable();

        return createTable;
    }

    CreateTable createCallFrameTable() {
        CreateTable createTable = new CreateTable();
        createTable.setName("ago_frame");
        createTable.setPkName("pk_ago_frame");
        List<Column> columns = createTable.getColumn();
        RdbType idType = rdbAdapter.idType();

        createInstanceColumns(idType, columns);

        objectColumn(columns, "caller", idType);
        columns.add(createColumn("state", "int"));
        columns.add(createColumn("pc", "int"));
        columns.add(createColumn("receiver_slot", "int"));
        objectColumn(columns,"exception",idType);
        columns.add(createColumn("runspace","text"));

        return createTable;
    }


    CreateTable createClassTable(){
        CreateTable createTable = new CreateTable();
        createTable.setName("ago_class");
        createTable.setPkName("pk_ago_class");
        List<Column> columns = createTable.getColumn();
        RdbType idType = rdbAdapter.idType();

        createInstanceColumns(idType, columns);
        createClassColumns(columns);

        return createTable;
    }

    CreateTable createStringsTable() {
        CreateTable createTable = new CreateTable();
        createTable.setName("ago_string");
        createTable.setPkName("pk_ago_string");
        List<Column> columns = createTable.getColumn();
        RdbType idType = rdbAdapter.idType();

        columns.add(createColumn("id", idType.getTypeName()));
        var app = createColumn("application", "int");
        columns.add(app);
        var index = createColumn("index", "int");
        columns.add(index);
        app.setPrimaryKey(true);
        index.setPrimaryKey(true);

        columns.add(createColumn("value","text"));

        return createTable;
    }

    CreateTable createBlobsTable() {
        CreateTable createTable = new CreateTable();
        createTable.setName("ago_blob");
        createTable.setPkName("pk_ago_blob");
        List<Column> columns = createTable.getColumn();
        RdbType idType = rdbAdapter.idType();

        columns.add(createColumn("id", idType.getTypeName()));
        var app = createColumn("application", "int");
        columns.add(app);
        var index = createColumn("index", "int");
        columns.add(index);
        app.setPrimaryKey(true);
        index.setPrimaryKey(true);
        columns.add(createColumn("data", "bytea"));

        return createTable;
    }

    private static void createClassColumns(List<Column> columns) {
        columns.add(createColumn("fullname","text"));
        columns.add(createColumn("class_id", "int"));
        columns.add(createColumn("class_type", "int"));
        columns.add(createColumn("name","varchar(1024)"));
        columns.add(createColumn("fields", "jsonb[]"));
        columns.add(createColumn("slotDefs", "jsonb[]"));
        columns.add(createColumn("has_slots_creator", "bool"));
        columns.add(createColumn("modifiers", "int"));
        columns.add(createColumn("super_class", "text"));
        columns.add(createColumn("interfaces", "text[]"));
        columns.add(createColumn("children", "text[]"));
        columns.add(createColumn("methods", "text[]"));
        columns.add(createColumn("parent", "text"));
        columns.add(createColumn("permit_class", "text"));
        columns.add(createColumn("parameterized_base_class", "text"));
        columns.add(createColumn("concrete_type_info", "jsonb"));
        columns.add(createColumn("source_location", "jsonb"));
    }

    CreateTable createFunctionTable() {
        CreateTable createTable = new CreateTable();
        createTable.setName("ago_function");
        createTable.setPkName("pk_ago_function");
        List<Column> columns = createTable.getColumn();
        RdbType idType = rdbAdapter.idType();

        createInstanceColumns(idType, columns);

        createClassColumns(columns);

        columns.add(createColumn("variables","jsonb[]"));
        columns.add(createColumn("parameters", "jsonb[]"));
        columns.add(createColumn("switch_tables", "jsonb[]"));
        columns.add(createColumn("try_catch_items", "jsonb[]"));
        columns.add(createColumn("source_map_entries", "jsonb[]"));
        columns.add(createColumn("code","int[]"));
        columns.add(createColumn("native_function_entrance", "varchar(1024)"));
        columns.add(createColumn("native_function_result_slot", "int"));
        columns.add(createColumn("result_type", "jsonb"));

        return createTable;
    }


    private static void createInstanceColumns(RdbType idType, List<Column> columns) {
        var id = createColumn("id", idType.getTypeName());
        id.setPrimaryKey(true);

        columns.add(id);

        columns.add(createColumn("application", idType.getTypeName()));        // application id, each app has an id, which means a storage space

        columns.add(createColumn("ago_class","text"));

        objectColumn(columns, "parent_scope", idType);

        objectColumn(columns,"creator",idType);

        columns.add(createColumn("slots", "jsonb"));

    }

    static Column createColumn(String columnName, String type){
        var col = new Column();
        col.setName(columnName);
        col.setType(type);
        return col;
    }

    static void objectColumn(List<Column> columns, String fieldName, RdbType idType){
        var id = new Column();
        id.setName(fieldName + "_id");
        id.setType(idType.getTypeName());
        columns.add(id);

        var className = new Column();
        className.setName(fieldName + "_class");
        className.setType("text");
        columns.add(className);
    }
}
