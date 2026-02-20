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

// async entrance callframe has caller, in another runspace, which state is WAITING_RESULT
public class AsyncEntranceCallFrame<T extends AgoFunction> extends EntranceCallFrame<T> {

    public AsyncEntranceCallFrame(CallFrame<T> inner) {
        super(inner);
    }

    @Override
    public CallFrame<?> getCaller() {
        return inner.getCaller();
    }

    public void finishVoid() {
        if (stateHandler != null) stateHandler.complete(null);
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptVoidByAsync();
    }

    public void finishBoolean(boolean result) {
        if (stateHandler != null) ((CallFrameStateHandler<Boolean>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptBooleanByAsync(result);
    }

    public void finishByte(byte result) {
        if (stateHandler != null) ((CallFrameStateHandler<Byte>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptByteByAsync(result);
    }

    public void finishShort(short result) {
        if (stateHandler != null) ((CallFrameStateHandler<Short>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptShortByAsync(result);
    }

    public void finishInt(int result) {
        if (stateHandler != null) ((CallFrameStateHandler<Integer>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptIntByAsync(result);
    }

    public void finishLong(long result) {
        if (stateHandler != null) ((CallFrameStateHandler<Long>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptLongByAsync(result);
    }

    public void finishFloat(float result) {
        if (stateHandler != null) ((CallFrameStateHandler<Float>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptFloatByAsync(result);
    }

    public void finishDouble(double result) {
        if (stateHandler != null) ((CallFrameStateHandler<Double>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptDoubleByAsync(result);
    }

    public void finishChar(char result) {
        if (stateHandler != null) ((CallFrameStateHandler<Character>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptCharByAsync(result);
    }

    public void finishObject(Instance<?> result) {
        if (stateHandler != null) ((CallFrameStateHandler<Instance<?>>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptObjectByAsync(result);
    }

    public void finishString(String result) {
        if (stateHandler != null) ((CallFrameStateHandler<String>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptStringByAsync(result);
    }

    public void finishNull() {
        if (stateHandler != null) stateHandler.complete(null);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptNullByAsync();
    }

    public void finishClassRef(AgoClass result) {
        if (stateHandler != null) ((CallFrameStateHandler<AgoClass>) stateHandler).complete(result);

        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptClassRefByAsync(result);
    }

    @Override
    public void finishException(Instance<?> exception, boolean throwOut) {
        if (!fail(exception)) {
            getCaller().getRunSpace().acceptExceptionByAsync(exception);
        }
    }
}
