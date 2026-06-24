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

public class CallFrameWithRunningState<T extends AgoFunction> extends CallFrame<T> {
    private final CallFrame<T> inner;
    private final byte runningState;
    private final int pc;

    public CallFrameWithRunningState(CallFrame<T> inner, byte runningState, int pc){
        super(inner.getSlots(), inner.getAgoClass());
        this.inner = inner;
        this.runningState = runningState;
        this.pc = pc;
    }

    public CallFrameWithRunningState(CallFrame<T> inner, byte runningState){
        this(inner, runningState, -1);
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

    public int getPc() {
        return pc;
    }

    @Override
    public String toString() {
        if(pc == -1){
            return "(CallFrameWithRunningState %s %s)".formatted(inner, runningState);
        } else {
            return "(CallFrameWithRunningState %s %s pc:%d)".formatted(inner, runningState, pc);
        }
    }
}
