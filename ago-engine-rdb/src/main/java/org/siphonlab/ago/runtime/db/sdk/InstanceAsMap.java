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
package org.siphonlab.ago.runtime.db.sdk;

import org.siphonlab.ago.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DECIMAL_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.NULL_VALUE;
import static org.siphonlab.ago.TypeCode.OBJECT_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;

public class InstanceAsMap implements Map<String, Object> {

    private final Instance<?> instance;
    private final AgoClass instanceClass;

    public InstanceAsMap(Instance<?> instance) {
        this.instance = instance;
        this.instanceClass = instance.getAgoClass();
    }

    @Override
    public int size() {
        return instanceClass.getSlotDefs().length;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return Arrays.stream(instanceClass.getSlotDefs()).anyMatch(slot -> slot.getName().equals(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Object get(Object key) {
        Optional<AgoSlotDef> slotDefOptional = Arrays.stream(instanceClass.getSlotDefs()).filter(slot -> slot.getName().equals(key)).findFirst();
        if(slotDefOptional.isPresent()){
            Slots slots = instance.getSlots();
            AgoSlotDef slotDef = slotDefOptional.get();
            int index = slotDef.getIndex();
            switch (slotDef.getTypeCode().value){
                case INT_VALUE:
                    return slots.getInt(index);
                case LONG_VALUE:
                    return slots.getLong(index);
                case FLOAT_VALUE:
                    return slots.getFloat(index);
                case DOUBLE_VALUE:
                    return slots.getDouble(index);
                case DECIMAL_VALUE:
                    return slots.getDecimal(index);
                case BOOLEAN_VALUE:
                    return slots.getBoolean(index);
                case STRING_VALUE:
                    return slots.getString(index);
                case SHORT_VALUE:
                    return slots.getShort(index);
                case BYTE_VALUE:
                    return slots.getByte(index);
                case CHAR_VALUE:
                    return slots.getChar(index);
                case OBJECT_VALUE:
                    if(slotDef.getAgoClass() instanceof MetaClass){
                        return slotDef.getAgoClass().getFullname();
                    } else {
//                        if(rdbType.getAdditional() == null){     // box type
//                            slots.setObject(slotIndex, box(rdbType.getTypeCode(), slotDef.getAgoClass(), resultSet, columnIndex++));
//                        } else {
//                            throw new RuntimeException("TODO");
////                            slots.setObject();
//                        }
                    }
                    break;
                case NULL_VALUE:
                    throw new UnsupportedOperationException("null??");
                case UNION_VALUE:
                    return slots.getUnion(index);
                case CLASS_REF_VALUE:
//                    var classname = resultSet.getString(columnIndex++);
//                    var classId = agoEngine.getClass(classname).getClassId();
//                    slots.setClassRef(slotIndex, classId);
//                    break;
            }
        }
        return null;
    }

    @Override
    public Object put(String key, Object value) {
        return null;
    }

    @Override
    public Object remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<String> keySet() {
        return Arrays.stream(instanceClass.getSlotDefs()).map(AgoSlotDef::getName).collect(Collectors.toSet());
    }

    @Override
    public Collection<Object> values() {
        return List.of();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return Set.of();
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
