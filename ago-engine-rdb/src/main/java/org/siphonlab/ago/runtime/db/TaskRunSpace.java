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

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.DbEngine;
import org.siphonlab.ago.runtime.db.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentDbEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;

public class TaskRunSpace<Id> extends RunSpace{
    private final static Logger logger = LoggerFactory.getLogger(TaskRunSpace.class);

    protected final WorkflowAdapter<Id> rdbAdapter;
    public final Id id;

    public TaskRunSpace(DbEngine agoEngine, WorkflowAdapter<Id> rdbAdapter, RunSpaceHost runSpaceHost) {
        this(agoEngine, rdbAdapter, runSpaceHost, rdbAdapter.nextId());
    }

    public TaskRunSpace(DbEngine agoEngine, WorkflowAdapter<Id> rdbAdapter, RunSpaceHost runSpaceHost, Id id) {
        super(agoEngine, runSpaceHost);
        this.id = id;
        this.rdbAdapter = rdbAdapter;
    }

    public Id getId() {
        return id;
    }

    private boolean isEntranceOrTask(CallFrame<?> frame) {
        if (frame == null) {
            return false;
        }
        if (frame instanceof EntranceCallFrame<?>) {
            return true;
        }
        return frame.getAgoClass().isThatOrDerivedFrom(agoEngine.getLangClasses().getTaskInterface());
    }

    @Override
    protected boolean tryComplete() {
        boolean r = super.tryComplete();
        if(r && agoEngine instanceof PersistentDbEngine persistentRdbEngine){
            persistentRdbEngine.releaseRunSpace(this.getId());
        }
        return r;
    }

    @Override
    public Object awaitTillComplete(CallFrame<?> frame) {
        rdbAdapter.saveInstance(frame);
        return super.awaitTillComplete(frame);
    }

    protected void save(Instance<?> instance){
        ((DbEngine)getAgoEngine()).saveInstance(instance);
    }

    @Override
    protected void addPausingParent(RunSpace parent) {
        super.addPausingParent(parent);
        rdbAdapter.updateRunSpace(this);
    }

    @Override
    protected boolean removePausingParent(RunSpace parent) {
        var r = super.removePausingParent(parent);
        rdbAdapter.updateRunSpace(this);
        return r;
    }

    @Override
    public RunSpace createChildRunSpace(ForkContext forkContext) {
        var r = super.createChildRunSpace(forkContext);
        rdbAdapter.updateRunSpace(this);        // add forkedRunSpace
        return r;
    }

    @Override
    protected void removeForkedSpace(RunSpace forkedRunSpace) {
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
        if (ObjectRefOwner.equals(this.currCallFrame, currCallFrame)) return;
        super.setCurrCallFrame(currCallFrame);
        rdbAdapter.updateRunSpace(this);
    }

    @Override
    public void fork(CallFrame<?> frame) {
        super.fork(frame);
        rdbAdapter.saveInstance(new CallFrameWithRunningState<>(frame, frame.getRunSpace().getRunningState()));
    }

    @Override
    public void spawn(CallFrame<?> frame) {
        super.spawn(frame);
    }

    @Override
    public void fork(CallFrame<?> frame, ForkContext forkContext) {
        if(frame instanceof ObjectRefCallFrame<?> objectRefCallFrame) {
            frame = objectRefCallFrame.deference();
        }

        var curRunSpace = (TaskRunSpace<?>) frame.getRunSpace();
        var nextRunSpace = (TaskRunSpace<?>) this.createChildRunSpace(forkContext);
        frame.setRunSpace(nextRunSpace);

        if (forkContext == null) {
            logger.info("{} fork {} got {}", this, nextRunSpace, this.forkedSpaces.size());
        }
        else {
            logger.info("{} fork {} via {}, got {}", this, nextRunSpace, forkContext, forkedSpaces.size());
        }

        var transactionAdapter = this.rdbAdapter.beginTransaction();
        transactionAdapter.saveRunSpace(curRunSpace, curRunSpace.getCurrentCallFrame());
        transactionAdapter.saveRunSpace(nextRunSpace, frame);
        transactionAdapter.saveInstance(frame);
        transactionAdapter.updateCallFrameRunningState(new CallFrameWithRunningState<>(frame.getCaller(), curRunSpace.getRunningState()));
        transactionAdapter.commitTransaction();

        nextRunSpace.start(new AsyncEntranceCallFrame<>(frame));
    }

    @Override
    public Future<?> startAsync(CallFrame<?> frame) {
        return super.startAsync(frame);
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
        CallFrame<?> callFrame = this.currCallFrame;
        super.interrupt();
        rdbAdapter.saveInstance(new CallFrameWithRunningState<>(callFrame, RunningState.INTERRUPTED));
    }

    public void resumeByRestore() {
        if (this.getRunningState() == RunningState.RUNNING || this.getRunningState() == RunningState.PENDING) {  // only works for RUNNING
            setRunningState(RunningState.PENDING);
            runSpaceHost.execute(this);
        }
    }

    public void restore(byte runningState, CallFrame<?> currCallFrame, RunSpace parent,
                        List<RunSpace> forkedRunspaces, List<RunSpace> pausingParents,
                        Instance<?> exception, ResultSlots resultSlots) {
        this.runningState = runningState;
        this.currCallFrame = currCallFrame;
        this.parent = parent;
        if(forkedRunspaces != null) this.forkedSpaces.addAll(forkedRunspaces);
        if(pausingParents != null) this.pausingParents.addAll(pausingParents);
        this.exception = exception;
        if(resultSlots != null) this.resultSlots = resultSlots;
    }

    public void saveTask(CallFrame<?> prev, CallFrame<?> cur, int pc) {
        if (cur == null) {
            return ;
        }
        if (isEntranceOrTask(cur)) {
            var t =this.rdbAdapter.beginTransaction();
            logger.debug("saving task instances {}", prev);
            t.saveInstance(prev);
            t.updateCallFrameRunningState(new CallFrameWithRunningState<>(prev, prev.getRunSpace().getRunningState(), pc));
        }
    }

}
