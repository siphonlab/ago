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
package org.siphonlab.ago.runtime.rdb.h2;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.IdGenerator;
import org.siphonlab.ago.runtime.rdb.EntityDbAdapter;
import org.siphonlab.ago.runtime.rdb.TypeMapping;

import javax.sql.DataSource;

public class H2Adapter<Id> extends EntityDbAdapter<Id> {

    public H2Adapter(ClassManager classManager, TypeCode idType, IdGenerator<Id> idGenerator, BoxTypes boxTypes, TypeMapping typeMapping, DataSource dataSource) {
        super(classManager, idType, idGenerator, boxTypes, typeMapping, dataSource);
    }

    @Override
    public H2Adapter<Id> beginTransaction() {
        return null;
    }

    @Override
    public void commitTransaction() {

    }

    @Override
    public void rollbackTransaction() {

    }
}
