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

import org.siphonlab.ago.AgoSlotDef;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;

/**
 * this Slots transfer get/set functions to slotsAdapter.get/set, it needs an objectRef to identity who is it
 */
public class RdbRefSlots implements Slots, ObjectRefOwner {

    protected final ObjectRef objectRef;
    protected final SlotsAdapter<Slots> slotsAdapter;
    protected final AgoSlotDef[] slotDefs;

    private boolean saved = true;

    public RdbRefSlots(ObjectRef objectRef, SlotsAdapter<Slots> slotsAdapter, AgoSlotDef[] slotDefs) {
        this.objectRef = objectRef;
        this.slotsAdapter = slotsAdapter;
        this.slotDefs = slotDefs;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public AgoSlotDef getSlotDef(int slotIndex) {
        return slotDefs[slotIndex];
    }

    public ObjectRef getObjectRef() {
        return objectRef;
    }

    @Override
    public int getInt(int slot) {
        return slotsAdapter.getInt(this, objectRef, slot);
    }

    @Override
    public int getClassRef(int slot) {
        return slotsAdapter.getClassRef(this, objectRef, slot);
    }

    @Override
    public long getLong(int slot) {
        return slotsAdapter.getLong(this, objectRef, slot);
    }

    @Override
    public float getFloat(int slot) {
        return slotsAdapter.getFloat(this, objectRef, slot);
    }

    @Override
    public double getDouble(int slot) {
        return slotsAdapter.getDouble(this, objectRef, slot);
    }

    @Override
    public byte getByte(int slot) {
        return slotsAdapter.getByte(this, objectRef, slot);
    }

    @Override
    public short getShort(int slot) {
        return slotsAdapter.getShort(this, objectRef, slot);
    }

    @Override
    public char getChar(int slot) {
        return slotsAdapter.getChar(this, objectRef, slot);
    }

    @Override
    public boolean getBoolean(int slot) {
        return slotsAdapter.getBoolean(this, objectRef, slot);
    }

    @Override
    public String getString(int slot) {
        return slotsAdapter.getString(this, objectRef, slot);
    }

    @Override
    public void setInt(int slot, int value) {
        slotsAdapter.setInt(this, objectRef, slot, value);
    }

    @Override
    public void setClassRef(int slot, int value) {
        slotsAdapter.setClassRef(this, objectRef, slot, value);
    }

    @Override
    public void setLong(int slot, long value) {
        slotsAdapter.setLong(this, objectRef, slot, value);
    }

    @Override
    public void setFloat(int slot, float value) {
        slotsAdapter.setFloat(this, objectRef, slot, value);
    }

    @Override
    public void setDouble(int slot, double value) {
        slotsAdapter.setDouble(this, objectRef, slot, value);
    }

    @Override
    public void setByte(int slot, byte value) {
        slotsAdapter.setByte(this, objectRef, slot, value);
    }

    @Override
    public void setShort(int slot, short value) {
        slotsAdapter.setShort(this, objectRef, slot, value);
    }

    @Override
    public void setChar(int slot, char value) {
        slotsAdapter.setChar(this, objectRef, slot, value);
    }

    @Override
    public void setBoolean(int slot, boolean value) {
        slotsAdapter.setBoolean(this, objectRef, slot, value);
    }

    @Override
    public void setString(int slot, String value) {
        slotsAdapter.setString(this, objectRef, slot, value);
    }

    @Override
    public void setObject(int slot, Instance<?> value) {
        slotsAdapter.setObject(this, objectRef, slot, value);
    }

    @Override
    public Instance<?> getObject(int slot) {
        return slotsAdapter.getObject(this, objectRef, slot);
    }

    @Override
    public void incInt(int slot, int value) {
        int current = slotsAdapter.getInt(this, objectRef, slot);
        slotsAdapter.setInt(this, objectRef, slot, current + value);
    }

    @Override
    public void incFloat(int slot, float value) {
        float current = slotsAdapter.getFloat(this, objectRef, slot);
        slotsAdapter.setFloat(this, objectRef, slot, current + value);
    }

    @Override
    public void incDouble(int slot, double value) {
        double current = slotsAdapter.getDouble(this, objectRef, slot);
        slotsAdapter.setDouble(this, objectRef, slot, current + value);
    }

    @Override
    public void incByte(int slot, byte value) {
        byte current = slotsAdapter.getByte(this, objectRef, slot);
        slotsAdapter.setByte(this, objectRef, slot, (byte) (current + value));
    }

    @Override
    public void incShort(int slot, short value) {
        short current = slotsAdapter.getShort(this, objectRef, slot);
        slotsAdapter.setShort(this, objectRef, slot, (short) (current + value));
    }

    @Override
    public void incLong(int slot, long value) {
        long current = slotsAdapter.getLong(this, objectRef, slot);
        slotsAdapter.setLong(this, objectRef, slot, current + value);
    }
}
