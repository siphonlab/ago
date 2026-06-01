package org.siphonlab.ago.runtime.rdb;

import io.ebean.config.dbplatform.DatabasePlatform;
import io.ebeaninternal.dbmigration.ddlgeneration.DdlWrite;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.BaseTableDdl;
import io.ebeaninternal.dbmigration.migration.Column;
import io.ebeaninternal.dbmigration.migration.CreateTable;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoInterface;
import org.siphonlab.ago.AgoSlotDef;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.runtime.db.EntityAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityRdbDDLGenerator<Id> extends RdbDDLGenerator<Id>{

    private final EntityAdapter<Id> entityAdapter;

    private final AgoClass entityClass;

    public EntityRdbDDLGenerator(AgoClassLoader classLoader, EntityAdapter<Id> entityAdapter, DatabasePlatform databasePlatform) {
        super(classLoader, (DbAdapter<Id>) entityAdapter, databasePlatform);
        this.entityAdapter = entityAdapter;
        this.entityClass = this.classLoader.getClass("Entity");
    }

    @Override
    protected void generate(BaseTableDdl ddlGen, DdlWrite writer) {
        for (AgoClass agoClass : classLoader.getClasses()) {
            if (!agoClass.isGenericTemplate()
                    && !agoClass.isInGenericTemplate()
                    && !(agoClass instanceof AgoInterface)
                    && agoClass.getSlotDefs().length > 0
                    && entityClass.isThatOrSuperOfThat(agoClass)
            ) {
                ddlGen.generate(writer, createTable(agoClass));
            }
        }
    }

    protected CreateTable createTable(AgoClass agoClass) {
        CreateTable createTable = new CreateTable();
        String tableName = entityAdapter.tableName(agoClass);
        createTable.setName(tableName);
        createTable.setPkName(entityAdapter.primaryKeyName(agoClass));

        List<Column> columns = createTable.getColumn();

        Set<String> usedNames = new HashSet<>();

        ColumnDesc pkColumnDesc = dbAdapter.composeIdColumn(usedNames);

        createInstanceColumns(dbAdapter.idRdbType(), columns);

        Column pk = pkColumnDesc.toColumn();
        pk.setPrimaryKey(true);
        columns.add(pk);

        List<ColumnDesc> columnsOfSlots = new ArrayList<>();
        for (AgoSlotDef slotDef : agoClass.getSlotDefs()) {
            createColumn(slotDef, columns, usedNames, columnsOfSlots);
        }
        tables.put(agoClass, new RdbTable(tableName, columnsOfSlots));

        return createTable;
    }

    protected void createColumn(AgoSlotDef slotDef, List<Column> columns, Set<String> usedNames, List<ColumnDesc> columnDescs) {
        ColumnDesc columnDesc = dbAdapter.composeColumnDesc(slotDef, usedNames);
        columns.add(columnDesc.toColumn());

        if(columnDesc.getAdditional() != null){
            ColumnDesc additional = columnDesc.getAdditional();
            columns.add(additional.toColumn());
        }

        columnDescs.add(columnDesc);
    }
}
