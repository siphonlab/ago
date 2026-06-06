package org.siphonlab.ago.runtime.rdb.pg;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.IdGenerator;
import org.siphonlab.ago.runtime.rdb.EntityRdbAdapter;
import org.siphonlab.ago.runtime.rdb.TransactionBoundDataSource;

import javax.sql.DataSource;

public class PGEntityAdapter<Id> extends EntityRdbAdapter<Id> {

    public PGEntityAdapter(ClassManager classManager, TypeCode idType, IdGenerator<Id> idGenerator, BoxTypes boxTypes, DataSource dataSource) {
        super(classManager, idType, idGenerator, boxTypes, new PGTypeMapping(boxTypes), dataSource);
    }

    @Override
    public void lockInstance(Id id) {

    }

    @Override
    public PGEntityAdapter<Id> beginTransaction() {
        var adapter = new PGEntityAdapter<Id>(classManager, idType, idGenerator, boxTypes, new TransactionBoundDataSource(dataSource, true));
        adapter.tablesByClass = this.tablesByClass;
        adapter.tablesByClassName = this.tablesByClassName;
        return adapter;
    }

}
