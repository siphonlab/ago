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

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.RdbType;
import org.siphonlab.ago.runtime.rdb.reactive.json.ReactiveJsonPGAdapter;

/**
 * the persistent(save immediately) Slots don't store data in memory, it maps  getInt/setInt to SlotsAdapter.getInt/setInt,
 * and the adapter of this kind engine even support OP_CODE, see {@link ReactiveJsonPGAdapter#binaryOp(String, RdbType, ObjectRef, String, String, String)}
 * @param <T>
 */
public interface SlotsAdapter<T extends Slots> {

    int getInt(T slots, ObjectRef objectRef, int slot);

    void setInt(T slots, ObjectRef objectRef, int slot, int value);

    int getClassRef(T slots, ObjectRef objectRef, int slot);

    void setClassRef(T slots, ObjectRef objectRef, int slot, int value);

    long getLong(T slots, ObjectRef objectRef, int slot);

    void setLong(T slots, ObjectRef objectRef, int slot, long value);

    float getFloat(T slots, ObjectRef objectRef, int slot);

    void setFloat(T slots, ObjectRef objectRef, int slot, float value);

    double getDouble(T slots, ObjectRef objectRef, int slot);

    void setDouble(T slots, ObjectRef objectRef, int slot, double value);

    byte getByte(T slots, ObjectRef objectRef, int slot);

    void setByte(T slots, ObjectRef objectRef, int slot, byte value);

    short getShort(T slots, ObjectRef objectRef, int slot);

    void setShort(T slots, ObjectRef objectRef, int slot, short value);

    char getChar(T slots, ObjectRef objectRef, int slot);

    void setChar(T slots, ObjectRef objectRef, int slot, char value);

    boolean getBoolean(T slots, ObjectRef objectRef, int slot);

    void setBoolean(T slots, ObjectRef objectRef, int slot, boolean value);

    String getString(T slots, ObjectRef objectRef, int slot);

    void setString(T slots, ObjectRef objectRef, int slot, String value);

    Instance<?> getObject(T slots, ObjectRef objectRef, int slot);

    void setObject(T slots, ObjectRef objectRef, int slot, Instance<?> value);

    void incInt(T slots, ObjectRef objectRef, int slot, int value);

    void incFloat(T slots, ObjectRef objectRef, int slot, float value);

    void incDouble(T slots, ObjectRef objectRef, int slot, double value);

    void incByte(T slots, ObjectRef objectRef, int slot, byte value);

    void incShort(T slots, ObjectRef objectRef, int slot, short value);

    void incLong(T slots, ObjectRef objectRef, int slot, long value);

    String mapType(TypeCode typeCode, AgoClass agoClass);
}
