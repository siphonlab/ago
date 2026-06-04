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
package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.ObjectRef;

public class Entity {

    public static Instance<?> getRowById(CallFrame<?> callFrame, Instance<?> instance, long id) {
        AgoClass entityClass = (AgoClass) instance;
        DbEngine<Long> dbEngine = (DbEngine<Long>) callFrame.getAgoEngine();
        return dbEngine.getDbAdapter().getById(ObjectRef.create(entityClass.getFullname(), id));
    }

//    public static Instance<?> fetchAll(AgoEngine agoEngine, CallFrame<?> callFrame, Instance instance) {
//        AgoClass entityClass = (AgoClass) instance;
//        DbEngine dbEngine = (DbEngine) agoEngine;
//
//        AgoClass queryResultClass = callFrame.getAgoClass().getResultClass();
//
//        NativeInstance queryResultInstance = (NativeInstance) dbEngine.createNativeInstance(null, queryResultClass, callFrame);
//        queryResultInstance.setNativePayload((ResultSetMapper) dbEngine.fetchAll(entityClass, callFrame));
//
//        return queryResultInstance;
//    }
}
