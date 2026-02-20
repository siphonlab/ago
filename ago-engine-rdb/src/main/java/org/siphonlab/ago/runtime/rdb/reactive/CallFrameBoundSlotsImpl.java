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
package org.siphonlab.ago.runtime.rdb.reactive;

import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;

public class CallFrameBoundSlotsImpl<T extends Slots> implements CallFrameBoundSlots<T> {

    private final CallFrame<?> callFrame;
    private final T inner;

    public CallFrameBoundSlotsImpl(CallFrame<?> callFrame, T inner) {
        this.callFrame = callFrame;
        this.inner = inner;
    }

    @Override
    public CallFrame<?> getCallFrame() {
        return callFrame;
    }

    public T getInnerSlots() {
        return inner;
    }

    @Override
    public Object get(int slot, Class<?> type, AgoEngine agoEngine) {
        return inner.get(slot, type, agoEngine);
    }

    @Override
    public <T> void set(int slot, T value, Class<T> type) {
        inner.set(slot, value, type);
    }

    @Override
    public int getInt(int slot) {
        return inner.getInt(slot);
    }

    @Override
    public int getClassRef(int slot) {
        return inner.getClassRef(slot);
    }

    @Override
    public long getLong(int slot) {
        return inner.getLong(slot);
    }

    @Override
    public float getFloat(int slot) {
        return inner.getFloat(slot);
    }

    @Override
    public double getDouble(int slot) {
        return inner.getDouble(slot);
    }

    @Override
    public byte getByte(int slot) {
        return inner.getByte(slot);
    }

    @Override
    public short getShort(int slot) {
        return inner.getShort(slot);
    }

    @Override
    public char getChar(int slot) {
        return inner.getChar(slot);
    }

    @Override
    public boolean getBoolean(int slot) {
        return inner.getBoolean(slot);
    }

    @Override
    public String getString(int slot) {
        return inner.getString(slot);
    }

    @Override
    public void setInt(int slot, int value) {
        inner.setInt(slot, value);
    }

    @Override
    public void setClassRef(int slot, int value) {
        inner.setClassRef(slot, value);
    }

    @Override
    public void setLong(int slot, long value) {
        inner.setLong(slot, value);
    }

    @Override
    public void setFloat(int slot, float value) {
        inner.setFloat(slot, value);
    }

    @Override
    public void setDouble(int slot, double value) {
        inner.setDouble(slot, value);
    }

    @Override
    public void setByte(int slot, byte value) {
        inner.setByte(slot, value);
    }

    @Override
    public void setShort(int slot, short value) {
        inner.setShort(slot, value);
    }

    @Override
    public void setChar(int slot, char value) {
        inner.setChar(slot, value);
    }

    @Override
    public void setBoolean(int slot, boolean value) {
        inner.setBoolean(slot, value);
    }

    @Override
    public void setString(int slot, String value) {
        inner.setString(slot, value);
    }

    @Override
    public void setObject(int slot, Instance<?> value) {
        inner.setObject(slot, value);
    }

    @Override
    public Instance<?> getObject(int slot) {
        return inner.getObject(slot);
    }

    @Override
    public void incInt(int slot, int value) {
        inner.incInt(slot, value);
    }

    @Override
    public void incFloat(int slot, float value) {
        inner.incFloat(slot, value);
    }

    @Override
    public void incDouble(int slot, double value) {
        inner.incDouble(slot, value);
    }

    @Override
    public void incByte(int slot, byte value) {
        inner.incByte(slot, value);
    }

    @Override
    public void incShort(int slot, short value) {
        inner.incShort(slot, value);
    }

    @Override
    public void incLong(int slot, long value) {
        inner.incLong(slot, value);
    }
}
