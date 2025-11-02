package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeInstance;

public class Table {

    public static Instance<?> getRowById(AgoEngine agoEngine, CallFrame<?> callFrame, Instance instance, long id) {
        AgoClass entityClass = (AgoClass) instance;
        RdbEngine rdbEngine = (RdbEngine) agoEngine;

        return rdbEngine.getById(entityClass, id);
    }

    public static Instance<?> fetchAll(AgoEngine agoEngine, CallFrame<?> callFrame, Instance instance) {
        AgoClass entityClass = (AgoClass) instance;
        RdbEngine rdbEngine = (RdbEngine) agoEngine;

        AgoClass queryResultClass = callFrame.getAgoClass().getResultClass();

        NativeInstance queryResultInstance = (NativeInstance) rdbEngine.createNativeInstance(null, queryResultClass, callFrame);
        queryResultInstance.setNativePayload((ResultSetMapper)rdbEngine.fetchAll(entityClass, callFrame));

        return queryResultInstance;
    }
}
