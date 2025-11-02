package org.siphonlab.ago.runtime.stateful;

public abstract class RunningStateStore {
    private RunningState runningState = RunningState.PENDING;

    private StatefulCallFrame callFrame;

    public void setCallFrame(StatefulCallFrame callFrame) {
        this.callFrame = callFrame;
    }

    public abstract void saveState(StatefulCallFrame callFrame);

    public RunningState getRunningState() {
        return runningState;
    }

    public void setRunningState(RunningState runningState) {
        if (this.runningState != runningState) {
            this.runningState = runningState;
            saveState(this.callFrame);
        }
    }
}
