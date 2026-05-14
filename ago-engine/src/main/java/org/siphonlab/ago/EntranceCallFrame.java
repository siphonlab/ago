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

import org.siphonlab.ago.runtime.UnhandledException;

import java.math.BigDecimal;

// entrance callframe has no caller
public class EntranceCallFrame<T extends AgoFunction> extends CallFrame<T> {

    protected final CallFrame<T> inner;

    public EntranceCallFrame(CallFrame<T> inner) {
        super(null, inner.getAgoClass());
        this.inner = inner;
    }

    @Override
    public SourceLocation resolveSourceLocation() {
        return inner.resolveSourceLocation();
    }

    @Override
    public Slots getSlots() {
        return inner.getSlots();
    }

    @Override
    public void run(CallFrame<?> self) {
        inner.run(this);
    }

    @Override
    public RunSpace getRunSpace() {
        return inner.getRunSpace();
    }

    @Override
    public void setRunSpace(RunSpace runSpace) {
        inner.setRunSpace(runSpace);
    }

    @Override
    public CallFrame<?> getCaller() {
        return null;
    }

    @Override
    public void setCaller(CallFrame<?> caller) {

    }

    @Override
    public Instance getParentScope() {
        return inner.getParentScope();
    }

    @Override
    public void setParentScope(Instance parentScope) {
        inner.setParentScope(parentScope);
    }

    public void finishVoid() {
        getRunSpace().acceptVoid(null);
    }

    public void finishBoolean(boolean result) {
        getRunSpace().acceptBoolean(result, null);
    }

    public void finishByte(byte result) {
        getRunSpace().acceptByte(result, null);
    }

    public void finishShort(short result) {
        getRunSpace().acceptShort(result, null);
    }

    public void finishInt(int result) {
        getRunSpace().acceptInt(result, null);
    }

    public void finishLong(long result) {
        getRunSpace().acceptLong(result, null);
    }

    public void finishFloat(float result) {
        getRunSpace().acceptFloat(result, null);
    }

    public void finishDouble(double result) {
        getRunSpace().acceptDouble(result, null);
    }

    public void finishDecimal(BigDecimal result) {
        getRunSpace().acceptDecimal(result, null);
    }

    public void finishChar(char result) {
        getRunSpace().acceptChar(result, null);
    }

    public void finishObject(Instance<?> result) {
        getRunSpace().acceptObject(result, null);
    }

    public void finishString(String result) {
        getRunSpace().acceptString(result, null);
    }

    public void finishClassRef(AgoClass result) {
        getRunSpace().acceptClassRef(result, null);
    }

    @Override
    public boolean handleException(Instance<?> exception) {
        return inner.handleException(exception);
    }

    @Override
    public void finishException(Instance<?> exception) {        // entrance callframe has no caller
        throw new UnhandledException(getAgoEngine(), exception);
    }

    @Override
    public void setSuspended(boolean suspended) {
        inner.setSuspended(suspended);
    }

    @Override
    public boolean isSuspended() {
        return inner.isSuspended();
    }

    @Override
    public void resume() {
        inner.resume();
    }

    @Override
    public void interrupt() {
        inner.interrupt();
    }

    public CallFrame<T> getInner() {
        return inner;
    }

    @Override
    protected boolean reenter(ReentrantProxyFrame<?> reentrantProxyFrame, int state, int additionalState) {
        inner.reenter(reentrantProxyFrame, state, additionalState);
        return false;
    }
}
