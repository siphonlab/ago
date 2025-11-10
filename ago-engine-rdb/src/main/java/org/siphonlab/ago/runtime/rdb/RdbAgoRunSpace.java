package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class RdbAgoRunSpace extends AgoRunSpace {

    private final static Logger logger = LoggerFactory.getLogger(RdbAgoRunSpace.class);

    private final RdbAdapter rdbAdapter;
    private final long id;

    public RdbAgoRunSpace(RdbEngine agoEngine, RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        this(agoEngine, rdbAdapter, runSpaceHost, rdbAdapter.nextId());
    }

    public RdbAgoRunSpace(RdbEngine agoEngine, RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost, long id) {
        super(agoEngine, runSpaceHost);
        this.rdbAdapter = rdbAdapter;
        this.id = id;
    }


    public long getId() {
        return id;
    }

    @Override
    public void run() {
        if (this.currCallFrame == null) {
            if (logger.isDebugEnabled()) logger.debug(this + " has no callframe, exit");
        } else {
            if (logger.isDebugEnabled()) logger.debug(this + " run callframe " + this.currCallFrame);
        }

        this.setRunningState(RunningState.RUNNING);
        CallFrame<?> cf = null;
        boolean saveAtEnd = false;
        while (this.currCallFrame != null && ((this.getRunningState() & RunningState.PAUSE_OR_WAIT_RESULT) == 0)) {
            cf = currCallFrame;
            if(cf instanceof ObjectRefCallFrame<?> objectRefCallFrame){
                cf = currCallFrame = objectRefCallFrame.recomposeAsCallFrame();
            }
            rdbAdapter.saveInstance(new CallFrameWithRunningState<>(cf, this.runningState));
            this.currCallFrame.run();
            if(this.currCallFrame == null) {     // exited goto tryComplete
                saveAtEnd = true;
            } else {        // whenever this.currCallFrame == cf or this.currCallFrame != cf, only save cf, for currCallFrame will save at LN39
                if(cf != currCallFrame){
                    assert !cf.isSuspended();
                    // cf is calling currCallFrame
                    rdbAdapter.saveInstance(new CallFrameWithRunningState<>(cf, this.runningState));
                } else {
                    // it's suspended
                    rdbAdapter.saveInstance(new CallFrameWithRunningState<>(cf, this.runningState));
                }
            }
        }
        tryComplete();
        if(saveAtEnd && cf != null)
            rdbAdapter.saveInstance(new CallFrameWithRunningState<>(cf, this.runningState));
    }

    @Override
    protected boolean tryComplete() {
        boolean r = super.tryComplete();
        if(r && agoEngine instanceof PersistentRdbEngine persistentRdbEngine){
            persistentRdbEngine.releaseRunSpace(this.getId());
        }
        return r;
    }

    @Override
    public Object awaitTillComplete(CallFrame<?> frame) {
        if(frame.getRunSpace() instanceof RdbAgoRunSpace rdbAgoRunSpace){
            rdbAgoRunSpace.save(frame);
        }
        return super.awaitTillComplete(frame);
    }

    protected void save(Instance<?> instance){
        ((RdbEngine)getAgoEngine()).saveInstance(instance);
    }

    public Set<AgoRunSpace> getPausingParents() {
        return pausingParents;
    }

    public Set<AgoRunSpace> getForkedSpaces() {
        return forkedSpaces;
    }

    @Override
    protected void addPausingParent(AgoRunSpace parent) {
        super.addPausingParent(parent);
        rdbAdapter.updateRunSpace(this);
    }

    @Override
    protected boolean removePausingParent(AgoRunSpace parent) {
        var r = super.removePausingParent(parent);
        rdbAdapter.updateRunSpace(this);
        return r;
    }

    @Override
    public AgoRunSpace createChildRunSpace() {
        var r = super.createChildRunSpace();
        rdbAdapter.updateRunSpace(this);        // add forkedRunSpace
        return r;
    }

    @Override
    protected void removeForkedSpace(AgoRunSpace forkedRunSpace) {
        super.removeForkedSpace(forkedRunSpace);
        rdbAdapter.updateRunSpace(this);
    }

    @Override
    public void setRunningState(byte runningState) {
        if(this.runningState == runningState) return;
        super.setRunningState(runningState);
        rdbAdapter.updateRunSpace(this);
    }

    @Override
    public void setCurrCallFrame(CallFrame<?> currCallFrame) {
        if(this.currCallFrame == currCallFrame) return;
        super.setCurrCallFrame(currCallFrame);
        rdbAdapter.updateRunSpace(this);
    }

    @Override
    protected void setException(Instance<?> exception) {
        super.setException(exception);
        rdbAdapter.updateRunSpace(this);
    }

    @Override
    public void resumeByAcceptResult() {
        super.resumeByAcceptResult();
        rdbAdapter.updateRunSpace(this);
    }

    @Override
    public void interrupt() {
        rdbAdapter.saveInstance(new CallFrameWithRunningState<>(this.currCallFrame, RunningState.INTERRUPTED));
        super.interrupt();
    }

    public void resumeByRestore() {
        if (this.getRunningState() == RunningState.RUNNING) {  // only works for RUNNING
            runSpaceHost.execute(this);
        }
    }

    public void restore(byte runningState, CallFrame<?> currCallFrame, AgoRunSpace parent,
                        List<AgoRunSpace> forkedRunspaces, List<AgoRunSpace> pausingParents,
                        Instance<?> exception, ResultSlots resultSlots) {
        this.runningState = runningState;
        this.currCallFrame = currCallFrame;
        this.parent = parent;
        if(forkedRunspaces != null) this.forkedSpaces.addAll(forkedRunspaces);
        if(pausingParents != null) this.pausingParents.addAll(pausingParents);
        this.exception = exception;
        if(resultSlots != null) this.resultSlots = resultSlots;
    }
}
