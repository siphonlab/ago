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
package org.siphonlab.ago.runtime.rdb.json.lazy;

import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefInstance;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefObject;

public class DeferenceObjectState {
    protected final ObjectRefObject objectRefInstance;
    protected boolean saveRequired = false;

    private ObjectRef creator;

    public DeferenceObjectState(ObjectRefObject objectRefInstance) {this.objectRefInstance = objectRefInstance;}

    public ObjectRefObject getObjectRefInstance() {
        assert (this.objectRefInstance != null);
        return objectRefInstance;
    }

    public void markSaved() {
        saveRequired = false;
    }

    public boolean isSaveRequired() {
        return saveRequired;
    }

    public void setSaveRequired() {
        this.saveRequired = true;
    }

    public ObjectRef getCreator() {
        return creator;
    }

    public void setCreator(ObjectRef creator) {
        this.creator = creator;
    }
}
