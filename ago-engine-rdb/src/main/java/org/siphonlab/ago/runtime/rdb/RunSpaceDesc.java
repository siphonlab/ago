package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.ResultSlots;

import java.util.List;
import java.util.Objects;

public final class RunSpaceDesc {
    private long id;
    private String runSpaceHostClass;
    private ObjectRef currFrame;
    private ResultSlots resultSlots;
    private byte runningState;
    private ObjectRef exception;

    private List<RunSpaceDesc> pausingParents;

    private List<RunSpaceDesc> forkedRunSpaces;
    private RunSpaceDesc parentRunSpace;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRunSpaceHostClass() {
        return runSpaceHostClass;
    }

    public void setRunSpaceHostClass(String runSpaceHostClass) {
        this.runSpaceHostClass = runSpaceHostClass;
    }

    public ObjectRef getCurrFrame() {
        return currFrame;
    }

    public void setCurrFrame(ObjectRef currFrame) {
        this.currFrame = currFrame;
    }

    public ResultSlots getResultSlots() {
        return resultSlots;
    }

    public void setResultSlots(ResultSlots resultSlots) {
        this.resultSlots = resultSlots;
    }

    public byte getRunningState() {
        return runningState;
    }

    public void setRunningState(byte runningState) {
        this.runningState = runningState;
    }

    public ObjectRef getException() {
        return exception;
    }

    public void setException(ObjectRef exception) {
        this.exception = exception;
    }

    public List<RunSpaceDesc> getPausingParents() {
        return pausingParents;
    }

    public void setPausingParents(List<RunSpaceDesc> pausingParents) {
        this.pausingParents = pausingParents;
    }

    public List<RunSpaceDesc> getForkedRunSpaces() {
        return forkedRunSpaces;
    }

    public void setForkedRunSpaces(List<RunSpaceDesc> forkedRunSpaces) {
        this.forkedRunSpaces = forkedRunSpaces;
    }

    public RunSpaceDesc getParentRunSpace() {
        return parentRunSpace;
    }

    public void setParentRunSpace(RunSpaceDesc parentRunSpace) {
        this.parentRunSpace = parentRunSpace;
    }

}
