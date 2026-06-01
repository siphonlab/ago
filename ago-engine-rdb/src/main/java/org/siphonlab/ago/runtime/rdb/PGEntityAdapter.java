package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.IdGenerator;

import javax.sql.DataSource;

public class PGEntityAdapter<Id> extends EntityDbAdapter<Id> {

    public PGEntityAdapter(ClassManager classManager, TypeCode idType, IdGenerator<Id> idGenerator, BoxTypes boxTypes, TypeMapping typeMapping, DataSource dataSource) {
        super(classManager, idType, idGenerator, boxTypes, typeMapping, dataSource);
    }

    @Override
    public PGEntityAdapter<Id> beginTransaction() {
        return null;
    }

    @Override
    public void commitTransaction() {

    }

    @Override
    public void rollbackTransaction() {

    }
}
