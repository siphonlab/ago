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
import org.apache.commons.io.IOUtils;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RdbDDLGenerator {

    protected final AgoClassLoader classLoader;
    protected final RdbAdapter rdbAdapter;
    protected final DatabasePlatform databasePlatform;

    protected Map<AgoClass, TableOfClass> tables = new LinkedHashMap<>();

    public RdbDDLGenerator(AgoClassLoader classLoader, RdbAdapter rdbAdapter, DatabasePlatform databasePlatform) {
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

    public void dumpClassMapper(OutputStream outputStream) throws IOException {
        var s = TableOfClass.dump(tables);
        IOUtils.write(s, outputStream, StandardCharsets.UTF_8);
    }

    protected CreateTable createTable(AgoClass agoClass) {
        CreateTable createTable = new CreateTable();
        String tableName = rdbAdapter.tableName(agoClass);
        createTable.setName(tableName);
        createTable.setPkName(rdbAdapter.primaryKeyName(agoClass));

        List<Column> columns = createTable.getColumn();

        Set<String> usedNames = new HashSet<>();

        ColumnDesc pkColumnDesc = rdbAdapter.composeIdColumn(usedNames);

        rdbAdapter.composeInstanceColumns(createTable, agoClass, usedNames);

        Column pk = rdbAdapter.toColumn(pkColumnDesc);
        pk.setPrimaryKey(true);
        columns.add(pk);

        List<ColumnDesc> columnsOfSlots = new ArrayList<>();
        for (AgoSlotDef slotDef : agoClass.getSlotDefs()) {
            createColumn(slotDef, columns, usedNames, columnsOfSlots);
        }
        tables.put(agoClass, new TableOfClass(agoClass, tableName, columnsOfSlots));

        return createTable;
    }

    protected void createColumn(AgoSlotDef slotDef, List<Column> columns, Set<String> usedNames, List<ColumnDesc> columnDescs) {
        ColumnDesc columnDesc = rdbAdapter.composeColumnDesc(slotDef, usedNames);
        columns.add(rdbAdapter.toColumn(columnDesc));

        if(columnDesc.getAdditional() != null){
            ColumnDesc additional = columnDesc.getAdditional();
            columns.add(rdbAdapter.toColumn(additional));
        }

        columnDescs.add(columnDesc);
    }

}
