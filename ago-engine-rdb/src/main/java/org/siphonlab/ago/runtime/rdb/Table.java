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
