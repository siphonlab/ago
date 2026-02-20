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
package org.siphonlab.ago.runtime.rdb.lazy;

import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.RdbSlots;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandableSlots implements Slots {
    private final static Logger logger = LoggerFactory.getLogger(ExpandableSlots.class);

    private ExpandableObject<?> owner;
    private Slots innerSlots;

    public ExpandableSlots() {
    }

    public void setOwner(ExpandableObject<?> owner) {
        this.owner = owner;
    }

    public ExpandableObject<?> getOwner() {
        return owner;
    }

    public void setInnerSlots(Slots innerSlots) {
        this.innerSlots = innerSlots;
        if(logger.isDebugEnabled()) logger.debug("%s setInnerSlots %s".formatted(owner, innerSlots));
        if(innerSlots instanceof RdbSlots rdbSlots){
            rdbSlots.beginRestore();
            for (Pair<Instance<?>, Integer> p : rdbSlots.getObjectSlots()) {
                if(p != null && p.getLeft() != null){
                    rdbSlots.setObject(p.getRight(),transform(p.getLeft()));
                }
            }
            rdbSlots.endRestore();
        }
    }

    public Slots getInnerSlots() {
        return innerSlots;
    }

    @Override
    public Object get(int slot, Class<?> type, AgoEngine agoEngine) {return innerSlots.get(slot, type, agoEngine);}

    @Override
    public <T> void set(int slot, T value, Class<T> type) {innerSlots.set(slot, value, type);}

    @Override
    public int getInt(int slot) {return innerSlots.getInt(slot);}

    @Override
    public int getClassRef(int slot) {return innerSlots.getClassRef(slot);}

    @Override
    public long getLong(int slot) {return innerSlots.getLong(slot);}

    @Override
    public float getFloat(int slot) {return innerSlots.getFloat(slot);}

    @Override
    public double getDouble(int slot) {return innerSlots.getDouble(slot);}

    @Override
    public byte getByte(int slot) {return innerSlots.getByte(slot);}

    @Override
    public short getShort(int slot) {return innerSlots.getShort(slot);}

    @Override
    public char getChar(int slot) {return innerSlots.getChar(slot);}

    @Override
    public boolean getBoolean(int slot) {return innerSlots.getBoolean(slot);}

    @Override
    public String getString(int slot) {return innerSlots.getString(slot);}

    @Override
    public void setInt(int slot, int value) {innerSlots.setInt(slot, value);}

    @Override
    public void setClassRef(int slot, int value) {innerSlots.setClassRef(slot, value);}

    @Override
    public void setLong(int slot, long value) {innerSlots.setLong(slot, value);}

    @Override
    public void setFloat(int slot, float value) {innerSlots.setFloat(slot, value);}

    @Override
    public void setDouble(int slot, double value) {innerSlots.setDouble(slot, value);}

    @Override
    public void setByte(int slot, byte value) {innerSlots.setByte(slot, value);}

    @Override
    public void setShort(int slot, short value) {innerSlots.setShort(slot, value);}

    @Override
    public void setChar(int slot, char value) {innerSlots.setChar(slot, value);}

    @Override
    public void setBoolean(int slot, boolean value) {innerSlots.setBoolean(slot, value);}

    @Override
    public void setString(int slot, String value) {innerSlots.setString(slot, value);}

    @Override
    public void setObject(int slot, Instance<?> value) {
        var existed = innerSlots.getObject(slot);
        if(ObjectRefOwner.equals(existed, value)){
            return;
        }

        if(existed instanceof ExpandableObject<?> object){
            object.fold();
        }

        // always transform to ExpandableInstance
        value = transform(value);
        innerSlots.setObject(slot, value);
    }

    private Instance<?> transform(Instance<?> value) {
        if(value == null) return null;
        if(value instanceof CallFrame<?> callFrame){
            if (value instanceof ObjectRefCallFrame<?> objectRefCallFrame) {
                return  (Instance<?>) objectRefCallFrame.expandFor(this.owner);
            } else if (value instanceof ExpandableCallFrame<?> expandableInstance) {
                return  (Instance<?>) expandableInstance.expandFor(this.owner);
            }
        } else {
            if (value instanceof ObjectRefInstance<?> objectRefInstance) {
                return  (Instance<?>) objectRefInstance.expandFor(this.owner);
            } else if (value instanceof ExpandableInstance<?> expandableInstance) {
                return  (Instance<?>) expandableInstance.expandFor(this.owner);
            }
        }
        return value;
    }

    @Override
    public Instance<?> getObject(int slot) {return innerSlots.getObject(slot);}

    @Override
    public void incInt(int slot, int value) {innerSlots.incInt(slot, value);}

    @Override
    public void incFloat(int slot, float value) {innerSlots.incFloat(slot, value);}

    @Override
    public void incDouble(int slot, double value) {innerSlots.incDouble(slot, value);}

    @Override
    public void incByte(int slot, byte value) {innerSlots.incByte(slot, value);}

    @Override
    public void incShort(int slot, short value) {innerSlots.incShort(slot, value);}

    @Override
    public void incLong(int slot, long value) {innerSlots.incLong(slot, value);}

    @Override
    public Object getVoid(int slot) {return innerSlots.getVoid(slot);}

    @Override
    public void setVoid(int slot, Object value) {innerSlots.setVoid(slot, value);}
}
