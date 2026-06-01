package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.Instance;

public interface DbAdapter<IdType> {

    void saveInstance(Instance<?> instance);

    Instance<?> getById(ObjectRef<IdType> objectRef);

    DbAdapter<IdType> beginTransaction();
    void commitTransaction();
    void rollbackTransaction();

    IdType nextId();
}
