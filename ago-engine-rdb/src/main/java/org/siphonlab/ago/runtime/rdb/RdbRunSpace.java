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
package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.lazy.DeferenceCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ExpandableCallFrame;
import org.siphonlab.ago.runtime.rdb.lazy.ObjectRefCallFrame;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static org.siphonlab.ago.runtime.rdb.ReferenceCounter.*;

public class RdbRunSpace extends RunSpace {

    private final static Logger logger = LoggerFactory.getLogger(RdbRunSpace.class);

    private final RdbAdapter rdbAdapter;
    private final long id;

    public RdbRunSpace(RdbEngine agoEngine, RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost) {
        this(agoEngine, rdbAdapter, runSpaceHost, rdbAdapter.nextId());
    }

    public RdbRunSpace(RdbEngine agoEngine, RdbAdapter rdbAdapter, RunSpaceHost runSpaceHost, long id) {
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
        while (this.currCallFrame != null && !RunningState.isPausingOrWaitingResult(this.getRunningState())) {
            cf = currCallFrame;

            if(isRefCallFrame(cf)){
                increaseRef(cf, Reason.RunCallFrame);
                rdbAdapter.saveInstance(new CallFrameWithRunningState<>(cf, this.runningState));
            }

            this.currCallFrame.run();

            if(this.currCallFrame == null) {     // exited goto tryComplete
                saveAtEnd = true;
            } else {        // whenever this.currCallFrame == cf or this.currCallFrame != cf, only save cf, for currCallFrame will save at LN39
                if(isRefCallFrame(cf)) {
                    if (!ObjectRefOwner.equals(cf, this.currCallFrame)) {
                        assert !cf.isSuspended();
                        // cf is calling currCallFrame
                        rdbAdapter.saveInstance(new CallFrameWithRunningState<>(cf, this.runningState));
                    } else {
                        // it's suspended
                        rdbAdapter.saveInstance(new CallFrameWithRunningState<>(cf, this.runningState));
                    }
                }

                if(isRefCallFrame(cf)){
                    if(isRefCallFrame(this.currCallFrame)) {
                        releaseCaller(cf);
                        foldObjectRefFrame(cf);     // the frame always referenced by caller, it won't release slots/scope now
                    }
                    releaseRef(cf,Reason.CleanSlotsForCallFrameQuit);
                } else {    // not reference frame, that means, basic AgoFrame and NativeFrame, don't release prev frame
                    //
                }
            }
        }
        tryComplete();
        if(saveAtEnd && isRefCallFrame(cf)) {
            rdbAdapter.saveInstance(new CallFrameWithRunningState<>(cf, this.runningState));
            foldObjectRefFrame(cf);
            releaseCaller(cf);
            releaseRef(cf, Reason.CleanSlotsForCallFrameQuit);
        }
    }

    private static boolean isRefCallFrame(CallFrame<?> currCallFrame) {
        if(currCallFrame instanceof EntranceCallFrame<?> entranceCallFrame){
            currCallFrame = entranceCallFrame.getInner();
        }
        return currCallFrame instanceof ReferenceCounter || currCallFrame instanceof DeferenceCallFrame;
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
        if(frame.getRunSpace() instanceof RdbRunSpace rdbAgoRunSpace){
            rdbAgoRunSpace.save(frame);
        }
        return super.awaitTillComplete(frame);
    }

    protected void save(Instance<?> instance){
        ((RdbEngine)getAgoEngine()).saveInstance(instance);
    }

    public Set<RunSpace> getPausingParents() {
        return pausingParents;
    }

    public Set<RunSpace> getForkedSpaces() {
        return forkedSpaces;
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

        rdbAdapter.updateRunSpace(this);
    }

    @Override
    public void fork(CallFrame<?> frame) {
        if(frame instanceof ObjectRefCallFrame<?> objectRefCallFrame){
            frame = objectRefCallFrame.expandFor(objectRefCallFrame);
        }   // for ExpandableCallFrame, let's move on
        super.fork(frame);
        rdbAdapter.saveInstance(new CallFrameWithRunningState<>(frame, frame.getRunSpace().getRunningState()));
    }

    @Override
    public void spawn(CallFrame<?> frame) {
        if (frame instanceof ObjectRefCallFrame<?> objectRefCallFrame) {
            frame = objectRefCallFrame.expandFor(objectRefCallFrame, false);
        }
        super.spawn(frame);
        rdbAdapter.saveInstance(new CallFrameWithRunningState<>(frame, frame.getRunSpace().getRunningState()));
    }

    @Override
    public Future<?> startAsync(CallFrame<?> frame) {
        if (frame instanceof ObjectRefCallFrame<?> objectRefCallFrame) {
            frame = objectRefCallFrame.expandFor(objectRefCallFrame, false);
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

        rdbAdapter.saveInstance(new CallFrameWithRunningState<>(callFrame, RunningState.INTERRUPTED));
    }

    public void resumeByRestore() {
        if (this.getRunningState() == RunningState.RUNNING) {  // only works for RUNNING
            runSpaceHost.execute(this);
        }
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
}
