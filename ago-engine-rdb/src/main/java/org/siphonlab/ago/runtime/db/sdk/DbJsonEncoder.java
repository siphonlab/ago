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
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.ObjectRef;

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
        if (slots instanceof DbSlots<?>) {
            var dbSlots = (DbSlots<?>) slots;
            var usingSet = dbSlots.getUsingInstances();
            if (usingSet == null || usingSet.isEmpty()) {
                frame.finishObject(null);
                return;
            }
            AgoEngine engine = frame.getAgoEngine();
            AgoClass resultClass = frame.getAgoClass().getResultClass();
            var arr = engine.createObjectArray(resultClass, usingSet.size());
            int i = 0;
            for (Instance<?> inst : usingSet) {
                arr.value[i++] = inst;
            }
            frame.finishObject(arr);
        } else {
            frame.finishObject(null);
        }
    }

    // Get slot value at given index.
    public static void getSlotValue(NativeFrame frame, Instance<?> obj, int index) {
        AgoEngine engine = frame.getAgoEngine();
        Slots slots = obj.getSlots();
        AgoClass agoClass = obj.getAgoClass();
        AgoSlotDef[] slotDefs = agoClass.getSlotDefs();

        if (slotDefs == null || index >= slotDefs.length) {
            frame.raiseException(frame.self(), "IndexOutOfBoundsException",
                    "slot index %d out of bounds for object with %d slots".formatted(index,
                            slotDefs == null ? 0 : slotDefs.length));
            return;
        }

        AgoSlotDef slotDef = slotDefs[index];
        int typeCode = slotDef.getTypeCode().value;
        frame.finishUnion(Union.toUnionValue(engine, slots, index, typeCode));
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
