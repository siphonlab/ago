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

import java.math.BigDecimal;

/**
 * an intermediate frame for a frame to invoke another.
 * the caller frame install ReentrantProxyFrame to wait result, and when the result come, it invokes CallFrame.reenter
 * callee must work in same runspace with caller. therefore it must be not an EntranceFrame.
 */
public class ReentrantProxyFrame<T extends AgoFunction> extends CallFrame<T>{

    private final CallFrame<T> callee;
    private final int state;
    private final int additionalState;
    private TypeCode resultType;

    public ReentrantProxyFrame(CallFrame<?> caller, CallFrame<T> callee, int state, int additionalState){
        super(callee.getSlots(), callee.getAgoClass());
        this.setCaller(caller);
        callee.setCaller(this);
        callee.setRunSpace(caller.getRunSpace());
        this.callee = callee;
        this.state = state;
        this.additionalState = additionalState;
    }

    @Override
    public SourceLocation resolveSourceLocation() {
        return callee.resolveSourceLocation();
    }

    @Override
    public void run(CallFrame<?> self) {
        callee.run(this);
    }

    @Override
    public RunSpace getRunSpace() {
        return callee.getRunSpace();
    }

    @Override
    public void setRunSpace(RunSpace runSpace) {
        callee.setRunSpace(runSpace);
    }

    @Override
    public Instance getParentScope() {
        return callee.getParentScope();
    }

    @Override
    public void setParentScope(Instance parentScope) {
        callee.setParentScope(parentScope);
    }

    public void finishVoid() {
        getRunSpace().acceptVoid(null);
        this.resultType = TypeCode.NULL;
        caller.reenter(this, state, additionalState);
    }

    public void finishBoolean(boolean result) {
        getRunSpace().acceptBoolean(result, null);
        this.resultType = TypeCode.BOOLEAN;
        caller.reenter(this, state, additionalState);
    }

    public void finishByte(byte result) {
        getRunSpace().acceptByte(result, null);
        this.resultType = TypeCode.BYTE;
        caller.reenter(this, state, additionalState);
    }

    public void finishShort(short result) {
        getRunSpace().acceptShort(result, null);
        this.resultType = TypeCode.SHORT;
        caller.reenter(this, state, additionalState);
    }

    public void finishInt(int result) {
        getRunSpace().acceptInt(result, null);
        this.resultType = TypeCode.INT;
        caller.reenter(this, state, additionalState);
    }

    public void finishLong(long result) {
        getRunSpace().acceptLong(result, null);
        this.resultType = TypeCode.LONG;
        caller.reenter(this, state, additionalState);
    }

    public void finishFloat(float result) {
        getRunSpace().acceptFloat(result, null);
        this.resultType = TypeCode.FLOAT;
        caller.reenter(this, state, additionalState);
    }

    public void finishDouble(double result) {
        getRunSpace().acceptDouble(result, null);
        this.resultType = TypeCode.DOUBLE;
        caller.reenter(this, state, additionalState);
    }

    public void finishDecimal(BigDecimal result) {
        getRunSpace().acceptDecimal(result, null);
        this.resultType = TypeCode.DECIMAL;
        caller.reenter(this, state, additionalState);
    }

    public void finishChar(char result) {
        getRunSpace().acceptChar(result, null);
        this.resultType = TypeCode.CHAR;
        caller.reenter(this, state, additionalState);
    }

    public void finishObject(Instance<?> result) {
        getRunSpace().acceptObject(result, null);
        this.resultType = TypeCode.OBJECT;
        caller.reenter(this, state, additionalState);
    }

    public void finishString(String result) {
        getRunSpace().acceptString(result, null);
        this.resultType = TypeCode.STRING;
        caller.reenter(this, state, additionalState);
    }

    public void finishNull() {
        getRunSpace().acceptNull(null);
        this.resultType = TypeCode.NULL;
        caller.reenter(this, state, additionalState);
    }

    public void finishClassRef(AgoClass result) {
        getRunSpace().acceptClassRef(result, null);
        this.resultType = TypeCode.CLASS_REF;
        caller.reenter(this, state, additionalState);
    }

    @Override
    public boolean handleException(Instance<?> exception) {
        return callee.handleException(exception);
    }

    @Override
    public void finishException(Instance<?> exception) {
        super.finishException(exception);       // let caller handle the exception, assume callee can't handle it, for if callee can handle the exception, when raise exception, the callee should already eat it
    }

    @Override
    public void setSuspended(boolean suspended) {
        this.callee.setSuspended(suspended);
    }

    @Override
    public boolean isSuspended() {
        return this.callee.isSuspended();
    }

    @Override
    public void resume() {
        this.callee.resume();
    }

    @Override
    public void interrupt() {
        this.callee.interrupt();
    }

    public CallFrame<T> getCallee() {
        return callee;
    }

    public TypeCode getResultType() {
        return resultType;
    }
}
