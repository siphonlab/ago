package org.siphonlab.ago;

import org.siphonlab.ago.runtime.UnhandledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CallFrame<F extends AgoFunction> extends Instance<F> {

    private final static Logger logger = LoggerFactory.getLogger(CallFrame.class);

    protected CallFrame<?> caller;

    protected AgoRunSpace runSpace;

    protected CallFrameStateHandler<?> stateHandler;
    protected boolean suspended = false;

    public CallFrame(Slots slots, F agoClass) {
        super(slots, agoClass);
    }

    public CallFrame<?> getCaller() {
        return caller;
    }

    public void setCaller(CallFrame<?> caller) {
        this.caller = caller;
    }

    @Override
    public String toString() {
        return "CallFrame" + "@" + this.agoClass;
    }

    public abstract SourceLocation resolveSourceLocation();

    public AgoRunSpace getRunSpace() {
        return runSpace;
    }

    protected boolean fail(Instance<?> exception) {
        if (stateHandler != null) {
            return stateHandler.fail(new UnhandledException(this.runSpace.getAgoEngine(), exception));
        }
        return false;
    }

    public void finishVoid() {
        if (stateHandler != null) stateHandler.complete(null);

        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptVoid(caller);
    }

    public void finishBoolean(boolean result) {
        if (stateHandler != null) ((CallFrameStateHandler<Boolean>) stateHandler).complete(result);

        this.getCaller().getRunSpace().acceptBoolean(result, caller);
    }

    public void finishByte(byte result) {
        if (stateHandler != null) ((CallFrameStateHandler<Byte>) stateHandler).complete(result);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if(callerRunSpace != runSpace){
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptByte(result, caller);
    }

    public void finishShort(short result) {
        if (stateHandler != null) ((CallFrameStateHandler<Short>) stateHandler).complete(result);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptShort(result, caller);
    }

    public void finishInt(int result) {
        if (stateHandler != null) ((CallFrameStateHandler<Integer>) stateHandler).complete(result);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptInt(result, caller);
    }

    public void finishLong(long result) {
        if (stateHandler != null) ((CallFrameStateHandler<Long>) stateHandler).complete(result);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptLong(result, caller);
    }

    public void finishFloat(float result) {
        if (stateHandler != null) ((CallFrameStateHandler<Float>) stateHandler).complete(result);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptFloat(result, caller);
    }

    public void finishDouble(double result) {
        if (stateHandler != null) ((CallFrameStateHandler<Double>) stateHandler).complete(result);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptDouble(result, caller);
    }

    public void finishChar(char result) {
        if (stateHandler != null) ((CallFrameStateHandler<Character>) stateHandler).complete(result);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptFloat(result, caller);
    }

    public void finishObject(Instance<?> result) {
        if (stateHandler != null) ((CallFrameStateHandler<Instance<?>>) stateHandler).complete(result);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptObject(result, caller);
    }

    public void finishString(String result) {
        if (stateHandler != null) ((CallFrameStateHandler<String>) stateHandler).complete(result);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptString(result, caller);
    }

    public void finishNull() {
        if (stateHandler != null) stateHandler.complete(null);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptNull(caller);
    }

    public void finishClassRef(AgoClass result){
        if (stateHandler != null) ((CallFrameStateHandler<AgoClass>) stateHandler).complete(result);

        var caller = this.getCaller();
        AgoRunSpace callerRunSpace = caller.getRunSpace();
        AgoRunSpace runSpace = getRunSpace();
        if (callerRunSpace != runSpace) {
            runSpace.setCurrCallFrame(null);
        }
        callerRunSpace.acceptClassRef(result, caller);
    }

    public void finishException(Instance<?> exception, boolean throwOut) {
        if(throwOut) {
            if (!fail(exception)) {
                var caller = this.getCaller();
                if(caller == null){
                    throw new UnhandledException(getRunSpace().getAgoEngine(), exception);
                }
                var callerRunSpace = caller.getRunSpace();
                if(callerRunSpace != this.getRunSpace()){
                    getRunSpace().setCurrCallFrame(null);
                }
                callerRunSpace.acceptException(exception, caller);
            } else {
                getRunSpace().setCurrCallFrame(null);
                throw new UnhandledException(getRunSpace().getAgoEngine(), exception);
            }
        } else {
            getRunSpace().acceptException(exception);
        }
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean handleException(Instance<?> exception){
        return false;
    }

    public void setRunSpace(AgoRunSpace runSpace) {
        this.runSpace = runSpace;
    }

    public void run(){
        run(this);
    }

    public abstract void run(CallFrame<?> self);

    public void resume() {
        this.setSuspended(false);
        this.getRunSpace().resumeByAcceptResult();
    }

    public void interrupt(){
    }
}
