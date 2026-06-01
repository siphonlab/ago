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
package org.siphonlab.ago.runtime.db.lazy


import groovy.transform.CompileStatic
import org.agrona.concurrent.IdGenerator
import org.siphonlab.ago.*
import org.siphonlab.ago.runtime.db.WorkflowAdapter
import org.siphonlab.ago.runtime.db.ObjectRef
import org.siphonlab.ago.runtime.rdb.json.JsonPGAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
public class LazyJsonPGAdapter extends JsonPGAdapter implements DereferenceAdapter, WorkflowAdapter{

    private final static Logger logger = LoggerFactory.getLogger(LazyJsonPGAdapter)

    private Map<ObjectRef, Instance> objectReferenceInstancesPool = new ConcurrentHashMap<>();

    public LazyJsonPGAdapter(BoxTypes boxTypes, ClassManager classManager, int applicationId, IdGenerator idGenerator) {
        super(boxTypes, classManager, applicationId, idGenerator);
    }


    @Override
    Instance<?> getById(AgoClass agoClass, AgoEngine rdbEngine, Object id) {
        return null
    }
}
