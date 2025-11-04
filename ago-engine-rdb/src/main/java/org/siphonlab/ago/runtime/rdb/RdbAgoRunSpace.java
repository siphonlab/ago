package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.*;

import java.util.Set;

public class RdbAgoRunSpace extends AgoRunSpace {

    private final RdbAdapter rdbAdapter;
    private final long id;

    public RdbAgoRunSpace(RdbEngine agoEngine, RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(agoEngine, runSpaceHost);
        this.rdbAdapter = rdbAdapter;
        this.id = rdbAdapter.nextId();
    }

    public long getId() {
        return id;
    }

    @Override
    public void run() {
        for (var cf = this.currCallFrame; cf != null; cf = this.currCallFrame) {
            cf.run();

            save(cf);     // save after function run

            if (this.currCallFrame == cf) {       // not set new CallFrame
                this.setCurrCallFrame(null);
                return;
            }
        }
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
        super.setRunningState(runningState);
        rdbAdapter.updateRunSpace(this);
    }

    @Override
    public void setCurrCallFrame(CallFrame<?> currCallFrame) {
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
}
