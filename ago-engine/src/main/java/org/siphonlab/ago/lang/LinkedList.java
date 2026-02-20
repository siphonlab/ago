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

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;

import java.util.Iterator;

import static org.siphonlab.ago.TypeCode.*;

public class LinkedList {

    public static void create(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        instance.setNativePayload(new java.util.LinkedList<>());
        callFrame.finishVoid();
    }

    public static void getCount(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        var ls = (java.util.LinkedList<?>) instance.getNativePayload();
        callFrame.finishInt(ls.size());
    }

    public static void isReadOnly(NativeFrame callFrame) {
        callFrame.finishBoolean(false);
    }

    public static void clear(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<?> ls = (java.util.LinkedList<?>) instance.getNativePayload();
        ls.clear();
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, int item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, Instance<?> item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Instance> ls = (java.util.LinkedList<Instance>) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, long item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, float item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, double item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, boolean item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, String item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, short item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, byte item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void add(NativeFrame callFrame, char item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        ls.add(item);
        callFrame.finishVoid();
    }

    public static void contains(NativeFrame callFrame, Instance<?> item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Instance> ls = (java.util.LinkedList<Instance>) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, int item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, long item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, float item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, double item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, boolean item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, String item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, short item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, byte item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void contains(NativeFrame callFrame, char item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();
        callFrame.finishBoolean(ls.contains(item));
    }

    public static void copyTo(NativeFrame callFrame, Instance<?> array, int arrayIndex) {
        // 示例（伪代码）：
        // ((java.util.LinkedList<Object>) instance.getNativePayload()).toArray(array, idx);
        callFrame.finishVoid();   // 先留空
    }

    public static void remove(NativeFrame callFrame, int item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Integer> ls = (java.util.LinkedList<Integer>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Integer.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, long item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Long> ls = (java.util.LinkedList<Long>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Long.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, float item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Float> ls = (java.util.LinkedList<Float>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Float.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, double item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Double> ls = (java.util.LinkedList<Double>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Double.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, boolean item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Boolean> ls = (java.util.LinkedList<Boolean>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Boolean.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, String item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<String> ls = (java.util.LinkedList<String>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(item));
    }

    public static void remove(NativeFrame callFrame, short item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Short> ls = (java.util.LinkedList<Short>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Short.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, byte item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Byte> ls = (java.util.LinkedList<Byte>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Byte.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, char item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Character> ls = (java.util.LinkedList<Character>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(Character.valueOf(item)));
    }

    public static void remove(NativeFrame callFrame, Instance<?> item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Instance> ls = (java.util.LinkedList<Instance>) instance.getNativePayload();
        callFrame.finishBoolean(ls.remove(item));
    }

    public static void getAtIndex(NativeFrame callFrame, int index) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        Object ls = instance.getNativePayload();
        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) instance.getAgoClass().getConcreteTypeInfo();
        TypeInfo typeInfo = genericArgumentsInfo.getArguments()[0];
        switch (typeInfo.getTypeCode().value){
            case INT_VALUE:
                callFrame.finishInt(((java.util.LinkedList<Integer>) ls).get(index));
                break;
            case LONG_VALUE:
                callFrame.finishLong(((java.util.LinkedList<Long>) ls).get(index));
                break;
            case FLOAT_VALUE:
                callFrame.finishFloat(((java.util.LinkedList<Float>) ls).get(index));
                break;
            case DOUBLE_VALUE:
                callFrame.finishDouble(((java.util.LinkedList<Double>) ls).get(index));
                break;
            case BOOLEAN_VALUE:
                callFrame.finishBoolean(((java.util.LinkedList<Boolean>) ls).get(index));
                break;
            case STRING_VALUE:
                callFrame.finishString(((java.util.LinkedList<String>) ls).get(index));
                break;
            case SHORT_VALUE:
                callFrame.finishShort(((java.util.LinkedList<Short>) ls).get(index));
                break;
            case BYTE_VALUE:
                callFrame.finishByte(((java.util.LinkedList<Byte>) ls).get(index));
                break;
            case CHAR_VALUE:
                callFrame.finishChar(((java.util.LinkedList<Character>) ls).get(index));
                break;
            case OBJECT_VALUE:
                callFrame.finishObject(((java.util.LinkedList<Instance<?>>) ls).get(index));
                break;
            case CLASS_REF_VALUE:
                callFrame.finishInt(((java.util.LinkedList<Integer>) ls).get(index));
                break;
            default:
                throw new IllegalArgumentException("unknown type: %s".formatted(typeInfo.getTypeCode()));
        }
    }

    public static void setAtIndex(NativeFrame callFrame, int index, Instance<?> item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Instance> ls = (java.util.LinkedList<Instance>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, int item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Integer> ls = (java.util.LinkedList<Integer>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, long item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Long> ls = (java.util.LinkedList<Long>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, float item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Float> ls = (java.util.LinkedList<Float>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, double item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Double> ls = (java.util.LinkedList<Double>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, boolean item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Boolean> ls = (java.util.LinkedList<Boolean>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, String item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<String> ls = (java.util.LinkedList<String>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, short item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Short> ls = (java.util.LinkedList<Short>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, byte item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Byte> ls = (java.util.LinkedList<Byte>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void setAtIndex(NativeFrame callFrame, int index, char item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<Character> ls = (java.util.LinkedList<Character>) instance.getNativePayload();
        ls.set(index, item);
        callFrame.finishVoid();
    }

    public static void Iterator_create(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope().getParentScope();
        java.util.LinkedList ls = (java.util.LinkedList) instance.getNativePayload();

        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        var it = ls.iterator();
        iteratorInstance.setNativePayload(it);
        callFrame.finishVoid();
    }

    public static void Iterator_hasNext(NativeFrame callFrame) {
        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        Iterator iterator = (Iterator) iteratorInstance.getNativePayload();
        callFrame.finishBoolean(iterator.hasNext());
    }

    public static void Iterator_next(NativeFrame callFrame) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope().getParentScope();

        NativeInstance iteratorInstance = (NativeInstance) callFrame.getParentScope();
        Iterator iterator = (Iterator) iteratorInstance.getNativePayload();
        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) instance.getAgoClass().getConcreteTypeInfo();
        TypeInfo typeInfo = genericArgumentsInfo.getArguments()[0];
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
                callFrame.finishObject(((Iterator<Instance>) iterator).next());
                break;
            case CLASS_REF_VALUE:
                callFrame.finishClassRef(callFrame.getAgoEngine().getClass(((Iterator<Integer>) iterator).next()));
                break;
            default:
                throw new IllegalArgumentException("unknown type: %s".formatted(typeInfo.getTypeCode()));
        }

    }
}
