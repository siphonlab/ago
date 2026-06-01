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
package org.siphonlab.ago.runtime.rdb.task;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.db.TaskRunSpace;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.json.JsonPGAdapter;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentDbEngine;

public class TaskEngine extends PersistentDbEngine {
    public TaskEngine(DbAdapter dbAdapter, RunSpaceHost runSpaceHost) {
        super(dbAdapter, runSpaceHost);
    }

    @Override
    public JsonPGAdapter getRdbAdapter() {
        return (JsonPGAdapter) super.getRdbAdapter();
    }

    protected TaskRunSpace createRunSpaceInner(RunSpaceHost host) {
        return new TaskRunSpace(this, this.getRdbAdapter(), host);
    }

}
