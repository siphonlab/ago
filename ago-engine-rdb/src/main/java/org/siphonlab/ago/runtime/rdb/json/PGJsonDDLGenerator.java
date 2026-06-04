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

public class PGJsonDDLGenerator<Id> extends RdbDDLGenerator<Id> {

    public PGJsonDDLGenerator(AgoClassLoader classLoader,
                              RdbAdapter<Id> rdbAdapter,
                              DatabasePlatform databasePlatform) {
        super(classLoader, rdbAdapter, databasePlatform);
    }

    @Override
    protected void generate(BaseTableDdl ddlGen, DdlWrite writer) {
        // 1. runspace table – must be first because other tables refer to it
        ddlGen.generate(writer, createRunspaceTable());

        // 2. string / blob tables (no PK on id column)
        ddlGen.generate(writer, createStringsTable());
        ddlGen.generate(writer, createBlobsTable());

        // 3. class & function – both inherit the instance columns
        ddlGen.generate(writer, createClassTable());
        ddlGen.generate(writer, createFunctionTable());

        // 4. instance table
        ddlGen.generate(writer, createInstanceTable());

        // 5. frame (callframe) table
        ddlGen.generate(writer, createCallFrameTable());
    }

    /* ------------------------------------------------------------------ */
    /* runspace table                                                     */
    /* ------------------------------------------------------------------ */

    private CreateTable createRunspaceTable() {
        CreateTable ct = new CreateTable();
        ct.setName("ago_runspace");
        ct.setPkName("pk_ago_runspace");

        List<Column> cols = ct.getColumn();
        RdbType idT = rdbAdapter.idRdbType();

        // primary key
        Column idCol = createColumn("id", idT.getTypeName());
        idCol.setPrimaryKey(true);
        cols.add(idCol);

        // other columns
        cols.add(createColumn("application", idT.getTypeName()));
        cols.add(createColumn("native_host_class", "text"));
        cols.add(createColumn("curr_frame_table", "varchar(1024)"));
        cols.add(createColumn("curr_frame_id", "bigint"));
        cols.add(createColumn("result_slots", "jsonb")); // json in SQL – use jsonb
        cols.add(createColumn("running_state", "smallint"));
        cols.add(createColumn("exception_id", "bigint"));

        cols.add(createColumn("pausing_parents", "bigint[]"));
        cols.add(createColumn("forked_runspaces", "bigint[]"));
        cols.add(createColumn("parent_runspace", "bigint"));

        return ct;
    }

    /* ------------------------------------------------------------------ */
    /* string table                                                       */
    /* ------------------------------------------------------------------ */

    private CreateTable createStringsTable() {
        CreateTable ct = new CreateTable();
        ct.setName("ago_string");
        ct.setPkName("pk_ago_string");

        List<Column> cols = ct.getColumn();

        RdbType idT = rdbAdapter.idRdbType();

        // primary key
        Column idCol = createColumn("id", idT.getTypeName());
        idCol.setPrimaryKey(true);
        cols.add(idCol);

        Column app = createColumn("application", "int");
        Column idx = createColumn("index", "int");

        cols.add(app);
        cols.add(idx);
        cols.add(createColumn("value", "text"));

        return ct;
    }

    /* ------------------------------------------------------------------ */
    /* blob table                                                        */
    /* ------------------------------------------------------------------ */

    private CreateTable createBlobsTable() {
        CreateTable ct = new CreateTable();
        ct.setName("ago_blob");
        ct.setPkName("pk_ago_blob");

        List<Column> cols = ct.getColumn();

        RdbType idT = rdbAdapter.idRdbType();

        // primary key
        Column idCol = createColumn("id", idT.getTypeName());
        idCol.setPrimaryKey(true);
        cols.add(idCol);

        Column app = createColumn("application", "int");
        Column idx = createColumn("index", "int");
//        app.setPrimaryKey(true);
//        idx.setPrimaryKey(true);

        cols.add(app);
        cols.add(idx);
        cols.add(createColumn("data", "bytea"));

        return ct;
    }

    /* ------------------------------------------------------------------ */
    /* class table                                                        */
    /* ------------------------------------------------------------------ */

    private CreateTable createClassTable() {
        CreateTable ct = new CreateTable();
        ct.setName("ago_class");
        ct.setPkName("pk_ago_class");

        List<Column> cols = ct.getColumn();

        // id + instance‑common columns
        createInstanceColumns(rdbAdapter.idRdbType(), cols, true);

        // class‑specific columns
        createClassColumns(cols);

        return ct;
    }

    /* ------------------------------------------------------------------ */
    /* function table                                                    */
    /* ------------------------------------------------------------------ */

    private CreateTable createFunctionTable() {
        CreateTable ct = new CreateTable();
        ct.setName("ago_function");
        ct.setPkName("pk_ago_function");

        List<Column> cols = ct.getColumn();

        // id + instance‑common columns
        createInstanceColumns(rdbAdapter.idRdbType(), cols, true);

        // class‑specific columns (same as ago_class)
        createClassColumns(cols);

        // function‑specific columns
        cols.add(createColumn("variables", "jsonb[]"));
        cols.add(createColumn("parameters", "jsonb[]"));
        cols.add(createColumn("switch_tables", "jsonb[]"));
        cols.add(createColumn("try_catch_items", "jsonb[]"));
        cols.add(createColumn("source_map_entries", "jsonb[]"));
        cols.add(createColumn("code", "int[]"));
        cols.add(createColumn("native_function_entrance", "varchar(1024)"));
        cols.add(createColumn("native_function_result_slot", "int"));
        cols.add(createColumn("result_type", "jsonb"));

        return ct;
    }

    /* ------------------------------------------------------------------ */
    /* instance table                                                    */
    /* ------------------------------------------------------------------ */

    private CreateTable createInstanceTable() {
        CreateTable ct = new CreateTable();
        ct.setName("ago_instance");
        ct.setPkName("pk_ago_instance");

        List<Column> cols = ct.getColumn();

        // id + instance‑common columns
        createInstanceColumns(rdbAdapter.idRdbType(), cols, true);

        return ct;
    }

    /* ------------------------------------------------------------------ */
    /* frame (callframe) table                                           */
    /* ------------------------------------------------------------------ */

    private CreateTable createCallFrameTable() {
        CreateTable ct = new CreateTable();
        ct.setName("ago_frame");
        ct.setPkName("pk_ago_frame");

        List<Column> cols = ct.getColumn();

        // id + instance‑common columns
        createInstanceColumns(rdbAdapter.idRdbType(), cols, true);

        // caller_* columns (id + class)
        objectColumn(cols, "caller", rdbAdapter.idRdbType());

        // frame‑specific columns
        cols.add(createColumn("suspended", "bool"));
        cols.add(createColumn("state", "int"));
        cols.add(createColumn("pc", "int"));
        cols.add(createColumn("receiver_slot", "int"));
        cols.add(createColumn("exception_id", "bigint"));
        cols.add(createColumn("exception_class", "text"));
        cols.add(createColumn("runspace", "text"));

        // slots / payload
        cols.add(createColumn("slots", "jsonb"));
        cols.add(createColumn("payload", "jsonb"));

        // entrance flags
        cols.add(createColumn("is_entrance", "bool"));
        cols.add(createColumn("is_async_entrance", "bool"));

        return ct;
    }

}
