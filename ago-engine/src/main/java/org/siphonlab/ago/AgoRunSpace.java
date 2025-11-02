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

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.INT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;

public class AgoRunSpace implements Runnable{

    public static class RunningState{
        public static final byte PENDING     = 1;
        public static final byte RUNNING     = 2;
        public static final byte PAUSE       = 0b0000_0100;
        public static final byte WAITING_RESULT = 0b0000_1000;
        public static final byte DONE        = 0x08;
        public static final byte ERROR       = 0x10;
        public static final byte INTERRUPTED = 0x20;

        public static final byte PAUSE_OR_WAIT_RESULT = 0b0000_1100;
    }

    private final static Logger logger = LoggerFactory.getLogger(AgoRunSpace.class);

    protected final AgoEngine agoEngine;
    protected final RunSpaceHost runSpaceHost;

    protected CallFrame<?> currCallFrame;
    protected Instance<?> exception;
    protected ResultSlots resultSlots = new ResultSlots();

    protected byte runningState = RunningState.PENDING;
    protected final Set<AgoRunSpace> pausingParents = new ConcurrentHashSet<>();

    protected Set<AgoRunSpace> forkedSpaces = new ConcurrentHashSet<>();
    protected AgoRunSpace parent;

    protected Exception unhandledException;

    public interface CompleteListener {
        void handle();
    }

    private List<CompleteListener> completeListeners = new CopyOnWriteArrayList<>();

    public AgoEngine getAgoEngine() {
        return agoEngine;
    }

