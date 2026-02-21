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
package org.siphonlab.ago;

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.native_.NativeInstance;import org.siphonlab.ago.opcode.*;
import org.siphonlab.ago.opcode.compare.*;
import org.siphonlab.ago.opcode.logic.*;
import org.siphonlab.ago.opcode.arithmetic.*;
import org.siphonlab.ago.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;

/**
 * Call frame
 */
public class AgoFrame extends CallFrame<AgoFunction>{

    private final static Logger LOGGER = LoggerFactory.getLogger(AgoFrame.class);

    protected final int[] code;        // copy from AgoFunction, a bit speedup

    protected int pc;             // current position at code

    protected final AgoEngine engine;

    public AgoFrame(Slots slots, AgoFunction agoFunction, AgoEngine engine) {
        super(slots, agoFunction );
        this.setAgoClass(agoFunction);
        this.engine = engine;
        code = agoFunction.getCode();
    }

    protected CallFrame<?> getCallFrameAt(int slot){
        return (CallFrame<?>) this.getSlots().getObject(slot);
    }

    public void run(CallFrame<?> self){
        if(this.debugger != null){
            this.debugger.enterFrame(this);
        }
        if(LOGGER.isDebugEnabled()) LOGGER.debug(pc == 0 ? "run %s".formatted(this.agoClass) : "resume %s".formatted(this.agoClass));
        if(suspended) {
            this.getRunSpace().waitResult();
            if(this.debugger != null) this.debugger.leaveFrame(this);
            return;
        }

        final Slots slots = self.getSlots();
        final int[] code = this.code;
        while(pc < code.length){
            final int instruction = code[pc++];
            if(LOGGER.isDebugEnabled()) LOGGER.debug("%s(%s): %s".formatted(this, pc, OpCode.getName(instruction)));

            switch (instruction >> 24){
                case Const.OP: pc = evaluateConst(slots, pc, instruction); break;
                case Move.OP: pc = evaluateMove(slots, pc, instruction); break;
                case Add.OP:  pc = evaluateAdd(slots, pc, instruction); break;
                case New.OP:  pc = evaluateNew(slots, pc, instruction); break;
                case Invoke.OP: {
                    if(evaluateInvoke(self, instruction)) {
                        if(this.debugger != null) this.debugger.leaveFrame(this);
                        return;
                    } else break;
                }
                case Accept.OP: pc = evaluateAccept(slots, pc, instruction); break;
                case TryCatch.OP: {
                    if(evaluateTryCatch(slots, instruction)) break; else {
                        if(this.debugger != null) this.debugger.leaveFrame(this);
                        return;
                    }
                }
                case Pause.OP: {
                    evaluatePause();
                    if(this.debugger != null) this.debugger.leaveFrame(this);
                    return;
                }
                case Jump.OP: pc = evaluateJump(slots, pc, instruction); break;
                case Concat.OP: pc = evaluateConcat(slots, pc, instruction); break;
                case Return.OP: pc = evaluateReturn(self, slots, pc, instruction); break;
                case Cast.OP: pc = evaluateCast(slots,pc, instruction); break;
                case Load.OP: pc = evaluateLoad(slots, pc, instruction); break;
                case Array.OP: pc = evaluateArray(slots, pc, instruction); break;
                case Box.OP: pc = evaluateBox(slots, pc, instruction); break;
                case Equals.OP: pc = evaluateEquals(slots, pc, instruction); break;
                case NotEquals.OP: pc = evaluateNotEquals(slots, pc, instruction); break;
                case LittleThan.OP: pc = evaluateLittleThan(slots, pc, instruction); break;
                case GreaterThan.OP: pc = evaluateGreaterThan(slots, pc, instruction); break;
                case GreaterEquals.OP: pc = evaluateGreaterEquals(slots, pc, instruction); break;
                case LittleEquals.OP: pc = evaluateLittleEquals(slots, pc, instruction); break;
                case Subtract.OP: pc = evaluateSub(slots, pc, instruction); break;
                case Multiply.OP: pc = evaluateMultiply(slots, pc, instruction); break;
                case Div.OP: pc = evaluateDiv(slots, pc, instruction); break;
                case Mod.OP: pc = evaluateMod(slots, pc, instruction); break;
                case Neg.OP: pc = evaluateNeg(slots, pc, instruction); break;
                case IncDec.OP: pc = evaluateIncDec(slots, pc, instruction); break;
                case And.OP: pc = evaluateAnd(slots, pc, instruction); break;
                case Or.OP: pc = evaluateOr(slots, pc, instruction); break;
                case Not.OP: pc = evaluateNot(slots, pc, instruction); break;
                case BitAnd.OP: pc = evaluateBitAnd(slots, pc, instruction); break;
                case BitOr.OP: pc = evaluateBitOr(slots, pc, instruction); break;
                case BitXor.OP: pc = evaluateBitXor(slots, pc, instruction); break;
                case BitNot.OP: pc = evaluateBitNot(slots, pc, instruction); break;
                case BitShiftLeft.OP: pc = evaluateBitLShift(slots, pc, instruction); break;
                case BitShiftRight.OP: pc = evaluateBitRShift(slots, pc, instruction); break;
                case BitUnsignedRight.OP: pc = evaluateBitURShift(slots, pc, instruction); break;
                case InstanceOf.OP: pc = evaluateInstanceOf(slots, pc, instruction); break;
                default:
                    throw new UnsupportedOperationException("%s not implemented yet, at '%s'".formatted(OpCode.getName(instruction), this));
            }
            nextPC();
        }

        if(this.debugger != null) this.debugger.leaveFrame(this);
    }

    protected void evaluatePause() {
        setSuspended(true);
        this.getRunSpace().waitResult();
    }

    protected void nextPC() {
        if(this.debugger != null){
            this.debugger.updatePC(this,this.pc);
        }
    }

    /**
     *
     * @param self
     * @param instruction
     * @return true -> this frame should exit to wait the invoked CallFrame callback, include invoke/await; false -> continue evaluation loop, include spawn/fork
     */
    protected boolean evaluateInvoke(CallFrame<?> self, int instruction){
        CallFrame<?> frame = getCallFrameAt(code[pc++]);
        frame.setCaller(self);
        switch (instruction){
            case Invoke.invoke_v:
                frame.setRunSpace(this.getRunSpace());
                runSpace.setCurrCallFrame(frame);
                return true;
            case Invoke.spawn_v:
                runSpace.spawn(frame);
                return false;
            case Invoke.fork_v:
                runSpace.fork(frame);
                return false;
            case Invoke.await_v:
                runSpace.await(frame);
                return true;
            case Invoke.spawn_vv:
                self.getSlots().setObject(code[pc++], frame);
                runSpace.spawn(frame);
                return false;
            case Invoke.fork_vv:
                self.getSlots().setObject(code[pc++], frame);
                runSpace.fork(frame);
                return false;

            case Invoke.spawnc_vo:
                runSpace.spawn(frame, extractForkContext(self.getSlots().getObject(code[pc++])));
                return false;
            case Invoke.forkc_vo:
                runSpace.fork(frame, extractForkContext(self.getSlots().getObject(code[pc++])));
                return false;
            case Invoke.awaitc_vo:
                runSpace.await(frame, extractForkContext(self.getSlots().getObject(code[pc++])));
                return true;
            case Invoke.spawnc_vvo:
                self.getSlots().setObject(code[pc++], frame);
                runSpace.spawn(frame,extractForkContext(self.getSlots().getObject(code[pc++])));
                return false;
            case Invoke.forkc_vvo:
                self.getSlots().setObject(code[pc++], frame);
                runSpace.fork(frame,extractForkContext(self.getSlots().getObject(code[pc++])));
                return false;
        }
        throw new UnsupportedOperationException("unknow instruction " + OpCode.getName(instruction));
    }

    private ForkContext extractForkContext(Instance<?> forkContext) {
        return (ForkContext)((NativeInstance) forkContext).getNativePayload();
    }

    /**
     *
     * @param slots
     * @param instruction
     * @return false -> the error not handled, stop evaluation loop; true -> the error handled, continue evaluation loop
    */
    protected boolean evaluateTryCatch(Slots slots, int instruction){
        switch (instruction){
            case TryCatch.except_store_v:   slots.setObject(code[pc++], this.getRunSpace().getException()); this.getRunSpace().cleanException(); break;
            case TryCatch.except_clean:     this.getRunSpace().cleanException(); break;
            case TryCatch.except_throw_v: {
                this.handleException(slots.getObject(code[pc]));
                if(this.pc == -1) {
                    this.pc = this.code.length;
                    return false;   // not handled, exit
                }
            }
            break;
            case TryCatch.set_final_exit_vc:    slots.setInt(code[pc++], code[pc++]); break;
            case TryCatch.except_throw_if_v: {
                var exception = slots.getObject(code[pc++]);
                if(exception != null){
                    this.handleException(exception);
                    if(this.pc == -1) return false;   // not handled, exit
                }
            }
            break;
        }
        return true;
    }

    protected int evaluateInstanceOf(Slots slots, int pc, int instruction) {
        switch (instruction){
            case InstanceOf.instanceof_i_vv :  slots.setBoolean(code[pc++], agoClass.slotsCreator.getSlotType(code[pc++]) == int.class); pc++; break;
            case InstanceOf.instanceof_S_vv :  slots.setBoolean(code[pc++], agoClass.slotsCreator.getSlotType(code[pc++]) == String.class); pc++; break;
            case InstanceOf.instanceof_B_vv :  slots.setBoolean(code[pc++], agoClass.slotsCreator.getSlotType(code[pc++]) == Boolean.class); pc++; break;
            case InstanceOf.instanceof_c_vv :  slots.setBoolean(code[pc++], agoClass.slotsCreator.getSlotType(code[pc++]) == char.class); pc++; break;
            case InstanceOf.instanceof_f_vv :  slots.setBoolean(code[pc++], agoClass.slotsCreator.getSlotType(code[pc++]) == float.class); pc++; break;
            case InstanceOf.instanceof_d_vv :  slots.setBoolean(code[pc++], agoClass.slotsCreator.getSlotType(code[pc++]) == double.class); pc++; break;
            case InstanceOf.instanceof_b_vv :  slots.setBoolean(code[pc++], agoClass.slotsCreator.getSlotType(code[pc++]) == byte.class); pc++; break;
            case InstanceOf.instanceof_s_vv :  slots.setBoolean(code[pc++], agoClass.slotsCreator.getSlotType(code[pc++]) == short.class); pc++; break;
            case InstanceOf.instanceof_l_vv :  slots.setBoolean(code[pc++], agoClass.slotsCreator.getSlotType(code[pc++]) == long.class); pc++; break;
            //case InstanceOf.instanceof_C_vvC :  slots.setBoolean(code[pc++], agoClass.slotsCreator.getSlotType(code[pc++]) == .class); break;
            case InstanceOf.instanceof_o_vvC:  slots.setBoolean(code[pc++],
                        agoClass.slotsCreator.getSlotType(code[pc]) == Instance.class && isInstanceOf(slots.getObject(code[pc++]), engine.getClass(code[pc++])));break;

            case InstanceOf.instanceof_p_vvC:  slots.setBoolean(code[pc++],
                        isInstanceOfPrimitive(agoClass.slotsCreator.getSlotType(code[pc++]), engine.getClass(code[pc++])));break;

        }
        return pc;
    }

    private boolean isInstanceOfPrimitive(Class<?> slotType, AgoClass agoClass) {
        if(agoClass == engine.PRIMITIVE_TYPE){
            return slotType == int.class || slotType == long.class || slotType == short.class || slotType == byte.class
                    || slotType == String.class || slotType == double.class || slotType == float.class
                          || slotType == boolean.class || slotType == char.class;
        } else if(agoClass == engine.PRIMITIVE_NUMBER_TYPE){
            return slotType == int.class || slotType == long.class || slotType == short.class || slotType == byte.class
                                || slotType == double.class || slotType == float.class;
        }
        throw new IllegalArgumentException("illegal type '%s', only lang.Primitive and lang.PrimitiveNumber allowed".formatted(agoClass.getFullname()));
    }

    private boolean isInstanceOf(Instance<?> object, AgoClass aClass) {
        if(object == null) return false;
        //TODO generic variance, copy from ClassDef
        return object.getAgoClass() == aClass || object.getAgoClass().isThatOrDerivedFrom(aClass);
    }

