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

import org.agrona.collections.Long2ObjectHashMap;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;

import java.util.List;

public class TaskEngine extends LazyJsonAgoEngine {
    public TaskEngine(RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(rdbAdapter, runSpaceHost);
    }

    @Override
    public TaskAdapter getRdbAdapter() {
        return (TaskAdapter) super.getRdbAdapter();
    }

    protected TaskRunSpace createRunSpaceInner(RunSpaceHost host) {
        return new TaskRunSpace(this, this.getRdbAdapter(), host);
    }

    public void resume(){
        TaskAdapter adapter = this.getRdbAdapter();
        List<RunSpaceDesc> runSpaceDescs = adapter.loadResumableRunSpaces();

        Long2ObjectHashMap<TaskRunSpace> runspaces = new Long2ObjectHashMap<>();
        for (RunSpaceDesc runSpaceDesc : runSpaceDescs) {
            var r  = new TaskRunSpace(this, adapter, this.runSpaceHost, runSpaceDesc.getId()); //TODO multiple runSpaceHost
            runspaces.put(runSpaceDesc.getId(),r);
        }
        this.runspaces.putAll(runspaces);

        for (RunSpaceDesc runSpaceDesc : runSpaceDescs) {
            var r = runspaces.get(runSpaceDesc.getId());
            CallFrame<?> currCallFrame = (CallFrame<?>) adapter.restoreInstance(runSpaceDesc.getCurrFrame());
            if(currCallFrame instanceof ObjectRefCallFrame<?> objectRefCallFrame){
                currCallFrame = (CallFrame<?>) objectRefCallFrame.recomposeAsCallFrame();
            }
            List<RunSpace> forkedRunspaces = runSpaceDesc.getForkedRunSpaces() == null ? null : runSpaceDesc.getForkedRunSpaces().stream().map(d -> (RunSpace) runspaces.get(d.getId())).toList();
            RunSpace parent = runSpaceDesc.getParentRunSpace() == null ? null : runspaces.get(runSpaceDesc.getParentRunSpace().getId());
            List<RunSpace> pausingParents = runSpaceDesc.getPausingParents() == null ? null : runSpaceDesc.getPausingParents().stream().map(d -> (RunSpace)runspaces.get(d.getId())).toList();
            byte runningState = runSpaceDesc.getRunningState();
            Instance<?> exception = adapter.restoreInstance(runSpaceDesc.getException());
            r.restore(runningState, currCallFrame, parent, forkedRunspaces, pausingParents, exception, runSpaceDesc.getResultSlots());
        }

        for (var runSpace : runspaces.values()) {
            var state = runSpace.getRunningState();
            if(state == RunSpace.RunningState.RUNNING || state == RunSpace.RunningState.PENDING) {
                runSpace.resumeByRestore();
            }
        }
    }
}
