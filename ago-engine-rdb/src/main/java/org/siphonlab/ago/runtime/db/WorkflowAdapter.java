package org.siphonlab.ago.runtime.db;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.CallFrame;
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

    AgoClass loadScopedAgoClass(AgoClass baseClass, Id id);

    void saveCallChainIncludeCurrent(@NonNull CallFrame<?> frame);

    void saveFrameAndRunspace(@Nullable CallFrame<?> frame);
}
