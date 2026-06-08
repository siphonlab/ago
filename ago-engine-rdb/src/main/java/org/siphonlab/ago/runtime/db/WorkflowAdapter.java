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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.RunSpace;
import org.siphonlab.ago.runtime.rdb.RunSpaceDesc;

import java.util.List;

// workflow adapter need save all data of workflow frame
public interface WorkflowAdapter<Id> extends DbAdapter<Id> {

    void saveStrings(List<String> strings);

    void saveBlobs(List<byte[]> blobs);

    void updateCallFrameRunningState(CallFrameWithRunningState<?> callFrame);

    void insertRunSpace(WorkflowRunSpace<Id> runSpace);

    List<RunSpaceDesc<Id>> loadResumableRunSpaces();

    void updateRunSpace(WorkflowRunSpace<Id> idWorkflowRunSpace);

    @Override
    WorkflowAdapter<Id> beginTransaction();

    void updateRunSpace(WorkflowRunSpace<?> runSpace, CallFrame<?> currentCallFrame);

    AgoClass loadScopedAgoClass(AgoClass baseClass, Id id, RunSpace runSpace);

    void saveCallChainIncludeCurrent(@NonNull CallFrame<?> frame);

    void saveFrameAndRunspace(@Nullable CallFrame<?> frame);
}
