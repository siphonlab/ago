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
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptVoidByAsync();
    }

    public void finishBoolean(boolean result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptBooleanByAsync(result);
    }

    public void finishByte(byte result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptByteByAsync(result);
    }

    public void finishShort(short result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptShortByAsync(result);
    }

    public void finishInt(int result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptIntByAsync(result);
    }

    public void finishLong(long result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptLongByAsync(result);
    }

    public void finishFloat(float result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptFloatByAsync(result);
    }

    public void finishDouble(double result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptDoubleByAsync(result);
    }

    public void finishDecimal(BigDecimal result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptDecimalByAsync(result);
    }

    public void finishChar(char result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptCharByAsync(result);
    }

    public void finishObject(Instance<?> result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptObjectByAsync(result);
    }

    public void finishString(String result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptStringByAsync(result);
    }

    public void finishClassRef(AgoClass result) {
        this.getRunSpace().setCurrCallFrame(null);
        getCaller().getRunSpace().acceptClassRefByAsync(result);
    }

    @Override
    public void finishException(Instance<?> exception) {
        getCaller().getRunSpace().acceptExceptionByAsync(exception);
    }

    public void yieldVoid() {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptVoidByAsync();
    }

    public void yieldBoolean(boolean value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptBooleanByAsync(value);
    }

    public void yieldByte(byte value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptByteByAsync(value);
    }

    public void yieldShort(short value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptShortByAsync(value);
    }

    public void yieldInt(int value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptIntByAsync(value);
    }

    public void yieldLong(long value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptLongByAsync(value);
    }

    public void yieldFloat(float value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptFloatByAsync(value);
    }

    public void yieldDouble(double value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptDoubleByAsync(value);
    }

    public void yieldDecimal(BigDecimal value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptDecimalByAsync(value);
    }

    public void yieldChar(char value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptCharByAsync(value);
    }

    public void yieldObject(Instance<?> value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptObjectByAsync(value);
    }

    public void yieldUnion(Object value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptUnionByAsync(value);
    }

    public void yieldString(String value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptStringByAsync(value);
    }

    public void yieldClassRef(AgoClass value) {
        this.setSuspended(true);
        getCaller().getRunSpace().acceptClassRefByAsync(value);
    }
}
