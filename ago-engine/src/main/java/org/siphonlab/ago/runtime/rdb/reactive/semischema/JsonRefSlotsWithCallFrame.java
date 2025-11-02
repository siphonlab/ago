package org.siphonlab.ago.runtime.rdb.reactive.semischema;

import org.siphonlab.ago.AgoSlotDef;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.runtime.rdb.reactive.CallFrameBoundSlots;
import org.siphonlab.ago.runtime.rdb.ObjectRef;

public class JsonRefSlotsWithCallFrame extends JsonRefSlots implements CallFrameBoundSlots<JsonRefSlots> {

    private final CallFrame<?> callFrame;
    private final PGJsonSlotsAdapter pgJsonSlotsAdapter;

    public JsonRefSlotsWithCallFrame(ObjectRef objectRef, PGJsonSlotsAdapter slotsAdapter, AgoSlotDef[] slotDefs, CallFrame<?> callFrame) {
        super(objectRef, slotsAdapter, slotDefs);
        this.callFrame = callFrame;
        this.pgJsonSlotsAdapter = slotsAdapter;
    }

    @Override
    public CallFrame<?> getCallFrame() {
        return callFrame;
    }

    @Override
    public int getInt(int slot) {
        return pgJsonSlotsAdapter.getInt(this, objectRef, slot);
    }

    @Override
    public int getClassRef(int slot) {
        return pgJsonSlotsAdapter.getClassRef(this, objectRef, slot);
    }

    @Override
    public long getLong(int slot) {
        return pgJsonSlotsAdapter.getLong(this, objectRef, slot);
    }

    @Override
    public float getFloat(int slot) {
        return pgJsonSlotsAdapter.getFloat(this, objectRef, slot);
    }

    @Override
    public double getDouble(int slot) {
        return pgJsonSlotsAdapter.getDouble(this, objectRef, slot);
    }

    @Override
    public byte getByte(int slot) {
        return pgJsonSlotsAdapter.getByte(this, objectRef, slot);
    }

    @Override
    public short getShort(int slot) {
        return pgJsonSlotsAdapter.getShort(this, objectRef, slot);
    }

    @Override
    public char getChar(int slot) {
        return pgJsonSlotsAdapter.getChar(this, objectRef, slot);
    }

    @Override
    public boolean getBoolean(int slot) {
        return pgJsonSlotsAdapter.getBoolean(this, objectRef, slot);
    }

    @Override
    public String getString(int slot) {
        return pgJsonSlotsAdapter.getString(this, objectRef, slot);
    }

    @Override
    public void setInt(int slot, int value) {
        pgJsonSlotsAdapter.setInt(this, objectRef, slot, value);
    }

    @Override
    public void setClassRef(int slot, int value) {
        pgJsonSlotsAdapter.setClassRef(this, objectRef, slot, value);
    }

    @Override
    public void setLong(int slot, long value) {
        pgJsonSlotsAdapter.setLong(this, objectRef, slot, value);
    }

    @Override
    public void setFloat(int slot, float value) {
        pgJsonSlotsAdapter.setFloat(this, objectRef, slot, value);
    }

    @Override
    public void setDouble(int slot, double value) {
        pgJsonSlotsAdapter.setDouble(this, objectRef, slot, value);
    }

    @Override
    public void setByte(int slot, byte value) {
        pgJsonSlotsAdapter.setByte(this, objectRef, slot, value);
    }

    @Override
    public void setShort(int slot, short value) {
        pgJsonSlotsAdapter.setShort(this, objectRef, slot, value);
    }

    @Override
    public void setChar(int slot, char value) {
        pgJsonSlotsAdapter.setChar(this, objectRef, slot, value);
    }

    @Override
    public void setBoolean(int slot, boolean value) {
        pgJsonSlotsAdapter.setBoolean(this, objectRef, slot, value);
    }

    @Override
    public void setString(int slot, String value) {
        pgJsonSlotsAdapter.setString(this, objectRef, slot, value);
    }

    @Override
    public void setObject(int slot, Instance<?> value) {
        pgJsonSlotsAdapter.setObject(this, objectRef, slot, value);
    }

    @Override
    public Instance<?> getObject(int slot) {
        return pgJsonSlotsAdapter.getObject(this, objectRef, slot);
    }

    @Override
    public void incInt(int slot, int value) {
        int current = pgJsonSlotsAdapter.getInt(this, objectRef, slot);
        pgJsonSlotsAdapter.setInt(this, objectRef, slot, current + value);
    }

    @Override
    public void incFloat(int slot, float value) {
        float current = pgJsonSlotsAdapter.getFloat(this, objectRef, slot);
        pgJsonSlotsAdapter.setFloat(this, objectRef, slot, current + value);
    }

    @Override
    public void incDouble(int slot, double value) {
        double current = pgJsonSlotsAdapter.getDouble(this, objectRef, slot);
        pgJsonSlotsAdapter.setDouble(this, objectRef, slot, current + value);
    }

    @Override
    public void incByte(int slot, byte value) {
        byte current = pgJsonSlotsAdapter.getByte(this, objectRef, slot);
        pgJsonSlotsAdapter.setByte(this, objectRef, slot, (byte) (current + value));
    }

    @Override
    public void incShort(int slot, short value) {
        short current = pgJsonSlotsAdapter.getShort(this, objectRef, slot);
        pgJsonSlotsAdapter.setShort(this, objectRef, slot, (short) (current + value));
    }

    @Override
    public void incLong(int slot, long value) {
        long current = pgJsonSlotsAdapter.getLong(this, objectRef, slot);
        pgJsonSlotsAdapter.setLong(this, objectRef, slot, current + value);
    }

    @Override
    public String toString() {
        return "(JsonRefSlotsWithCallFrame " + objectRef.className() + " " + objectRef.id() + ")";
    }
}
