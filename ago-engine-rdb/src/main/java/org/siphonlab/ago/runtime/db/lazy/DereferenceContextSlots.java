package org.siphonlab.ago.runtime.db.lazy;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.RunSpace;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.runtime.db.DbAdapter;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;

import java.math.BigDecimal;

// DereferencibleContextSlots can carry current runspace/adapter
public class DereferenceContextSlots<Id> implements Slots, ObjectRefOwner<Id> {

    private final ObjectRef<Id> objectRef;

    private RunSpace currentRunSpace;
    private DbAdapter<Id> dereferenceAdapter;

    private final Slots baseSlots;

    public DereferenceContextSlots(ObjectRef<Id> objectRef, Slots baseSlots) {
        this.objectRef = objectRef;
        this.baseSlots = baseSlots;
    }

    public void bindContext(RunSpace currentRunSpace, DbAdapter<Id> dereferenceAdapter) {
        this.currentRunSpace = currentRunSpace;
        this.dereferenceAdapter = dereferenceAdapter;
    }

    public RunSpace getCurrentRunSpace() {
        return currentRunSpace;
    }

    public DbAdapter<Id> getDereferenceAdapter() {
        return dereferenceAdapter;
    }

    public Slots getBaseSlots() {
        return baseSlots;
    }

    public ObjectRef<Id> getObjectRef() {
        return objectRef;
    }

    @Override
    public int getInt(int slot) {
        return baseSlots.getInt(slot);
    }

    @Override
    public int getClassRef(int slot) {
        return baseSlots.getClassRef(slot);
    }

    @Override
    public long getLong(int slot) {
        return baseSlots.getLong(slot);
    }

    @Override
    public float getFloat(int slot) {
        return baseSlots.getFloat(slot);
    }

    @Override
    public double getDouble(int slot) {
        return baseSlots.getDouble(slot);
    }

    @Override
    public BigDecimal getDecimal(int slot) {
        return baseSlots.getDecimal(slot);
    }

    @Override
    public byte getByte(int slot) {
        return baseSlots.getByte(slot);
    }

    @Override
    public short getShort(int slot) {
        return baseSlots.getShort(slot);
    }

    @Override
    public char getChar(int slot) {
        return baseSlots.getChar(slot);
    }

    @Override
    public boolean getBoolean(int slot) {
        return baseSlots.getBoolean(slot);
    }

    @Override
    public String getString(int slot) {
        return baseSlots.getString(slot);
    }

    @Override
    public void setInt(int slot, int value) {
        baseSlots.setInt(slot, value);
    }

    @Override
    public void setClassRef(int slot, int value) {
        baseSlots.setClassRef(slot, value);
    }

    @Override
    public void setLong(int slot, long value) {
        baseSlots.setLong(slot, value);
    }

    @Override
    public void setFloat(int slot, float value) {
        baseSlots.setFloat(slot, value);
    }

    @Override
    public void setDouble(int slot, double value) {
        baseSlots.setDouble(slot, value);
    }

    @Override
    public void setDecimal(int slot, BigDecimal value) {
        baseSlots.setDecimal(slot, value);
    }

    @Override
    public void setByte(int slot, byte value) {
        baseSlots.setByte(slot, value);
    }

    @Override
    public void setShort(int slot, short value) {
        baseSlots.setShort(slot, value);
    }

    @Override
    public void setChar(int slot, char value) {
        baseSlots.setChar(slot, value);
    }

    @Override
    public void setBoolean(int slot, boolean value) {
        baseSlots.setBoolean(slot, value);
    }

    @Override
    public void setString(int slot, String value) {
        baseSlots.setString(slot, value);
    }

    @Override
    public void setObject(int slot, Instance<?> value) {
        baseSlots.setObject(slot, value);
    }

    @Override
    public void setUnion(int slot, Object value) {
        baseSlots.setUnion(slot, value);
    }

    @Override
    public Object getUnion(int slot) {
        var r = baseSlots.getUnion(slot);
        if (r instanceof Instance<?> instance) {
            processInstanceSlot(instance);
        }
        return r;
    }

    @Override
    public Instance<?> getObject(int slot) {

        var inst = baseSlots.getObject(slot);
        if (inst != null) processInstanceSlot(inst);
        return inst;
    }

    private void processInstanceSlot(Instance<?> inst) {
        if (inst instanceof ObjectRefObject<?> objectRefObject) {
            ((ObjectRefObject<Id>) objectRefObject).bindDereferenceContext(currentRunSpace, dereferenceAdapter);
        } else {
            if (inst.getSlots() instanceof DereferenceContextSlots objectRefSlots) {
                objectRefSlots.bindContext(currentRunSpace, dereferenceAdapter);
            }
            if (inst.getParentScope() != null && inst.getParentScope().getSlots() instanceof DereferenceContextSlots dereferenceContextSlots) {
                dereferenceContextSlots.bindContext(currentRunSpace, dereferenceAdapter);
            }
        }
//        if(inst instanceof CallFrame<?> callFrame){
//            if(callFrame.getCaller() != null && callFrame.getCaller().getSlots() instanceof  ObjectRefSlots objectRefSlots){
//
//            }
//        }
    }

    @Override
    public void incInt(int slot, int value) {
        baseSlots.incInt(slot, value);
    }

    @Override
    public void incFloat(int slot, float value) {
        baseSlots.incFloat(slot, value);
    }

    @Override
    public void incDouble(int slot, double value) {
        baseSlots.incDouble(slot, value);
    }

    @Override
    public void incDecimal(int slot, BigDecimal value) {
        baseSlots.incDecimal(slot, value);
    }

    @Override
    public void incByte(int slot, byte value) {
        baseSlots.incByte(slot, value);
    }

    @Override
    public void incShort(int slot, short value) {
        baseSlots.incShort(slot, value);
    }

    @Override
    public void incLong(int slot, long value) {
        baseSlots.incLong(slot, value);
    }

    @Override
    public Object getVoid(int slot) {
        return baseSlots.getVoid(slot);
    }

    @Override
    public void setVoid(int slot, Object value) {
        baseSlots.setVoid(slot, value);
    }
}
