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
package org.siphonlab.ago.runtime.rdb;

import org.agrona.collections.IntHashSet;
import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.*;

import java.util.*;

public class RdbSlots implements Slots {

    protected final Slots baseSlots;

    private RowState rowState;

    private IntHashSet changedSlots = new IntHashSet();

    private Set<Instance<?>> usingInstances = null;
    private Set<Instance<?>> detachedInstances = null;

    private long id;
    private Pair<Instance<?>, Integer>[] objectSlots;

    protected boolean restoring = false;    // when restoring, don't change state and don't collect change log

    public RdbSlots(Slots baseSlots){
        this.baseSlots = baseSlots;
        this.rowState = RowState.Added;
    }

    public IntHashSet getChangedSlots() {
        return changedSlots;
    }

    private void collectChangedSlot(int slot){
        if(this.rowState == RowState.Unchanged){
            rowState = RowState.Modified;
        }
        changedSlots.add(slot);
    }

    public Slots getBaseSlots() {
        return baseSlots;
    }

    public void setRowState(RowState rowState) {
        this.rowState = rowState;
    }

    public RowState getRowState() {
        return rowState;
    }

    public Set<Instance<?>> getUsingInstances() {
        return usingInstances;
    }

    @Override
    public void setInt(int slot, int value) {
        if (!restoring && value != this.getInt(slot)) {
            collectChangedSlot(slot);
        }
        baseSlots.setInt(slot, value);
    }

    @Override
    public void setClassRef(int slot, int value) {
        if(!restoring && value != this.getClassRef(slot)){
            collectChangedSlot(slot);
        }
        baseSlots.setClassRef(slot, value);
    }

    @Override
    public void setLong(int slot, long value) {
        if (!restoring && value != this.getLong(slot)) {
            collectChangedSlot(slot);
        }
        baseSlots.setLong(slot, value);
    }

    @Override
    public void setFloat(int slot, float value) {
        if (!restoring && value != this.getFloat(slot)) {
            collectChangedSlot(slot);
        }
        baseSlots.setFloat(slot, value);
    }

    @Override
    public void setDouble(int slot, double value) {
        if (!restoring && value != this.getDouble(slot)) {
            collectChangedSlot(slot);
        }
        baseSlots.setDouble(slot, value);
    }

    @Override
    public void setByte(int slot, byte value) {
        if (!restoring && value != this.getByte(slot)) {
            collectChangedSlot(slot);
        }
        baseSlots.setByte(slot, value);
    }

    @Override
    public void setShort(int slot, short value) {
        if (!restoring && value != this.getShort(slot)) {
            collectChangedSlot(slot);
        }
        baseSlots.setShort(slot, value);
    }

    @Override
    public void setChar(int slot, char value) {
        if (!restoring && value != this.getChar(slot)) {
            collectChangedSlot(slot);
        }
        baseSlots.setChar(slot, value);
    }

    @Override
    public void setBoolean(int slot, boolean value) {
        if (!restoring && value != this.getBoolean(slot)) {
            collectChangedSlot(slot);
        }
        baseSlots.setBoolean(slot, value);
    }

    @Override
    public void setString(int slot, String value) {
        if (!Objects.equals(value, this.getString(slot))) {
            collectChangedSlot(slot);
        }
        baseSlots.setString(slot, value);
    }

    public void allocateObjectSlots(int slotDefsLength){
        if(this.objectSlots == null || this.objectSlots.length < slotDefsLength)
            this.objectSlots = new Pair[slotDefsLength];
    }

    @Override
    public void setObject(int slot, Instance<?> value) {
        if(!restoring) {
            Instance<?> prev = baseSlots.getObject(slot);
            if (prev != value) {
                collectChangedSlot(slot);
                if (value != null) {
                    if (this.usingInstances == null) {
                        this.usingInstances = new HashSet<>();
                    }
                    usingInstances.add(value);
                }
                if (prev != null) {
                    if (this.detachedInstances == null) {
                        this.detachedInstances = new HashSet<>();
                    }
                    detachedInstances.add(prev);
                }
            }
        }
        baseSlots.setObject(slot, value);
        this.objectSlots[slot] = Pair.of(value, slot);
    }

    public Pair<Instance<?>, Integer>[] getObjectSlots() {
        return objectSlots;
    }

    public void clearDetachedInstances(){
        if(detachedInstances != null && !detachedInstances.isEmpty()) {
            usingInstances.removeAll(detachedInstances);
            detachedInstances.clear();
        }
    }

    @Override
    public Instance<?> getObject(int slot) {
        return baseSlots.getObject(slot);
    }

    @Override
    public void incInt(int slot, int value) {
        if(!restoring) collectChangedSlot(slot);
        baseSlots.incInt(slot, value);
    }

    @Override
    public void incFloat(int slot, float value) {
        if (!restoring) collectChangedSlot(slot);
        baseSlots.incFloat(slot, value);
    }

    @Override
    public void incDouble(int slot, double value) {
        if (!restoring) collectChangedSlot(slot);
        baseSlots.incDouble(slot, value);
    }

    @Override
    public void incByte(int slot, byte value) {
        if (!restoring) collectChangedSlot(slot);
        baseSlots.incByte(slot, value);
    }

    @Override
    public void incShort(int slot, short value) {
        if (!restoring) collectChangedSlot(slot);
        baseSlots.incShort(slot, value);
    }

    @Override
    public void incLong(int slot, long value) {
        if (!restoring) collectChangedSlot(slot);
        baseSlots.incLong(slot, value);
    }

    @Override
    public Object get(int slotIndex, Class<?> clazz, AgoEngine agoEngine) {
        if (clazz == int.class) {
            return getInt(slotIndex);
        }
        if (clazz == double.class) {
            return getDouble(slotIndex);
        }
        if (clazz == String.class) {
            return getString(slotIndex);
        }
        if (clazz == long.class) {
            return getLong(slotIndex);
        }
        if (clazz == boolean.class) {
            return getBoolean(slotIndex);
        }
        if (clazz == short.class) {
            return getShort(slotIndex);
        }
        if (clazz == byte.class) {
            return getByte(slotIndex);
        }
        if (clazz == float.class) {
            return getFloat(slotIndex);
        }
        if (clazz == char.class) {
            return getChar(slotIndex);
        }
        if (AgoClass.class.isAssignableFrom(clazz)) {
            return agoEngine.getClass(getClassRef(slotIndex));       // class id
        }
        if (Instance.class.isAssignableFrom(clazz)) {
            return (Instance<?>) getObject(slotIndex);
        }
        throw new UnsupportedOperationException("unknown class " + clazz);
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


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void beginRestore(List<Instance<?>> usingInstances, RowState rowState) {
        restoring = true;

        this.rowState = rowState;
        if (usingInstances != null)
            this.usingInstances = new HashSet<>(usingInstances);
    }

    public void beginRestore(){
        restoring = true;
    }

    public void endRestore() {
        restoring = false;
    }

    @Override
    public String toString() {
        return "(RdbSlots %d)".formatted(id);
    }
}
