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
import org.siphonlab.ago.runtime.rdb.EntityRdbAdapter;
import org.siphonlab.ago.runtime.rdb.TransactionBoundDataSource;
import org.siphonlab.ago.runtime.rdb.TypeMapping;

import javax.sql.DataSource;
import java.sql.SQLException;

public class H2Adapter<Id> extends EntityRdbAdapter<Id> {

    public H2Adapter(ClassManager classManager, TypeCode idType, IdGenerator<Id> idGenerator, BoxTypes boxTypes, DataSource dataSource) {
        super(classManager, idType, idGenerator, boxTypes, new H2TypeMapping(boxTypes), dataSource);
    }

    @Override
    public void lockInstance(Id id) {

    }

    @Override
    public H2Adapter<Id> beginTransaction() {
        var adapter = new H2Adapter<Id>(classManager, idType, idGenerator, boxTypes, new TransactionBoundDataSource(dataSource, true));
        return adapter;
    }

}
