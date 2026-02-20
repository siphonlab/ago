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
package org.siphonlab.ago.compiler;

import org.agrona.collections.Object2IntCounterMap;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.*;

public class SlotsAllocator {

    protected final ClassDef classDef;

    private Object2IntCounterMap<SlotDef> lockedRegisters = new Object2IntCounterMap<>(0);

    private Set<SlotDef> allRegisters = new HashSet<>();

    public SlotsAllocator(ClassDef classDef){
        this.classDef = classDef;
    }

    protected List<SlotDef> slots = new ArrayList<>(){
        @Override
        public boolean add(SlotDef slotDef) {
            return super.add(slotDef);
        }
    };

    public SlotDef allocateSlot(Variable variable) throws CompilationError {
        assert (!(variable.getType() instanceof ParameterizedClassDef.PlaceHolder));

        SlotDef slot = new SlotDef();
        slot.setName(variable.getName());
        ClassDef type = variable.getType();
        slot.setClassDef(type);
        slot.setIndex(this.slots.size());
        slot.setVariable(variable);
        classDef.idOfConstString(variable.name);
        if(type.getTypeCode() == TypeCode.OBJECT){
            classDef.idOfConstString(type.getFullname());
        }
        this.slots.add(slot);
        return slot;
    }

    // allocate a temp slot for register usage
    public SlotDef allocateRegisterSlot(ClassDef classDef) {
        SlotDef slot = new SlotDef();
        slot.setClassDef(classDef);
        int id = this.slots.size();
        slot.setIndex(id);
        slot.setName("@v" + id);
        this.classDef.idOfConstString(slot.getName());
        if(classDef.getTypeCode() == TypeCode.OBJECT){
            this.classDef.idOfConstString(classDef.getFullname());
        }
        this.slots.add(slot);
        return slot;
    }

    public List<SlotDef> getSlots() {
        return slots;
    }

    public void inheritsSlots(SlotsAllocator allocator){
        List<SlotDef> slots = allocator.getSlots();
        if(slots.isEmpty()) return;

        for (SlotDef slot : slots) {
            classDef.idOfConstString(slot.getName());
            if(slot.getTypeCode() == TypeCode.OBJECT) {
                classDef.idOfClass(slot.getClassDef());
            }
        }
        assert this.slots.isEmpty();
        this.slots.addAll(slots);
        for (SlotDef slot : this.slots) {
            if(slot.getIndex() != this.slots.indexOf(slot)){
                throw new RuntimeException("slot index error");
            }
        }
    }

    public SlotDef allocateSlot(String name, TypeCode typeCode, ClassDef slotClass) {
        SlotDef slot = new SlotDef();
        slot.setClassDef(slotClass);
        int id = this.slots.size();
        slot.setIndex(id);
        slot.setName(name);
        this.classDef.idOfConstString(slot.getName());
        if(typeCode == TypeCode.OBJECT){
            this.classDef.idOfConstString(slotClass.getFullname());
        }
        this.slots.add(slot);
        return slot;
    }


    public SlotDef acquireRegister(ClassDef classDef){
        assert (classDef != null);
        TypeCode typeCode = classDef.getTypeCode();
        for (SlotDef slotDef : allRegisters) {
            if(lockedRegisters.containsKey(slotDef)) continue;
            if(slotDef.getTypeCode() == typeCode){
                if(typeCode == TypeCode.OBJECT){
                    // must be same class type, for different class means different storage way in RDB and other persistent storage
                    if(slotDef.getClassDef() == classDef)
                        return slotDef;
                } else {
                    return slotDef;
                }
            }
        }
        var r = allocateRegisterSlot(classDef);
        allRegisters.add(r);
        return r;
    }

    public void lockRegister(SlotDef slot){
        if(slot == null) return;
        if(slot.isRegister()) {
            lockedRegisters.incrementAndGet(slot);
        }
    }

    public void releaseRegister(SlotDef slot){
        if(slot == null) return;
        if(slot.isRegister()) {
            lockedRegisters.decrementAndGet(slot);
        }
    }

    public SlotDef getSlot(int slotIndex) {
        return slots.get(slotIndex);
    }
}
