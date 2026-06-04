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

import io.ebean.config.DatabaseConfig;
import io.ebean.config.dbplatform.DatabasePlatform;
import io.ebeaninternal.dbmigration.ddlgeneration.DdlWrite;
import io.ebeaninternal.dbmigration.ddlgeneration.PlatformDdlBuilder;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.BaseTableDdl;
import io.ebeaninternal.dbmigration.migration.Column;
import io.ebeaninternal.dbmigration.migration.CreateTable;
import io.ebeaninternal.dbmigration.migration.DropTable;
import org.apache.commons.io.IOUtils;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class RdbDDLGenerator<Id> {

    protected final AgoClassLoader classLoader;
    protected final RdbAdapter<Id> rdbAdapter;
    protected final DatabasePlatform databasePlatform;

    protected Map<AgoClass, RdbTable> tables = new LinkedHashMap<>();

    public RdbDDLGenerator(AgoClassLoader classLoader, RdbAdapter<Id> rdbAdapter, DatabasePlatform databasePlatform) {
        this.classLoader = classLoader;
        this.rdbAdapter = rdbAdapter;
        this.databasePlatform = databasePlatform;
    }

    public void generate(OutputStream outputStream) throws IOException {
        // to create meta information of AgoClass and AgoInstance?
        DatabaseConfig config = new DatabaseConfig();

        BaseTableDdl ddlGen = new BaseTableDdl(config, PlatformDdlBuilder.create(databasePlatform));

        DdlWrite writer = new DdlWrite();

        generate(ddlGen, writer);

        String apply = writer.apply().getBuffer();
        String applyLast = writer.applyForeignKeys().getBuffer();

        IOUtils.write(apply,outputStream, StandardCharsets.UTF_8);
        IOUtils.write(applyLast, outputStream, StandardCharsets.UTF_8);
    }

    protected void generate(BaseTableDdl ddlGen, DdlWrite writer) {
        for (AgoClass agoClass : classLoader.getClasses()) {
            if (!agoClass.isGenericTemplate()
                    && !agoClass.isInGenericTemplate()
                    && !(agoClass instanceof AgoInterface)
                    && agoClass.getSlotDefs().length > 0) {
                ddlGen.generate(writer, createTable(agoClass));
            }
        }
    }

    protected CreateTable createTable(AgoClass agoClass) {
        CreateTable createTable = new CreateTable();
        String tableName = rdbAdapter.tableName(agoClass);
        createTable.setName(tableName);
        createTable.setPkName(rdbAdapter.primaryKeyName(agoClass));

        List<Column> columns = createTable.getColumn();

        Set<String> usedNames = new HashSet<>();

        createInstanceColumns(rdbAdapter.idRdbType(), columns, false);

        List<ColumnDesc> columnsOfSlots = new ArrayList<>();
        for (AgoSlotDef slotDef : agoClass.getSlotDefs()) {
            createColumn(slotDef, columns, usedNames, columnsOfSlots);
        }
        tables.put(agoClass, new RdbTable(tableName, columnsOfSlots));

        return createTable;
    }

    protected void createColumn(AgoSlotDef slotDef, List<Column> columns, Set<String> usedNames, List<ColumnDesc> columnDescs) {
        ColumnDesc columnDesc = rdbAdapter.composeColumnDesc(slotDef, usedNames);
        columns.add(columnDesc.toColumn());

        if(columnDesc.getAdditional() != null){
            ColumnDesc additional = columnDesc.getAdditional();
            columns.add(additional.toColumn());
        }

        columnDescs.add(columnDesc);
    }

    public void dumpClassMapper(OutputStream outputStream) throws IOException {
        var s = RdbTable.dump(tables);
        IOUtils.write(s, outputStream, StandardCharsets.UTF_8);
    }

    /** Common columns for all instance‑like tables (id, application, ... , slots) */
    protected void createInstanceColumns(RdbType idT, List<Column> cols, boolean slotsAsJson) {
        // id – PK
        Column idCol = createColumn("id", idT.getTypeName());
        idCol.setPrimaryKey(true);
        cols.add(idCol);

        // application (bigint in all tables that need it)
        cols.add(createColumn("application", idT.getTypeName()));

        // ago_class text
        cols.add(createColumn("ago_class", "text"));

        // parent_scope_id / class + creator_id / class
        objectColumn(cols, "parent_scope", idT);

        objectColumn(cols, "creator", idT);

        if(slotsAsJson) {
            cols.add(createColumn("slots", "json"));
        }
    }

    /** Class‑specific columns – called after createInstanceColumns() */
    protected void createClassColumns(List<Column> cols) {
        cols.add(createColumn("fullname", "text"));
        cols.add(createColumn("class_id", "int"));
        cols.add(createColumn("class_type", "int"));
        cols.add(createColumn("name", "varchar(1024)"));
        cols.add(createColumn("fields", "json[]"));
        cols.add(createColumn("slotDefs", "json[]"));
        cols.add(createColumn("has_slots_creator", "bool"));
        cols.add(createColumn("modifiers", "int"));
        cols.add(createColumn("super_class", "text"));
        cols.add(createColumn("interfaces", "text[]"));
        cols.add(createColumn("children", "text[]"));
        cols.add(createColumn("methods", "text[]"));
        cols.add(createColumn("parent", "text"));
        cols.add(createColumn("permit_class", "text"));
        cols.add(createColumn("parameterized_base_class", "text"));
        cols.add(createColumn("concrete_type_info", "json"));
        cols.add(createColumn("source_location", "json"));
    }

    /** Simple column helper */
    protected Column createColumn(String name, String type) {
        Column c = new Column();
        c.setName(name);
        c.setType(type);
        return c;
    }

    /** Helper to generate *_id + *_class columns for an object reference */
    protected void objectColumn(List<Column> cols, String field, RdbType idT) {
        Column idCol = createColumn(field + "_id", idT.getTypeName());
        cols.add(idCol);

        Column classCol = createColumn(field + "_class", "text");
        cols.add(classCol);
    }

}
