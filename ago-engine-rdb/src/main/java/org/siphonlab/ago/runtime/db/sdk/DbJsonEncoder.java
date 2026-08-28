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

import org.apache.commons.collections4.SetUtils;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.ObjectRef;

import java.math.BigDecimal;

import static org.siphonlab.ago.TypeCode.*;

public class DbJsonEncoder {

    // Check if an object has DbSlots internally.
    public static void isDbObject(NativeFrame frame, Instance<?> obj) {
        Slots slots = obj.getSlots();
        frame.finishBoolean(slots instanceof DbSlots);
    }

    // Get row state name string from DbSlots.
    public static void getRowStateString(NativeFrame frame, Instance<?> obj) {
        Slots slots = obj.getSlots();
        if (slots instanceof DbSlots<?>) {
            var dbSlots = (DbSlots<?>) slots;
            frame.finishString(dbSlots.getRowState().name());
        } else {
            frame.finishString("");
        }
    }

    // Get using instances array from DbSlots.
    public static void getUsingInstances(NativeFrame frame, Instance<?> obj) {
        Slots slots = obj.getSlots();
        var dbSlots = (DbSlots<?>) slots;
        var usingSet = dbSlots.getUsingInstances();
        if (usingSet == null) {
            usingSet = SetUtils.emptySet();
        }
        AgoEngine engine = frame.getAgoEngine();
        AgoClass resultClass = frame.getAgoClass().getResultClass();
        var arr = engine.createObjectArray(resultClass, usingSet.size(), frame.getRunSpace());
        int i = 0;
        for (Instance<?> inst : usingSet) {
            arr.value[i++] = inst;
        }
        frame.finishObject(arr);
    }

    // Get slot value at given index. Returns typed value based on the caller's expected result type.
    public static void getSlotValue(NativeFrame frame, Instance<?> obj, int index) {
        Slots slots = obj.getSlots();
        int resultTypeCode = frame.getAgoClass().getResultTypeCode().value;
        switch (resultTypeCode) {
            case INT_VALUE:       frame.finishInt(slots.getInt(index)); break;
            case LONG_VALUE:      frame.finishLong(slots.getLong(index)); break;
            case FLOAT_VALUE:     frame.finishFloat(slots.getFloat(index)); break;
            case DOUBLE_VALUE:    frame.finishDouble(slots.getDouble(index)); break;
            case BOOLEAN_VALUE:   frame.finishBoolean(slots.getBoolean(index)); break;
            case BYTE_VALUE:      frame.finishByte(slots.getByte(index)); break;
            case SHORT_VALUE:     frame.finishShort(slots.getShort(index)); break;
            case CHAR_VALUE:      frame.finishChar(slots.getChar(index)); break;
            case DECIMAL_VALUE:   frame.finishDecimal(slots.getDecimal(index)); break;
            case STRING_VALUE:    frame.finishString(slots.getString(index)); break;
            case OBJECT_VALUE:    frame.finishObject(slots.getObject(index)); break;
            case CLASS_REF_VALUE: frame.finishClassRef(frame.getAgoEngine().getClass(slots.getClassRef(index))); break;
            case UNION_VALUE:     frame.finishUnion(slots.getUnion(index)); break;
            default:              frame.finishVoid(); break;
        }
    }

    // Check if a union (nullable) slot at the given index is null.
    public static void isSlotNull(NativeFrame frame, Instance<?> obj, int index) {
        Slots slots = obj.getSlots();
        Object value = slots.getUnion(index);
        frame.finishBoolean(value == null);
    }

    // Get union slot value at given index for nullable/union slots.
    // Returns typed value based on the caller's expected result type.
    public static void getSlotUnionValue(NativeFrame frame, Instance<?> obj, int index) {
        Slots slots = obj.getSlots();
        Object unionValue = slots.getUnion(index);
        int resultTypeCode = frame.getAgoClass().getResultTypeCode().value;

        if (unionValue == null) {
            frame.raiseException(frame.self(), "lang.NullPointerException",
                    "slot index %d is null, cannot extract typed value".formatted(index));
            return;
        }

        switch (resultTypeCode) {
            case INT_VALUE:       frame.finishInt(((Number) unionValue).intValue()); break;
            case LONG_VALUE:      frame.finishLong(((Number) unionValue).longValue()); break;
            case FLOAT_VALUE:     frame.finishFloat(((Number) unionValue).floatValue()); break;
            case DOUBLE_VALUE:    frame.finishDouble(((Number) unionValue).doubleValue()); break;
            case BOOLEAN_VALUE:   frame.finishBoolean((Boolean) unionValue); break;
            case BYTE_VALUE:      frame.finishByte(((Number) unionValue).byteValue()); break;
            case SHORT_VALUE:     frame.finishShort(((Number) unionValue).shortValue()); break;
            case CHAR_VALUE:      frame.finishChar((Character) unionValue); break;
            case DECIMAL_VALUE:   frame.finishDecimal((BigDecimal) unionValue); break;
            case STRING_VALUE:    frame.finishString((String) unionValue); break;
            case OBJECT_VALUE:    frame.finishObject((Instance<?>) unionValue); break;
            default:              frame.finishUnion(unionValue); break;
        }
    }

    // Get object reference string for an instance: "ClassName:id".
    public static void getObjectRefString(NativeFrame frame, Instance<?> obj) {
        Slots slots = obj.getSlots();
        if (slots instanceof DbSlots<?>) {
            var dbSlots = (DbSlots<?>) slots;
            ObjectRef<?> ref = dbSlots.getObjectRef();
            if (ref != null) {
                frame.finishString(ref.className() + ":" + ref.id());
            } else {
                frame.finishString(obj.getAgoClass().getFullname() + ":null");
            }
        } else {
            // Non-DB object: use class name and hashCode as identifier.
            String className = obj.getAgoClass().getFullname();
            frame.finishString(className + ":" + System.identityHashCode(obj));
        }
    }

    // Check if a ClassRef represents a Boxer type.
    public static void isBoxerType(NativeFrame frame, Instance<?> classRefInst) {
        AgoEngine engine = frame.getAgoEngine();
        AgoClass targetClass = Boxer.getClassFromClassRef(classRefInst);
        boolean isBoxer = engine.getBoxTypes().isBoxType(targetClass);
        frame.finishBoolean(isBoxer);
    }

    // Get the base class for a nullable type, or null if not nullable.
    public static void getNullableBaseClass(NativeFrame frame, Instance<?> classRefInst) {
        AgoEngine engine = frame.getAgoEngine();
        AgoClass targetClass = Boxer.getClassFromClassRef(classRefInst);

        if (targetClass.isNullable()) {
            AgoClass baseClass = targetClass.getNullableBaseClass();
            if (baseClass != null) {
                Instance<?> boxed = engine.getBoxer().boxClassRef(frame,
                        engine.getLangClasses().getClassRefClass(), baseClass);
                frame.finishUnion(boxed);
            } else {
                frame.finishUnion(null);
            }
        } else {
            frame.finishUnion(null);
        }
    }


}
