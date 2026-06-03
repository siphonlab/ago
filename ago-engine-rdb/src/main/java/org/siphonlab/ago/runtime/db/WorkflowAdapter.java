package org.siphonlab.ago.runtime.db;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.runtime.rdb.RunSpaceDesc;

import java.util.List;

// workflow adapter need save all data of workflow frame
public interface WorkflowAdapter<Id> extends DbAdapter<Id> {

    void saveStrings(List<String> strings);

    void saveBlobs(List<byte[]> blobs);

    void updateCallFrameRunningState(CallFrameWithRunningState<?> callFrame);

    void insertRunSpace(TaskRunSpace<Id> runSpace);

    List<RunSpaceDesc> loadResumableRunSpaces();

    void updateRunSpace(TaskRunSpace<Id> idTaskRunSpace);

    @Override
    WorkflowAdapter<Id> beginTransaction();

    void updateRunSpace(TaskRunSpace<?> runSpace, CallFrame<?> currentCallFrame);

    AgoClass loadScopedAgoClass(AgoClass baseClass, Id id);
}
