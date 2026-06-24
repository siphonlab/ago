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
package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.*;

import java.util.Map;

public interface EntityAdapter<Id> extends DbAdapter<Id>{

    ResultSetToEntityMapper<Id> fetchAll(AgoClass agoClass, RunSpace runSpace);

    // for EntityAdapter, the saveInstance only log the changed instances, and `flush` really save them to db, and saveInstance may lock the id
    void flush(RunSpace runSpace);

    void lockInstance(Id id);

    @Override
    EntityAdapter<Id> beginTransaction();

    boolean isEntityClass(AgoClass agoClass);

    String tableName(AgoClass agoClass);

    ColumnDesc getColumnDesc(String className, int slot);

    ColumnDesc idColumnDesc();

    ResultSetToQueryResultMapper<Id> executeQuery(String sql, Map<String, Object> arguments, AgoClass entityClass, RunSpace runSpace);
}
