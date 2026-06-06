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
package org.siphonlab.ago.runtime.db.lazy;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.db.DbAdapter;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * lazy instance
 */
public interface ObjectRefObject<Id> {
    static final Logger logger = LoggerFactory.getLogger(ObjectRefObject.class);

    ObjectRef<Id> getObjectRef();

    Instance<?> getDeferencedInstance();

    Instance<?> deference();

    default Instance<?> deference(Instance<?> deferencedInstance,
                                  DbAdapter<Id> dereferenceAdapter,
                                  ObjectRef<Id> objectRef) {
        if (deferencedInstance != null)
            return deferencedInstance;

        if (logger.isDebugEnabled()) logger.debug(getObjectRef() + " expand deference");
        Instance<?> r = dereferenceAdapter.getById(objectRef);
        setDeferencedInstance(r);
        return r;
    }

    void setDeferencedInstance(Instance<?> inst);
}
