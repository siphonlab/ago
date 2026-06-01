package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.*;

import java.util.List;

public interface EntityAdapter<Id> extends DbAdapter<Id>{

    ResultSetMapper fetchAll(AgoClass agoClass);

    // for EntityAdapter, the saveInstance only log the changed instances, and `flush` really save them to db, and saveInstance may lock the id
    void flush();

    void lockInstance(Id id);

    String tableName(AgoClass agoClass);

    String primaryKeyName(AgoClass agoClass);

    @Override
    EntityAdapter<Id> beginTransaction();
}
