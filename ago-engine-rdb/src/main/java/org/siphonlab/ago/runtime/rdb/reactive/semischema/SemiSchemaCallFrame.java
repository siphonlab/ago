package org.siphonlab.ago.runtime.rdb.reactive.semischema;

import org.siphonlab.ago.*;
import org.siphonlab.ago.opcode.Move;
import org.siphonlab.ago.opcode.arithmetic.Add;
import org.siphonlab.ago.runtime.rdb.reactive.CallFrameBoundSlots;
import org.siphonlab.ago.runtime.rdb.semischema.lazy.RunningStateStoreViaAdapter;
import org.siphonlab.ago.runtime.stateful.StatefulAgoFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.opcode.OpCode.DTYPE_MASK;
import static org.siphonlab.ago.opcode.OpCode.DTYPE_MASK_NEG;

public class SemiSchemaCallFrame extends StatefulAgoFrame {

    private final static Logger LOGGER = LoggerFactory.getLogger(SemiSchemaCallFrame.class);
    private final SemiSchemaPGAdapter adapter;

    public SemiSchemaCallFrame(Slots slots, AgoFunction agoFunction, SemiSchemaEngine engine) {
        super(slots, agoFunction, engine, new RunningStateStoreViaAdapter(engine.getRdbAdapter()));
        this.slots = wrapSlots(slots,engine, this);
        this.adapter = (SemiSchemaPGAdapter) engine.getRdbAdapter();
    }

    static CallFrameBoundSlots<JsonRefSlots> wrapSlots(Slots slots, SemiSchemaEngine engine, SemiSchemaCallFrame callFrame){
        if(slots instanceof CallFrameBoundSlots<?>) return (CallFrameBoundSlots<JsonRefSlots>) slots;
        JsonRefSlots jsonRefSlots = (JsonRefSlots) slots;
        var jcf = new JsonRefSlotsWithCallFrame(jsonRefSlots.getObjectRef(), (PGJsonSlotsAdapter) ((SemiSchemaPGAdapter)engine.getRdbAdapter()).getSlotsAdapter(), jsonRefSlots.getSlotDefs(), callFrame);
        jcf.setSaved(jsonRefSlots.isSaved());
        return jcf;
    }

    CallFrameBoundSlots<JsonRefSlots> wrapSlots(Slots slots) {
        if (slots instanceof CallFrameBoundSlots<?>) return (CallFrameBoundSlots<JsonRefSlots>) slots;
        JsonRefSlots jsonRefSlots = (JsonRefSlots) slots;
        SemiSchemaEngine engine = (SemiSchemaEngine) this.engine;
        return new JsonRefSlotsWithCallFrame(jsonRefSlots.getObjectRef(), (PGJsonSlotsAdapter) (adapter.getSlotsAdapter()), jsonRefSlots.getSlotDefs(), this);
    }

    protected int evaluateMove(Slots slots, int pc, int instruction) {
        if(instruction == Move.move_copy_ooC){
            JsonRefSlots dest = (JsonRefSlots) slots.getObject(code[pc++]).getSlots();
            JsonRefSlots src = (JsonRefSlots) slots.getObject(code[pc++]).getSlots();
            AgoClass byClass = engine.getClass(code[pc++]);
            List<String> fields = new ArrayList<>();
            for (AgoSlotDef slotDef : byClass.getSlotDefs()) {
                fields.add(dest.getFieldName(slotDef.getIndex()));
            }
            adapter.copyAssign(dest.getObjectRef(), src.getObjectRef(), fields);
        } else {
            JsonRefSlots jsonRefSlots = (JsonRefSlots) slots;
            switch ((instruction & DTYPE_MASK_NEG)){
                case Move.move_vv:
                    adapter.move(jsonRefSlots.getObjectRef(), jsonRefSlots.getFieldName(code[pc++]), jsonRefSlots.getFieldName(code[pc++])); break;
                case Move.move_fld_ovv: {
                    JsonRefSlots obj = (JsonRefSlots) slots.getObject(code[pc++]).getSlots();
                    adapter.move(obj.getObjectRef(), obj.getFieldName(code[pc++]), jsonRefSlots.getObjectRef(), jsonRefSlots.getFieldName(code[pc++]));
                }
                break;
                case Move.move_fld_vov: {
                    var slot = code[pc++];
                    JsonRefSlots obj = (JsonRefSlots) slots.getObject(code[pc++]).getSlots();
                    adapter.move(jsonRefSlots.getObjectRef(), jsonRefSlots.getFieldName(slot), obj.getObjectRef(), obj.getFieldName(code[pc++]));
                }
                break;
            }
        }
        return pc;
    }


    @Override
    protected int evaluateAdd(Slots slots, int pc, int instruction) {
        return evaluateMathOp((JsonRefSlots) slots,pc, instruction,"+");
    }

    @Override
    protected int evaluateSub(Slots slots, int pc, int instruction) {
        return evaluateMathOp((JsonRefSlots) slots, pc, instruction, "-");
    }

    @Override
    protected int evaluateMultiply(Slots slots, int pc, int instruction) {
        return evaluateMathOp((JsonRefSlots) slots, pc, instruction, "*");
    }

    @Override
    protected int evaluateDiv(Slots slots, int pc, int instruction) {
        return evaluateMathOp((JsonRefSlots) slots, pc, instruction, "/");
    }

    @Override
    protected int evaluateMod(Slots slots, int pc, int instruction) {
        return evaluateMathOp((JsonRefSlots) slots, pc, instruction, "%");
    }

    @Override
    protected int evaluateAnd(Slots slots, int pc, int instruction) {
        return evaluateMathOp((JsonRefSlots) slots, pc, instruction, "and");
    }

    @Override
    protected int evaluateOr(Slots slots, int pc, int instruction) {
        return evaluateMathOp((JsonRefSlots) slots, pc, instruction, "or");
    }

    @Override
    protected int evaluateConcat(Slots slots, int pc, int instruction) {
        return evaluateMathOp((JsonRefSlots) slots, pc, instruction, "||");
    }

    protected int evaluateMathOp(JsonRefSlots slots, int pc, int instruction, String op) {
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
