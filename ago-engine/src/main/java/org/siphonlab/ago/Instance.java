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

/*
    Instance 是 AgoClass 的 ago 实例, 不是 AgoClass 的 Java 实例. AgoClass 的实例相当于 Class<T>
 */
public class Instance<C extends AgoClass>  {

    protected C agoClass;
    protected Instance parentScope;

    protected Slots slots;

    public Instance(final Slots slots, C agoClass){
        this.slots = slots;
        this.agoClass = agoClass;
    }

    public Instance(C agoClass){
        this.agoClass = agoClass;
    }

    public C getAgoClass() {
        return agoClass;
    }

    protected void setAgoClass(C agoClass) {
        this.agoClass = agoClass;
    }

    public Instance<?> getParentScope() {
        return parentScope;
    }

    public void setParentScope(Instance<?> parentScope) {
        this.parentScope = parentScope;
    }

    public Slots getSlots() {
        return slots;
    }

    @Override
    public String toString() {
        return "Instance" + "@" + this.agoClass;
    }

    public void invokeMethod(CallFrame<?> caller, int reenterState, int additionalState, AgoFunction method, Object... arguments){
        var runSpace = caller.getRunSpace();
        CallFrame<?> frame = runSpace.getAgoEngine().createFunctionInstance(this, method, caller);
        var interframe = new ReentrantProxyFrame<>(caller, frame, reenterState, additionalState);
        frame.assignArguments(arguments);
        runSpace.setCurrCallFrame(interframe);
    }

    public String getStringField(String fieldName){
        return this.getSlots().getString(this.getAgoClass().findField(fieldName).getSlotIndex());
    }

    public void setStringField(String fieldName, String value) {
        this.getSlots().setString(this.getAgoClass().findField(fieldName).getSlotIndex(), value);
    }

    public int getIntField(String fieldName) {
        return this.getSlots().getInt(this.getAgoClass().findField(fieldName).getSlotIndex());
    }

    public void setIntField(String fieldName, int value) {
        this.getSlots().setInt(this.getAgoClass().findField(fieldName).getSlotIndex(), value);
    }

    public boolean getBooleanField(String fieldName) {
        return this.getSlots().getBoolean(this.getAgoClass().findField(fieldName).getSlotIndex());
    }

    public void setBooleanField(String fieldName, boolean value) {
        this.getSlots().setBoolean(this.getAgoClass().findField(fieldName).getSlotIndex(), value);
    }

    public double getDoubleField(String fieldName) {
        return this.getSlots().getDouble(this.getAgoClass().findField(fieldName).getSlotIndex());
    }

    public void setDoubleField(String fieldName, double value) {
        this.getSlots().setDouble(this.getAgoClass().findField(fieldName).getSlotIndex(), value);
    }

    public float getFloatField(String fieldName) {
        return this.getSlots().getFloat(this.getAgoClass().findField(fieldName).getSlotIndex());
    }

    public void setFloatField(String fieldName, float value) {
        this.getSlots().setFloat(this.getAgoClass().findField(fieldName).getSlotIndex(), value);
    }

    public long getLongField(String fieldName) {
        return this.getSlots().getLong(this.getAgoClass().findField(fieldName).getSlotIndex());
    }

    public void setLongField(String fieldName, long value) {
        this.getSlots().setLong(this.getAgoClass().findField(fieldName).getSlotIndex(), value);
    }

    public short getShortField(String fieldName) {
        return this.getSlots().getShort(this.getAgoClass().findField(fieldName).getSlotIndex());
    }

    public void setShortField(String fieldName, short value) {
        this.getSlots().setShort(this.getAgoClass().findField(fieldName).getSlotIndex(), value);
    }

    public byte getByteField(String fieldName) {
        return this.getSlots().getByte(this.getAgoClass().findField(fieldName).getSlotIndex());
    }

    public void setByteField(String fieldName, byte value) {
        this.getSlots().setByte(this.getAgoClass().findField(fieldName).getSlotIndex(), value);
    }

    public Instance<?> getObjectField(String fieldName) {
        return this.getSlots().getObject(this.getAgoClass().findField(fieldName).getSlotIndex());
    }

    public void setObjectField(String fieldName, Instance<?> value) {
        this.getSlots().setObject(this.getAgoClass().findField(fieldName).getSlotIndex(), value);
    }

    public AgoClass getClassRefField(String fieldName) {
        return this.getAgoClass().getClassLoader().getClass(this.getSlots().getClassRef(this.getAgoClass().findField(fieldName).getSlotIndex()));
    }

    public void setClassRefField(String fieldName, AgoClass value) {
        this.getSlots().setClassRef(this.getAgoClass().findField(fieldName).getSlotIndex(), value.classId);
    }

    public Object getNativePayload() {
        throw new UnsupportedOperationException("only works for native instance");
    }

    public void setNativePayload(Object nativePayload) {
        throw new UnsupportedOperationException("only works for native instance");
    }

}
