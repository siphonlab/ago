package org.siphonlab.ago.runtime.rdb.reactive;

import io.ebean.config.dbplatform.DatabasePlatform;
import io.ebeaninternal.dbmigration.ddlgeneration.DdlWrite;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.BaseTableDdl;
import io.ebeaninternal.dbmigration.migration.Column;
import io.ebeaninternal.dbmigration.migration.CreateTable;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.RdbDDLGenerator;

import java.util.*;

public class PersistentRdbDDLGenerator extends RdbDDLGenerator {


    public PersistentRdbDDLGenerator(AgoClassLoader classLoader, RdbAdapter rdbAdapter, DatabasePlatform databasePlatform) {
        super(classLoader, rdbAdapter, databasePlatform);
    }

    @Override
    protected void generate(BaseTableDdl ddlGen, DdlWrite writer) {
        ddlGen.generate(writer, createRunSpaceTable());
        super.generate(ddlGen, writer);
    }

    private CreateTable createRunSpaceTable() {
        CreateTable createTable = new CreateTable();
        List<Column> columns = createTable.getColumn();
        var names = new HashSet<String>();

        var pkColumnDesc = rdbAdapter.composeIdColumn(names);
        Column pk = rdbAdapter.toColumn(pkColumnDesc);
        pk.setPrimaryKey(true);
        columns.add(pk);

        var name = rdbAdapter.composeField("name", TypeCode.STRING, names);
        columns.add(rdbAdapter.toColumn(name));

        var node = rdbAdapter.composeField("node", TypeCode.STRING, names);
        columns.add(rdbAdapter.toColumn(node));

        var currCallFrame = rdbAdapter.composeObjectField("currCallFrame", names);
        columns.add(rdbAdapter.toColumn(currCallFrame));

        var state = rdbAdapter.composeField("state", TypeCode.INT, names);  // see RunningState
        columns.add(rdbAdapter.toColumn(state));

        return createTable;
    }
}
