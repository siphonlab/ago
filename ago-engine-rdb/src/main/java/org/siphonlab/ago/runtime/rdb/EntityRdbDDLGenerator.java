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


    public EntityRdbDDLGenerator(AgoClassLoader classLoader, RdbAdapter<Id> rdbAdapter, DatabasePlatform databasePlatform) {
        super(classLoader, rdbAdapter, databasePlatform);
    }

    @Override
    protected void generate(BaseTableDdl ddlGen, DdlWrite writer) {
        for (AgoClass agoClass : classLoader.getClasses()) {
            if (((EntityAdapter<?>)rdbAdapter).isEntityClass(agoClass)) {
                ddlGen.generate(writer, createTable(agoClass));
            }
        }
    }

}
