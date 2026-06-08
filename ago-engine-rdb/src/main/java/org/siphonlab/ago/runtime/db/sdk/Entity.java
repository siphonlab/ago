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
package org.siphonlab.ago.runtime.db.sdk;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.rdb.DbEngine;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;

public class Entity {

    public static void getEntityById(NativeFrame callFrame, long id) {
        AgoClass entityClass = callFrame.getAgoClass().getResultClass();
        DbEngine<Long> dbEngine = (DbEngine<Long>) callFrame.getAgoEngine();
        callFrame.finishObject(dbEngine.getDbAdapter().getById(ObjectRef.create(entityClass.getFullname(), id), callFrame.getRunSpace()));
    }

    public static void getId(NativeFrame callFrame) {
        var instance = callFrame.getParentScope();
        DbSlots<?> slots = (DbSlots<?>) instance.getSlots();
        Object id = slots.getObjectRef().id();
        if(id instanceof Long l) {
            callFrame.finishLong(l);
        } else if(id instanceof String s){
            callFrame.finishString(s);
        } else if(id instanceof Integer i){
            callFrame.finishInt(i);
        } else {
            callFrame.raiseException(callFrame, "lang.ClassCastException", "can't cast to '%s'".formatted(callFrame.getAgoClass().getResultTypeCode()));
        }
    }

    public static void fetchAll(NativeFrame callFrame) {
        DbEngine<?> dbEngine = (DbEngine<?>) callFrame.getAgoEngine();

        AgoClass queryResultClass = callFrame.getAgoClass().getResultClass();
        var entityClass = queryResultClass.getConcreteTypeInfoAsGenericArguments().getArguments()[0];

        //TODO parentScope
        var queryResultInstance = (NativeInstance) dbEngine.createNativeInstance(null, queryResultClass, callFrame.getRunSpace());
        RdbAdapter<?> dbAdapter = (RdbAdapter<?>) dbEngine.getDbAdapter();
        System.out.println(1);
//        queryResultInstance.setNativePayload((ResultSetMapper) ((DbAdapter<?>) dbAdapter).(entityClass, callFrame));
//
//        return queryResultInstance;
    }
}
