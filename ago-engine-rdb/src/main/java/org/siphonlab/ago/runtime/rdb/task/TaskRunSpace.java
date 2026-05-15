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
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;

import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.*;
import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.foldObjectRefFrame;
import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.increaseRef;
import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.releaseCaller;
import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.releaseRef;

public class TaskRunSpace extends SavableRunSpace {
    private final TaskAdapter rdbAdapter;
    private final static Logger logger = LoggerFactory.getLogger(TaskRunSpace.class);

    private static boolean isEntranceOrTask(CallFrame<?> frame) {
        if (frame == null) {
            return false;
        }
        if (frame instanceof EntranceCallFrame<?>) {
            return true;
        }

        AgoInterface taskType;
        if (frame instanceof ExpandableCallFrame<?> ef) {
            taskType = ef.expand().getAgoEngine().getLangClasses().taskInterface;
        }
        else {
            taskType = frame.getAgoEngine().getLangClasses().taskInterface;
        }

        return frame.getAgoClass().isThatOrDerivedFrom(taskType);
    }

    public TaskRunSpace(RdbEngine agoEngine, TaskAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        super(agoEngine, rdbAdapter, runSpaceHost);
        this.rdbAdapter = rdbAdapter;
    }

    public TaskRunSpace(RdbEngine agoEngine, TaskAdapter rdbAdapter, RunSpaceHost runSpaceHost, long id) {
        super(agoEngine, rdbAdapter, runSpaceHost, id);
        this.rdbAdapter = rdbAdapter;
    }

    HashSet<CallFrame<?>> callFrames = new HashSet<>();

    @Override
    public void run() {
        if (this.currCallFrame == null) {
            if (logger.isDebugEnabled()) logger.debug("{} has no callframe, exit", this);
        } else {
            if (logger.isDebugEnabled()) logger.debug("{} run callframe {}", this, this.currCallFrame);
        }

        this.setRunningState(RunningState.RUNNING);
        CallFrame<?> cf;
        while (this.currCallFrame != null && !RunningState.isPausingOrWaitingResult(this.getRunningState())) {
            cf = currCallFrame;

            if(isRefCallFrame(cf)){
                if (callFrames.add(cf)) {
                    increaseRef(cf, ReferenceCounter.Reason.RunCallFrame);
                }
            }

            this.currCallFrame.run();
        }
        tryComplete();
    }

    static void runRealease(CallFrame<?> caller) {
        if (isRefCallFrame(caller)) {
            releaseCaller(caller);
            foldObjectRefFrame(caller);
        }
        releaseRef(caller, Reason.CleanSlotsForCallFrameQuit);
    }

    @Override
    public void acceptVoid(CallFrame<?> caller) {
        super.acceptVoid(caller);
        runRealease(caller);
    }

    @Override
    public void acceptBoolean(boolean result, CallFrame<?> caller) {
        super.acceptBoolean(result, caller);
        runRealease(caller);
    }

    @Override
    public void acceptObject(Instance<?> result, CallFrame<?> caller) {
        super.acceptObject(result, caller);
        runRealease(caller);
    }

    @Override
    public void acceptString(String result, CallFrame<?> caller) {
        super.acceptString(result, caller);
        runRealease(caller);
    }

    @Override
    public void acceptInt(int result, CallFrame<?> caller) {
        super.acceptInt(result, caller);
        runRealease(caller);
    }

    private static boolean isRefCallFrame(CallFrame<?> currCallFrame) {
        if(currCallFrame instanceof EntranceCallFrame<?> entranceCallFrame){
            currCallFrame = entranceCallFrame.getInner();
        }
        // not sure, it causes foldObjectRefFrame got a DeferenceCallFrame
        //return currCallFrame instanceof ReferenceCounter || currCallFrame instanceof DeferenceCallFrame;
        return currCallFrame instanceof ReferenceCounter;
    }

    @Override
    protected boolean tryComplete() {
        boolean r = super.tryComplete();
        if(r && agoEngine instanceof PersistentRdbEngine persistentRdbEngine){
            persistentRdbEngine.releaseRunSpace(this.id);
        }
        return r;
    }

