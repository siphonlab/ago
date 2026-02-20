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
package org.siphonlab.ago;

import org.apache.mina.util.ConcurrentHashSet;
import org.siphonlab.ago.runtime.UnhandledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RunSpace implements Runnable{

    public static class RunningState{
        public static final byte PENDING                = 1;
        public static final byte RUNNING                = 2;
        public static final byte PAUSE                  = 0b0000_0100;
        public static final byte WAITING_RESULT         = 0b0000_1000;
        public static final byte DONE                   = 0x10;
        public static final byte ERROR                  = 0x20;
        public static final byte INTERRUPTED            = 0x40;

        public static final byte PAUSE_OR_WAIT_RESULT   = 0b0000_1100;
        public static final byte DE_PAUSE_MASK          = (byte)0b1111_1011;
        public static final byte DE_AWAIT_RESULT_MASK   = (byte)0b1111_0111;

        public static boolean isPausingOrWaitingResult(byte runningState){
            return (runningState & PAUSE_OR_WAIT_RESULT) != 0;
        }
        public static boolean isFinish(byte runningState){
            return (runningState & (DONE | ERROR | INTERRUPTED)) != 0;
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(RunSpace.class);

    protected final AgoEngine agoEngine;
    protected final RunSpaceHost runSpaceHost;

    protected CallFrame<?> currCallFrame;
    protected Instance<?> exception;
    protected ResultSlots resultSlots = new ResultSlots();

    protected byte runningState = RunningState.PENDING;
    protected final Set<RunSpace> pausingParents = new ConcurrentHashSet<>();

    protected Set<RunSpace> forkedSpaces = new ConcurrentHashSet<>();
    protected RunSpace parent;

    protected Exception unhandledException;

    public interface CompleteListener {
        void handle();
    }

    private List<CompleteListener> completeListeners = new CopyOnWriteArrayList<>();

    public AgoEngine getAgoEngine() {
        return agoEngine;
    }

    public RunSpace(AgoEngine agoEngine, RunSpaceHost runSpaceHost) {
        this.agoEngine = agoEngine;
        this.runSpaceHost = runSpaceHost;
    }

    public RunSpaceHost getRunSpaceHost() {
        return runSpaceHost;
    }

    public void setCurrCallFrame(CallFrame<?> currCallFrame) {
        this.currCallFrame = currCallFrame;
    }

    public CallFrame<?> getCurrentCallFrame() {
        return this.currCallFrame;
    }

    public void addCompleteListener(CompleteListener listener){
        completeListeners.add(listener);
    }

    // start or resume
    public void start(CallFrame<?> frame) {
        if(logger.isDebugEnabled()) logger.debug(this + " run " + frame);
        // when call run, current callframe is already running, and will "return" to exit
        // so it's safe to shift to the new callframe
        if(this.currCallFrame != null)
            throw new IllegalStateException("already running");

        if(this.parent != null){
            this.addCompleteListener(()->{
                parent.removeForkedSpace(this);
                parent.tryComplete();
            });
        }

        this.setCurrCallFrame(frame);
        runSpaceHost.execute(this);    // see this.run()
    }

    protected void removeForkedSpace(RunSpace forkedRunSpace){
        this.forkedSpaces.remove(forkedRunSpace);
    }

    CompletableFuture<?> runningFuture;
    public Future<?> startAsync(CallFrame<?> frame) {
        if (this.currCallFrame != null)
            throw new IllegalStateException("already running");

        runningFuture = new CompletableFuture<>();
        this.addCompleteListener(()->{
            runningFuture.complete(null);
        });

        this.start(new EntranceCallFrame<>(frame));
        return runningFuture;
    }

    @Override
    public void run() {
        if (this.currCallFrame == null) {
            if (logger.isDebugEnabled()) logger.debug(this + " has no callframe, exit");
        } else {
            if (logger.isDebugEnabled()) logger.debug(this + " run callframe " + this.currCallFrame);
        }

        this.setRunningState(RunningState.RUNNING);
        while (this.currCallFrame != null && !RunningState.isPausingOrWaitingResult(this.getRunningState())) {
            this.currCallFrame.run();
        }
        tryComplete();
    }

    protected boolean tryComplete() {
        if(this.getRunningState() == RunningState.RUNNING && this.forkedSpaces.isEmpty()){    // wait children complete
            if(this.unhandledException != null)
                this.setRunningState(RunningState.ERROR);
            else
                this.setRunningState(RunningState.DONE);

            for (CompleteListener completeListener : this.completeListeners) {
                completeListener.handle();
            }
            return true;
        }
        return false;
    }

    public void waitResult() {
        if(this.getRunningState() == RunningState.PAUSE) {
            this.setRunningState((byte) (this.getRunningState() | RunningState.WAITING_RESULT));
        } else {
            this.setRunningState(RunningState.WAITING_RESULT);
        }
    }

    public void pause() {
        pauseByParent(null);
    }

    protected void pauseByParent(RunSpace parent) {
        logger.info("pause " + this);
        synchronized (this.pausingParents) {
            if (this.getRunningState() == RunningState.WAITING_RESULT) {
                this.setRunningState((byte) (this.getRunningState() | RunningState.PAUSE));
            } else if(this.getRunningState() == RunningState.RUNNING || this.getRunningState() == RunningState.PENDING){
                this.setRunningState(RunningState.PAUSE);
                if(this.currCallFrame != null) this.currCallFrame.setSuspended(true);
            }
            if(parent != null) addPausingParent(parent);

            for (RunSpace childSpace : this.forkedSpaces) {
                childSpace.pauseByParent(parent == null ? this : parent);
            }
        }
    }

    protected void addPausingParent(RunSpace parent) {
        pausingParents.add(parent);
    }

    protected boolean removePausingParent(RunSpace parent) {
        return pausingParents.remove(parent);
    }

    public void interrupt() {
        if(this.currCallFrame != null){
            if(this.currCallFrame instanceof AgoFrame agoFrame){
                agoFrame.interrupt();
            }   //TODO cannot stop native frame
        }
        this.setRunningState(RunningState.INTERRUPTED);
        for (RunSpace forkedSpace : this.forkedSpaces) {
            forkedSpace.interrupt();
        }
        for (CompleteListener completeListener : this.completeListeners) {
            completeListener.handle();
        }
    }

    public void resumeByParentResume(RunSpace parent){
        if (parent != null && !this.removePausingParent(parent)) return;

        if((this.getRunningState() & RunningState.PAUSE) == RunningState.PAUSE){
            synchronized (this.pausingParents) {
                if (this.pausingParents.isEmpty()) {
                    if (this.getRunningState() == RunningState.PAUSE) {  // only pause
                        this.setRunningState(RunningState.RUNNING);
                        runSpaceHost.execute(this);
                    } else {
                        this.setRunningState((byte) (this.getRunningState() & RunningState.DE_PAUSE_MASK));    // remove pause
                    }
                }
                for (RunSpace childSpace : this.forkedSpaces) {
                    childSpace.resumeByParentResume(parent == null ? this : parent);
                }
            }
        }
    }

    public void resumeByAcceptResult(){
        if ((this.getRunningState() & RunningState.WAITING_RESULT) == RunningState.WAITING_RESULT) {
            if(this.getRunningState() == RunningState.WAITING_RESULT){
                this.setRunningState(RunningState.RUNNING);   //TODO concurrent
                runSpaceHost.execute(this);     // resume
            } else {
                this.setRunningState((byte) (this.getRunningState() & RunningState.DE_AWAIT_RESULT_MASK));    // only remove waiting result
            }
        }
    }

    public void resume(){
        resumeByParentResume(null);
    }

    public Object awaitTillComplete(CallFrame<?> frame){
        var space = agoEngine.createRunSpace(runSpaceHost);
        try {
            frame.setRunSpace(space);
            space.startAsync(frame).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if(unhandledException instanceof RuntimeException r){
            throw r;
        } else if(unhandledException != null){
            throw new RuntimeException(unhandledException);
        }
        return space.getResultSlots().getResultAsObject();
    }

    public void fork(CallFrame<?> frame) {
        fork(frame, null);
    }

    public void spawn(CallFrame<?> frame) {
        spawn(frame, null);
    }

    public void await(CallFrame<?> frame) {
        await(frame, null);
    }

    public void fork(CallFrame<?> frame, ForkContext forkContext) {
        var space = createChildRunSpace(forkContext);
        frame.setRunSpace(space);
        space.start(new EntranceCallFrame<>(frame));
        if(forkContext != null){
            logger.info(this + " fork " + space + " via " + forkContext + ", got " + forkedSpaces.size());
        } else {
            logger.info(this + " fork " + space + ", got " + forkedSpaces.size());
        }
    }

    // spawn semantic as below:
    //      for child runspace, it redirects parent to fork, otherwise fork as its child
    public void spawn(CallFrame<?> frame, ForkContext forkContext) {
        if(this.parent != null) {
            this.parent.fork(frame, forkContext);
        } else {
            this.fork(frame, forkContext);
        }
    }

    public void await(CallFrame<?> frame, ForkContext forkContext) {
        var space = createChildRunSpace(forkContext);
        frame.setRunSpace(space);
        space.start(new AsyncEntranceCallFrame<>(frame));
        this.waitResult();
    }

    public RunSpace createChildRunSpace(ForkContext forkContext) {
        var space = agoEngine.createRunSpace(runSpaceHost);
        this.forkedSpaces.add(space);
        space.setParent(this);
        return space;
    }

    public void setParent(RunSpace parent) {
        this.parent = parent;
    }

    public RunSpace getParent() {
        return parent;
    }

    public void acceptVoid(CallFrame<?> caller) {
        resultSlots.setVoidValue();
        this.setCurrCallFrame(caller);
    }

    public void acceptVoidByAsync() {
        resultSlots.setVoidValue();
        resumeByAcceptResult();
    }

    public void acceptNull(CallFrame<?> caller) {
        resultSlots.setNullValue();
        this.setCurrCallFrame(caller);
    }

    public void acceptNullByAsync() {
        resultSlots.setNullValue();
        resumeByAcceptResult();
    }

    public void acceptIntByAsync(int result) {
        resultSlots.setIntValue(result);
        resumeByAcceptResult();
    }

    public void acceptByteByAsync(byte result) {
        resultSlots.setByteValue(result);
        resumeByAcceptResult();
    }

    public void acceptShortByAsync(short result) {
        resultSlots.setShortValue(result);
        resumeByAcceptResult();
    }

    public void acceptFloatByAsync(float result) {
        resultSlots.setFloatValue(result);
        resumeByAcceptResult();
    }

    public void acceptDoubleByAsync(double result) {
        resultSlots.setDoubleValue(result);
        resumeByAcceptResult();
    }

    public void acceptLongByAsync(long result) {
        resultSlots.setLongValue(result);
        resumeByAcceptResult();
    }

    public void acceptCharByAsync(char result) {
        resultSlots.setCharValue(result);
        resumeByAcceptResult();
    }

    public void acceptStringByAsync(String result) {
        resultSlots.setStringValue(result);
        resumeByAcceptResult();
    }

    public void acceptBooleanByAsync(boolean result) {
        resultSlots.setBooleanValue(result);
        resumeByAcceptResult();
    }

    public void acceptObjectByAsync(Instance<?> result) {
        resultSlots.setObjectValue(result);
        resumeByAcceptResult();
    }

    public void acceptClassRefByAsync(AgoClass result) {
        resultSlots.setClassRefValue(result);
        resumeByAcceptResult();
    }

    public void acceptInt(int result, CallFrame<?> caller) {
        resultSlots.setIntValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptByte(byte result, CallFrame<?> caller) {
        resultSlots.setByteValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptShort(short result, CallFrame<?> caller) {
        resultSlots.setShortValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptFloat(float result, CallFrame<?> caller) {
        resultSlots.setFloatValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptDouble(double result, CallFrame<?> caller) {
        resultSlots.setDoubleValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptLong(long result, CallFrame<?> caller) {
        resultSlots.setLongValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptChar(char result, CallFrame<?> caller) {
        resultSlots.setCharValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptString(String result, CallFrame<?> caller) {
        resultSlots.setStringValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptBoolean(boolean result, CallFrame<?> caller) {
        resultSlots.setBooleanValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptObject(Instance<?> result, CallFrame<?> caller) {
        resultSlots.setObjectValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptClassRef(AgoClass result, CallFrame<?> caller) {
        resultSlots.setClassRefValue(result);
        setCurrCallFrame(caller);
    }

    public void acceptException(Instance<?> exception) {
        this.setException(exception);
    }

    public void acceptException(Instance<?> exception, CallFrame<?> caller) {
        this.setException(exception);
        if(caller == null) {
            this.setRunningState(RunningState.ERROR);
            this.setCurrCallFrame(null);
            throw new UnhandledException(getAgoEngine(), exception);
        }

        if(caller.handleException(exception)){
            this.setCurrCallFrame(null);
            start(caller);
            return;
        }
        throw new UnhandledException(getAgoEngine(), exception);
    }

    public void acceptExceptionByAsync(Instance<?> exception) {
        this.setException(exception);
        var caller = this.currCallFrame;
        if (caller.handleException(exception)) {
            this.setCurrCallFrame(null);
            start(caller);
            return;
        }
        throw new UnhandledException(getAgoEngine(), exception);
    }

    protected void setException(Instance<?> exception) {
        this.exception = exception;
    }

    public ResultSlots getResultSlots() {
        return resultSlots;
    }

    public Instance<?> getException() {
        return exception;
    }

    public void cleanException() {
        setException(null);
    }

    public byte getRunningState() {
        return runningState;
    }

    public void setRunningState(byte runningState) {
        this.runningState = runningState;
    }

}
