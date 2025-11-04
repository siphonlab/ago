package org.siphonlab.ago.runtime.stateful;

import org.siphonlab.ago.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatefulAgoFrame extends AgoFrame implements StatefulCallFrame {
    private final static Logger LOGGER = LoggerFactory.getLogger(AgoFrame.class);

    protected RunningStateStore runningStateStore;

    public StatefulAgoFrame(Slots slots, AgoFunction agoFunction, AgoEngine engine, RunningStateStore runningStateStore) {
        super(slots, agoFunction, engine);
        this.runningStateStore = runningStateStore;
        runningStateStore.setCallFrame(this);
    }

    public int getPc() {
        return pc;
    }

    @Override
    public RunningState getRunningState() {
        return runningStateStore.getRunningState();
    }

    @Override
    public void setRunningState(RunningState runningState) {
        this.runningStateStore.setRunningState(runningState);
    }

    @Override
    public void run() {
        if(LOGGER.isDebugEnabled()) LOGGER.debug(pc == 0 ? "run %s".formatted(this) : "resume %s".formatted(this));

        this.setRunningState(RunningState.RUNNING);

        super.run();
    }

    @Override
    protected void evaluatePause() {
        super.evaluatePause();
        this.setRunningState(RunningState.SUSPENDED);
    }

    @Override
    protected boolean evaluateInvoke(CallFrame<?> self, int instruction) {
        if(super.evaluateInvoke(self, instruction)){
            this.setRunningState(RunningState.WAITING_RESULT);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean evaluateTryCatch(Slots slots, int instruction) {
        if(super.evaluateTryCatch(slots, instruction)){
            return true;
        } else {
            this.setRunningState(RunningState.ERROR);
            return false;
        }
    }

    @Override
    protected int evaluateReturn(CallFrame<?> self, Slots slots, int pc, int instruction) {
        int p = super.evaluateReturn(self, slots, pc, instruction);
        this.setRunningState(RunningState.DONE);
        return p;
    }

    @Override
    protected void nextPC() {
        runningStateStore.saveState(this);
    }


}