    protected void save(Instance<?> instance){
        ((RdbEngine)getAgoEngine()).saveInstance(instance);
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
        return super.createChildRunSpace(forkContext);
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
        releaseRef(this.currCallFrame, ReferenceCounter.Reason.DropCurrentCallFrame);

        if(currCallFrame instanceof ExpandableCallFrame<?> ex){     // ExpandableCallFrame will release for expander quit
            if(ObjectRefOwner.equals(ex.getExpander(), this.currCallFrame)){
                ExpandableCallFrame<?> selfExpand = (ExpandableCallFrame<?>) ex.expandFor(ex);
                selfExpand.expand();
                currCallFrame = selfExpand;
                if(ex.isExpanded()){
                    ex.fold();
                }
            }
        }
        if(currCallFrame instanceof ObjectRefCallFrame<?> objectRefCallFrame){
            if(objectRefCallFrame.getDeferencedCallFrame() instanceof EntranceCallFrame<?> en){
                currCallFrame = en; // Entrance(Expandable())
            } else {
                currCallFrame = objectRefCallFrame.expandFor(objectRefCallFrame);
            }
        }

        super.setCurrCallFrame(currCallFrame);

        increaseRef(currCallFrame, ReferenceCounter.Reason.InstallCurrentCallFrame);
    }

    @Override
    public void fork(CallFrame<?> frame, ForkContext forkContext) {
        if(frame instanceof ObjectRefCallFrame<?> objectRefCallFrame) {
            frame = objectRefCallFrame.expandFor(objectRefCallFrame);
        }

        var curRunSpace = (TaskRunSpace) frame.getRunSpace();
        var nextRunSpace = (TaskRunSpace) this.createChildRunSpace(forkContext);
        frame.setRunSpace(nextRunSpace);

        if (forkContext == null) {
            logger.info("{} fork {} got {}", this, nextRunSpace, this.forkedSpaces.size());
        }
        else {
            logger.info("{} fork {} via {}, got {}", this, nextRunSpace, forkContext, forkedSpaces.size());
        }

        try (var conn = this.rdbAdapter.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            this.rdbAdapter.saveRunspaceWithTx(conn, curRunSpace, curRunSpace.getCurrentCallFrame());
            this.rdbAdapter.saveRunspaceWithTx(conn, nextRunSpace, frame);
            this.rdbAdapter.saveWithConn(conn, frame);
            this.rdbAdapter.updateCallFrameRunningState(
                    conn,
                    frame.getCaller(),
                    curRunSpace.runningState);
            conn.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        nextRunSpace.start(new AsyncEntranceCallFrame<>(frame));
    }

    @Override
    public void spawn(CallFrame<?> frame) {
        if (frame instanceof ObjectRefCallFrame<?> objectRefCallFrame) {
            frame = objectRefCallFrame.expandFor(objectRefCallFrame, false);
        }
        super.spawn(frame);
        // rdbAdapter.saveInstance(new CallFrameWithRunningState<>(frame, frame.getRunSpace().getRunningState()));
    }

    @Override
    public Future<?> startAsync(CallFrame<?> frame) {
        if (frame instanceof ObjectRefCallFrame<?> objectRefCallFrame) {
            frame = objectRefCallFrame.expandFor(objectRefCallFrame, false);
        }

        try (var conn = this.rdbAdapter.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            if (frame instanceof ExpandableCallFrame<?> exframe) {
                this.save(exframe.expand());
            }
            else {
                this.save(frame);
            }
            this.rdbAdapter.saveRunspaceWithTx(
                    conn,
                    (TaskRunSpace) frame.getRunSpace(),
                    frame);
            conn.commit();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

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

        foldObjectRefFrame(callFrame);
        releaseCaller(callFrame);
        releaseRef(callFrame,Reason.CallFrameInterrupt);
    }

    public void resumeByRestore() {
        runSpaceHost.execute(this);
    }

    public void restore(byte runningState, CallFrame<?> currCallFrame, RunSpace parent,
                        List<RunSpace> forkedRunspaces, List<RunSpace> pausingParents,
                        Instance<?> exception, ResultSlots resultSlots) {
        this.runningState = runningState;
        this.currCallFrame = currCallFrame;
        increaseRef(currCallFrame, ReferenceCounter.Reason.RestoreCallFrame);
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
            try (var conn = this.rdbAdapter.getDataSource().getConnection()) {
                logger.debug("saving task instances {}", prev);
                this.save(prev);
                this.rdbAdapter.updateCallFrameRunningState(conn, prev, prev.getRunSpace().getRunningState(), pc);
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
