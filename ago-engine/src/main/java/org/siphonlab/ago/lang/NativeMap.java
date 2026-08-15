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
package org.siphonlab.ago.lang;

import org.eclipse.collections.impl.list.mutable.primitive.*;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.GenericArgumentsInfo;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import static org.siphonlab.ago.TypeCode.*;

public class NativeMap {

    private static Map<?, ?> getPayload(NativeFrame callFrame) {
        return (Map<?, ?>) ((NativeInstance) callFrame.getParentScope()).getNativePayload();
    }

    // --- size / isReadOnly / clear ---

    public static void getSize(NativeFrame callFrame) {
        callFrame.finishInt(getPayload(callFrame).size());
    }

    public static void isReadOnly(NativeFrame callFrame) {
        callFrame.finishBoolean(false);
    }

    public static void clear(NativeFrame callFrame) {
        getPayload(callFrame).clear();
        callFrame.finishVoid();
    }

    // --- get#key ---

    private static void finishGet(NativeFrame callFrame, Object valObj) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) instance.getAgoClass().getConcreteTypeInfo();
        var valueType = genericArgumentsInfo.getArguments()[1];
        switch (valueType.getTypeCode().value){
            case INT_VALUE:
                callFrame.finishInt((Integer) valObj);
                break;
            case LONG_VALUE:
                callFrame.finishLong((Long) valObj);
                break;
            case FLOAT_VALUE:
                callFrame.finishFloat((Float) valObj);
                break;
            case DOUBLE_VALUE:
                callFrame.finishDouble((Double) valObj);
                break;
            case DECIMAL_VALUE:
                callFrame.finishDecimal((BigDecimal) valObj);
                break;
            case BOOLEAN_VALUE:
                callFrame.finishBoolean((Boolean) valObj);
                break;
            case STRING_VALUE:
                callFrame.finishString((String) valObj);
                break;
            case SHORT_VALUE:
                callFrame.finishShort((Short) valObj);
                break;
            case BYTE_VALUE:
                callFrame.finishByte((Byte) valObj);
                break;
            case CHAR_VALUE:
                callFrame.finishChar((Character) valObj);
                break;
            case OBJECT_VALUE:
                callFrame.finishObject((Instance<?>) valObj);
                break;
            case UNION_VALUE:
                callFrame.finishUnion(valObj);
                break;
            case CLASS_REF_VALUE:
                callFrame.finishInt((Integer) valObj);
                break;
            default:
                throw new IllegalArgumentException("unknown value type: %s".formatted(valueType.getTypeCode()));
        }
    }

    public static void get(NativeFrame callFrame, int key) {
        Object valObj = getPayload(callFrame).get(key);
        finishGet(callFrame, valObj);
    }

    public static void get(NativeFrame callFrame, long key) {
        Object valObj = getPayload(callFrame).get(key);
        finishGet(callFrame, valObj);
    }

    public static void get(NativeFrame callFrame, float key) {
        Object valObj = getPayload(callFrame).get(Float.floatToIntBits(key));
        finishGet(callFrame, valObj);
    }

    public static void get(NativeFrame callFrame, double key) {
        Object valObj = getPayload(callFrame).get(Double.doubleToLongBits(key));
        finishGet(callFrame, valObj);
    }

    public static void get(NativeFrame callFrame, boolean key) {
        Object valObj = getPayload(callFrame).get(key ? 1 : 0);
        finishGet(callFrame, valObj);
    }

    public static void get(NativeFrame callFrame, String key) {
        Object valObj = getPayload(callFrame).get(key);
        finishGet(callFrame, valObj);
    }

    public static void get(NativeFrame callFrame, short key) {
        Object valObj = getPayload(callFrame).get((int) key);
        finishGet(callFrame, valObj);
    }

    public static void get(NativeFrame callFrame, byte key) {
        Object valObj = getPayload(callFrame).get((int) key);
        finishGet(callFrame, valObj);
    }

    public static void get(NativeFrame callFrame, char key) {
        Object valObj = getPayload(callFrame).get((int) key);
        finishGet(callFrame, valObj);
    }

    public static void get(NativeFrame callFrame, Instance<?> key) {
        Object valObj = getPayload(callFrame).get(key);
        finishGet(callFrame, valObj);
    }

    // --- put ---

    public static void put(NativeFrame callFrame, int key, Instance<?> value) {
        Map map = (Map) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, int key, int value) {
        Map<Integer, Integer> map = (Map<Integer, Integer>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, int key, long value) {
        Map<Integer, Long> map = (Map<Integer, Long>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, int key, float value) {
        Map<Integer, Float> map = (Map<Integer, Float>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, int key, double value) {
        Map<Integer, Double> map = (Map<Integer, Double>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, int key, boolean value) {
        Map<Integer, Boolean> map = (Map<Integer, Boolean>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, int key, String value) {
        Map<Integer, String> map = (Map<Integer, String>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, long key, Instance<?> value) {
        Map<Long, Instance<?>> map = (Map<Long, Instance<?>>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, String key, Instance<?> value) {
        Map<String, Instance<?>> map = (Map<String, Instance<?>>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, String key, int value) {
        Map<String, Integer> map = (Map<String, Integer>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, String key, long value) {
        Map<String, Long> map = (Map<String, Long>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, String key, float value) {
        Map<String, Float> map = (Map<String, Float>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, String key, double value) {
        Map<String, Double> map = (Map<String, Double>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, String key, boolean value) {
        Map<String, Boolean> map = (Map<String, Boolean>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    public static void put(NativeFrame callFrame, String key, String value) {
        Map<String, String> map = (Map<String, String>) getPayload(callFrame);
        map.put(key, value);
        callFrame.finishVoid();
    }

    // --- containsKey ---

    public static void containsKey(NativeFrame callFrame, int key) {
        callFrame.finishBoolean(getPayload(callFrame).containsKey(key));
    }

    public static void containsKey(NativeFrame callFrame, long key) {
        callFrame.finishBoolean(getPayload(callFrame).containsKey(key));
    }

    public static void containsKey(NativeFrame callFrame, float key) {
        callFrame.finishBoolean(getPayload(callFrame).containsKey(Float.floatToIntBits(key)));
    }

    public static void containsKey(NativeFrame callFrame, double key) {
        callFrame.finishBoolean(getPayload(callFrame).containsKey(Double.doubleToLongBits(key)));
    }

    public static void containsKey(NativeFrame callFrame, boolean key) {
        callFrame.finishBoolean(getPayload(callFrame).containsKey(key ? 1 : 0));
    }

    public static void containsKey(NativeFrame callFrame, String key) {
        callFrame.finishBoolean(getPayload(callFrame).containsKey(key));
    }

    public static void containsKey(NativeFrame callFrame, short key) {
        callFrame.finishBoolean(getPayload(callFrame).containsKey((int) key));
    }

    public static void containsKey(NativeFrame callFrame, byte key) {
        callFrame.finishBoolean(getPayload(callFrame).containsKey((int) key));
    }

    public static void containsKey(NativeFrame callFrame, char key) {
        callFrame.finishBoolean(getPayload(callFrame).containsKey((int) key));
    }

    public static void containsKey(NativeFrame callFrame, Instance<?> key) {
        callFrame.finishBoolean(getPayload(callFrame).containsKey(key));
    }

    // --- removeByKey ---

    public static void removeByKey(NativeFrame callFrame, int key) {
        Map<?, ?> map = getPayload(callFrame);
        boolean res = map.remove(key) != null;
        callFrame.finishBoolean(res);
    }

    public static void removeByKey(NativeFrame callFrame, long key) {
        Map<?, ?> map = getPayload(callFrame);
        boolean res = map.remove(key) != null;
        callFrame.finishBoolean(res);
    }

    public static void removeByKey(NativeFrame callFrame, float key) {
        Map<?, ?> map = getPayload(callFrame);
        boolean res = map.remove(Float.floatToIntBits(key)) != null;
        callFrame.finishBoolean(res);
    }

    public static void removeByKey(NativeFrame callFrame, double key) {
        Map<?, ?> map = getPayload(callFrame);
        boolean res = map.remove(Double.doubleToLongBits(key)) != null;
        callFrame.finishBoolean(res);
    }

    public static void removeByKey(NativeFrame callFrame, boolean key) {
        Map<?, ?> map = getPayload(callFrame);
        boolean res = map.remove(key ? 1 : 0) != null;
        callFrame.finishBoolean(res);
    }

    public static void removeByKey(NativeFrame callFrame, String key) {
        Map<?, ?> map = getPayload(callFrame);
        boolean res = map.remove(key) != null;
        callFrame.finishBoolean(res);
    }

    public static void removeByKey(NativeFrame callFrame, short key) {
        Map<?, ?> map = getPayload(callFrame);
        boolean res = map.remove((int) key) != null;
        callFrame.finishBoolean(res);
    }

    public static void removeByKey(NativeFrame callFrame, byte key) {
        Map<?, ?> map = getPayload(callFrame);
        boolean res = map.remove((int) key) != null;
        callFrame.finishBoolean(res);
    }

    public static void removeByKey(NativeFrame callFrame, char key) {
        Map<?, ?> map = getPayload(callFrame);
        boolean res = map.remove((int) key) != null;
        callFrame.finishBoolean(res);
    }

    public static void removeByKey(NativeFrame callFrame, Instance<?> key) {
        Map<?, ?> map = getPayload(callFrame);
        boolean res = map.remove(key) != null;
        callFrame.finishBoolean(res);
    }

    // --- keys / values ---

    private static void addToArrayList(Object ls, Object v, int typeCodeValue) {
        switch (typeCodeValue){
            case INT_VALUE, CLASS_REF_VALUE:
                ((IntArrayList)ls).add((Integer)v);
                break;
            case LONG_VALUE:
                ((LongArrayList)ls).add((Long)v);
                break;
            case FLOAT_VALUE:
                ((FloatArrayList)ls).add(Float.intBitsToFloat((Integer) v));
                break;
            case DOUBLE_VALUE:
                ((DoubleArrayList)ls).add(Double.longBitsToDouble((Long) v));
                break;
            case BOOLEAN_VALUE:
                ((BooleanArrayList)ls).add(((Integer) v) == 1);
                break;
            case STRING_VALUE, OBJECT_VALUE, DECIMAL_VALUE, UNION_VALUE:
                ((java.util.ArrayList<Object>)ls).add(v);
                break;
            case SHORT_VALUE:
                ((ShortArrayList)ls).add(((Integer)v).shortValue());
                break;
            case BYTE_VALUE:
                ((ByteArrayList)ls).add(((Integer)v).byteValue());
                break;
            case CHAR_VALUE:
                ((CharArrayList)ls).add((char)((Integer)v).intValue());
                break;
            default:
                throw new IllegalStateException("Unsupported type: " + typeCodeValue);
        }
    }

    public static void keys(NativeFrame callFrame, Instance<?> arrayList) {
        NativeInstance mapInst = (NativeInstance) callFrame.getParentScope();
        Map<?, ?> map = getPayload(callFrame);
        Object ls = ((NativeInstance)arrayList).getNativePayload();

        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) mapInst.getAgoClass().getConcreteTypeInfo();
        var keyType = genericArgumentsInfo.getArguments()[0];

        for (Object v : map.keySet()) {
            addToArrayList(ls, v, keyType.getTypeCode().value);
        }
        callFrame.finishVoid();
    }

    public static void values(NativeFrame callFrame, Instance<?> arrayList) {
        NativeInstance mapInst = (NativeInstance) callFrame.getParentScope();
        Map<?, ?> map = getPayload(callFrame);
        Object ls = ((NativeInstance)arrayList).getNativePayload();

        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) mapInst.getAgoClass().getConcreteTypeInfo();
        var valueType = genericArgumentsInfo.getArguments()[1];

        for (Object v : map.values()) {
            addToArrayList(ls, v, valueType.getTypeCode().value);
        }
        callFrame.finishVoid();
    }

    // --- putAll ---

    public static void putAll(NativeFrame frame, Instance<?> anotherMap){
        Map map = (Map) getPayload(frame);
        Map another = (Map) ((NativeInstance)anotherMap).getNativePayload();
        map.putAll(another);
        frame.finishVoid();
    }

    // --- Iterator ---

    public static void Iterator_create(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope().getParentScope();
        Map<?, ?> map = (Map<?, ?>) instance.getNativePayload();

        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        iteratorInstance.setNativePayload(map.entrySet().iterator());
        callFrame.finishVoid();
    }

    public static void Iterator_hasNext(NativeFrame callFrame) {
        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        Iterator<?> iterator = (Iterator<?>) iteratorInstance.getNativePayload();
        callFrame.finishBoolean(iterator.hasNext());
    }

    public static void Iterator_next(NativeFrame callFrame) {
        NativeInstance mapInst = (NativeInstance) callFrame.getParentScope().getParentScope();

        NativeInstance iterInst = (NativeInstance) callFrame.getParentScope();
        Object itObj = iterInst.getNativePayload();

        var IteratorKeyValuePairType = iterInst.getAgoClass().getInterfaces()[0];
        var KeyValuePairType = ((GenericArgumentsInfo)IteratorKeyValuePairType.getConcreteTypeInfo()).getArguments()[0];

        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) mapInst.getAgoClass().getConcreteTypeInfo();
        var keyType = genericArgumentsInfo.getArguments()[0];
        var valueType = genericArgumentsInfo.getArguments()[1];

        AgoEngine agoEngine = callFrame.getAgoEngine();
        var r = agoEngine.createInstance(KeyValuePairType, callFrame.getRunSpace());
        Slots slots = r.getSlots();

        @SuppressWarnings("unchecked")
        Map.Entry<Object, Object> entry = ((Iterator<Map.Entry<Object, Object>>)itObj).next();
        Object keyObj = entry.getKey();
        Object valObj = entry.getValue();

        switch (keyType.getTypeCode().value) {
            case INT_VALUE, CLASS_REF_VALUE:
                slots.setInt(0, (Integer) keyObj);
                break;
            case LONG_VALUE:
                slots.setLong(0, (Long) keyObj);
                break;
            case FLOAT_VALUE:
                slots.setFloat(0, Float.intBitsToFloat((Integer) keyObj));
                break;
            case DOUBLE_VALUE:
                slots.setDouble(0, Double.longBitsToDouble((Long) keyObj));
                break;
            case BOOLEAN_VALUE:
                slots.setBoolean(0, (Integer) keyObj != 0);
                break;
            case STRING_VALUE:
                slots.setString(0, (String) keyObj);
                break;
            case SHORT_VALUE:
                slots.setShort(0, ((Integer) keyObj).shortValue());
                break;
            case BYTE_VALUE:
                slots.setByte(0, ((Integer) keyObj).byteValue());
                break;
            case CHAR_VALUE:
                slots.setChar(0, (char)((Integer) keyObj).intValue());
                break;
            case OBJECT_VALUE:
                slots.setObject(0, (Instance<?>) keyObj);
                break;
            case UNION_VALUE:
                slots.setUnion(0, keyObj);
                break;
            default:
                throw new IllegalStateException("Unsupported key type: " + keyType.getTypeCode());
        }

        writeValue(slots, 1, valueType, valObj);
        callFrame.finishObject(r);
    }

    private static void writeValue(Slots slots, int index, AgoClass valueType, Object val) {
        switch (valueType.getTypeCode().value) {
            case INT_VALUE:
                slots.setInt(index, (Integer) val);
                break;
            case LONG_VALUE:
                slots.setLong(index, (Long) val);
                break;
            case FLOAT_VALUE:
                slots.setFloat(index, (Float) val);
                break;
            case DOUBLE_VALUE:
                slots.setDouble(index, (Double) val);
                break;
            case DECIMAL_VALUE:
                slots.setDecimal(index, (BigDecimal) val);
                break;
            case BOOLEAN_VALUE:
                slots.setBoolean(index, (Boolean) val);
                break;
            case STRING_VALUE:
                slots.setString(index, (String) val);
                break;
            case SHORT_VALUE:
                slots.setShort(index, (Short) val);
                break;
            case BYTE_VALUE:
                slots.setByte(index, (Byte) val);
                break;
            case CHAR_VALUE:
                slots.setChar(index, (Character) val);
                break;
            case OBJECT_VALUE:
                slots.setObject(index, (Instance<?>) val);
                break;
            case UNION_VALUE:
                slots.setUnion(index, val);
                break;
            case CLASS_REF_VALUE:
                slots.setInt(index, (Integer) val);
                break;
        }
    }

    private static int extractKey(Object keyVal, int typeCodeValue) {
        switch (typeCodeValue){
            case INT_VALUE, CLASS_REF_VALUE:
                return (Integer) keyVal;
            case LONG_VALUE:
                return (int) ((Long) keyVal).longValue();
            case FLOAT_VALUE:
                return Float.floatToIntBits((Float) keyVal);
            case DOUBLE_VALUE:
                return (int) Double.doubleToLongBits((Double) keyVal);
            case BOOLEAN_VALUE:
                return (Boolean) keyVal ? 1 : 0;
            case SHORT_VALUE:
                return ((Short) keyVal).shortValue();
            case BYTE_VALUE:
                return ((Byte) keyVal).byteValue();
            case CHAR_VALUE:
                return (int) (Character) keyVal;
            default:
                throw new IllegalArgumentException("unknown key type: %s".formatted(typeCodeValue));
        }
    }
}