    protected int evaluateBitOr(Slots slots, int pc, int instruction) {
        switch (instruction){
            case BitOr.bor_i_vc:  slots.setInt(code[pc], slots.getInt(code[pc++]) | code[pc++]); break;
            case BitOr.bor_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) | code[pc++]); break;
            case BitOr.bor_i_vv:  slots.setInt(code[pc],  slots.getInt(code[pc++]) | slots.getInt(code[pc++])); break;
            case BitOr.bor_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) | slots.getInt(code[pc++])); break;

            case BitOr.bor_b_vc:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) | (byte)code[pc++])); break;
            case BitOr.bor_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) | (byte)code[pc++])); break;
            case BitOr.bor_b_vv:  slots.setByte(code[pc], (byte)(slots.getByte(code[pc++]) | slots.getByte(code[pc++]))); break;
            case BitOr.bor_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) | slots.getByte(code[pc++]))); break;

            case BitOr.bor_s_vc:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) | (short)code[pc++])); break;
            case BitOr.bor_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) | (short)code[pc++])); break;
            case BitOr.bor_s_vv:  slots.setShort(code[pc], (short)(slots.getShort(code[pc++]) | slots.getShort(code[pc++]))); break;
            case BitOr.bor_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) | slots.getShort(code[pc++]))); break;

            case BitOr.bor_l_vc:  slots.setLong(code[pc], slots.getLong(code[pc++]) | toLong(code[pc++], code[pc++])); break;
            case BitOr.bor_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) | toLong(code[pc++], code[pc++])); break;
            case BitOr.bor_l_vv:  slots.setLong(code[pc], slots.getLong(code[pc++]) | slots.getLong(code[pc++])); break;
            case BitOr.bor_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) | slots.getLong(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateBitAnd(Slots slots, int pc, int instruction) {
        switch (instruction){
            case BitAnd.band_i_vc:  slots.setInt(code[pc], slots.getInt(code[pc++]) & code[pc++]); break;
            case BitAnd.band_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) & code[pc++]); break;
            case BitAnd.band_i_vv:  slots.setInt(code[pc],  slots.getInt(code[pc++]) & slots.getInt(code[pc++])); break;
            case BitAnd.band_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) & slots.getInt(code[pc++])); break;

            case BitAnd.band_b_vc:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) & (byte)code[pc++])); break;
            case BitAnd.band_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) & (byte)code[pc++])); break;
            case BitAnd.band_b_vv:  slots.setByte(code[pc], (byte)(slots.getByte(code[pc++]) & slots.getByte(code[pc++]))); break;
            case BitAnd.band_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) & slots.getByte(code[pc++]))); break;

            case BitAnd.band_s_vc:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) & (short)code[pc++])); break;
            case BitAnd.band_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) & (short)code[pc++])); break;
            case BitAnd.band_s_vv:  slots.setShort(code[pc], (short)(slots.getShort(code[pc++]) & slots.getShort(code[pc++]))); break;
            case BitAnd.band_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) & slots.getShort(code[pc++]))); break;

            case BitAnd.band_l_vc:  slots.setLong(code[pc], slots.getLong(code[pc++]) & toLong(code[pc++], code[pc++])); break;
            case BitAnd.band_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) & toLong(code[pc++], code[pc++])); break;
            case BitAnd.band_l_vv:  slots.setLong(code[pc], slots.getLong(code[pc++]) & slots.getLong(code[pc++])); break;
            case BitAnd.band_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) & slots.getLong(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateBitXor(Slots slots, int pc, int instruction) {
        switch (instruction){
            case BitXor.bxor_i_vc:  slots.setInt(code[pc], slots.getInt(code[pc++]) ^ code[pc++]); break;
            case BitXor.bxor_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) ^ code[pc++]); break;
            case BitXor.bxor_i_vv:  slots.setInt(code[pc],  slots.getInt(code[pc++]) ^ slots.getInt(code[pc++])); break;
            case BitXor.bxor_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) ^ slots.getInt(code[pc++])); break;

            case BitXor.bxor_b_vc:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) ^ (byte)code[pc++])); break;
            case BitXor.bxor_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) ^ (byte)code[pc++])); break;
            case BitXor.bxor_b_vv:  slots.setByte(code[pc], (byte)(slots.getByte(code[pc++]) ^ slots.getByte(code[pc++]))); break;
            case BitXor.bxor_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) ^ slots.getByte(code[pc++]))); break;

            case BitXor.bxor_s_vc:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) ^ (short)code[pc++])); break;
            case BitXor.bxor_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) ^ (short)code[pc++])); break;
            case BitXor.bxor_s_vv:  slots.setShort(code[pc], (short)(slots.getShort(code[pc++]) ^ slots.getShort(code[pc++]))); break;
            case BitXor.bxor_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) ^ slots.getShort(code[pc++]))); break;

            case BitXor.bxor_l_vc:  slots.setLong(code[pc], slots.getLong(code[pc++]) ^ toLong(code[pc++], code[pc++])); break;
            case BitXor.bxor_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) ^ toLong(code[pc++], code[pc++])); break;
            case BitXor.bxor_l_vv:  slots.setLong(code[pc], slots.getLong(code[pc++]) ^ slots.getLong(code[pc++])); break;
            case BitXor.bxor_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) ^ slots.getLong(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateBitNot(Slots slots, int pc, int instruction) {
        switch (instruction){
            case BitNot.bnot_i_vv: slots.setInt(code[pc++], ~slots.getInt(code[pc++]));  break;
            case BitNot.bnot_b_vv: slots.setByte(code[pc++], (byte)~slots.getByte(code[pc++])); break;
            case BitNot.bnot_s_vv: slots.setShort(code[pc++], (short) ~slots.getShort(code[pc++])); break;
            case BitNot.bnot_l_vv: slots.setLong(code[pc++], ~slots.getLong(code[pc++])); break;
        }
        return pc;
    }

    protected int evaluateBitLShift(Slots slots, int pc, int instruction) {
        switch (instruction){
            case BitShiftLeft.lshift_i_vc:  slots.setInt(code[pc], slots.getInt(code[pc++]) << code[pc++]); break;
            case BitShiftLeft.lshift_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) << code[pc++]); break;
            case BitShiftLeft.lshift_i_vv:  slots.setInt(code[pc],  slots.getInt(code[pc++]) <<  slots.getInt(code[pc++])); break;
            case BitShiftLeft.lshift_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) << slots.getInt(code[pc++])); break;
            case BitShiftLeft.lshift_i_vcv: slots.setInt(code[pc++], code[pc++] << slots.getInt(code[pc++])); break;

            case BitShiftLeft.lshift_b_vc:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) << code[pc++])); break;
            case BitShiftLeft.lshift_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) << code[pc++])); break;
            case BitShiftLeft.lshift_b_vv:  slots.setByte(code[pc], (byte)(slots.getByte(code[pc++]) << slots.getInt(code[pc++]))); break;
            case BitShiftLeft.lshift_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) << slots.getInt(code[pc++]))); break;
            case BitShiftLeft.lshift_b_vcv: slots.setByte(code[pc++], (byte) ((byte)code[pc++] << slots.getInt(code[pc++]))); break;

            case BitShiftLeft.lshift_s_vc:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) << code[pc++])); break;
            case BitShiftLeft.lshift_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) << code[pc++])); break;
            case BitShiftLeft.lshift_s_vv:  slots.setShort(code[pc], (short)(slots.getShort(code[pc++]) << slots.getInt(code[pc++]))); break;
            case BitShiftLeft.lshift_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) << slots.getInt(code[pc++]))); break;
            case BitShiftLeft.lshift_s_vcv:  slots.setShort(code[pc], (short)((short)code[pc++] << slots.getInt(code[pc++]))); break;

            case BitShiftLeft.lshift_l_vc:  slots.setLong(code[pc], slots.getLong(code[pc++]) << code[pc++]); break;
            case BitShiftLeft.lshift_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) << code[pc++]); break;
            case BitShiftLeft.lshift_l_vv:  slots.setLong(code[pc], slots.getLong(code[pc++]) << slots.getInt(code[pc++])); break;
            case BitShiftLeft.lshift_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) << slots.getInt(code[pc++])); break;
            case BitShiftLeft.lshift_l_vcv:  slots.setLong(code[pc], toLong(code[pc++], code[pc++]) << slots.getInt(code[pc++])); break;
        }
        return pc;
    }

    protected int evaluateBitRShift(Slots slots, int pc, int instruction) {
        switch (instruction){
            case BitShiftRight.rshift_i_vc:  slots.setInt(code[pc], slots.getInt(code[pc++]) >> code[pc++]); break;
            case BitShiftRight.rshift_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) >> code[pc++]); break;
            case BitShiftRight.rshift_i_vv:  slots.setInt(code[pc],  slots.getInt(code[pc++]) >>  slots.getInt(code[pc++])); break;
            case BitShiftRight.rshift_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) >> slots.getInt(code[pc++])); break;
            case BitShiftRight.rshift_i_vcv: slots.setInt(code[pc++], code[pc++] >> slots.getInt(code[pc++])); break;

            case BitShiftRight.rshift_b_vc:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) >> code[pc++])); break;
            case BitShiftRight.rshift_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) >> code[pc++])); break;
            case BitShiftRight.rshift_b_vv:  slots.setByte(code[pc], (byte)(slots.getByte(code[pc++]) >> slots.getInt(code[pc++]))); break;
            case BitShiftRight.rshift_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) >> slots.getInt(code[pc++]))); break;
            case BitShiftRight.rshift_b_vcv: slots.setByte(code[pc++], (byte) ((byte)code[pc++] >> slots.getInt(code[pc++]))); break;

            case BitShiftRight.rshift_s_vc:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) >> code[pc++])); break;
            case BitShiftRight.rshift_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) >> code[pc++])); break;
            case BitShiftRight.rshift_s_vv:  slots.setShort(code[pc], (short)(slots.getShort(code[pc++]) >> slots.getInt(code[pc++]))); break;
            case BitShiftRight.rshift_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) >> slots.getInt(code[pc++]))); break;
            case BitShiftRight.rshift_s_vcv:  slots.setShort(code[pc], (short)((short)code[pc++] >> slots.getInt(code[pc++]))); break;

            case BitShiftRight.rshift_l_vc:  slots.setLong(code[pc], slots.getLong(code[pc++]) >> code[pc++]); break;
            case BitShiftRight.rshift_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) >> code[pc++]); break;
            case BitShiftRight.rshift_l_vv:  slots.setLong(code[pc], slots.getLong(code[pc++]) >> slots.getInt(code[pc++])); break;
            case BitShiftRight.rshift_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) >> slots.getInt(code[pc++])); break;
            case BitShiftRight.rshift_l_vcv:  slots.setLong(code[pc], toLong(code[pc++], code[pc++]) >> slots.getInt(code[pc++])); break;
        }
        return pc;
    }

    protected int evaluateBitURShift(Slots slots, int pc, int instruction) {
        switch (instruction){
            case BitUnsignedRight.urshift_i_vc:  slots.setInt(code[pc], slots.getInt(code[pc++]) >>> code[pc++]); break;
            case BitUnsignedRight.urshift_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) >>> code[pc++]); break;
            case BitUnsignedRight.urshift_i_vv:  slots.setInt(code[pc],  slots.getInt(code[pc++]) >>>  slots.getInt(code[pc++])); break;
            case BitUnsignedRight.urshift_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) >>> slots.getInt(code[pc++])); break;
            case BitUnsignedRight.urshift_i_vcv: slots.setInt(code[pc++], code[pc++] >>> slots.getInt(code[pc++])); break;

            case BitUnsignedRight.urshift_b_vc:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) >>> code[pc++])); break;
            case BitUnsignedRight.urshift_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) >>> code[pc++])); break;
            case BitUnsignedRight.urshift_b_vv:  slots.setByte(code[pc], (byte)(slots.getByte(code[pc++]) >>> slots.getInt(code[pc++]))); break;
            case BitUnsignedRight.urshift_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) >>> slots.getInt(code[pc++]))); break;
            case BitUnsignedRight.urshift_b_vcv: slots.setByte(code[pc++], (byte) ((byte)code[pc++] >>> slots.getInt(code[pc++]))); break;

            case BitUnsignedRight.urshift_s_vc:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) >>> code[pc++])); break;
            case BitUnsignedRight.urshift_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) >>> code[pc++])); break;
            case BitUnsignedRight.urshift_s_vv:  slots.setShort(code[pc], (short)(slots.getShort(code[pc++]) >>> slots.getInt(code[pc++]))); break;
            case BitUnsignedRight.urshift_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) >>> slots.getInt(code[pc++]))); break;
            case BitUnsignedRight.urshift_s_vcv:  slots.setShort(code[pc], (short)((short)code[pc++] >>> slots.getInt(code[pc++]))); break;

            case BitUnsignedRight.urshift_l_vc:  slots.setLong(code[pc], slots.getLong(code[pc++]) >>> code[pc++]); break;
            case BitUnsignedRight.urshift_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) >>> code[pc++]); break;
            case BitUnsignedRight.urshift_l_vv:  slots.setLong(code[pc], slots.getLong(code[pc++]) >>> slots.getInt(code[pc++])); break;
            case BitUnsignedRight.urshift_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) >>> slots.getInt(code[pc++])); break;
            case BitUnsignedRight.urshift_l_vcv:  slots.setLong(code[pc], toLong(code[pc++], code[pc++]) >>> slots.getInt(code[pc++])); break;
        }
        return pc;
    }

    public int resolveExceptionHandler(int pc, Instance<?> exception) {
        TryCatchItem[] tryCatchItems = this.agoClass.getTryCatchItems();
        if(tryCatchItems == null) return -1;
        for (TryCatchItem tryCatchItem : tryCatchItems) {
            int handler = tryCatchItem.resolve(pc, exception.agoClass);
            if(handler != -1){
                return handler;
            }
        }
        return -1;
    }

    protected int evaluateAnd(final Slots slots, int pc, final int instruction){
        switch (instruction){
                case And.and_vv:      slots.setBoolean(code[pc], and(slots.getBoolean(code[pc++]), slots.getBoolean(code[pc++]))); break;
                case And.and_vvv:     slots.setBoolean(code[pc++], and(slots.getBoolean(code[pc++]), slots.getBoolean(code[pc++]))); break;
//                case And.and_vov:     slots.setBoolean(code[pc], slots.getBoolean(code[pc++]) && slots.getObject(code[pc++]).getSlots().getBoolean(code[pc++]) ); break;

        }
        return pc;
    }

    protected int evaluateOr(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case Or.or_vv:      slots.setBoolean(code[pc], or(slots.getBoolean(code[pc++]), slots.getBoolean(code[pc++]))); break;
            case Or.or_vvv:     slots.setBoolean(code[pc++], or(slots.getBoolean(code[pc++]), slots.getBoolean(code[pc++]))); break;
//                case Or.or_vov:     slots.setBoolean(code[pc], slots.getBoolean(code[pc++]) || slots.getObject(code[pc++]).getSlots().getBoolean(code[pc++])); break;
        }
        return pc;
    }

    protected int evaluateNot(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case Not.not_v:      slots.setBoolean(code[pc], !slots.getBoolean(code[pc++])); break;
            case Not.not_vv:      slots.setBoolean(code[pc++], !slots.getBoolean(code[pc++])); break;
    //                case Not.not_vov:     slots.setBoolean(code[pc++], !slots.getObject(code[pc++]).getSlots().getBoolean(code[pc++])); break;

        }
        return pc;
    }

    private int evaluateDebug(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case Debug.print_S_v:
                System.out.println(slots.getString(code[pc++]));
                break;
            case Debug.print_S_c:
                System.out.println(engine.toString(code[pc++]));
                break;
        }
        return pc;
    }


    protected int evaluateIncDec(final Slots slots,  int pc, final int instruction){
        switch (instruction){
            case IncDec.inc_i_ovc:   slots.getObject(code[pc++]).getSlots().incInt(code[pc++], code[pc++]);      break;
            case IncDec.inc_i_ovv:   slots.getObject(code[pc++]).getSlots().incInt(code[pc++], slots.getInt(code[pc++]));       break;
            case IncDec.inc_f_ov  :  slots.getObject(code[pc++]).getSlots().incFloat(code[pc++], Float.intBitsToFloat(code[pc++]));       break;
            case IncDec.inc_f_ovc :  slots.getObject(code[pc++]).getSlots().incFloat(code[pc++], slots.getFloat(code[pc++])); break;
            case IncDec.inc_d_ov  :  slots.getObject(code[pc++]).getSlots().incDouble(code[pc++], toDouble(code[pc++], code[pc++]));       break;
            case IncDec.inc_d_ovcc:  slots.getObject(code[pc++]).getSlots().incDouble(code[pc++], slots.getDouble(code[pc++]));       break;
            case IncDec.inc_b_ov  :  slots.getObject(code[pc++]).getSlots().incByte(code[pc++], (byte) code[pc++]);       break;
            case IncDec.inc_b_ovcc:  slots.getObject(code[pc++]).getSlots().incByte(code[pc++], slots.getByte(code[pc++]));       break;
            case IncDec.inc_s_ov  :  slots.getObject(code[pc++]).getSlots().incShort(code[pc++], (short) code[pc++]);       break;
            case IncDec.inc_s_ovcc:  slots.getObject(code[pc++]).getSlots().incShort(code[pc++], slots.getShort(code[pc++]));      break;
            case IncDec.inc_l_ov  :  slots.getObject(code[pc++]).getSlots().incLong(code[pc++], toLong(code[pc++], code[pc++]));        break;
            case IncDec.inc_l_ovcc:  slots.getObject(code[pc++]).getSlots().incLong(code[pc++], slots.getLong(code[pc++]));       break;
        }
        return pc;
    }
    protected int evaluateNeg(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case Neg.neg_i_vv: slots.setInt(code[pc++], -slots.getInt(code[pc++]));  break;
            case Neg.neg_f_vv: slots.setFloat(code[pc++], -slots.getFloat(code[pc++]));  break;
            case Neg.neg_d_vv: slots.setDouble(code[pc++], -slots.getDouble(code[pc++])); break;
            case Neg.neg_b_vv: slots.setByte(code[pc++], (byte)-slots.getByte(code[pc++])); break;
            case Neg.neg_s_vv: slots.setShort(code[pc++], (short) -slots.getShort(code[pc++])); break;
            case Neg.neg_l_vv: slots.setLong(code[pc++], -slots.getLong(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateMod(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case Mod.mod_i_vc:  slots.setInt(code[pc], slots.getInt(code[pc++]) %  code[pc++]); break;
            case Mod.mod_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) %  code[pc++]); break;
            case Mod.mod_i_vv:  slots.setInt(code[pc], slots.getInt(code[pc++]) %  slots.getInt(code[pc++])); break;
            case Mod.mod_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) %  slots.getInt(code[pc++])); break;
            case Mod.mod_i_vcv: slots.setInt(code[pc++], code[pc++] %  slots.getInt(code[pc++])); break;

            case Mod.mod_f_vc:  slots.setFloat(code[pc], slots.getFloat(code[pc++]) %  Float.intBitsToFloat(code[pc++])); break;
            case Mod.mod_f_vvc: slots.setFloat(code[pc++], slots.getFloat(code[pc++]) %  Float.intBitsToFloat(code[pc++])); break;
            case Mod.mod_f_vv:  slots.setFloat(code[pc], slots.getFloat(code[pc++]) %  slots.getFloat(code[pc++])); break;
            case Mod.mod_f_vvv: slots.setFloat(code[pc++], slots.getFloat(code[pc++]) %  slots.getFloat(code[pc++])); break;
            case Mod.mod_f_vcv: slots.setFloat(code[pc++], Float.intBitsToFloat(code[pc++]) %  slots.getFloat(code[pc++])); break;

            case Mod.mod_b_vc:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) %  code[pc++])); break;
            case Mod.mod_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) %  code[pc++])); break;
            case Mod.mod_b_vv:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) %  slots.getByte(code[pc++]))); break;
            case Mod.mod_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) %  slots.getByte(code[pc++]))); break;
            case Mod.mod_b_vcv: slots.setByte(code[pc++], (byte) ((byte)code[pc++] %  slots.getByte(code[pc++]))); break;

            case Mod.mod_s_vc:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) %  code[pc++])); break;
            case Mod.mod_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) %  code[pc++])); break;
            case Mod.mod_s_vv:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) %  slots.getShort(code[pc++]))); break;
            case Mod.mod_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) %  slots.getShort(code[pc++]))); break;
            case Mod.mod_s_vcv: slots.setShort(code[pc++], (short) ((short) code[pc++] %  slots.getShort(code[pc++]))); break;

            case Mod.mod_l_vc:  slots.setLong(code[pc], slots.getLong(code[pc++]) %  toLong(code[pc++], code[pc++])); break;
            case Mod.mod_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) %  toLong(code[pc++], code[pc++]));break;
            case Mod.mod_l_vv:  slots.setLong(code[pc], slots.getLong(code[pc++]) %  slots.getLong(code[pc++])); break;
            case Mod.mod_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) %  slots.getLong(code[pc++])); break;
            case Mod.mod_l_vcv: slots.setLong(code[pc++], toLong(code[pc++], code[pc++]) %  slots.getLong(code[pc++])); break;

            case Mod.mod_d_vc:  slots.setDouble(code[pc], slots.getDouble(code[pc++]) %  toDouble(code[pc++], code[pc++])); break;
            case Mod.mod_d_vvc: slots.setDouble(code[pc++], slots.getDouble(code[pc++]) %  toDouble(code[pc++], code[pc++])); break;
            case Mod.mod_d_vv:  slots.setDouble(code[pc], slots.getDouble(code[pc++]) %  slots.getDouble(code[pc++])); break;
            case Mod.mod_d_vvv: slots.setDouble(code[pc++], slots.getDouble(code[pc++]) %  slots.getDouble(code[pc++])); break;
            case Mod.mod_d_vcv: slots.setDouble(code[pc++], toDouble(code[pc++], code[pc++]) %  slots.getDouble(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateDiv(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case Div.div_i_vc:  slots.setInt(code[pc], slots.getInt(code[pc++]) / code[pc++]); break;
            case Div.div_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) / code[pc++]); break;
            case Div.div_i_vv:  slots.setInt(code[pc], slots.getInt(code[pc++]) / slots.getInt(code[pc++])); break;
            case Div.div_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) / slots.getInt(code[pc++])); break;
            case Div.div_i_vcv: slots.setInt(code[pc++], code[pc++] / slots.getInt(code[pc++])); break;

            case Div.div_f_vc:  slots.setFloat(code[pc], slots.getFloat(code[pc++]) / Float.intBitsToFloat(code[pc++])); break;
            case Div.div_f_vvc: slots.setFloat(code[pc++], slots.getFloat(code[pc++]) / Float.intBitsToFloat(code[pc++])); break;
            case Div.div_f_vv:  slots.setFloat(code[pc], slots.getFloat(code[pc++]) / slots.getFloat(code[pc++])); break;
            case Div.div_f_vvv: slots.setFloat(code[pc++], slots.getFloat(code[pc++]) / slots.getFloat(code[pc++])); break;
            case Div.div_f_vcv: slots.setFloat(code[pc++], Float.intBitsToFloat(code[pc++]) / slots.getFloat(code[pc++])); break;

            case Div.div_b_vc:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) / code[pc++])); break;
            case Div.div_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) / code[pc++])); break;
            case Div.div_b_vv:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) / slots.getByte(code[pc++]))); break;
            case Div.div_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) / slots.getByte(code[pc++]))); break;
            case Div.div_b_vcv: slots.setByte(code[pc++], (byte) ((byte)code[pc++] / slots.getByte(code[pc++]))); break;

            case Div.div_s_vc:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) / code[pc++])); break;
            case Div.div_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) / code[pc++])); break;
            case Div.div_s_vv:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) / slots.getShort(code[pc++]))); break;
            case Div.div_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) / slots.getShort(code[pc++]))); break;
            case Div.div_s_vcv: slots.setShort(code[pc++], (short) ((short) code[pc++] / slots.getShort(code[pc++]))); break;

            case Div.div_l_vc:  slots.setLong(code[pc], slots.getLong(code[pc++]) / toLong(code[pc++], code[pc++])); break;
            case Div.div_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) / toLong(code[pc++], code[pc++]));break;
            case Div.div_l_vv:  slots.setLong(code[pc], slots.getLong(code[pc++]) / slots.getLong(code[pc++])); break;
            case Div.div_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) / slots.getLong(code[pc++])); break;
            case Div.div_l_vcv: slots.setLong(code[pc++], toLong(code[pc++], code[pc++]) / slots.getLong(code[pc++])); break;

            case Div.div_d_vc:  slots.setDouble(code[pc], slots.getDouble(code[pc++]) / toDouble(code[pc++], code[pc++])); break;
            case Div.div_d_vvc: slots.setDouble(code[pc++], slots.getDouble(code[pc++]) / toDouble(code[pc++], code[pc++])); break;
            case Div.div_d_vv:  slots.setDouble(code[pc], slots.getDouble(code[pc++]) / slots.getDouble(code[pc++])); break;
            case Div.div_d_vvv: slots.setDouble(code[pc++], slots.getDouble(code[pc++]) / slots.getDouble(code[pc++])); break;
            case Div.div_d_vcv: slots.setDouble(code[pc++], toDouble(code[pc++], code[pc++]) / slots.getDouble(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateMultiply(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case Multiply.mul_i_vc:  slots.setInt(code[pc], slots.getInt(code[pc++]) * code[pc++]); break;
            case Multiply.mul_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) * code[pc++]); break;
            case Multiply.mul_i_vv:  slots.setInt(code[pc], slots.getInt(code[pc++]) * slots.getInt(code[pc++])); break;
            case Multiply.mul_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) * slots.getInt(code[pc++])); break;

            case Multiply.mul_f_vc:  slots.setFloat(code[pc], slots.getFloat(code[pc++]) * Float.intBitsToFloat(code[pc++])); break;
            case Multiply.mul_f_vvc: slots.setFloat(code[pc++], slots.getFloat(code[pc++]) * Float.intBitsToFloat(code[pc++])); break;
            case Multiply.mul_f_vv:  slots.setFloat(code[pc], slots.getFloat(code[pc++]) * slots.getFloat(code[pc++])); break;
            case Multiply.mul_f_vvv: slots.setFloat(code[pc++], slots.getFloat(code[pc++]) * slots.getFloat(code[pc++])); break;

            case Multiply.mul_b_vc:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) * code[pc++])); break;
            case Multiply.mul_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) * code[pc++])); break;
            case Multiply.mul_b_vv:  slots.setByte(code[pc], (byte) (slots.getByte(code[pc++]) * slots.getByte(code[pc++]))); break;
            case Multiply.mul_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) * slots.getByte(code[pc++]))); break;

            case Multiply.mul_s_vc:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) * code[pc++])); break;
            case Multiply.mul_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) * code[pc++])); break;
            case Multiply.mul_s_vv:  slots.setShort(code[pc], (short) (slots.getShort(code[pc++]) * slots.getShort(code[pc++]))); break;
            case Multiply.mul_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) * slots.getShort(code[pc++]))); break;

            case Multiply.mul_l_vc:  slots.setLong(code[pc], slots.getLong(code[pc++]) * toLong(code[pc++], code[pc++])); break;
            case Multiply.mul_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) * toLong(code[pc++], code[pc++]));break;
            case Multiply.mul_l_vv:  slots.setLong(code[pc], slots.getLong(code[pc++]) * slots.getLong(code[pc++])); break;
            case Multiply.mul_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) * slots.getLong(code[pc++])); break;

            case Multiply.mul_d_vc:  slots.setDouble(code[pc], slots.getDouble(code[pc++]) * toDouble(code[pc++], code[pc++])); break;
            case Multiply.mul_d_vvc: slots.setDouble(code[pc++], slots.getDouble(code[pc++]) * toDouble(code[pc++], code[pc++])); break;
            case Multiply.mul_d_vv:  slots.setDouble(code[pc], slots.getDouble(code[pc++]) * slots.getDouble(code[pc++])); break;
            case Multiply.mul_d_vvv: slots.setDouble(code[pc++], slots.getDouble(code[pc++]) * slots.getDouble(code[pc++])); break;
        }
        return pc;
    }

    protected static long toLong(int i1, int i2){
        return ((long)i1 << 32) | ((long)i2 & 0x0000_0000_ffff_ffffL);
    }

    protected static double toDouble(int i1, int i2){
        return Double.longBitsToDouble(((long)i1 << 32) | ((long)i2 & 0x0000_0000_ffff_ffffL));
    }

    protected int evaluateSub(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case Subtract.sub_i_vc:  slots.incInt(code[pc++], -code[pc++]); break;
            case Subtract.sub_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) - code[pc++]); break;
            case Subtract.sub_i_vv:  slots.incInt(code[pc++], -slots.getInt(code[pc++])); break;
            case Subtract.sub_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) - slots.getInt(code[pc++])); break;
            case Subtract.sub_i_vcv: slots.setInt(code[pc++], code[pc++] - slots.getInt(code[pc++])); break;

            case Subtract.sub_f_vc:  slots.incFloat(code[pc++], -Float.intBitsToFloat(code[pc++])); break;
            case Subtract.sub_f_vvc: slots.setFloat(code[pc++], slots.getFloat(code[pc++]) - Float.intBitsToFloat(code[pc++])); break;
            case Subtract.sub_f_vv:  slots.incFloat(code[pc++], -slots.getFloat(code[pc++])); break;
            case Subtract.sub_f_vvv: slots.setFloat(code[pc++], slots.getFloat(code[pc++]) - slots.getFloat(code[pc++])); break;
            case Subtract.sub_f_vcv: slots.setFloat(code[pc++], Float.intBitsToFloat(code[pc++]) - slots.getFloat(code[pc++])); break;

            case Subtract.sub_b_vc:  slots.incByte(code[pc++], (byte) (-code[pc++])); break;
            case Subtract.sub_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) - code[pc++])); break;
            case Subtract.sub_b_vv:  slots.incByte(code[pc++], (byte)-slots.getByte(code[pc++])); break;
            case Subtract.sub_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) - slots.getByte(code[pc++]))); break;
            case Subtract.sub_b_vcv: slots.setByte(code[pc++], (byte) ((byte)code[pc++] - slots.getByte(code[pc++]))); break;

            case Subtract.sub_s_vc:  slots.incShort(code[pc++], (short) (-code[pc++])); break;
            case Subtract.sub_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) - code[pc++])); break;
            case Subtract.sub_s_vv:  slots.incShort(code[pc++], (short) -slots.getShort(code[pc++])); break;
            case Subtract.sub_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) - slots.getShort(code[pc++]))); break;
            case Subtract.sub_s_vcv: slots.setShort(code[pc++], (short) ((short) code[pc++] - slots.getShort(code[pc++]))); break;

            case Subtract.sub_l_vc:  slots.incLong(code[pc++], -toLong(code[pc++], code[pc++])); break;
            case Subtract.sub_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) - toLong(code[pc++], code[pc++]));break;
            case Subtract.sub_l_vv:  slots.incLong(code[pc++], -slots.getLong(code[pc++])); break;
            case Subtract.sub_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) - slots.getLong(code[pc++])); break;
            case Subtract.sub_l_vcv: slots.setLong(code[pc++], toLong(code[pc++], code[pc++]) - slots.getLong(code[pc++])); break;

            case Subtract.sub_d_vc:  slots.incDouble(code[pc++], -toDouble(code[pc++], code[pc++])); break;
            case Subtract.sub_d_vvc: slots.setDouble(code[pc++], slots.getDouble(code[pc++]) - toDouble(code[pc++], code[pc++])); break;
            case Subtract.sub_d_vv:  slots.incDouble(code[pc++], -slots.getDouble(code[pc++])); break;
            case Subtract.sub_d_vvv: slots.setDouble(code[pc++], slots.getDouble(code[pc++]) - slots.getDouble(code[pc++])); break;
            case Subtract.sub_d_vcv: slots.setDouble(code[pc++], toDouble(code[pc++], code[pc++]) - slots.getDouble(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateLittleEquals(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case LittleEquals.le_i_vvc: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) <= code[pc++]); break;
            case LittleEquals.le_i_vvv: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) <= slots.getInt(code[pc++])); break;
            case LittleEquals.le_f_vvc: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) <= Float.intBitsToFloat(code[pc++])); break;
            case LittleEquals.le_f_vvv: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) <= slots.getFloat(code[pc++])); break;
            case LittleEquals.le_d_vvcc: slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) <= toDouble(code[pc++], code[pc++])); break;
            case LittleEquals.le_d_vvv:  slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) <= slots.getDouble(code[pc++])); break;
            case LittleEquals.le_b_vvc: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) <= (byte)code[pc++]); break;
            case LittleEquals.le_b_vvv: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) <= slots.getByte(code[pc++])); break;
            case LittleEquals.le_c_vvc: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) <= code[pc++]); break;
            case LittleEquals.le_c_vvv: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) <= slots.getChar(code[pc++])); break;
            case LittleEquals.le_s_vvc: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) <= code[pc++]); break;
            case LittleEquals.le_s_vvv: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) <= slots.getShort(code[pc++])); break;
            case LittleEquals.le_l_vvcc:  slots.setBoolean(code[pc++], slots.getLong(code[pc++]) <= toLong(code[pc++], code[pc++])); break;
            case LittleEquals.le_l_vvv:   slots.setBoolean(code[pc++], slots.getLong(code[pc++]) <= slots.getLong(code[pc++])); break;
            case LittleEquals.le_S_vvc: slots.setBoolean(code[pc++], littleEquals(slots.getString(code[pc++]), engine.toString(code[pc++]))); break;
            case LittleEquals.le_S_vvv: slots.setBoolean(code[pc++], littleEquals(slots.getString(code[pc++]), slots.getString(code[pc++]))); break;
            case LittleEquals.le_C_vvc: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) <= code[pc++]); break;
            case LittleEquals.le_C_vvv: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) <= slots.getClassRef(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateGreaterEquals(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case GreaterEquals.ge_i_vvc: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) >= code[pc++]); break;
            case GreaterEquals.ge_i_vvv: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) >= slots.getInt(code[pc++])); break;
            case GreaterEquals.ge_f_vvc: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) >= Float.intBitsToFloat(code[pc++])); break;
            case GreaterEquals.ge_f_vvv: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) >= slots.getFloat(code[pc++])); break;
            case GreaterEquals.ge_d_vvcc: slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) >= toDouble(code[pc++], code[pc++])); break;
            case GreaterEquals.ge_d_vvv:  slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) >= slots.getDouble(code[pc++])); break;
            case GreaterEquals.ge_b_vvc: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) >= (byte)code[pc++]); break;
            case GreaterEquals.ge_b_vvv: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) >= slots.getByte(code[pc++])); break;
            case GreaterEquals.ge_c_vvc: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) >= code[pc++]); break;
            case GreaterEquals.ge_c_vvv: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) >= slots.getChar(code[pc++])); break;
            case GreaterEquals.ge_s_vvc: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) >= code[pc++]); break;
            case GreaterEquals.ge_s_vvv: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) >= slots.getShort(code[pc++])); break;
            case GreaterEquals.ge_l_vvcc:  slots.setBoolean(code[pc++], slots.getLong(code[pc++]) >= toLong(code[pc++], code[pc++])); break;
            case GreaterEquals.ge_l_vvv:   slots.setBoolean(code[pc++], slots.getLong(code[pc++]) >= slots.getLong(code[pc++])); break;
            case GreaterEquals.ge_S_vvc: slots.setBoolean(code[pc++], greaterEquals(slots.getString(code[pc++]), engine.toString(code[pc++]))); break;
            case GreaterEquals.ge_S_vvv: slots.setBoolean(code[pc++], greaterEquals(slots.getString(code[pc++]), slots.getString(code[pc++]))); break;
            case GreaterEquals.ge_C_vvc: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) >= code[pc++]); break;
            case GreaterEquals.ge_C_vvv: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) >= slots.getClassRef(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateGreaterThan(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case GreaterThan.gt_i_vvc: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) > code[pc++]); break;
            case GreaterThan.gt_i_vvv: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) > slots.getInt(code[pc++])); break;
            case GreaterThan.gt_f_vvc: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) > Float.intBitsToFloat(code[pc++])); break;
            case GreaterThan.gt_f_vvv: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) > slots.getFloat(code[pc++])); break;
            case GreaterThan.gt_d_vvcc: slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) > toDouble(code[pc++], code[pc++])); break;
            case GreaterThan.gt_d_vvv:  slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) > slots.getDouble(code[pc++])); break;
            case GreaterThan.gt_b_vvc: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) > (byte)code[pc++]); break;
            case GreaterThan.gt_b_vvv: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) > slots.getByte(code[pc++])); break;
            case GreaterThan.gt_c_vvc: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) > code[pc++]); break;
            case GreaterThan.gt_c_vvv: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) > slots.getChar(code[pc++])); break;
            case GreaterThan.gt_s_vvc: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) > code[pc++]); break;
            case GreaterThan.gt_s_vvv: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) > slots.getShort(code[pc++])); break;
            case GreaterThan.gt_l_vvcc:  slots.setBoolean(code[pc++], slots.getLong(code[pc++]) > toLong(code[pc++], code[pc++])); break;
            case GreaterThan.gt_l_vvv:   slots.setBoolean(code[pc++], slots.getLong(code[pc++]) > slots.getLong(code[pc++])); break;
            case GreaterThan.gt_S_vvc: slots.setBoolean(code[pc++], 1==StringUtils.compare(slots.getString(code[pc++]), engine.toString(code[pc++]))); break;
            case GreaterThan.gt_S_vvv: slots.setBoolean(code[pc++], 1 == StringUtils.compare(slots.getString(code[pc++]), slots.getString(code[pc++]))); break;
            case GreaterThan.gt_C_vvc: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) > code[pc++]); break;
            case GreaterThan.gt_C_vvv: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) > slots.getClassRef(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateNotEquals(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case NotEquals.ne_i_vvc: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) != code[pc++]); break;
            case NotEquals.ne_i_vvv: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) != slots.getInt(code[pc++])); break;
            case NotEquals.ne_f_vvc: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) != Float.intBitsToFloat(code[pc++])); break;
            case NotEquals.ne_f_vvv: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) != slots.getFloat(code[pc++])); break;
            case NotEquals.ne_d_vvcc: slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) != toDouble(code[pc++], code[pc++])); break;
            case NotEquals.ne_d_vvv:  slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) != slots.getDouble(code[pc++])); break;
            case NotEquals.ne_b_vvc: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) != (byte)code[pc++]); break;
            case NotEquals.ne_b_vvv: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) != slots.getByte(code[pc++])); break;
            case NotEquals.ne_c_vvc: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) != code[pc++]); break;
            case NotEquals.ne_c_vvv: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) != slots.getChar(code[pc++])); break;
            case NotEquals.ne_s_vvc: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) != code[pc++]); break;
            case NotEquals.ne_s_vvv: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) != slots.getShort(code[pc++])); break;
            case NotEquals.ne_l_vvcc:  slots.setBoolean(code[pc++], slots.getLong(code[pc++]) != toLong(code[pc++], code[pc++])); break;
            case NotEquals.ne_l_vvv:   slots.setBoolean(code[pc++], slots.getLong(code[pc++]) != slots.getLong(code[pc++])); break;
            case NotEquals.ne_S_vvc: slots.setBoolean(code[pc++], !StringUtils.equals(slots.getString(code[pc++]), engine.toString(code[pc++]))); break;
            case NotEquals.ne_S_vvv: slots.setBoolean(code[pc++], !StringUtils.equals(slots.getString(code[pc++]), slots.getString(code[pc++]))); break;
            case NotEquals.ne_B_vvc: slots.setBoolean(code[pc++], slots.getBoolean(code[pc++]) != (code[pc++] !=0)); break;
            case NotEquals.ne_B_vvv: slots.setBoolean(code[pc++], slots.getBoolean(code[pc++]) != slots.getBoolean(code[pc++])); break;
            case NotEquals.ne_o_vvv: slots.setBoolean(code[pc++], slots.getObject(code[pc++]) != slots.getObject(code[pc++])); break;
            case NotEquals.ne_o_vvn: slots.setBoolean(code[pc++], slots.getObject(code[pc++]) != null); break;
            case NotEquals.ne_C_vvc: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) != code[pc++]); break;
            case NotEquals.ne_C_vvv: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) != slots.getClassRef(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateEquals(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case Equals.equals_i_vvc: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) == code[pc++]); break;
            case Equals.equals_i_vvv: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) == slots.getInt(code[pc++])); break;
            case Equals.equals_f_vvc: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) == Float.intBitsToFloat(code[pc++])); break;
            case Equals.equals_f_vvv: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) == slots.getFloat(code[pc++])); break;
            case Equals.equals_d_vvcc: slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) == toDouble(code[pc++], code[pc++])); break;
            case Equals.equals_d_vvv:  slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) == slots.getDouble(code[pc++])); break;
            case Equals.equals_b_vvc: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) == (byte)code[pc++]); break;
            case Equals.equals_b_vvv: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) == slots.getByte(code[pc++])); break;
            case Equals.equals_c_vvc: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) == code[pc++]); break;
            case Equals.equals_c_vvv: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) == slots.getChar(code[pc++])); break;
            case Equals.equals_s_vvc: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) == code[pc++]); break;
            case Equals.equals_s_vvv: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) == slots.getShort(code[pc++])); break;
            case Equals.equals_l_vvcc:  slots.setBoolean(code[pc++], slots.getLong(code[pc++]) == toLong(code[pc++], code[pc++])); break;
            case Equals.equals_l_vvv:   slots.setBoolean(code[pc++], slots.getLong(code[pc++]) == slots.getLong(code[pc++])); break;
            case Equals.equals_S_vvc: slots.setBoolean(code[pc++], StringUtils.equals(slots.getString(code[pc++]), engine.toString(code[pc++]))); break;
            case Equals.equals_S_vvv: slots.setBoolean(code[pc++], StringUtils.equals(slots.getString(code[pc++]), slots.getString(code[pc++]))); break;
            case Equals.equals_B_vvc: slots.setBoolean(code[pc++], slots.getBoolean(code[pc++]) == (code[pc++] !=0)); break;
            case Equals.equals_B_vvv: slots.setBoolean(code[pc++], slots.getBoolean(code[pc++]) == slots.getBoolean(code[pc++])); break;
            case Equals.equals_o_vvn: slots.setBoolean(code[pc++], slots.getObject(code[pc++]) == null); break;
            case Equals.equals_o_vvv: slots.setBoolean(code[pc++], slots.getObject(code[pc++]) == slots.getObject(code[pc++])); break;
            case Equals.equals_C_vvc: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) == code[pc++]); break;
            case Equals.equals_C_vvv: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) == slots.getClassRef(code[pc++])); break;
        }
        return pc;
    }
    protected int evaluateBox(final Slots slots, int pc, final int instruction){
        switch (instruction){
            case Box.box_i_vv:              slots.setObject(code[pc++], engine.getBoxer().boxInt(slots.getInt(code[pc++])));break;
            case Box.box_i_vc:              slots.setObject(code[pc++], engine.getBoxer().boxInt(code[pc++]));break;
            case Box.unbox_i_vo:            slots.setInt(code[pc++], slots.getObject(code[pc++]).getSlots().getInt(0));break;

            case Box.box_C_vv:              slots.setObject(code[pc++], engine.getBoxer().boxClassRef(slots.getClassRef(code[pc++]))); break;
            case Box.box_C_vC:              slots.setObject(code[pc++], engine.getBoxer().boxClassRef(code[pc++]));break;
            case Box.unbox_C_vo:            slots.setClassRef(code[pc++], slots.getObject(code[pc++]).getSlots().getClassRef(0));break;

            case Box.box_C_vvC:             slots.setObject(code[pc++], engine.getBoxer().boxClassRef(slots.getClassRef(code[pc++]), engine.getClass(code[pc++])));break;
            case Box.box_C_vCC:             slots.setObject(code[pc++], engine.getBoxer().boxClassRef(code[pc++], engine.getClass(code[pc++])));break;

            case Box.box_S_vv:              slots.setObject(code[pc++], engine.getBoxer().boxString(slots.getString(code[pc++])));break;
            case Box.box_S_vc:              slots.setObject(code[pc++], engine.getBoxer().boxString(engine.toString(code[pc++])));break;
            case Box.unbox_S_vo:            slots.setString(code[pc++], slots.getObject(code[pc++]).getSlots().getString(0));break;

            case Box.box_B_vv:              slots.setObject(code[pc++], engine.getBoxer().boxBoolean(slots.getBoolean(code[pc++])));break;
            case Box.box_B_vc:              slots.setObject(code[pc++], engine.getBoxer().boxBoolean(code[pc++] == 1));break;
            case Box.unbox_B_vo:            slots.setBoolean(code[pc++], slots.getObject(code[pc++]).getSlots().getBoolean(0));break;

            case Box.box_c_vv:              slots.setObject(code[pc++], engine.getBoxer().boxChar(slots.getChar(code[pc++])));break;
            case Box.box_c_vc:              slots.setObject(code[pc++], engine.getBoxer().boxChar((char)code[pc++]));break;
            case Box.unbox_c_vo:            slots.setChar(code[pc++], slots.getObject(code[pc++]).getSlots().getChar(0));break;

            case Box.box_b_vv:              slots.setObject(code[pc++], engine.getBoxer().boxByte(slots.getByte(code[pc++])));break;
            case Box.box_b_vc:              slots.setObject(code[pc++], engine.getBoxer().boxByte((byte)code[pc++]));break;
            case Box.unbox_b_vo:            slots.setByte(code[pc++], slots.getObject(code[pc++]).getSlots().getByte(0));break;

            case Box.box_s_vv:              slots.setObject(code[pc++], engine.getBoxer().boxShort(slots.getShort(code[pc++])));break;
            case Box.box_s_vc:              slots.setObject(code[pc++], engine.getBoxer().boxShort((short) code[pc++]));break;
            case Box.unbox_s_vo:            slots.setShort(code[pc++], slots.getObject(code[pc++]).getSlots().getShort(0));break;

            case Box.box_f_vv:              slots.setObject(code[pc++], engine.getBoxer().boxFloat(slots.getFloat(code[pc++])));break;
            case Box.box_f_vc:              slots.setObject(code[pc++], engine.getBoxer().boxFloat(Float.floatToIntBits(code[pc++])));break;
            case Box.unbox_f_vo:            slots.setFloat(code[pc++], slots.getObject(code[pc++]).getSlots().getFloat(0));break;

            case Box.box_d_vv:              slots.setObject(code[pc++], engine.getBoxer().boxDouble(slots.getDouble(code[pc++])));break;
            case Box.box_d_vc:              slots.setObject(code[pc++], engine.getBoxer().boxDouble(toDouble(code[pc++], code[pc++])));break;
            case Box.unbox_d_vo:            slots.setDouble(code[pc++], slots.getObject(code[pc++]).getSlots().getDouble(0));break;

            case Box.box_l_vv:              slots.setObject(code[pc++], engine.getBoxer().boxLong(slots.getLong(code[pc++])));break;
            case Box.box_l_vc:              slots.setObject(code[pc++], engine.getBoxer().boxLong(toLong(code[pc++], code[pc++])));break;
            case Box.unbox_l_vo:            slots.setLong(code[pc++], slots.getObject(code[pc++]).getSlots().getLong(0));break;

            case Box.unbox_o_vo:            {
                slots.setObject(code[pc++], slots.getObject(code[pc++]));
            }
            break;

            case Box.box_o_vv:              {
                slots.setObject(code[pc++], engine.getBoxer().boxAny(agoClass.getSlotDefs()[code[pc]], slots, code[pc++] ));
            }
            break;

            case Box.unbox_force_vot:{
                var r = engine.getBoxer().forceUnbox(this, code[pc++], slots.getObject(code[pc++]), slots.getInt(code[pc++]));
                if(!r) return code.length;
            }
            break;


        }
        return pc;
    }

    protected int evaluateArray(Slots slots, int pc, int instruction) {
        switch (instruction){
            case Array.array_create_i_vCc:   slots.setObject(code[pc++], engine.createIntArray(engine.getClass(code[pc++]), code[pc++])); break;
            case Array.array_create_i_vCv:   slots.setObject(code[pc++], engine.createIntArray(engine.getClass(code[pc++]), slots.getInt(code[pc++]))); break;
            case Array.array_fill_i_acL:     ((IntArrayInstance)slots.getObject(code[pc++])).fillBytes(code[pc++], engine.getBlob(code[pc++])); break;
            case Array.array_get_i_vac:     slots.setInt(code[pc++], ((IntArrayInstance)slots.getObject(code[pc++])).value[code[pc++]]); break;
            case Array.array_get_i_vav:     slots.setInt(code[pc++], ((IntArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])]); break;
            case Array.array_put_i_acc:     ((IntArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = code[pc++]; break;
            case Array.array_put_i_acv:     ((IntArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = slots.getInt(code[pc++]); break;
            case Array.array_put_i_avc:     ((IntArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = code[pc++]; break;
            case Array.array_put_i_avv:     ((IntArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = slots.getInt(code[pc++]); break;

            case Array.array_create_B_vCc:   slots.setObject(code[pc++], engine.createBooleanArray(engine.getClass(code[pc++]), code[pc++])); break;
            case Array.array_create_B_vCv:   slots.setObject(code[pc++], engine.createBooleanArray(engine.getClass(code[pc++]), slots.getInt(code[pc++]))); break;
            case Array.array_fill_B_acL:     ((BooleanArrayInstance)slots.getObject(code[pc++])).fillBytes(code[pc++], engine.getBlob(code[pc++])); break;
            case Array.array_get_B_vac:     slots.setBoolean(code[pc++], ((BooleanArrayInstance)slots.getObject(code[pc++])).value[code[pc++]]); break;
            case Array.array_get_B_vav:     slots.setBoolean(code[pc++], ((BooleanArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])]); break;
            case Array.array_put_B_acc:     ((BooleanArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = (code[pc++] == 1); break;
            case Array.array_put_B_acv:     ((BooleanArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = slots.getBoolean(code[pc++]); break;
            case Array.array_put_B_avc:     ((BooleanArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = code[pc++] == 1; break;
            case Array.array_put_B_avv:     ((BooleanArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = slots.getBoolean(code[pc++]); break;

            case Array.array_create_b_vCc:   slots.setObject(code[pc++], engine.createByteArray(engine.getClass(code[pc++]), code[pc++])); break;
            case Array.array_create_b_vCv:   slots.setObject(code[pc++], engine.createByteArray(engine.getClass(code[pc++]), slots.getInt(code[pc++]))); break;
            case Array.array_fill_b_acL:     ((ByteArrayInstance)slots.getObject(code[pc++])).fillBytes(code[pc++], engine.getBlob(code[pc++])); break;
            case Array.array_get_b_vac:     slots.setByte(code[pc++], ((ByteArrayInstance)slots.getObject(code[pc++])).value[code[pc++]]); break;
            case Array.array_get_b_vav:     slots.setByte(code[pc++], ((ByteArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])]); break;
            case Array.array_put_b_acc:     ((ByteArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = (byte)code[pc++]; break;
            case Array.array_put_b_acv:     ((ByteArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = slots.getByte(code[pc++]); break;
            case Array.array_put_b_avc:     ((ByteArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = (byte)code[pc++]; break;
            case Array.array_put_b_avv:     ((ByteArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = slots.getByte(code[pc++]); break;

            case Array.array_create_s_vCc:   slots.setObject(code[pc++], engine.createShortArray(engine.getClass(code[pc++]), code[pc++])); break;
            case Array.array_create_s_vCv:   slots.setObject(code[pc++], engine.createShortArray(engine.getClass(code[pc++]), slots.getInt(code[pc++]))); break;
            case Array.array_fill_s_acL:     ((ShortArrayInstance)slots.getObject(code[pc++])).fillBytes(code[pc++], engine.getBlob(code[pc++])); break;
            case Array.array_get_s_vac:     slots.setShort(code[pc++], ((ShortArrayInstance)slots.getObject(code[pc++])).value[code[pc++]]); break;
            case Array.array_get_s_vav:     slots.setShort(code[pc++], ((ShortArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])]); break;
            case Array.array_put_s_acc:     ((ShortArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = (short)(code[pc++]); break;
            case Array.array_put_s_acv:     ((ShortArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = slots.getShort(code[pc++]); break;
            case Array.array_put_s_avc:     ((ShortArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = (short)code[pc++]; break;
            case Array.array_put_s_avv:     ((ShortArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = slots.getShort(code[pc++]); break;

            case Array.array_create_c_vCc:   slots.setObject(code[pc++], engine.createCharArray(engine.getClass(code[pc++]), code[pc++])); break;
            case Array.array_create_c_vCv:   slots.setObject(code[pc++], engine.createCharArray(engine.getClass(code[pc++]), slots.getInt(code[pc++]))); break;
            case Array.array_fill_c_acL:     ((CharArrayInstance)slots.getObject(code[pc++])).fillBytes(code[pc++], engine.getBlob(code[pc++])); break;
            case Array.array_get_c_vac:     slots.setChar(code[pc++], ((CharArrayInstance)slots.getObject(code[pc++])).value[code[pc++]]); break;
            case Array.array_get_c_vav:     slots.setChar(code[pc++], ((CharArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])]); break;
            case Array.array_put_c_acc:     ((CharArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = (char)(code[pc++]); break;
            case Array.array_put_c_acv:     ((CharArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = slots.getChar(code[pc++]); break;
            case Array.array_put_c_avc:     ((CharArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = (char)code[pc++]; break;
            case Array.array_put_c_avv:     ((CharArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = slots.getChar(code[pc++]); break;

            case Array.array_create_f_vCc:   slots.setObject(code[pc++], engine.createFloatArray(engine.getClass(code[pc++]), code[pc++])); break;
            case Array.array_create_f_vCv:   slots.setObject(code[pc++], engine.createFloatArray(engine.getClass(code[pc++]), slots.getInt(code[pc++]))); break;
            case Array.array_fill_f_acL:     ((FloatArrayInstance)slots.getObject(code[pc++])).fillBytes(code[pc++], engine.getBlob(code[pc++])); break;
            case Array.array_get_f_vac:     slots.setFloat(code[pc++], ((FloatArrayInstance)slots.getObject(code[pc++])).value[code[pc++]]); break;
            case Array.array_get_f_vav:     slots.setFloat(code[pc++], ((FloatArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])]); break;
            case Array.array_put_f_acc:     ((FloatArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = Float.intBitsToFloat(code[pc++]); break;
            case Array.array_put_f_acv:     ((FloatArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = slots.getFloat(code[pc++]); break;
            case Array.array_put_f_avc:     ((FloatArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = Float.intBitsToFloat(code[pc++]); break;
            case Array.array_put_f_avv:     ((FloatArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = slots.getFloat(code[pc++]); break;

            case Array.array_create_d_vCc:   slots.setObject(code[pc++], engine.createDoubleArray(engine.getClass(code[pc++]), code[pc++])); break;
            case Array.array_create_d_vCv:   slots.setObject(code[pc++], engine.createDoubleArray(engine.getClass(code[pc++]), slots.getInt(code[pc++]))); break;
            case Array.array_fill_d_acL:     ((DoubleArrayInstance)slots.getObject(code[pc++])).fillBytes(code[pc++], engine.getBlob(code[pc++])); break;
            case Array.array_get_d_vac:     slots.setDouble(code[pc++], ((DoubleArrayInstance)slots.getObject(code[pc++])).value[code[pc++]]); break;
            case Array.array_get_d_vav:     slots.setDouble(code[pc++], ((DoubleArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])]); break;
            case Array.array_put_d_acc:     ((DoubleArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = toDouble(code[pc++], code[pc++]); break;
            case Array.array_put_d_acv:     ((DoubleArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = slots.getDouble(code[pc++]); break;
            case Array.array_put_d_avc:     ((DoubleArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = toDouble(code[pc++], code[pc++]); break;
            case Array.array_put_d_avv:     ((DoubleArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = slots.getDouble(code[pc++]); break;

            case Array.array_create_l_vCc:   slots.setObject(code[pc++], engine.createLongArray(engine.getClass(code[pc++]), code[pc++])); break;
            case Array.array_create_l_vCv:   slots.setObject(code[pc++], engine.createLongArray(engine.getClass(code[pc++]), slots.getInt(code[pc++]))); break;
            case Array.array_fill_l_acL:     ((LongArrayInstance)slots.getObject(code[pc++])).fillBytes(code[pc++], engine.getBlob(code[pc++])); break;
            case Array.array_get_l_vac:     slots.setLong(code[pc++], ((LongArrayInstance)slots.getObject(code[pc++])).value[code[pc++]]); break;
            case Array.array_get_l_vav:     slots.setLong(code[pc++], ((LongArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])]); break;
            case Array.array_put_l_acc:     ((LongArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = (long) code[pc++] << 32 | code[pc++]; break;
            case Array.array_put_l_acv:     ((LongArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = slots.getLong(code[pc++]); break;
            case Array.array_put_l_avc:     ((LongArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = (long) code[pc++] << 32 | code[pc++]; break;
            case Array.array_put_l_avv:     ((LongArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = slots.getLong(code[pc++]); break;

            case Array.array_create_S_vCc:   slots.setObject(code[pc++], engine.createStringArray(engine.getClass(code[pc++]), code[pc++])); break;
            case Array.array_create_S_vCv:   slots.setObject(code[pc++], engine.createStringArray(engine.getClass(code[pc++]), slots.getInt(code[pc++]))); break;
            case Array.array_fill_S_acL:     ((StringArrayInstance)slots.getObject(code[pc++])).fillBytes(code[pc++], engine.getBlob(code[pc++])); break;
            case Array.array_get_S_vac:     slots.setString(code[pc++], ((StringArrayInstance)slots.getObject(code[pc++])).value[code[pc++]]); break;
            case Array.array_get_S_vav:     slots.setString(code[pc++], ((StringArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])]); break;
            case Array.array_put_S_acc:     ((StringArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = engine.toString(code[pc++]); break;
            case Array.array_put_S_acv:     ((StringArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = slots.getString(code[pc++]); break;
            case Array.array_put_S_avc:     ((StringArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = engine.toString(code[pc++]); break;
            case Array.array_put_S_avv:     ((StringArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = slots.getString(code[pc++]); break;

            case Array.array_create_o_vCc:   slots.setObject(code[pc++], engine.createObjectArray(engine.getClass(code[pc++]), code[pc++])); break;
            case Array.array_create_o_vCv:   slots.setObject(code[pc++], engine.createObjectArray(engine.getClass(code[pc++]), slots.getInt(code[pc++]))); break;
            case Array.array_get_o_vac:     slots.setObject(code[pc++], ((ObjectArrayInstance)slots.getObject(code[pc++])).value[code[pc++]]); break;
            case Array.array_get_o_vav:     slots.setObject(code[pc++], ((ObjectArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])]); break;
            case Array.array_put_o_acn:     ((ObjectArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = null; break;
            case Array.array_put_o_aco:     ((ObjectArrayInstance)slots.getObject(code[pc++])).value[code[pc++]] = slots.getObject(code[pc++]); break;
            case Array.array_put_o_avn:     ((ObjectArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = null; break;
            case Array.array_put_o_avo:     ((ObjectArrayInstance)slots.getObject(code[pc++])).value[slots.getInt(code[pc++])] = slots.getObject(code[pc++]); break;

        }
        return pc;
    }

    protected int evaluateLoad(Slots slots, int pc, int instruction) {
        switch (instruction){
            case Load.loadscope_v:       slots.setObject(code[pc++], this.getParentScope());  break;
            case Load.loadscope_vc:      slots.setObject(code[pc++], getScope(code[pc++])); break;

            case Load.loadcls_scope_vc:     {
                int target = code[pc++];
                int offset = code[pc++];
                switch (offset) {
                    case 0: slots.setObject(target, this.agoClass); break;
                    case 1: slots.setObject(target, this.getParentScope().agoClass); break;
                    case 2: slots.setObject(target, this.getParentScope().getParentScope().agoClass); break;
                    default:
                        Instance<?> instance = this;
                        for (int i = 0; i < offset; i++) {
                            instance = instance.getParentScope();
                        }
                        slots.setObject(target, instance.agoClass);
                }
                break;
            }
            case Load.loadcls_scope_v:      slots.setObject(code[pc++], this.getParentScope().getAgoClass()); break;
            case Load.loadcls_vo:           slots.setObject(code[pc++], slots.getObject(code[pc++]).getAgoClass()); break;
            case Load.loadcls_vC:           slots.setObject(code[pc++], engine.getClass(code[pc++])); break;

            case Load.loadcls2_scope_vc:     {
                int target = code[pc++];
                int offset = code[pc++];
                switch (offset) {
                    case 0: slots.setObject(target, this.agoClass.agoClass); break;
                    case 1: slots.setObject(target, this.getParentScope().agoClass.agoClass); break;
                    case 2: slots.setObject(target, this.getParentScope().getParentScope().agoClass.agoClass); break;
                    default:
                        Instance<?> instance = this;
                        for (int i = 0; i < offset; i++) {
                            instance = instance.getParentScope();
                        }
                        slots.setObject(target, instance.agoClass.agoClass);
                }
                break;
            }
            case Load.loadcls2_scope_v:     slots.setObject(code[pc++], this.getParentScope().getAgoClass().getAgoClass()); break;
            case Load.loadcls2_vo:          slots.setObject(code[pc++], slots.getObject(code[pc++]).getAgoClass().getAgoClass()); break;

            case Load.bindcls_vCo:          slots.setObject(code[pc++], engine.createScopedClass(this, code[pc++], slots.getObject(code[pc++]))); break;
            case Load.bindcls_scope_vCc:    slots.setObject(code[pc++], engine.createScopedClass(this, code[pc++], getScope(code[pc++]))); break;

        }
        return pc;
    }

    protected int evaluateReturn(CallFrame<?> self, Slots slots, int pc, int instruction) {
        switch (instruction){
            case Return.return_i_c:     {self.finishInt(code[pc++]); break;}
            case Return.return_i_v:     {self.finishInt(slots.getInt(code[pc++])); break;}

            case Return.return_V:       {self.finishVoid(); break;}
            case Return.return_V_v:     {pc++; self.finishVoid(); break;}
            case Return.return_B_c:     {self.finishBoolean(code[pc++] != 0); break;}
            case Return.return_B_v:     {self.finishBoolean(slots.getBoolean(code[pc++])); break;}
            case Return.return_c_c:     {self.finishChar((char)code[pc++]); break;}
            case Return.return_c_v:     {self.finishChar(slots.getChar(code[pc++])); break;}

            case Return.return_f_c:     {self.finishFloat(Float.intBitsToFloat(code[pc++])); break;}
            case Return.return_f_v:     {self.finishFloat(slots.getFloat(code[pc++])); break;}
            case Return.return_d_c:     {self.finishDouble(toDouble(code[pc++], code[pc++])); break;}
            case Return.return_d_v:     {self.finishDouble(slots.getDouble(code[pc++])); break;}
            case Return.return_b_c:     {self.finishByte((byte) code[pc++]); break;}
            case Return.return_b_v:     {self.finishByte(slots.getByte(code[pc++])); break;}
            case Return.return_s_c:     {self.finishShort((short) code[pc++]); break;}
            case Return.return_s_v:     {self.finishShort(slots.getShort(code[pc++])); break;}
            case Return.return_l_c:     {self.finishLong(toLong(code[pc++], code[pc++])); break;}
            case Return.return_l_v:     {self.finishLong(slots.getLong(code[pc++])); break;}
            case Return.return_o_v:     {self.finishObject(slots.getObject(code[pc++])); break;}
            case Return.return_n:       {self.finishNull(); break;}
            case Return.return_S_c:     {self.finishString(engine.toString(code[pc++])); break;}
            case Return.return_S_v:     {self.finishString(slots.getString(code[pc++])); break;}

            case Return.return_C_v:     {self.finishClassRef(engine.getClass(slots.getClassRef(code[pc++]))); break;}
        }
        return code.length;
    }

    protected int evaluateConcat(Slots slots, int pc, int instruction) {
        switch (instruction){
            //TODO incString?
            case Concat.concat_S_vc:   slots.setString(code[pc], slots.getString(code[pc++]) + engine.toString(code[pc++])); break;
            case Concat.concat_S_vvc:  slots.setString(code[pc++], slots.getString(code[pc++]) + engine.toString(code[pc++])); break;
            case Concat.concat_S_vv:   slots.setString(code[pc], slots.getString(code[pc++]) + slots.getString(code[pc++])); break;
            case Concat.concat_S_vvv:  slots.setString(code[pc++], slots.getString(code[pc++]) + slots.getString(code[pc++])); break;
            case Concat.concat_S_vcv:  slots.setString(code[pc++], engine.toString(code[pc++]) + slots.getString(code[pc++])); break;
        }
        return pc;
    }

    protected int evaluateLittleThan(final Slots slots, int pc, final int instruction) {
        switch (instruction){
                case LittleThan.lt_i_vvc: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) < code[pc++]); break;
                case LittleThan.lt_i_vvv: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) < slots.getInt(code[pc++])); break;
                case LittleThan.lt_f_vvc: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) < Float.intBitsToFloat(code[pc++])); break;
                case LittleThan.lt_f_vvv: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) < slots.getFloat(code[pc++])); break;
                case LittleThan.lt_d_vvcc: slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) < toDouble(code[pc++], code[pc++])); break;
                case LittleThan.lt_d_vvv:  slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) < slots.getDouble(code[pc++])); break;
                case LittleThan.lt_b_vvc: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) < (byte)code[pc++]); break;
                case LittleThan.lt_b_vvv: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) < slots.getByte(code[pc++])); break;
                case LittleThan.lt_c_vvc: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) < code[pc++]); break;
                case LittleThan.lt_c_vvv: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) < slots.getChar(code[pc++])); break;
                case LittleThan.lt_s_vvc: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) < code[pc++]); break;
                case LittleThan.lt_s_vvv: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) < slots.getShort(code[pc++])); break;
                case LittleThan.lt_l_vvcc:  slots.setBoolean(code[pc++], slots.getLong(code[pc++]) < toLong(code[pc++], code[pc++])); break;
                case LittleThan.lt_l_vvv:   slots.setBoolean(code[pc++], slots.getLong(code[pc++]) < slots.getLong(code[pc++])); break;
                case LittleThan.lt_S_vvc: slots.setBoolean(code[pc++], -1 == StringUtils.compare(slots.getString(code[pc++]), engine.toString(code[pc++]))); break;
                case LittleThan.lt_S_vvv: slots.setBoolean(code[pc++], -1 == StringUtils.compare(slots.getString(code[pc++]), slots.getString(code[pc++]))); break;
                case LittleThan.lt_C_vvc: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) < code[pc++]); break;
                case LittleThan.lt_C_vvv: slots.setBoolean(code[pc++], slots.getClassRef(code[pc++]) < slots.getClassRef(code[pc++])); break;
        }
        return pc;
    }

    protected int evaluateJump(Slots slots,  int pc, int instruction) {
        switch (instruction){
            case Jump.jump_c:           pc = code[pc]; break;
            case Jump.jnz_v:            { int v = slots.getInt(code[pc++]); if(v != 0) pc = v; } break;
            case Jump.jump_t_B_vc:      if(slots.getBoolean(code[pc++])) pc = code[pc]; else pc++; break;
            case Jump.jump_f_B_vc:      if(!slots.getBoolean(code[pc++])) pc = code[pc]; else pc++; break;
            case Jump.jump_t_i_vc:      if(slots.getInt(code[pc++]) != 0) pc = code[pc]; else pc++; break;
            case Jump.jump_f_i_vc:      if(slots.getInt(code[pc++])== 0) pc = code[pc]; else pc++; break;
            case Jump.jump_t_C_vc:      if(slots.getClassRef(code[pc++]) != 0) pc = code[pc]; else pc++; break;
            case Jump.jump_f_C_vc:      if(slots.getClassRef(code[pc++]) == 0) pc = code[pc]; else pc++; break;
            case Jump.jump_t_c_vc:      if(slots.getChar(code[pc++]) != 0) pc = code[pc]; else pc++; break;
            case Jump.jump_f_c_vc:      if(slots.getChar(code[pc++]) == 0) pc = code[pc]; else pc++; break;
            case Jump.jump_t_f_vc:      if(slots.getFloat(code[pc++]) != 0) pc = code[pc]; else pc++; break;
            case Jump.jump_f_f_vc:      if(slots.getFloat(code[pc++]) == 0) pc = code[pc]; else pc++; break;
            case Jump.jump_t_d_vc:      if(slots.getDouble(code[pc++]) != 0) pc = code[pc]; else pc++; break;
            case Jump.jump_f_d_vc:      if(slots.getDouble(code[pc++]) == 0) pc = code[pc]; else pc++; break;
            case Jump.jump_t_b_vc:      if(slots.getByte(code[pc++]) != 0) pc = code[pc]; else pc++; break;
            case Jump.jump_f_b_vc:      if(slots.getByte(code[pc++]) == 0) pc = code[pc]; else pc++; break;
            case Jump.jump_t_s_vc:      if(slots.getShort(code[pc++]) != 0) pc = code[pc]; else pc++; break;
            case Jump.jump_f_s_vc:      if(slots.getShort(code[pc++]) == 0) pc = code[pc]; else pc++; break;
            case Jump.jump_t_l_vc:      if(slots.getLong(code[pc++]) != 0) pc = code[pc]; else pc++; break;
            case Jump.jump_f_l_vc:      if(slots.getLong(code[pc++]) == 0) pc = code[pc]; else pc++; break;
            case Jump.jump_t_o_vc:      if(slots.getObject(code[pc++]) != null) pc = code[pc]; else pc++; break;
            case Jump.jump_f_o_vc:      if(slots.getObject(code[pc++]) == null) pc = code[pc]; else pc++; break;
            case Jump.jump_t_S_vc:      if(StringUtils.isNotEmpty(slots.getString(code[pc++]))) pc = code[pc]; else pc++; break;
            case Jump.jump_f_S_vc:      if(StringUtils.isEmpty(slots.getString(code[pc++]))) pc = code[pc]; else pc++; break;

            case Jump.switch_dense_cv:
                pc = this.agoClass.getSwitchTables()[code[pc++]].resolve(slots.getInt(code[pc++])); break;
            case Jump.switch_sparse_cv:
                pc = this.agoClass.getSwitchTables()[code[pc++]].resolve(slots.getInt(code[pc++])); break;
        }
        return pc;
    }

    protected int evaluateNew(Slots slots, int pc, int instruction) {
        switch (instruction){

                // ------------------- new ----------------------
                case New.new_vC: {
                    slots.setObject(code[pc++], engine.createInstance(null, code[pc++], this, runSpace));
                    break;
                }
                case New.newn_vC: {
                    slots.setObject(code[pc++], engine.createNativeInstance(null, code[pc++], this ));
                    break;
                }
                case New.new_child_voC:{
                    slots.setObject(code[pc++], engine.createInstance(slots.getObject(code[pc++]), code[pc++], this,  runSpace));
                    break;
                }
                case New.newn_child_voC:{
                    slots.setObject(code[pc++], engine.createNativeInstance(slots.getObject(code[pc++]), code[pc++], this ));
                    break;
                }
                case New.new_vo: {
                    slots.setObject(code[pc++], engine.createInstanceFromScopedClass((AgoClass)slots.getObject(code[pc++]),this, runSpace));
                    break;
                }
                case New.new_method_voCm: {
                    // here the C was used in transform code, to locate method index
                    // and the class of scope will use to implement method overriding
                    Instance<?> scope;
                    slots.setObject(code[pc++], engine.createFunctionInstance(scope = slots.getObject(code[pc++]), scope.getAgoClass().getMethod(code[++pc]), this, this ));
                    pc++;
                    break;
                }
                case New.new_method_voIm: {
                    Instance<?> scope;
                    slots.setObject(code[pc++], engine.createFunctionInstance(scope = slots.getObject(code[pc++]), scope.getAgoClass().resolveMethodByInterface(code[pc++], code[pc++]), this, this ));
                    break;
                }
                case New.new_cls_method_vCm:{
                    AgoClass scopeClass;
                    slots.setObject(code[pc++], engine.createFunctionInstance(scopeClass = engine.getClass(code[pc++]), scopeClass.getAgoClass().getMethod(code[pc++]), this, this  ));
                    break;
                }
                case New.new_scope_child_vcC:{
                    slots.setObject(code[pc++], engine.createInstance(getScope(code[pc++]), engine.getClass(code[pc++]),  this ));
                    break;
                }
                case New.newn_scope_child_vcC:{
                    Instance<?> instance;
                    slots.setObject(code[pc++], instance = engine.createNativeInstance(getScope(code[pc++]), code[pc++],this ));
                    break;
                }
                case New.new_scope_method_vcCm:{
                    // like new_method_voCm, C wa used in transform code
                    Instance<?> scope;
                    slots.setObject(code[pc++], engine.createFunctionInstance(scope = getScope(code[pc++]), scope.getAgoClass().getMethod(code[++pc]),this,this  ));
                    pc++;
                    break;
                }
                case New.new_scope_method_fix_vcCm:{
                    slots.setObject(code[pc++], engine.createFunctionInstance(getScope(code[pc++]), engine.getClass(code[pc++]).getMethod(code[pc++]),this,this  ));
                    break;
                }
        }
        return pc;
    }

    protected int evaluateAdd(Slots slots,  int pc, int instruction) {
        switch (instruction){
                case Add.add_i_vc:  slots.incInt(code[pc++], code[pc++]); break;
                case Add.add_i_vvc: slots.setInt(code[pc++], slots.getInt(code[pc++]) + code[pc++]); break;
                case Add.add_i_vv:  slots.incInt(code[pc++], slots.getInt(code[pc++])); break;
                case Add.add_i_vvv: slots.setInt(code[pc++], slots.getInt(code[pc++]) + slots.getInt(code[pc++])); break;

                case Add.add_f_vc:  slots.incFloat(code[pc++], Float.intBitsToFloat(code[pc++])); break;
                case Add.add_f_vvc: slots.setFloat(code[pc++], slots.getFloat(code[pc++]) + Float.intBitsToFloat(code[pc++])); break;
                case Add.add_f_vv:  slots.incFloat(code[pc++], slots.getFloat(code[pc++])); break;
                case Add.add_f_vvv: slots.setFloat(code[pc++], slots.getFloat(code[pc++]) + slots.getFloat(code[pc++])); break;

                case Add.add_b_vc:  slots.incByte(code[pc++], (byte) (code[pc++])); break;
                case Add.add_b_vvc: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) + code[pc++])); break;
                case Add.add_b_vv:  slots.incByte(code[pc++], slots.getByte(code[pc++])); break;
                case Add.add_b_vvv: slots.setByte(code[pc++], (byte) (slots.getByte(code[pc++]) + slots.getByte(code[pc++]))); break;

                case Add.add_s_vc:  slots.incShort(code[pc++], (short) (code[pc++])); break;
                case Add.add_s_vvc: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) + code[pc++])); break;
                case Add.add_s_vv:  slots.incShort(code[pc++], slots.getShort(code[pc++])); break;
                case Add.add_s_vvv: slots.setShort(code[pc++], (short) (slots.getShort(code[pc++]) + slots.getShort(code[pc++]))); break;

                case Add.add_l_vc:  slots.incLong(code[pc++], toLong(code[pc++], code[pc++])); break;
                case Add.add_l_vvc: slots.setLong(code[pc++], slots.getLong(code[pc++]) + toLong(code[pc++], code[pc++]));break;
                case Add.add_l_vv:  slots.incLong(code[pc++], slots.getLong(code[pc++])); break;
                case Add.add_l_vvv: slots.setLong(code[pc++], slots.getLong(code[pc++]) + slots.getLong(code[pc++])); break;

                case Add.add_d_vc:  slots.incDouble(code[pc++], toDouble(code[pc++], code[pc++])); break;
                case Add.add_d_vvc: slots.setDouble(code[pc++], slots.getDouble(code[pc++]) + toDouble(code[pc++], code[pc++])); break;
                case Add.add_d_vv:  slots.incDouble(code[pc++], slots.getDouble(code[pc++])); break;
                case Add.add_d_vvv: slots.setDouble(code[pc++], slots.getDouble(code[pc++]) + slots.getDouble(code[pc++])); break;
        }
        return pc;
    }

    protected int evaluateAccept(Slots slots, int pc, int instruction) {
        var runSpace = this.getRunSpace();
        switch (instruction){
            case Accept.accept_V_v :  slots.setVoid(code[pc++], null); break;
            case Accept.accept_o_v :  slots.setObject(code[pc++], runSpace.getResultSlots().takeObjectValue()); break;
            case Accept.accept_n_v :  slots.setObject(code[pc++], null); break;
            case Accept.accept_S_v :  slots.setString(code[pc++], runSpace.getResultSlots().getStringValue()); break;
            case Accept.accept_B_v :  slots.setBoolean(code[pc++], runSpace.getResultSlots().getBooleanValue()); break;
            case Accept.accept_c_v :  slots.setChar(code[pc++], runSpace.getResultSlots().getCharValue()); break;
            case Accept.accept_f_v :  slots.setFloat(code[pc++], runSpace.getResultSlots().getFloatValue()); break;
            case Accept.accept_d_v :  slots.setDouble(code[pc++], runSpace.getResultSlots().getDoubleValue()); break;
            case Accept.accept_b_v :  slots.setByte(code[pc++], runSpace.getResultSlots().getByteValue()); break;
            case Accept.accept_s_v :  slots.setShort(code[pc++], runSpace.getResultSlots().getShortValue()); break;
            case Accept.accept_i_v :  slots.setInt(code[pc++], runSpace.getResultSlots().getIntValue()); break;
            case Accept.accept_l_v :  slots.setLong(code[pc++], runSpace.getResultSlots().getLongValue()); break;
            case Accept.accept_C_v :  slots.setClassRef(code[pc++], runSpace.getResultSlots().getClassRefValue().getClassId()); break;
            case Accept.accept_any_v:  slots.setObject(code[pc++], runSpace.getResultSlots().castAnyToObject(engine.getBoxer())); break;
        }
        return pc;
    }

    protected int evaluateMove(Slots slots, int pc, int instruction) {
        switch (instruction){
            case Move.move_i_vv:    slots.setInt(code[pc++], slots.getInt(code[pc++])); break;
            case Move.move_fld_i_ovv:   slots.getObject(code[pc++]).getSlots().setInt(code[pc++], slots.getInt(code[pc++])); break;
            case Move.move_fld_i_vov:   slots.setInt(code[pc++], slots.getObject(code[pc++]).getSlots().getInt(code[pc++])); break;

            case Move.move_C_vv:    slots.setClassRef(code[pc++], slots.getClassRef(code[pc++])); break;
            case Move.move_fld_C_ovv:   slots.getObject(code[pc++]).getSlots().setClassRef(code[pc++], slots.getClassRef(code[pc++])); break;
            case Move.move_fld_C_vov:   slots.setClassRef(code[pc++], slots.getObject(code[pc++]).getSlots().getClassRef(code[pc++])); break;

            case Move.move_B_vv :   slots.setBoolean(code[pc++], slots.getBoolean(code[pc++])); break;
            case Move.move_fld_B_ovv:   slots.getObject(code[pc++]).getSlots().setBoolean(code[pc++], slots.getBoolean(code[pc++])); break;
            case Move.move_fld_B_vov:   slots.setBoolean(code[pc++], slots.getObject(code[pc++]).getSlots().getBoolean(code[pc++]));break;

            case Move.move_c_vv :   slots.setChar(code[pc++], slots.getChar(code[pc++])); break;
            case Move.move_fld_c_ovv:   slots.getObject(code[pc++]).getSlots().setChar(code[pc++], slots.getChar(code[pc++])); break;
            case Move.move_fld_c_vov:   slots.setChar(code[pc++], slots.getObject(code[pc++]).getSlots().getChar(code[pc++]));break;

            case Move.move_f_vv :   slots.setFloat(code[pc++], slots.getFloat(code[pc++])); break;
            case Move.move_fld_f_ovv:   slots.getObject(code[pc++]).getSlots().setFloat(code[pc++], slots.getFloat(code[pc++])); break;
            case Move.move_fld_f_vov:   slots.setFloat(code[pc++], slots.getObject(code[pc++]).getSlots().getFloat(code[pc++]));break;

            case Move.move_d_vv :   slots.setDouble(code[pc++], slots.getDouble(code[pc++])); break;
            case Move.move_fld_d_ovv:   slots.getObject(code[pc++]).getSlots().setDouble(code[pc++], slots.getDouble(code[pc++])); break;
            case Move.move_fld_d_vov:   slots.setDouble(code[pc++], slots.getObject(code[pc++]).getSlots().getDouble(code[pc++]));break;

            case Move.move_b_vv :   slots.setByte(code[pc++], slots.getByte(code[pc++])); break;
            case Move.move_fld_b_ovv:   slots.getObject(code[pc++]).getSlots().setByte(code[pc++], slots.getByte(code[pc++])); break;
            case Move.move_fld_b_vov:   slots.setByte(code[pc++], slots.getObject(code[pc++]).getSlots().getByte(code[pc++]));break;

            case Move.move_s_vv :   slots.setShort(code[pc++], slots.getShort(code[pc++])); break;
            case Move.move_fld_s_ovv:   slots.getObject(code[pc++]).getSlots().setShort(code[pc++], slots.getShort(code[pc++])); break;
            case Move.move_fld_s_vov:   slots.setShort(code[pc++], slots.getObject(code[pc++]).getSlots().getShort(code[pc++]));break;

            case Move.move_l_vv :   slots.setLong(code[pc++], slots.getLong(code[pc++])); break;
            case Move.move_fld_l_ovv:   slots.getObject(code[pc++]).getSlots().setLong(code[pc++], slots.getLong(code[pc++])); break;
            case Move.move_fld_l_vov:   slots.setLong(code[pc++], slots.getObject(code[pc++]).getSlots().getLong(code[pc++]));break;

            case Move.move_o_vv :   slots.setObject(code[pc++], slots.getObject(code[pc++])); break;
            case Move.move_fld_o_ovv:   slots.getObject(code[pc++]).getSlots().setObject(code[pc++], slots.getObject(code[pc++])); break;
            case Move.move_fld_o_vov:   slots.setObject(code[pc++], slots.getObject(code[pc++]).getSlots().getObject(code[pc++])); break;

            case Move.move_S_vv :   slots.setString(code[pc++], slots.getString(code[pc++])); break;
            case Move.move_fld_S_ovv:   slots.getObject(code[pc++]).getSlots().setString(code[pc++], slots.getString(code[pc++])); break;
            case Move.move_fld_S_vov:   slots.setString(code[pc++], slots.getObject(code[pc++]).getSlots().getString(code[pc++])); break;

            case Move.move_V_vv :   slots.setVoid(code[pc++], slots.getVoid(code[pc++])); break;
            case Move.move_fld_V_ovv:   slots.getObject(code[pc++]).getSlots().setVoid(code[pc++], slots.getVoid(code[pc++])); break;
            case Move.move_fld_V_vov:   slots.setVoid(code[pc++], slots.getObject(code[pc++]).getSlots().getVoid(code[pc++]));break;

            case Move.move_copy_ooC:    copyAssign(slots.getObject(code[pc++]), slots.getObject(code[pc++]), engine.getClass(code[pc++])); break;
        }
        return pc;
    }
    protected void copyAssign(Instance<?> dest, Instance<?> src, AgoClass commonClass) {
        Slots targetSlots = dest.getSlots();
        Slots srcSlots = src.getSlots();
        for (AgoSlotDef slotDef : commonClass.getSlotDefs()) {
            switch (slotDef.getTypeCode().value){
                case INT_VALUE: targetSlots.setInt(slotDef.getIndex(), srcSlots.getInt(slotDef.getIndex())); break;
                case LONG_VALUE: targetSlots.setLong(slotDef.getIndex(), srcSlots.getLong(slotDef.getIndex())); break;
                case DOUBLE_VALUE: targetSlots.setDouble(slotDef.getIndex(), srcSlots.getDouble(slotDef.getIndex())); break;
                case BOOLEAN_VALUE: targetSlots.setBoolean(slotDef.getIndex(), srcSlots.getBoolean(slotDef.getIndex())); break;
                case STRING_VALUE: targetSlots.setString(slotDef.getIndex(), srcSlots.getString(slotDef.getIndex())); break;
                case CHAR_VALUE: targetSlots.setChar(slotDef.getIndex(), srcSlots.getChar(slotDef.getIndex())); break;
                case SHORT_VALUE: targetSlots.setShort(slotDef.getIndex(), srcSlots.getShort(slotDef.getIndex())); break;
                case BYTE_VALUE: targetSlots.setByte(slotDef.getIndex(), srcSlots.getByte(slotDef.getIndex())); break;
                case FLOAT_VALUE: targetSlots.setFloat(slotDef.getIndex(), srcSlots.getFloat(slotDef.getIndex())); break;
                case CLASS_REF_VALUE: targetSlots.setClassRef(slotDef.getIndex(), srcSlots.getClassRef(slotDef.getIndex())); break;
                case OBJECT_VALUE: targetSlots.setObject(slotDef.getIndex(), srcSlots.getObject(slotDef.getIndex())); break;

                default: throw new IllegalArgumentException("Unknown type code: " + slotDef.getTypeCode());
            }
        }
    }

    protected int evaluateConst(Slots slots, int pc, int instruction) {
        switch (instruction){
            case Const.const_i_vc:  slots.setInt(code[pc++], code[pc++]); break;
            case Const.const_fld_i_ovc: slots.getObject(code[pc++]).getSlots().setInt(code[pc++], code[pc++]); break;
            case Const.const_C_vC:  slots.setClassRef(code[pc++], code[pc++]); break;
            case Const.const_fld_C_ovC: slots.getObject(code[pc++]).getSlots().setClassRef(code[pc++], code[pc++]); break;

            case Const.const_B_vc : slots.setBoolean(code[pc++], code[pc++] != 0);  break;
            case Const.const_fld_B_ovc: slots.getObject(code[pc++]).getSlots().setBoolean(code[pc++], code[pc++] != 0); break;

            case Const.const_c_vc : slots.setChar(code[pc++], (char) code[pc++]); break;
            case Const.const_fld_c_ovc: slots.getObject(code[pc++]).getSlots().setChar(code[pc++], (char)code[pc++]);   break;

            case Const.const_f_vc : slots.setFloat(code[pc++], Float.intBitsToFloat(code[pc++])); break;
            case Const.const_fld_f_ovc: slots.getObject(code[pc++]).getSlots().setFloat(code[pc++], Float.intBitsToFloat(code[pc++])); break;

            case Const.const_d_vc : slots.setDouble(code[pc++], toDouble(code[pc++], code[pc++])); break;
            case Const.const_fld_d_ovc: slots.getObject(code[pc++]).getSlots().setDouble(code[pc++], toDouble(code[pc++], code[pc++]));   break;

            case Const.const_b_vc : slots.setByte(code[pc++], (byte)code[pc++]); break;
            case Const.const_fld_b_ovc: slots.getObject(code[pc++]).getSlots().setByte(code[pc++], (byte)code[pc++]);   break;

            case Const.const_s_vc : slots.setShort(code[pc++], (short) code[pc++]); break;
            case Const.const_fld_s_ovc: slots.getObject(code[pc++]).getSlots().setShort(code[pc++], (short) code[pc++]);   break;

            case Const.const_l_vc : slots.setLong(code[pc++], toLong(code[pc++], code[pc++])); break;
            case Const.const_fld_l_ovc: slots.getObject(code[pc++]).getSlots().setLong(code[pc++], toLong(code[pc++], code[pc++])); break;

            case Const.const_n_vc : slots.setObject(code[pc++], null); break;
            case Const.const_fld_n_ovc: slots.getObject(code[pc++]).getSlots().setObject(code[pc++], null); break;

            case Const.const_S_vc : slots.setString(code[pc++], engine.toString(code[pc++])); break;
            case Const.const_fld_S_ovc: slots.getObject(code[pc++]).getSlots().setString(code[pc++], engine.toString(code[pc++])); break;
        }
        return pc;
    }

    protected int evaluateCast(Slots slots, int pc, int instruction){
        switch (instruction){
            // ------------------------------------------- cast ----------------------------------
            case Cast.c2f: slots.setFloat(code[pc++], slots.getChar(code[pc++])); break;
            case Cast.c2d: slots.setDouble(code[pc++], slots.getChar(code[pc++])); break;
            case Cast.c2b: slots.setByte(code[pc++], (byte)slots.getChar(code[pc++])); break;
            case Cast.c2s: slots.setShort(code[pc++], (short) slots.getChar(code[pc++])); break;
            case Cast.c2i: slots.setInt(code[pc++], slots.getChar(code[pc++])); break;
            case Cast.c2l: slots.setLong(code[pc++], slots.getChar(code[pc++])); break;
            case Cast.c2c: slots.setChar(code[pc++], slots.getChar(code[pc++])); break;
            case Cast.c2S: slots.setString(code[pc++], String.valueOf(slots.getChar(code[pc++]))); break;
            case Cast.c2B: slots.setBoolean(code[pc++], slots.getChar(code[pc++]) != 0); break;

            case Cast.f2f: slots.setFloat(code[pc++], slots.getFloat(code[pc++])); break;
            case Cast.f2c: slots.setChar(code[pc++], (char) slots.getFloat(code[pc++])); break;
            case Cast.f2d: slots.setDouble(code[pc++], slots.getFloat(code[pc++])); break;
            case Cast.f2b: slots.setByte(code[pc++], (byte)slots.getFloat(code[pc++])); break;
            case Cast.f2s: slots.setShort(code[pc++], (short) slots.getFloat(code[pc++])); break;
            case Cast.f2i: slots.setInt(code[pc++], (int)slots.getFloat(code[pc++])); break;
            case Cast.f2l: slots.setLong(code[pc++], (long)slots.getFloat(code[pc++])); break;
            case Cast.f2S: slots.setString(code[pc++], String.valueOf(slots.getFloat(code[pc++]))); break;
            case Cast.f2B: slots.setBoolean(code[pc++], slots.getFloat(code[pc++]) != 0); break;

            case Cast.d2d: slots.setDouble(code[pc++], slots.getDouble(code[pc++])); break;
            case Cast.d2c: slots.setChar(code[pc++], (char)slots.getDouble(code[pc++])); break;
            case Cast.d2f: slots.setFloat(code[pc++], (float) slots.getDouble(code[pc++])); break;
            case Cast.d2b: slots.setDouble(code[pc++], (byte)slots.getDouble(code[pc++])); break;
            case Cast.d2s: slots.setShort(code[pc++], (short) slots.getDouble(code[pc++])); break;
            case Cast.d2i: slots.setInt(code[pc++], (int) slots.getDouble(code[pc++])); break;
            case Cast.d2l: slots.setLong(code[pc++], (long) slots.getDouble(code[pc++])); break;
            case Cast.d2S: slots.setString(code[pc++], String.valueOf(slots.getDouble(code[pc++]))); break;
            case Cast.d2B: slots.setBoolean(code[pc++], slots.getDouble(code[pc++]) != 0); break;

            case Cast.b2b: slots.setByte(code[pc++], slots.getByte(code[pc++])); break;
            case Cast.b2c: slots.setChar(code[pc++], (char)slots.getByte(code[pc++])); break;
            case Cast.b2f: slots.setFloat(code[pc++], slots.getByte(code[pc++])); break;
            case Cast.b2d: slots.setDouble(code[pc++], slots.getByte(code[pc++])); break;
            case Cast.b2s: slots.setShort(code[pc++], slots.getByte(code[pc++])); break;
            case Cast.b2i: slots.setInt(code[pc++], slots.getByte(code[pc++])); break;
            case Cast.b2l: slots.setLong(code[pc++], slots.getByte(code[pc++])); break;
            case Cast.b2S: slots.setString(code[pc++], String.valueOf(slots.getByte(code[pc++]))); break;
            case Cast.b2B: slots.setBoolean(code[pc++], slots.getByte(code[pc++]) != 0); break;

            case Cast.s2s: slots.setShort(code[pc++], slots.getShort(code[pc++])); break;
            case Cast.s2c: slots.setChar(code[pc++], (char)slots.getShort(code[pc++])); break;
            case Cast.s2f: slots.setFloat(code[pc++], slots.getShort(code[pc++])); break;
            case Cast.s2d: slots.setDouble(code[pc++], slots.getShort(code[pc++])); break;
            case Cast.s2b: slots.setByte(code[pc++], (byte)slots.getShort(code[pc++])); break;
            case Cast.s2i: slots.setInt(code[pc++], slots.getShort(code[pc++])); break;
            case Cast.s2l: slots.setLong(code[pc++], slots.getShort(code[pc++])); break;
            case Cast.s2S: slots.setString(code[pc++], String.valueOf(slots.getShort(code[pc++]))); break;
            case Cast.s2B: slots.setBoolean(code[pc++], slots.getShort(code[pc++]) != 0); break;

            case Cast.i2i: slots.setInt(code[pc++], slots.getInt(code[pc++])); break;
            case Cast.i2c: slots.setChar(code[pc++], (char) slots.getInt(code[pc++])); break;
            case Cast.i2f: slots.setFloat(code[pc++], slots.getInt(code[pc++])); break;
            case Cast.i2d: slots.setDouble(code[pc++], slots.getInt(code[pc++])); break;
            case Cast.i2b: slots.setByte(code[pc++], (byte)slots.getInt(code[pc++])); break;
            case Cast.i2s: slots.setShort(code[pc++], (short) slots.getInt(code[pc++])); break;
            case Cast.i2l: slots.setLong(code[pc++], slots.getInt(code[pc++])); break;
            case Cast.i2S: slots.setString(code[pc++], String.valueOf(slots.getInt(code[pc++]))); break;
            case Cast.i2B: slots.setBoolean(code[pc++], slots.getInt(code[pc++]) != 0); break;

            case Cast.l2l: slots.setLong(code[pc++], slots.getLong(code[pc++])); break;
            case Cast.l2c: slots.setChar(code[pc++], (char) slots.getLong(code[pc++])); break;
            case Cast.l2f: slots.setFloat(code[pc++], slots.getLong(code[pc++])); break;
            case Cast.l2d: slots.setDouble(code[pc++], slots.getLong(code[pc++])); break;
            case Cast.l2b: slots.setByte(code[pc++], (byte) slots.getLong(code[pc++])); break;
            case Cast.l2s: slots.setShort(code[pc++], (short) slots.getLong(code[pc++])); break;
            case Cast.l2i: slots.setInt(code[pc++], (int) slots.getLong(code[pc++])); break;
            case Cast.l2S: slots.setString(code[pc++], String.valueOf(slots.getLong(code[pc++]))); break;
            case Cast.l2B: slots.setBoolean(code[pc++], slots.getLong(code[pc++]) != 0); break;

            case Cast.B2B: slots.setBoolean(code[pc++], slots.getBoolean(code[pc++])); break;
            case Cast.B2S: slots.setString(code[pc++], String.valueOf(slots.getBoolean(code[pc++]))); break;
            case Cast.B2c : slots.setChar(code[pc++], slots.getBoolean(code[pc++]) ? 't' : 'f'); break;
            case Cast.B2f : slots.setFloat(code[pc++], slots.getBoolean(code[pc++]) ? 1 : 0); break;
            case Cast.B2i : slots.setInt(code[pc++], slots.getBoolean(code[pc++]) ? 1 : 0); break;
            case Cast.B2l : slots.setLong(code[pc++], slots.getBoolean(code[pc++]) ? 1 : 0); break;
            case Cast.B2d : slots.setDouble(code[pc++], slots.getBoolean(code[pc++]) ? 1 : 0); break;
            case Cast.B2b : slots.setByte(code[pc++], (byte)(slots.getBoolean(code[pc++]) ? 1 : 0)); break;
            case Cast.B2s : slots.setShort(code[pc++], (short)(slots.getBoolean(code[pc++]) ? 1 : 0)); break;

            case Cast.S2B: slots.setBoolean(code[pc++], StringUtils.isNotEmpty(slots.getString(code[pc++]))); break;
            case Cast.S2S: slots.setString(code[pc++], slots.getString(code[pc++])); break;
            case Cast.S2c : slots.setChar(code[pc++], stringToChar(slots.getString(code[pc++]))); break;
            case Cast.S2f : slots.setFloat(code[pc++], Float.parseFloat(slots.getString(code[pc++]))); break;
            case Cast.S2i : slots.setInt(code[pc++], Integer.parseInt(slots.getString(code[pc++]))); break;
            case Cast.S2l : slots.setLong(code[pc++], Long.parseLong(slots.getString(code[pc++]))); break;
            case Cast.S2d : slots.setDouble(code[pc++], Double.parseDouble(slots.getString(code[pc++]))); break;
            case Cast.S2b : slots.setByte(code[pc++], Byte.parseByte(slots.getString(code[pc++]))); break;
            case Cast.S2s : slots.setShort(code[pc++], Short.parseShort(slots.getString(code[pc++]))); break;

            case Cast.o2B: slots.setBoolean(code[pc++], slots.getObject(code[pc++])  != null); break;

            case Cast.C2S: slots.setString(code[pc++], String.valueOf(engine.getClass(slots.getClassRef(code[pc++])))); break;

            case Cast.cast_object_vvtC:
                if(!castObject(slots, code[pc++], slots.getObject(code[pc++]), code[pc++], getClass(code[pc++]))){
                    return code.length;
                }
                break;
            case Cast.cast_to_any_vtCvtC:
                if(!castToAny(slots, code[pc++], code[pc++], getClass(code[pc++]), code[pc++], code[pc++], getClass(code[pc++]))){
                    return code.length;
                }
                break;

            case Cast.C2sbr_oCC:
                slots.setObject(code[pc++], engine.createScopedClassRef(this, engine.getClass(code[pc++]), (AgoClass)slots.getObject(code[pc++])));
                break;
            case Cast.sbr2C_Cov:
                slots.setObject(code[pc++], extractScopedClass(slots.getObject(code[pc++]), code[pc++]));
                break;

            default:
                throw new UnsupportedOperationException("unsupported cast " + OpCode.getName(instruction));
        }
        return pc;
    }

    private AgoClass getClass(int classId) {
        if(classId == -1) return null;
        return engine.getClass(classId);
    }

    private Instance<?> extractScopedClass(Instance<?> scopedClassIntervalInstance, int slot){
        if(scopedClassIntervalInstance == null){
            raiseException("lang.NullPointerException", "scoped class interval instance is null");
            return null;
        }
        return scopedClassIntervalInstance.getSlots().getObject(slot);
    }

    private boolean castObject(Slots slots, int index, Instance<?> object, int typeCode, AgoClass expectedClass) {
        if(object == null){
            slots.setObject(index, null);
            return true;
        }
        if(!engine.validateClassInheritance(this,object.agoClass, expectedClass)) return false;
        slots.setObject(index, object);
        return true;
    }

    private boolean castToAny(Slots slots, int targetIndex, int targetTypeCode, AgoClass targetClass,
                                       int srcSlotIndex, int srcTypeCode, AgoClass srcClass) {
        if(srcTypeCode != TypeCode.OBJECT_VALUE){   // primitive src
            if(targetTypeCode != TypeCode.OBJECT_VALUE){
                // primitive to primitive;
                castPrimitiveToPrimitive(slots, targetIndex, targetTypeCode, srcSlotIndex, srcTypeCode);
            } else {
                // primitive to object
                if(targetClass instanceof AgoEnum agoEnum){
                    var instance = boxEnum(slots, agoEnum, srcSlotIndex, srcTypeCode);
                    if(instance == null){
                        raiseException("lang.ClassCastException", "'%s' can't cast to '%s'".formatted(TypeCode.of(srcTypeCode), agoEnum.getFullname()));
                        return false;
                    }
                    slots.setObject(targetIndex, instance);
                } else {
                    var instance = engine.getBoxer().boxAny(slots, srcSlotIndex, srcTypeCode);
                    engine.validateClassInheritance(this,instance.getAgoClass(), targetClass);
                    slots.setObject(targetIndex, instance);
                }
            }
        } else {
            if(targetTypeCode != OBJECT_VALUE){
                engine.getBoxer().forceUnbox(this,targetIndex,slots.getObject(srcSlotIndex),targetTypeCode);
            } else {
                castObject(slots, targetIndex,slots.getObject(srcSlotIndex),targetTypeCode,targetClass);
            }
        }
        return true;
    }

    public Instance<?> boxEnum(Slots slots, AgoEnum agoEnum, int srcIndex, int srcTypeCode) {
        switch (srcTypeCode) {
            case INT_VALUE:
                return agoEnum.findMember(slots.getInt(srcIndex));      // call valueOf# is fine too
            case BYTE_VALUE:
                return agoEnum.findMember(slots.getByte(srcIndex));
            case SHORT_VALUE:
                return agoEnum.findMember(slots.getShort(srcIndex));
            case LONG_VALUE:
                return agoEnum.findMember(slots.getLong(srcIndex));
            case FLOAT_VALUE:
                return agoEnum.findMember(slots.getFloat(srcIndex));
            case DOUBLE_VALUE:
                return agoEnum.findMember(slots.getDouble(srcIndex));
            case BOOLEAN_VALUE:
                return agoEnum.findMember(slots.getBoolean(srcIndex) ? 1 : 0);
            case CHAR_VALUE:
                return agoEnum.findMember((int) slots.getChar(srcIndex));
            case STRING_VALUE:
                return agoEnum.findMember(slots.getString(srcIndex));
        }
        return null;
    }

    public void raiseException(String exceptionClassName, String message){
        var ExceptionClass = engine.getClass(exceptionClassName);
        var exception = engine.createInstance(null, ExceptionClass, this );
        exception.invokeMethod(this, runSpace, ExceptionClass.findMethod("new#message"), message);
        if(!this.handleException(exception))
            this.pc = code.length;
    }

    private char stringToChar(String s){
        if(s.length() == 0) return '\0';
        return s.charAt(0);
    }

    private void castPrimitiveToPrimitive(Slots slots, int targetIndex, int targetTypeCode, int srcIndex, int srcTypeCode) {
        switch (targetTypeCode){
            case INT_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setInt(targetIndex, slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setInt(targetIndex, Integer.parseInt(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setInt(targetIndex, (int)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setInt(targetIndex, slots.getBoolean(srcIndex)? 1 : 0);
                        return;
                    case DOUBLE_VALUE:
                        slots.setInt(targetIndex, (int)slots.getDouble(srcIndex));
                        return;
                    case BYTE_VALUE:
                        slots.setInt(targetIndex, slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setInt(targetIndex, (int)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setInt(targetIndex, (int)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE: // Convert short to int
                        slots.setInt(targetIndex, slots.getShort(srcIndex));
                        return;
                }
                break;
            case STRING_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getInt(srcIndex)));
                        return;
                    case STRING_VALUE:
                        slots.setString(targetIndex, slots.getString(srcIndex));
                        return;
                    case LONG_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getLong(srcIndex)));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getBoolean(srcIndex)));
                        return;
                    case DOUBLE_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getDouble(srcIndex)));
                        return;
                    case BYTE_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getByte(srcIndex)));
                        return;
                    case FLOAT_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getFloat(srcIndex)));
                        return;
                    case CHAR_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getChar(srcIndex)));
                        return;
                    case SHORT_VALUE:
                        slots.setString(targetIndex, String.valueOf(slots.getShort(srcIndex)));
                        return;
                }
                break;
            case LONG_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setLong(targetIndex, slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setLong(targetIndex, Long.parseLong(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setLong(targetIndex, slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setLong(targetIndex, slots.getBoolean(srcIndex)? 1L : 0L);
                        return;
                    case DOUBLE_VALUE:
                        slots.setLong(targetIndex, (long)slots.getDouble(srcIndex));
                        return;
                    case BYTE_VALUE:
                        slots.setLong(targetIndex, slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setLong(targetIndex, (long)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setLong(targetIndex, (long)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setLong(targetIndex, slots.getShort(srcIndex));
                        return;
                }
                break;
            case BOOLEAN_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setBoolean(targetIndex, slots.getInt(srcIndex) != 0);
                        return;
                    case STRING_VALUE:
                        slots.setBoolean(targetIndex, StringUtils.isNotEmpty(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setBoolean(targetIndex, slots.getLong(srcIndex) != 0L);
                        return;
                    case BOOLEAN_VALUE:
                        slots.setBoolean(targetIndex, slots.getBoolean(srcIndex));
                        return;
                    case DOUBLE_VALUE:
                        slots.setBoolean(targetIndex, slots.getDouble(srcIndex) != 0.0);
                        return;
                    case BYTE_VALUE:
                        slots.setBoolean(targetIndex, slots.getByte(srcIndex) != 0);
                        return;
                    case FLOAT_VALUE:
                        slots.setBoolean(targetIndex, slots.getFloat(srcIndex) != 0.0f);
                        return;
                    case CHAR_VALUE:
                        slots.setBoolean(targetIndex, slots.getChar(srcIndex) != 'f');
                        return;
                    case SHORT_VALUE:
                        slots.setBoolean(targetIndex, slots.getShort(srcIndex) != 0);
                        return;
                }
                break;
            case DOUBLE_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setDouble(targetIndex, slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setDouble(targetIndex, Double.parseDouble(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setDouble(targetIndex, (double)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setDouble(targetIndex, slots.getBoolean(srcIndex)? 1.0 : 0.0);
                        return;
                    case DOUBLE_VALUE:
                        slots.setDouble(targetIndex, slots.getDouble(srcIndex));
                        return;
                    case BYTE_VALUE:
                        slots.setDouble(targetIndex, slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setDouble(targetIndex, (double)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setDouble(targetIndex, (double)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setDouble(targetIndex, slots.getShort(srcIndex));
                        return;
                }
                break;
            case BYTE_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setByte(targetIndex, Byte.parseByte(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setByte(targetIndex, slots.getBoolean(srcIndex)? (byte)1 : (byte)0);
                        return;
                    case DOUBLE_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getDouble(srcIndex));
                        return;
                    case BYTE_VALUE:
                        slots.setByte(targetIndex, slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setByte(targetIndex, (byte)slots.getShort(srcIndex));
                        return;
                }
                break;
            case FLOAT_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setFloat(targetIndex, Float.parseFloat(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setFloat(targetIndex, slots.getBoolean(srcIndex)? 1.0f : 0.0f);
                        return;
                    case DOUBLE_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getDouble(srcIndex));
                        return;
                    case BYTE_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setFloat(targetIndex, slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setFloat(targetIndex, (float)slots.getShort(srcIndex));
                        return;
                }
                break;
            case CHAR_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setChar(targetIndex, (char)slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setChar(targetIndex, stringToChar(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setChar(targetIndex, (char)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setChar(targetIndex, slots.getBoolean(srcIndex)? 't' : 'f');
                        return;
                    case DOUBLE_VALUE:
                        slots.setChar(targetIndex, (char)slots.getDouble(srcIndex));
                        return;
                    case BYTE_VALUE:
                        slots.setChar(targetIndex, (char)slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setChar(targetIndex, (char)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setChar(targetIndex, slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setChar(targetIndex, (char)slots.getShort(srcIndex));
                        return;
                }
                break;
            case SHORT_VALUE:
                switch(srcTypeCode){
                    case INT_VALUE:
                        slots.setShort(targetIndex, (short)slots.getInt(srcIndex));
                        return;
                    case STRING_VALUE:
                        slots.setShort(targetIndex, Short.parseShort(slots.getString(srcIndex)));
                        return;
                    case LONG_VALUE:
                        slots.setShort(targetIndex, (short)slots.getLong(srcIndex));
                        return;
                    case BOOLEAN_VALUE:
                        slots.setShort(targetIndex, slots.getBoolean(srcIndex)? (short)1 : (short)0);
                        return;
                    case DOUBLE_VALUE:
                        slots.setShort(targetIndex, (short)slots.getDouble(srcIndex));
                        return;
                    case BYTE_VALUE:
                        slots.setShort(targetIndex, (short)slots.getByte(srcIndex));
                        return;
                    case FLOAT_VALUE:
                        slots.setShort(targetIndex, (short)slots.getFloat(srcIndex));
                        return;
                    case CHAR_VALUE:
                        slots.setShort(targetIndex, (short)slots.getChar(srcIndex));
                        return;
                    case SHORT_VALUE:
                        slots.setShort(targetIndex, slots.getShort(srcIndex));
                        return;
                }
                break;
        }
    }

    private static boolean and(boolean b1, boolean b2) {
        return b1 && b2;
    }
    private static boolean or(boolean b1, boolean b2) {
        return b1 || b2;
    }

    private static boolean greaterEquals(String string1, String string2) {
        var r = StringUtils.compare(string1, string2);
        return r == 0 || r == 1;
    }

    private static boolean littleEquals(String string1, String string2) {
        var r = StringUtils.compare(string1, string2);
        return r == 0 || r == -1;
    }

    protected Instance<?> getScope(int depth) {
        if(depth == 0) return this;
        if(depth == 1) return this.getParentScope();
        if(depth == 2) return this.getParentScope().getParentScope();
        Instance<?> r = this;
        for(var i=0; i < depth; i++){
            r = r.getParentScope();
        }
        return r;
    }

    public boolean handleException(Instance<?> exception) {
        var pc = resolveExceptionHandler(this.pc, exception);
        this.pc = pc;
        if(pc == -1) {
            this.finishException(exception, true);
            return false;
        } else {
            this.finishException(exception, false);
            return true;
        }
    }

    private PreservableSearcher<SourceMapEntry> preservableSearcher = null;

    @Override
    public SourceLocation resolveSourceLocation() {
        if(preservableSearcher == null) preservableSearcher = new PreservableSearcher<>(Arrays.asList(this.agoClass.getSourceMap()));
        SourceMapEntry entry = preservableSearcher.search(s -> s.codeOffset() >= this.pc);
        if(entry != null) return entry.sourceLocation();
        return this.agoClass.getSourceLocation();
    }

    public boolean isFinished() {
        return this.pc >= this.code.length;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public void interrupt() {
        this.setPc(this.code.length);
    }

    public int getPc() {
        return pc;
    }
}
