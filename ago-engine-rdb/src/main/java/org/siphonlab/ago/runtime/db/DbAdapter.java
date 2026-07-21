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

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.RunSpace;

import java.sql.SQLException;

public interface DbAdapter<IdType> {

    void saveInstance(Instance<?> instance);

    // for EntityAdapter, it returns whole instance, but, for the scope and other linked Object,
    Instance<?> getById(ObjectRef<IdType> objectRef, RunSpace runSpace);

    DbAdapter<IdType> beginTransaction();
    void commitTransaction() throws SQLException;
    void rollbackTransaction() throws SQLException;
    void close() throws SQLException;

    IdType nextId();
}
