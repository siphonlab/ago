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
package org.siphonlab.ago.runtime.rdb.reactive.json;

import org.siphonlab.ago.*;
import org.siphonlab.ago.opcode.Move;
import org.siphonlab.ago.opcode.arithmetic.Add;
import org.siphonlab.ago.runtime.rdb.reactive.CallFrameBoundSlots;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.opcode.OpCode.DTYPE_MASK;
import static org.siphonlab.ago.opcode.OpCode.DTYPE_MASK_NEG;

/**
 * this call frame dispatch some commands to adapter, and adapter translate them to sql, i.e. update xx set slot1 = slot2 + slot3
 */
public class ReactiveJsonCallFrame extends AgoFrame {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReactiveJsonCallFrame.class);
    private final ReactiveJsonPGAdapter adapter;

    public ReactiveJsonCallFrame(Slots slots, AgoFunction agoFunction, ReactiveJsonAgoEngine engine) {
        super(slots, agoFunction, engine);
        this.slots = wrapSlots(slots,engine, this);
        this.adapter = (ReactiveJsonPGAdapter) engine.getRdbAdapter();
    }

    static CallFrameBoundSlots<ReactiveJsonRefSlots> wrapSlots(Slots slots, ReactiveJsonAgoEngine engine, ReactiveJsonCallFrame callFrame){
        if(slots instanceof CallFrameBoundSlots<?>) return (CallFrameBoundSlots<ReactiveJsonRefSlots>) slots;
        ReactiveJsonRefSlots reactiveJsonRefSlots = (ReactiveJsonRefSlots) slots;
        var jcf = new ReactiveJsonRefSlotsWithCallFrame(reactiveJsonRefSlots.getObjectRef(), (ReactivePGJsonSlotsAdapter) ((ReactiveJsonPGAdapter)engine.getRdbAdapter()).getSlotsAdapter(), reactiveJsonRefSlots.getSlotDefs(), callFrame);
        jcf.setSaved(reactiveJsonRefSlots.isSaved());
        return jcf;
    }

    CallFrameBoundSlots<ReactiveJsonRefSlots> wrapSlots(Slots slots) {
        if (slots instanceof CallFrameBoundSlots<?>) return (CallFrameBoundSlots<ReactiveJsonRefSlots>) slots;
        ReactiveJsonRefSlots reactiveJsonRefSlots = (ReactiveJsonRefSlots) slots;
        ReactiveJsonAgoEngine engine = (ReactiveJsonAgoEngine) this.engine;
        return new ReactiveJsonRefSlotsWithCallFrame(reactiveJsonRefSlots.getObjectRef(), (ReactivePGJsonSlotsAdapter) (adapter.getSlotsAdapter()), reactiveJsonRefSlots.getSlotDefs(), this);
    }

    protected int evaluateMove(Slots slots, int pc, int instruction) {
        if(instruction == Move.move_copy_ooC){
            ReactiveJsonRefSlots dest = (ReactiveJsonRefSlots) slots.getObject(code[pc++]).getSlots();
            ReactiveJsonRefSlots src = (ReactiveJsonRefSlots) slots.getObject(code[pc++]).getSlots();
            AgoClass byClass = engine.getClass(code[pc++]);
            List<String> fields = new ArrayList<>();
            for (AgoSlotDef slotDef : byClass.getSlotDefs()) {
                fields.add(dest.getFieldName(slotDef.getIndex()));
            }
            adapter.copyAssign(dest.getObjectRef(), src.getObjectRef(), fields);
        } else {
            ReactiveJsonRefSlots reactiveJsonRefSlots = (ReactiveJsonRefSlots) slots;
            switch ((instruction & DTYPE_MASK_NEG)){
                case Move.move_vv:
                    adapter.move(reactiveJsonRefSlots.getObjectRef(), reactiveJsonRefSlots.getFieldName(code[pc++]), reactiveJsonRefSlots.getFieldName(code[pc++])); break;
                case Move.move_fld_ovv: {
                    ReactiveJsonRefSlots obj = (ReactiveJsonRefSlots) slots.getObject(code[pc++]).getSlots();
                    adapter.move(obj.getObjectRef(), obj.getFieldName(code[pc++]), reactiveJsonRefSlots.getObjectRef(), reactiveJsonRefSlots.getFieldName(code[pc++]));
                }
                break;
                case Move.move_fld_vov: {
                    var slot = code[pc++];
                    ReactiveJsonRefSlots obj = (ReactiveJsonRefSlots) slots.getObject(code[pc++]).getSlots();
                    adapter.move(reactiveJsonRefSlots.getObjectRef(), reactiveJsonRefSlots.getFieldName(slot), obj.getObjectRef(), obj.getFieldName(code[pc++]));
                }
                break;
            }
        }
        return pc;
    }


    @Override
    protected int evaluateAdd(Slots slots, int pc, int instruction) {
        return evaluateMathOp((ReactiveJsonRefSlots) slots,pc, instruction,"+");
    }

    @Override
    protected int evaluateSub(Slots slots, int pc, int instruction) {
        return evaluateMathOp((ReactiveJsonRefSlots) slots, pc, instruction, "-");
    }

    @Override
    protected int evaluateMultiply(Slots slots, int pc, int instruction) {
        return evaluateMathOp((ReactiveJsonRefSlots) slots, pc, instruction, "*");
    }

    @Override
    protected int evaluateDiv(Slots slots, int pc, int instruction) {
        return evaluateMathOp((ReactiveJsonRefSlots) slots, pc, instruction, "/");
    }

    @Override
    protected int evaluateMod(Slots slots, int pc, int instruction) {
        return evaluateMathOp((ReactiveJsonRefSlots) slots, pc, instruction, "%");
    }

    @Override
    protected int evaluateAnd(Slots slots, int pc, int instruction) {
        return evaluateMathOp((ReactiveJsonRefSlots) slots, pc, instruction, "and");
    }

    @Override
    protected int evaluateOr(Slots slots, int pc, int instruction) {
        return evaluateMathOp((ReactiveJsonRefSlots) slots, pc, instruction, "or");
    }

    @Override
    protected int evaluateConcat(Slots slots, int pc, int instruction) {
        return evaluateMathOp((ReactiveJsonRefSlots) slots, pc, instruction, "||");
    }

    protected int evaluateMathOp(ReactiveJsonRefSlots slots, int pc, int instruction, String op) {
        int type = (instruction & DTYPE_MASK) >> 16;
        var typeCode = TypeCode.of(type);
        var rdbType = adapter.mapType(TypeCode.INT, null);

        switch (instruction & 0x0000ffff){
            case Add.add_vc & 0x00ffffff:
                var field = code[pc++];
                var c = switch (typeCode.getValue()){
                    case INT_VALUE -> code[pc++];
                    case FLOAT_VALUE -> Float.intBitsToFloat(code[pc++]);
                    case BYTE_VALUE -> (byte)code[pc++];
                    case SHORT_VALUE -> (short) code[pc++];
                    case LONG_VALUE -> toLong(code[pc++], code[pc++]);
                    case DOUBLE_VALUE -> toDouble(code[pc++], code[pc++]);
                    default -> throw new IllegalArgumentException("illegal type " + typeCode);
                };
                adapter.selfOp(op, rdbType, slots.getObjectRef(), slots.getFieldName(field), c);
                break;
            case Add.add_vvc & 0x00ffffff:
                var dest = slots.getFieldName(code[pc++]);
                var src = slots.getFieldName(code[pc++]);
                var c2 = switch (typeCode.getValue()) {
                    case INT_VALUE -> code[pc++];
                    case FLOAT_VALUE -> Float.intBitsToFloat(code[pc++]);
                    case BYTE_VALUE -> (byte) code[pc++];
                    case SHORT_VALUE -> (short) code[pc++];
                    case LONG_VALUE -> toLong(code[pc++], code[pc++]);
                    case DOUBLE_VALUE -> toDouble(code[pc++], code[pc++]);
                    default -> throw new IllegalArgumentException("illegal type " + typeCode);
                };
                adapter.binaryOp(op, rdbType, slots.getObjectRef(), dest, src, c2);
                break;
            case Add.add_vv & 0x00ffffff:
                adapter.selfOp(op, rdbType, slots.getObjectRef(), slots.getFieldName(code[pc++]), slots.getFieldName(code[pc++]));
                break;
            case Add.add_vvv & 0x00ffffff:
                adapter.binaryOp(op, rdbType, slots.getObjectRef(), slots.getFieldName(code[pc++]), slots.getFieldName(code[pc++]), slots.getFieldName(code[pc++]));
                break;
        }
        return pc;
    }

    @Override
    public String toString() {
        return "SemiSchemaCallFrame" + "@" + this.agoClass + ":" + this.slots;
    }
}
