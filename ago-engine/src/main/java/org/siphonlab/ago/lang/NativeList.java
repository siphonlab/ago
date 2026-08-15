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

import org.siphonlab.ago.GenericArgumentsInfo;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import static org.siphonlab.ago.TypeCode.*;

public class NativeList {

    public static void getSize(NativeFrame callFrame) {
        var instance = callFrame.getParentScope();
        List<?> ls = (List<?>) instance.getNativePayload();
        callFrame.finishInt(ls.size());
    }

    public static void isReadOnly(NativeFrame callFrame) {
        callFrame.finishBoolean(false);
    }

    public static void clear(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        List<?> ls = (List<?>) instance.getNativePayload();
        ls.clear();
        callFrame.finishVoid();
    }

    // --- add ---

    public static void add(NativeFrame callFrame, int item) {
        var instance = callFrame.getParentScope();
        List<Integer> ls = (List<Integer>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, Instance<?> item) {
        var instance = callFrame.getParentScope();
        List<Instance<?>> ls = (List<Instance<?>>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, long item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        List<Long> ls = (List<Long>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, float item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        List<Float> ls = (List<Float>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, double item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        List<Double> ls = (List<Double>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, boolean item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        List<Boolean> ls = (List<Boolean>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, String item) {
        var instance = callFrame.getParentScope();
        List<String> ls = (List<String>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, short item) {
        var instance = callFrame.getParentScope();
        List<Short> ls = (List<Short>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, byte item) {
        var instance = callFrame.getParentScope();
        List<Byte> ls = (List<Byte>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, char item) {
        var instance = callFrame.getParentScope();
        List<Character> ls = (List<Character>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callframe, Object union) {
        var instance = callframe.getParentScope();
        List<Object> ls = (List<Object>) instance.getNativePayload();
        ls.add(union);
        callframe.finishVoid();
    }

    // --- contains ---

    public static void contains(NativeFrame callFrame, Instance<?> item) {
        var instance = callFrame.getParentScope();
        List<Instance<?>> ls = (List<Instance<?>>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, int item) {
        var instance = callFrame.getParentScope();
        List<Integer> ls = (List<Integer>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, long item) {
        var instance = callFrame.getParentScope();
        List<Long> ls = (List<Long>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, float item) {
        var instance = callFrame.getParentScope();
        List<Float> ls = (List<Float>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, double item) {
        var instance = callFrame.getParentScope();
        List<Double> ls = (List<Double>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, boolean item) {
        var instance = callFrame.getParentScope();
        List<Boolean> ls = (List<Boolean>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, String item) {
        var instance = callFrame.getParentScope();
        List<String> ls = (List<String>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, short item) {
        var instance = callFrame.getParentScope();
        List<Short> ls = (List<Short>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, byte item) {
        var instance = callFrame.getParentScope();
        List<Byte> ls = (List<Byte>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, char item) {
        var instance = callFrame.getParentScope();
        List<Character> ls = (List<Character>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame frame, Object union) {
        var instance = frame.getParentScope();
        List<Object> ls = (List<Object>) instance.getNativePayload();
        frame.finishBoolean(ls.contains(union));
    }

    // --- remove ---

    public static void remove(NativeFrame callFrame, int item) {
        var instance = callFrame.getParentScope();
        List<Integer> ls = (List<Integer>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Integer.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, long item) {
        var instance = callFrame.getParentScope();
        List<Long> ls = (List<Long>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Long.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, float item) {
        var instance = callFrame.getParentScope();
        List<Float> ls = (List<Float>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Float.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, double item) {
        var instance = callFrame.getParentScope();
        List<Double> ls = (List<Double>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Double.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, boolean item) {
        var instance = callFrame.getParentScope();
        List<Boolean> ls = (List<Boolean>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Boolean.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, String item) {
        var instance = callFrame.getParentScope();
        List<String> ls = (List<String>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(item));
    }

    public static void remove(NativeFrame callFrame, short item) {
        var instance = callFrame.getParentScope();
        List<Short> ls = (List<Short>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Short.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, byte item) {
        var instance = callFrame.getParentScope();
        List<Byte> ls = (List<Byte>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Byte.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, char item) {
        var instance = callFrame.getParentScope();
        List<Character> ls = (List<Character>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Character.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, Instance<?> item) {
        var instance = callFrame.getParentScope();
        List<Instance<?>> ls = (List<Instance<?>>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(item));
    }

    public static void remove(NativeFrame frame, Object union) {
        var instance = frame.getParentScope();
        List<Object> ls = (List<Object>) instance.getNativePayload();
        frame.finishBoolean(ls.remove(union));
    }

    // --- getAtIndex ---

    public static void getAtIndex(NativeFrame callFrame, int index) {
        var instance = callFrame.getParentScope();
        Object ls = instance.getNativePayload();
        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) instance.getAgoClass().getConcreteTypeInfo();
        var typeInfo = genericArgumentsInfo.getArguments()[0];
        switch (typeInfo.getTypeCode().value){
            case INT_VALUE:
                callFrame.finishInt(((List<Integer>) ls).get(index));
                break;
            case LONG_VALUE:
                callFrame.finishLong(((List<Long>) ls).get(index));
                break;
            case FLOAT_VALUE:
                callFrame.finishFloat(((List<Float>) ls).get(index));
                break;
            case DOUBLE_VALUE:
                callFrame.finishDouble(((List<Double>) ls).get(index));
                break;
            case DECIMAL_VALUE:
                callFrame.finishDecimal(((List<BigDecimal>) ls).get(index));
                break;
            case BOOLEAN_VALUE:
                callFrame.finishBoolean(((List<Boolean>) ls).get(index));
                break;
            case STRING_VALUE:
                callFrame.finishString(((List<String>) ls).get(index));
                break;
            case SHORT_VALUE:
                callFrame.finishShort(((List<Short>) ls).get(index));
                break;
            case BYTE_VALUE:
                callFrame.finishByte(((List<Byte>) ls).get(index));
                break;
            case CHAR_VALUE:
                callFrame.finishChar(((List<Character>) ls).get(index));
                break;
            case OBJECT_VALUE:
                callFrame.finishObject(((List<Instance<?>>) ls).get(index));
                break;
            case UNION_VALUE:
                callFrame.finishUnion(((List<Object>) ls).get(index));
                break;
            case CLASS_REF_VALUE:
                callFrame.finishInt(((List<Integer>) ls).get(index));
                break;
            default:
                throw new IllegalArgumentException("unknown type: %s".formatted(typeInfo.getTypeCode()));
        }
    }

    // --- setAtIndex ---

    public static void setAtIndex(NativeFrame callFrame, int index, Instance<?> item) {
        var instance = callFrame.getParentScope();
        List<Instance<?>> ls = (List<Instance<?>>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, int item) {
        var instance = callFrame.getParentScope();
        List<Integer> ls = (List<Integer>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, long item) {
        var instance = callFrame.getParentScope();
        List<Long> ls = (List<Long>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, float item) {
        var instance = callFrame.getParentScope();
        List<Float> ls = (List<Float>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, double item) {
        var instance = callFrame.getParentScope();
        List<Double> ls = (List<Double>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, boolean item) {
        var instance = callFrame.getParentScope();
        List<Boolean> ls = (List<Boolean>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, String item) {
        var instance = callFrame.getParentScope();
        List<String> ls = (List<String>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, short item) {
        var instance = callFrame.getParentScope();
        List<Short> ls = (List<Short>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, byte item) {
        var instance = callFrame.getParentScope();
        List<Byte> ls = (List<Byte>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, char item) {
        var instance = callFrame.getParentScope();
        List<Character> ls = (List<Character>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame frame, int index, Object union) {
        var instance = frame.getParentScope();
        List<Object> ls = (List<Object>) instance.getNativePayload();
        ls.set(index, union);
        frame.finishVoid();
    }

    // --- Iterator ---

    public static void Iterator_create(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope().getParentScope();
        List<?> ls = (List<?>) instance.getNativePayload();

        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        iteratorInstance.setNativePayload(ls.iterator());
        callFrame.finishVoid();
    }

    public static void Iterator_hasNext(NativeFrame callFrame) {
        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        Iterator<?> iterator = (Iterator<?>) iteratorInstance.getNativePayload();
        callFrame.finishBoolean(iterator.hasNext());
    }

    public static void Iterator_next(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope().getParentScope();

        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        Iterator<?> iterator = (Iterator<?>) iteratorInstance.getNativePayload();
        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) instance.getAgoClass().getConcreteTypeInfo();
        var typeInfo = genericArgumentsInfo.getArguments()[0];
        switch (typeInfo.getTypeCode().value){
            case INT_VALUE:
                callFrame.finishInt(((Iterator<Integer>)iterator).next());
                break;
            case LONG_VALUE:
                callFrame.finishLong(((Iterator<Long>) iterator).next());
                break;
            case FLOAT_VALUE:
                callFrame.finishFloat(((Iterator<Float>) iterator).next());
                break;
            case DOUBLE_VALUE:
                callFrame.finishDouble(((Iterator<Double>) iterator).next());
                break;
            case DECIMAL_VALUE:
                callFrame.finishDecimal(((Iterator<BigDecimal>) iterator).next());
                break;
            case BOOLEAN_VALUE:
                callFrame.finishBoolean(((Iterator<Boolean>) iterator).next());
                break;
            case STRING_VALUE:
                callFrame.finishString(((Iterator<String>) iterator).next());
                break;
            case SHORT_VALUE:
                callFrame.finishShort(((Iterator<Short>) iterator).next());
                break;
            case BYTE_VALUE:
                callFrame.finishByte(((Iterator<Byte>) iterator).next());
                break;
            case CHAR_VALUE:
                callFrame.finishChar(((Iterator<Character>) iterator).next());
                break;
            case OBJECT_VALUE:
                callFrame.finishObject(((Iterator<Instance<?>>) iterator).next());
                break;
            case UNION_VALUE:
                callFrame.finishUnion(((Iterator<Object>) iterator).next());
                break;
            case CLASS_REF_VALUE:
                callFrame.finishClassRef(callFrame.getAgoEngine().getClass(((Iterator<Integer>) iterator).next()));
                break;
            default:
                throw new IllegalArgumentException("unknown type: %s".formatted(typeInfo.getTypeCode()));
        }
    }
}
