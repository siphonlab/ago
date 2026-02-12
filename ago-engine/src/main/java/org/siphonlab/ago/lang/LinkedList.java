package org.siphonlab.ago.lang;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;

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
        callFrame.finishVoid();   // 先留空
    }

    public static void add(NativeFrame callFrame, Instance<?> item) {
        NativeInstance instance = (NativeInstance) callFrame.getParentScope();
        java.util.LinkedList<?> ls = (java.util.LinkedList<?>) instance.getNativePayload();
//         ls.add(item);
        callFrame.finishVoid();   // 先留空
    }


    public static void contains(NativeFrame callFrame, Instance<?> item) {
        // 示例（伪代码）：
        // Object item = callFrame.getArg(0);
        // boolean found = ((java.util.LinkedList<Object>) instance.getNativePayload()).contains(item);
        // callFrame.finishBool(found);
        callFrame.finishBoolean(false);   // 先留空
    }

    public static void contains(NativeFrame callFrame, int item) {
        // 示例（伪代码）：
        // Object item = callFrame.getArg(0);
        // boolean found = ((java.util.LinkedList<Object>) instance.getNativePayload()).contains(item);
        // callFrame.finishBool(found);
        callFrame.finishBoolean(false);   // 先留空
    }

    /**
     * 把链表内容拷贝到指定数组的给定位置。<br>
     * TODO: 取参数并执行复制。
     */
    public static void copyTo(NativeFrame callFrame, Instance<?> array, int arrayIndex) {
        // 示例（伪代码）：
        // ((java.util.LinkedList<Object>) instance.getNativePayload()).toArray(array, idx);
        callFrame.finishVoid();   // 先留空
    }

    /**
     * 从链表中移除指定元素。<br>
     * TODO: 取参数并返回 {@link java.util.List#remove(Object)} 的结果。
     */
    public static void remove(NativeFrame callFrame, Instance<?> item) {
        // 示例（伪代码）：
        // Object item = callFrame.getArg(0);
        // boolean removed = ((java.util.LinkedList<Object>) instance.getNativePayload()).remove(item);
        // callFrame.finishBool(removed);
        callFrame.finishBoolean(false);   // 先留空
    }

    public static void remove(NativeFrame callFrame, int item) {
        // 示例（伪代码）：
        // Object item = callFrame.getArg(0);
        // boolean removed = ((java.util.LinkedList<Object>) instance.getNativePayload()).remove(item);
        // callFrame.finishBool(removed);
        callFrame.finishBoolean(false);   // 先留空
    }

    /**
     * 根据索引获取元素。<br>
     * TODO: 取参数并返回对应元素。
     */
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

    /**
     * 根据索引设置/替换元素。<br>
     * TODO: 取参数并执行 {@link java.util.List#set(int, Object)}。
     */
    public static void setAtIndex(NativeFrame callFrame, int index, Instance<?> item) {
        // 示例（伪代码）：
        // int idx   = (int) callFrame.getArg(0);
        // Object val = callFrame.getArg(1);
        // ((java.util.LinkedList<Object>) instance.getNativePayload()).set(idx, val);
        callFrame.finishVoid();   // 先留空
    }

    public static void setAtIndex(NativeFrame callFrame, int index, int item) {
        // 示例（伪代码）：
        // int idx   = (int) callFrame.getArg(0);
        // Object val = callFrame.getArg(1);
        // ((java.util.LinkedList<Object>) instance.getNativePayload()).set(idx, val);
        callFrame.finishVoid();   // 先留空
    }

    public static void Iterator_create(NativeFrame callFrame) {
        System.out.println(1);
    }

    public static void Iterator_hasNext(NativeFrame callFrame) {
        System.out.println(1);
    }

    public static void Iterator_next(NativeFrame callFrame) {
        System.out.println(1);
    }
}
