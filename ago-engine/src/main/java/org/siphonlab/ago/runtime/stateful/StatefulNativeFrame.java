package org.siphonlab.ago.runtime.stateful;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;

public class StatefulNativeFrame extends NativeFrame implements StatefulCallFrame {

    protected final RunningStateStore runningStateStore;

    public StatefulNativeFrame(AgoEngine engine, Slots slots, AgoNativeFunction agoClass, RunningStateStore runningStateStore) {
        super(engine, slots, agoClass);
        this.runningStateStore = runningStateStore;
        runningStateStore.setCallFrame(this);
    }

    @Override
    public RunningState getRunningState() {
        return runningStateStore.getRunningState();
    }

    @Override
    public void setRunningState(RunningState runningState) {
        runningStateStore.setRunningState(runningState);
    }

    @Override
    public void setCaller(CallFrame<?> caller) {
        super.setCaller(caller);
    }

    @Override
    public void run() {
        this.setRunningState(RunningState.RUNNING);
        super.run();
    }

    @Override
    public void finishVoid() {
        super.finishVoid();
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishBoolean(boolean result) {
        super.finishBoolean(result);
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishByte(byte result) {
        super.finishByte(result);
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishShort(short result) {
        super.finishShort(result);
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishInt(int result) {
        super.finishInt(result);
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishLong(long result) {
        super.finishLong(result);
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishFloat(float result) {
        super.finishFloat(result);
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishDouble(double result) {
        super.finishDouble(result);
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishChar(char result) {
        super.finishChar(result);
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishObject(Instance<?> result) {
        super.finishObject(result);
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishString(String result) {
        super.finishString(result);
        this.setRunningState(RunningState.DONE);
    }

    @Override
    public void finishNull() {
        super.finishNull();
        this.setRunningState(RunningState.DONE);
    }
}
