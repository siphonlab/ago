package org.siphonlab.ago.runtime.rdb;

import org.siphonlab.ago.*;

public class CallFrameWithRunningState<T extends AgoFunction> extends CallFrame<T> {
    private final CallFrame<T> inner;
    private final byte runningState;

    public CallFrameWithRunningState(CallFrame<T> inner, byte runningState){
        super(inner.getSlots(), inner.getAgoClass());
        this.inner = inner;
        this.runningState = runningState;
    }

    @Override
    public SourceLocation resolveSourceLocation() {
        return inner.resolveSourceLocation();
    }

    @Override
    public void run(CallFrame<?> self) {
        throw new UnsupportedOperationException("just for pass running state");
    }

    @Override
    public Slots getSlots() {
        return inner.getSlots();
    }

    public byte getRunningState() {
        return runningState;
    }

    public CallFrame<T> unwrap() {
        return inner;
    }

    @Override
    public T getAgoClass() {
        return inner.getAgoClass();
    }
}
