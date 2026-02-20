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
package org.siphonlab.ago.runtime.rdb.reactive.json;

import org.siphonlab.ago.AgoSlotDef;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.reactive.CallFrameBoundSlots;
import org.siphonlab.ago.runtime.rdb.ObjectRef;

public class ReactiveJsonRefSlotsWithCallFrame extends ReactiveJsonRefSlots implements CallFrameBoundSlots<ReactiveJsonRefSlots> {

    private final CallFrame<?> callFrame;
    private final ReactivePGJsonSlotsAdapter reactivePgJsonSlotsAdapter;

    public ReactiveJsonRefSlotsWithCallFrame(ObjectRef objectRef, ReactivePGJsonSlotsAdapter slotsAdapter, AgoSlotDef[] slotDefs, CallFrame<?> callFrame) {
        super(objectRef, slotsAdapter, slotDefs);
        this.callFrame = callFrame;
        this.reactivePgJsonSlotsAdapter = slotsAdapter;
    }

    @Override
    public CallFrame<?> getCallFrame() {
        return callFrame;
    }

    @Override
    public int getInt(int slot) {
        return reactivePgJsonSlotsAdapter.getInt(this, objectRef, slot);
    }

    @Override
    public int getClassRef(int slot) {
        return reactivePgJsonSlotsAdapter.getClassRef(this, objectRef, slot);
    }

    @Override
    public long getLong(int slot) {
        return reactivePgJsonSlotsAdapter.getLong(this, objectRef, slot);
    }

    @Override
    public float getFloat(int slot) {
        return reactivePgJsonSlotsAdapter.getFloat(this, objectRef, slot);
    }

    @Override
    public double getDouble(int slot) {
        return reactivePgJsonSlotsAdapter.getDouble(this, objectRef, slot);
    }

    @Override
    public byte getByte(int slot) {
        return reactivePgJsonSlotsAdapter.getByte(this, objectRef, slot);
    }

    @Override
    public short getShort(int slot) {
        return reactivePgJsonSlotsAdapter.getShort(this, objectRef, slot);
    }

    @Override
    public char getChar(int slot) {
        return reactivePgJsonSlotsAdapter.getChar(this, objectRef, slot);
    }

    @Override
    public boolean getBoolean(int slot) {
        return reactivePgJsonSlotsAdapter.getBoolean(this, objectRef, slot);
    }

    @Override
    public String getString(int slot) {
        return reactivePgJsonSlotsAdapter.getString(this, objectRef, slot);
    }

    @Override
    public void setInt(int slot, int value) {
        reactivePgJsonSlotsAdapter.setInt(this, objectRef, slot, value);
    }

    @Override
    public void setClassRef(int slot, int value) {
        reactivePgJsonSlotsAdapter.setClassRef(this, objectRef, slot, value);
    }

    @Override
    public void setLong(int slot, long value) {
        reactivePgJsonSlotsAdapter.setLong(this, objectRef, slot, value);
    }

    @Override
    public void setFloat(int slot, float value) {
        reactivePgJsonSlotsAdapter.setFloat(this, objectRef, slot, value);
    }

    @Override
    public void setDouble(int slot, double value) {
        reactivePgJsonSlotsAdapter.setDouble(this, objectRef, slot, value);
    }

    @Override
    public void setByte(int slot, byte value) {
        reactivePgJsonSlotsAdapter.setByte(this, objectRef, slot, value);
    }

    @Override
    public void setShort(int slot, short value) {
        reactivePgJsonSlotsAdapter.setShort(this, objectRef, slot, value);
    }

    @Override
    public void setChar(int slot, char value) {
        reactivePgJsonSlotsAdapter.setChar(this, objectRef, slot, value);
    }

    @Override
    public void setBoolean(int slot, boolean value) {
        reactivePgJsonSlotsAdapter.setBoolean(this, objectRef, slot, value);
    }

    @Override
    public void setString(int slot, String value) {
        reactivePgJsonSlotsAdapter.setString(this, objectRef, slot, value);
    }

    @Override
    public void setObject(int slot, Instance<?> value) {
        reactivePgJsonSlotsAdapter.setObject(this, objectRef, slot, value);
    }

    @Override
    public Instance<?> getObject(int slot) {
        return reactivePgJsonSlotsAdapter.getObject(this, objectRef, slot);
    }

    @Override
    public void incInt(int slot, int value) {
        int current = reactivePgJsonSlotsAdapter.getInt(this, objectRef, slot);
        reactivePgJsonSlotsAdapter.setInt(this, objectRef, slot, current + value);
    }

    @Override
    public void incFloat(int slot, float value) {
        float current = reactivePgJsonSlotsAdapter.getFloat(this, objectRef, slot);
        reactivePgJsonSlotsAdapter.setFloat(this, objectRef, slot, current + value);
    }

    @Override
    public void incDouble(int slot, double value) {
        double current = reactivePgJsonSlotsAdapter.getDouble(this, objectRef, slot);
        reactivePgJsonSlotsAdapter.setDouble(this, objectRef, slot, current + value);
    }

    @Override
    public void incByte(int slot, byte value) {
        byte current = reactivePgJsonSlotsAdapter.getByte(this, objectRef, slot);
        reactivePgJsonSlotsAdapter.setByte(this, objectRef, slot, (byte) (current + value));
    }

    @Override
    public void incShort(int slot, short value) {
        short current = reactivePgJsonSlotsAdapter.getShort(this, objectRef, slot);
        reactivePgJsonSlotsAdapter.setShort(this, objectRef, slot, (short) (current + value));
    }

    @Override
    public void incLong(int slot, long value) {
        long current = reactivePgJsonSlotsAdapter.getLong(this, objectRef, slot);
        reactivePgJsonSlotsAdapter.setLong(this, objectRef, slot, current + value);
    }

    @Override
    public String toString() {
        return "(JsonRefSlotsWithCallFrame " + objectRef.className() + " " + objectRef.id() + ")";
    }
}
