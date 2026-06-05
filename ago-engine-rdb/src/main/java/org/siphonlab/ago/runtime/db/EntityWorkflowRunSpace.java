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

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.ForkContext;
import org.siphonlab.ago.RunSpace;
import org.siphonlab.ago.RunSpaceHost;
import org.siphonlab.ago.runtime.db.sdk.ForkEntityRunSpace;
import org.siphonlab.ago.runtime.db.sdk.ForkEntityWorkflowRunSpace;
import org.siphonlab.ago.runtime.rdb.DbEngine;

public class EntityWorkflowRunSpace<Id> extends WorkflowRunSpace<Id>{

    private final EntityAdapter<Id> entityAdapter;

    public EntityWorkflowRunSpace(DbEngine<Id> agoEngine, WorkflowAdapter<Id> workflowAdapter, EntityAdapter<Id> entityAdapter, RunSpaceHost runSpaceHost) {
        super(agoEngine, workflowAdapter, runSpaceHost);
        this.entityAdapter = entityAdapter;
    }

    public EntityWorkflowRunSpace(DbEngine<Id> agoEngine, WorkflowAdapter<Id> workflowAdapter, EntityAdapter<Id> entityAdapter, RunSpaceHost runSpaceHost, Id id) {
        super(agoEngine, workflowAdapter, runSpaceHost, id);
        this.entityAdapter = entityAdapter;
    }

    public EntityAdapter<Id> getEntityAdapter() {
        return entityAdapter;
    }

    @Override
    protected boolean tryComplete() {
        var b = super.tryComplete();
        if(b){
            entityAdapter.flush();
        }
        return b;
    }

    @Override
    public RunSpace createChildRunSpace(ForkContext forkContext) {
        return super.createChildRunSpace(forkContext == null ? new ForkEntityWorkflowRunSpace() : forkContext);
    }
}
