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

/* I'll make a class for each Instance, it creates a class includes each data type
 for example, there will be a function : createSlots(int.class, int.class, String.class, Object.class)
 then we'll get
    class Slots_xxx{
        int slot0;
        int slot1;
        String slot2;
        Object slot3;
        int getInt(int slot){
            switch(slot){
                case 0: return slot0;
                case 1: return slot1;
                default: throw new IllegalArgumentException();
            }
        }
        setInt(int slot, int value){
            //...
        }
    }
 each Instance extends Slots, or to make it separate, has a Slots field
 */
public interface Slots {

    default Object get(int slot, Class<?> type, AgoEngine agoEngine) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default <T> void set(int slot, T value, Class<T> type) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default int getInt(int slot) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default int getClassRef(int slot) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default long getLong(int slot) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default float getFloat(int slot) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default double getDouble(int slot) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default byte getByte(int slot) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default short getShort(int slot) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default char getChar(int slot) {
        return (char)getInt(slot);
    }

    default boolean getBoolean(int slot) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default String getString(int slot) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    // 基本类型专用的 set 方法 (默认抛出 IllegalArgumentException)
    default void setInt(int slot, int value) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default void setClassRef(int slot, int value) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default void setLong(int slot, long value) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default void setFloat(int slot, float value) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default void setDouble(int slot, double value) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default void setByte(int slot, byte value) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default void setShort(int slot, short value) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default void setChar(int slot, char value) {
        setInt(slot, value);
    }

    default void setBoolean(int slot, boolean value) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default void setString(int slot, String value) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default void setObject(int slot, Instance<?> value) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default Instance<?> getObject(int slot) {
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default void incInt(int slot, int value){
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }
    default void incFloat(int slot, float value){
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }
    default void incDouble(int slot, double value){
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }
    default void incByte(int slot, byte value){
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }
    default void incShort(int slot, short value){
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }
    default void incLong(int slot, long value){
        throw new IllegalArgumentException("Unsupported slot access: " + slot);
    }

    default Object getVoid(int slot){return null;}

    default void setVoid(int slot, Object value){return;}
}

