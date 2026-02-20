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
package org.siphonlab.ago.runtime.json;

import java.util.Objects;

/**
 * usages:
 *   1. for restful output, pure view, writeTypeMode=Never,writeId=false, writeObjectAsReference=Never, writeSlot = false
 *   2. for restful output to callback, object interaction level, writeTypeMode=OnDemand,writeId=true, writeObjectAsReference=Inner, writeSlot = true
 *   3. for very low level interaction, writeTypeMode=Always, writeId=true, writeObjectAsReference=Inner, writeSlot=true
 *   4. for interaction based on ObjectRef,  writeObjectAsReference=Always, writeTypeMode=true, other argument not available
 * for Rdb usage, it only stores Slots of the input instance, and ObjectRef for inner Objects,
 *   use RdbJsonInstanceSerializer to make json the slots field
 */
public class AgoJsonConfig {

    public static final AgoJsonConfig NESTED_JSON_VIEW = new AgoJsonConfig(WriteTypeMode.Never, false, ObjectAsReferenceMode.Never, false);
    public static final AgoJsonConfig RPC_JSON_VIEW = new AgoJsonConfig(WriteTypeMode.OnDemand, true, ObjectAsReferenceMode.Inner, false);
    public static final AgoJsonConfig RPC_JSON_RAW = new AgoJsonConfig(WriteTypeMode.Always, true, ObjectAsReferenceMode.Inner, true);
    public static final AgoJsonConfig RPC_OBJECT_REF = new AgoJsonConfig(WriteTypeMode.Always, true, ObjectAsReferenceMode.Always, true);

    public enum WriteTypeMode {
        Never(0),
        /**
         * write type even for the input instance
         */
        Always(1),
        /**
         * write type only for object slots
         */
        Inner(2),
        /**
         * write type only for Object slot that the value not equals SlotDef, that means, descendant from `SlotDef.type`
         */
        OnDemand(3);

        private final int value;

        WriteTypeMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static WriteTypeMode valueOf(int value) {
            switch (value) {
                case 0:
                    return Never;
                case 1:
                    return Always;
                case 2:
                    return Inner;
                case 3:
                    return OnDemand;
                default:
                    throw new IllegalArgumentException("Invalid value: " + value);
            }
        }
    }

    public enum ObjectAsReferenceMode {
        Never,
        /**
         * always write object as ObjectRef, include the input instance
         * for box types, it will output primitive value
         * however if the box type not match given class, it will wrap as `{"@type": ..., "value": primitive value}` if writeType=OnDemand
         */
        Always,
        /**
         * only apply on inner object slot, exclude the input instance itself
         */
        Inner;

        public static ObjectAsReferenceMode valueOf(int value) {
            switch (value) {
                case 0:
                    return Never;
                case 1:
                    return Always;
                case 2:
                    return Inner;
                default:
                    throw new IllegalArgumentException("Invalid value: " + value);
            }
        }

        public int getValue() {
            return ordinal();
        }
    }

    /**
     * output field {"@type":className}
     */
    private WriteTypeMode writeType= WriteTypeMode.OnDemand;
    /**
     * output field {"@id": id}. only for rdb instances, include AgoRunSpace, they have id
     * but if out reference for the object, it will output {"@type": , "@id": ...}, ignore writeId
     */
    private boolean writeId;

    /**
     * serialize `Object slot` as reference, that means, {"@id": id},
     */
    private ObjectAsReferenceMode writeObjectAsReference = ObjectAsReferenceMode.Never;

    /**
     * output all slots, otherwise only public fields, and not include attributes (getter and setter)
     */
    private boolean writeSlots;

    public WriteTypeMode getWriteType() {
        return writeType;
    }

    public void setWriteType(WriteTypeMode writeType) {
        this.writeType = writeType;
    }

    public boolean isWriteId() {
        return writeId;
    }

    public void setWriteId(boolean writeId) {
        this.writeId = writeId;
    }

    public ObjectAsReferenceMode getWriteObjectAsReference() {
        return writeObjectAsReference;
    }

    public void setWriteObjectAsReference(ObjectAsReferenceMode writeObjectAsReference) {
        this.writeObjectAsReference = writeObjectAsReference;
    }

    public boolean isWriteSlots() {
        return writeSlots;
    }

    public void setWriteSlots(boolean writeSlots) {
        this.writeSlots = writeSlots;
    }

    public AgoJsonConfig(){

    }
    public AgoJsonConfig(WriteTypeMode writeTypeMode, boolean writeId, ObjectAsReferenceMode objectAsReferenceMode, boolean writeSlots) {
        this.writeType = writeTypeMode;
        this.writeId = writeId;
        this.writeObjectAsReference = objectAsReferenceMode;
        this.writeSlots = writeSlots;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AgoJsonConfig that)) return false;
        return writeType == that.writeType
                && writeId == that.writeId
                && writeObjectAsReference == that.writeObjectAsReference
                && writeSlots == that.writeSlots;
    }

    @Override
    public int hashCode() {
        return Objects.hash(writeType, writeId, writeObjectAsReference, writeSlots);
    }
}