    public AgoRunSpace(AgoEngine agoEngine, RunSpaceHost runSpaceHost) {
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
                parent.forkedSpaces.remove(this);
                parent.tryComplete();
            });
        }

        this.setCurrCallFrame(frame);
        runSpaceHost.execute(this);    // see this.run()
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

        this.runningState = RunningState.RUNNING;
        while (this.currCallFrame != null && ((this.runningState & RunningState.PAUSE_OR_WAIT_RESULT) == 0)) {
            this.currCallFrame.run();
        }
        tryComplete();
    }

    private void tryComplete() {
        if(this.runningState == RunningState.RUNNING && this.forkedSpaces.isEmpty()){    // wait children complete
            if(this.unhandledException != null)
                this.runningState = RunningState.ERROR;
            else
                this.runningState = RunningState.DONE;

            for (CompleteListener completeListener : this.completeListeners) {
                completeListener.handle();
            }
        }
    }

    public void waitResult() {
        if(this.runningState == RunningState.PAUSE) {
            this.runningState |= RunningState.WAITING_RESULT;
        } else {
            this.runningState = RunningState.WAITING_RESULT;
        }
    }

    public void pause() {
        pauseByParent(null);
    }

    protected void pauseByParent(AgoRunSpace parent) {
        logger.info("pause " + this);
        synchronized (this.pausingParents) {
            if (this.runningState == RunningState.WAITING_RESULT) {
                this.runningState |= RunningState.PAUSE;
            } else if(this.runningState == RunningState.RUNNING || this.runningState == RunningState.PENDING){
                this.runningState = RunningState.PAUSE;
                if(this.currCallFrame != null) this.currCallFrame.setSuspended(true);
            }
            if(parent != null) pausingParents.add(parent);

            for (AgoRunSpace childSpace : this.forkedSpaces) {
                childSpace.pauseByParent(parent == null ? this : parent);
            }
        }
    }

    public void interrupt() {
        if(this.currCallFrame != null){
            if(this.currCallFrame instanceof AgoFrame agoFrame){
                agoFrame.interrupt();
            }   //TODO cannot stop native frame
        }
        this.runningState = RunningState.INTERRUPTED;
        for (AgoRunSpace forkedSpace : this.forkedSpaces) {
            forkedSpace.interrupt();
        }
        for (CompleteListener completeListener : this.completeListeners) {
            completeListener.handle();
        }
    }

    public void resumeByParentResume(AgoRunSpace parent){
        if (parent != null && !this.pausingParents.remove(parent)) return;

        if((this.runningState & RunningState.PAUSE) == RunningState.PAUSE){
            synchronized (this.pausingParents) {
                if (this.pausingParents.isEmpty()) {
                    if (this.runningState == RunningState.PAUSE) {  // only pause
                        this.runningState = RunningState.RUNNING;
                        runSpaceHost.execute(this);
                    } else {
                        this.runningState &= (byte) 0b1111_1011;    // remove pause
                    }
                }
                for (AgoRunSpace childSpace : this.forkedSpaces) {
                    childSpace.resumeByParentResume(parent == null ? this : parent);
                }
            }
        }
    }

    public void resumeByAcceptResult(){
        if ((this.runningState & RunningState.WAITING_RESULT) == RunningState.WAITING_RESULT) {
            if(this.runningState == RunningState.WAITING_RESULT){
                this.runningState = RunningState.RUNNING;   //TODO concurrent
                runSpaceHost.execute(this);     // resume
            } else {
                this.runningState &= (byte) 0b1111_0111;    // only remove waiting result
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
        return space.getResultAsObject();
    }

    private Object getResultAsObject() {
        switch (resultSlots.getDataType()){
            case VOID_VALUE:
            case NULL_VALUE:
                return null;
            case OBJECT_VALUE:
                return resultSlots.getObjectValue();
            case INT_VALUE:
                return resultSlots.getIntValue();
            case BYTE_VALUE:
                return (resultSlots.getByteValue());
            case SHORT_VALUE:
                return (resultSlots.getShortValue());
            case LONG_VALUE:
                return (resultSlots.getLongValue());
            case FLOAT_VALUE:
                return (resultSlots.getFloatValue());
            case DOUBLE_VALUE:
                return (resultSlots.getDoubleValue());
            case BOOLEAN_VALUE:
                return (resultSlots.getBooleanValue());
            case CHAR_VALUE:
                return (resultSlots.getCharValue());
            case STRING_VALUE:
                return (resultSlots.getStringValue());
            case CLASS_REF_VALUE:
                return resultSlots.getClassRefValue();
            default:
                throw new UnsupportedOperationException("unexpected data type " + resultSlots.getDataType());
        }
    }

    public void fork(CallFrame<?> frame) {
        var space = createChildRunSpace();
        frame.setRunSpace(space);
        space.start(new EntranceCallFrame<>(frame));
        logger.info(this + " fork " + space + ", got " + forkedSpaces.size());
    }

    public AgoRunSpace createChildRunSpace() {
        var space = agoEngine.createRunSpace(runSpaceHost);
        this.forkedSpaces.add(space);
        space.setParent(this);
        return space;
    }

    public void spawn(CallFrame<?> frame) {
        var space = agoEngine.createRunSpace(runSpaceHost);
        frame.setRunSpace(space);
        space.start(new EntranceCallFrame<>(frame));
        logger.info(this + " spawn " + space);
    }

    public void await(CallFrame<?> frame) {
        var space = createChildRunSpace();
        frame.setRunSpace(space);
        space.start(new AsyncEntranceCallFrame<>(frame));
        this.waitResult();
    }

    public void setParent(AgoRunSpace parent) {
        this.parent = parent;
    }

    public AgoRunSpace getParent() {
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
        this.exception = exception;
    }

    public void acceptException(Instance<?> exception, CallFrame<?> caller) {
        this.exception = exception;
        if(caller == null)
            throw new UnhandledException(getAgoEngine(), exception);

        if(caller.handleException(exception)){
            start(caller);
            return;
        }
        throw new UnhandledException(getAgoEngine(), exception);
    }

    public void acceptExceptionByAsync(Instance<?> exception) {
        this.exception = exception;
        var caller = this.currCallFrame;
        if (caller.handleException(exception)) {
            start(caller);
            return;
        }
        throw new UnhandledException(getAgoEngine(), exception);
    }


    public ResultSlots getResultSlots() {
        return resultSlots;
    }

    public Instance<?> getException() {
        return exception;
    }

    public void cleanException() {
        exception = null;
    }
}
