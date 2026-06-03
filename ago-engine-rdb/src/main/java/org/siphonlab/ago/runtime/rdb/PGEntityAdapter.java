package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.IdGenerator;
import org.siphonlab.ago.runtime.rdb.pg.PGTypeMapping;

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
        return null;
    }

}
