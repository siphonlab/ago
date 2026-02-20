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

import org.agrona.collections.IntArrayList;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.SourceMapEntry;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.expression.Creator;
import org.siphonlab.ago.compiler.expression.Invoke.InvokeMode;
import org.siphonlab.ago.opcode.arithmetic.IncDec;
import org.siphonlab.ago.opcode.arithmetic.Neg;
import org.siphonlab.ago.opcode.compare.Equals;
import org.siphonlab.ago.opcode.compare.GreaterEquals;
import org.siphonlab.ago.opcode.compare.InstanceOf;
import org.siphonlab.ago.opcode.compare.NotEquals;
import org.siphonlab.ago.opcode.logic.BitNot;
import org.siphonlab.ago.opcode.logic.Not;
import org.siphonlab.ago.opcode.*;
import org.siphonlab.ago.compiler.statement.Label;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.literal.*;

import java.util.LinkedList;
import java.util.List;

import static org.siphonlab.ago.TypeCode.*;

public class CodeBuffer {
    private final IntArrayList ls = new IntArrayList();

    public int[] toArray(){
        return ls.toIntArray();
    }

    private final List<SourceMapEntry> sourceMapEntries = new LinkedList<>();

    private SourceMapEntry lastSourceMapEntry = null;

    public List<SourceMapEntry> getSourceMapEntries() {
        return sourceMapEntries;
    }

    private SourceLocation currSourceLocation;
    public void setSourceLocation(SourceLocation sourceLocation) {
        if(sourceLocation.equals(currSourceLocation)) return;
        currSourceLocation = sourceLocation;
        if(lastSourceMapEntry != null && lastSourceMapEntry.codeOffset() == ls.size()){     // no code output for this source location
            sourceMapEntries.removeLast();
        }
        sourceMapEntries.add(lastSourceMapEntry = new SourceMapEntry(ls.size(),currSourceLocation));
    }

    private void slot(SlotDef slotDef){
        ls.addInt(slotDef.getIndex());
    }

    public void biOperate(int opCode, TypeCode typeCode, SlotDef slot1, SlotDef slot2, SlotDef target) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        if(target.getIndex() == slot1.getIndex() && target.getClassDef() == slot1.getClassDef()){
            ls.addInt(opCode | (typeCode.getValue() << 16) | 0x0302);        // add_vv
            slot(slot1);
            slot(slot2);
        } else if(target.getIndex() == slot2.getIndex()
                            && target.getClassDef() == slot1.getClassDef()
                            && OpCode.isSymmetrical(opCode)) {

            ls.addInt(opCode | (typeCode.getValue() << 16) | 0x0302);        // add_vv
            slot(slot2);
            slot(slot1);
        } else {
            ls.addInt(opCode | (typeCode.getValue() << 16) | 0x0403);        // add_vvv
            slot(target);
            slot(slot1);
            slot(slot2);
        }
        sizeVerifier.verify();
    }

    private static int additionSizeOf(TypeCode typeCode) {
        int additionSize = 0;
        if (typeCode == DOUBLE || typeCode == LONG) {
            additionSize = 1;
        }
        return additionSize;
    }


    public void biOperateVariableLiteral(int opCode, TypeCode typeCode, SlotDef left, Literal<?> right, SlotDef target) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        boolean isSameSlot = (target.getIndex() == left.getIndex());
        int additionSize = 0;
        if(typeCode == DOUBLE || typeCode == LONG){
            additionSize = 1;
        }
        if(isSameSlot){
            ls.addInt(opCode | (typeCode.getValue() << 16) | (0x0102 + additionSize));        // add_vc
            slot(target);
        } else {
            ls.addInt(opCode | (typeCode.getValue() << 16) | (0x0203 + additionSize));        // add_vvc
            slot(target);
            slot(left);
        }
        literal(right);
        sizeVerifier.verify();
    }

    public void biOperateLiteralVariable(int opCode, TypeCode typeCode, Literal<?> left, SlotDef right, SlotDef target) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        boolean isSameSlot = (target.getIndex() == right.getIndex());
        int additionSize = 0;
        if(typeCode == DOUBLE || typeCode == LONG){
            additionSize = 1;
        }
        ls.addInt(opCode | (typeCode.getValue() << 16) | (0x0503 + additionSize));        // add_vcv
        slot(target);
        literal(left);
        slot(right);
        sizeVerifier.verify();
    }

    public void compareVariableLiteral(int opCode, TypeCode typeCode, SlotDef left, Literal<?> right, SlotDef target) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(opCode | (typeCode.getValue() << 16) | (0x0203 + additionSizeOf(typeCode)));        // equals_i_vvc
        slot(target);
        slot(left);
        literal(right);
        sizeVerifier.verify();
    }

    public void compareVariables(int opCode, TypeCode typeCode, SlotDef slot1, SlotDef slot2, SlotDef target) {
        ls.addInt(opCode | (typeCode.getValue() << 16) | 0x0403);        // cmp_vvv
        slot(target);
        slot(slot1);
        slot(slot2);
    }


    private void literal(Literal<?> literal){
        TypeCode typeCode = literal.inferType().getTypeCode();
        switch (typeCode.value){
            case VOID_VALUE:
                break;
            case NULL_VALUE:
                throw new RuntimeException("void or null should not enter here");
            case CHAR_VALUE:
                ls.addInt(((CharLiteral)literal).value);
                break;
            case BOOLEAN_VALUE:
                ls.addInt(((BooleanLiteral)literal).value ? 1 : 0);
                break;
            case FLOAT_VALUE:
                Float f = ((FloatLiteral) literal).value;
                ls.addInt(Float.floatToIntBits(f));
                break;
            case DOUBLE_VALUE:
                var d  = ((DoubleLiteral) literal).value;
                var l = Double.doubleToLongBits(d);
                ls.addInt((int)(l >>> 32));
                ls.addInt((int)(l));
                break;
            case BYTE_VALUE:
                ls.addInt((int)((ByteLiteral)literal).value);
                break;
            case SHORT_VALUE:
                ls.addInt((int)((ShortLiteral)literal).value);
                break;
            case INT_VALUE:
                ls.addInt(((IntLiteral)literal).value);
                break;
            case LONG_VALUE:
                var lng  = (long)((LongLiteral) literal).value;
                ls.addInt((int)(lng >>> 32));
                ls.addInt((int)(lng));
                break;
            case STRING_VALUE:
                ls.addInt(((StringLiteral)literal).value);
                break;
            case CLASS_REF_VALUE:
                ls.addInt(((ClassRefLiteral)literal).value);
                break;
            default:
                throw new IllegalArgumentException(typeCode + " not support");
        }
    }

    public void new_(SlotDef target, SlotDef parentScope, Creator.NewProps newProps) {
        if(parentScope == null){
            new_(target, newProps);
            return;
        }
        int op;
        if (newProps.isGenericCode())
            op = NewGeneric.newg_child_voC;
        else {
            if (newProps.forGenericInstantiation())
                op = NewGeneric.newG_child_voC;
            else {
                op = newProps.isNative() ? New.newn_child_voC : New.new_child_voC;
            }
        }
        ls.addInt(op);
        slot(target);
        slot(parentScope);
        ls.addInt(newProps.className());
    }

    public void new_(SlotDef target, Creator.NewProps props) {
        int op;
        if (props.isGenericCode()) {
            op = NewGeneric.newg_vC;
        } else {
            if (props.forGenericInstantiation())
                op = NewGeneric.newG_vC;
            else
                op = props.isNative() ? New.newn_vC : New.new_vC;
        }
        ls.addInt(op);
        slot(target);
        ls.addInt(props.className());
    }

    public void new_method(SlotDef target, SlotDef parentScope, int methodSimpleName, Creator.NewProps props) {
        int opcode;
        if (props.isGenericCode()) {
            opcode = props.isTraitInScope()? NewGeneric.newg_method_voTm : NewGeneric.newg_method_voCm;       // for the instantiation class of Interface/Trait, it will replace with new_method_voIc in class loader
        } else {
            if (props.forGenericInstantiation()) {
                opcode = props.isTraitInScope()? NewGeneric.newG_method_voTm : NewGeneric.newG_method_voCm;
            } else {
                opcode = props.isInterfaceInvoke() ? New.new_method_voIm : New.new_method_voCm;
            }
        }
        ls.addInt(opcode);
        slot(target);
        slot(parentScope);
        ls.addInt(props.className());
        ls.addInt(methodSimpleName);
    }

    public void new_method_static(SlotDef target, int methodSimpleName, Creator.NewProps props) {
        ls.addInt(props.isGenericCode() ? NewGeneric.newg_cls_method_vCm : props.forGenericInstantiation()? NewGeneric.newG_cls_method_vCm : New.new_cls_method_vCm);
        slot(target);
        ls.addInt(props.className());
        ls.addInt(methodSimpleName);
    }

    public void new_scope_child(SlotDef target,
                                int parentScopeDepth,
                                Creator.NewProps newProps){
        if (newProps.isGenericCode())
            ls.addInt(NewGeneric.newg_scope_child_vcC);
        else {
            if (newProps.forGenericInstantiation())
                ls.addInt(NewGeneric.newG_scope_child_vcC);
            else
                ls.addInt(newProps.isNative() ? New.newn_scope_child_vcC : New.new_scope_child_vcC);
        }
        slot(target);
        ls.addInt(parentScopeDepth);
        ls.addInt(newProps.className());
    }

    public void new_scope_method(SlotDef target, int parentScopeDepth, boolean fixed, int methodSimpleName, Creator.NewProps props){
        int opcode;
        if(props.isGenericCode()){
            opcode = fixed ? NewGeneric.newg_scope_method_fix_vcCm : NewGeneric.newg_scope_method_vcCm;
        } else if(props.forGenericInstantiation()){
            opcode = fixed ? NewGeneric.newG_scope_method_fix_vcCm : NewGeneric.newG_scope_method_vcCm;
        } else {
            opcode = fixed ? New.new_scope_method_fix_vcCm : New.new_scope_method_vcCm;
        }
        ls.addInt(opcode);
        slot(target);
        ls.addInt(parentScopeDepth);
        ls.addInt(props.className());
        ls.addInt(methodSimpleName);
    }

    public void new_bound_class(SlotDef target, SlotDef scopeBoundClass){
        ls.addInt(New.new_vo);
        slot(target);
        slot(scopeBoundClass);
    }

    /**
     * auto find constructor to match the args to create instance
     * @param target
     * @param scopeBoundClass
     * @param argumentsTuple
     */
    public void new_dynamic(SlotDef target, SlotDef scopeBoundClass, SlotDef argumentsTuple){
        ls.addInt(New.new_dynamic_voa);
        slot(target);
        slot(scopeBoundClass);
        slot(argumentsTuple);
    }

    /**
     * auto find method for the simple name that matches args
     * @param target
     * @param parentScopeInstance
     * @param methodSimpleNameStr  the string of method simple name, not index in `ClassInstance.methods`
     * @param argumentsTuple
     */
    public void new_dynamic_method(SlotDef target, SlotDef parentScopeInstance, int methodSimpleNameStr, SlotDef argumentsTuple){
        ls.addInt(New.new_scope_method_fix_voma);
        slot(target);
        slot(parentScopeInstance);
        ls.addInt(methodSimpleNameStr);
        slot(argumentsTuple);
    }

    public void assignLiteral(SlotDef targetSlot, Literal<?> value) {
        if(value instanceof NullLiteral){
            ls.addInt(Const.const_n_vc);
            slot(targetSlot);
            return;
        }
        TypeCode typeCode = targetSlot.getTypeCode();
        ls.addInt(Const.KIND_CONST | (typeCode.getValue() << 16) | 0x01_02 + additionSizeOf(typeCode));      // const_i_vc
        slot(targetSlot);
        literal(value);
    }

    public void assignLiteral(SlotDef targetInstanceSlot, SlotDef targetSlot, Literal<?> value) {
        if (value instanceof NullLiteral) {
            slot(targetInstanceSlot);
            ls.addInt(Const.const_fld_n_ovc);
            slot(targetSlot);
            return;
        }
        TypeCode typeCode = targetSlot.getTypeCode();
        ls.addInt(Const.KIND_CONST | (typeCode.getValue() << 16) | 0x02_03 + additionSizeOf(typeCode));    // const_fld_i_ovc
        slot(targetInstanceSlot);
        slot(targetSlot);
        literal(value);
    }

    public void assign(SlotDef targetSlot, TypeCode typeCode, SlotDef srcSlot) {
        ls.addInt(Move.KIND_MOVE | (typeCode.getValue() << 16) | 0x0102);       // move_i_vv
        slot(targetSlot);
        slot(srcSlot);
    }

    public void assign(SlotDef targetInstanceSlot, SlotDef targetSlot, TypeCode typeCode, SlotDef srcSlot) {
        ls.addInt(Move.KIND_MOVE | (typeCode.getValue() << 16) | 0x0203);       // move_fld_i_ovv
        slot(targetInstanceSlot);
        slot(targetSlot);
        slot(srcSlot);
    }

    public void assign(SlotDef targetSlot, TypeCode typeCode, SlotDef srcInstanceSlot, SlotDef srcSlot) {
        ls.addInt(Move.KIND_MOVE | (typeCode.getValue() << 16) | 0x0303);       // move_fld_i_vov
        slot(targetSlot);
        slot(srcInstanceSlot);
        slot(srcSlot);
    }

    public void copyAssign(SlotDef destObject, SlotDef srcObject, int className){
        ls.addInt(Move.move_copy_ooC);
        slot(destObject);
        slot(srcObject);
        ls.addInt(className);
    }

    public void invokeAsync(InvokeMode mode, SlotDef functionInstanceSlot, SlotDef resultSlot) {
        var op = switch (mode){
            case Spawn -> Invoke.spawn_vv;
            case Fork -> Invoke.fork_vv;
            default -> throw new IllegalStateException("Unexpected value: " + mode);
        };
        ls.addInt(op);
        slot(functionInstanceSlot);
        slot(resultSlot);
    }

    public void invoke(InvokeMode mode, SlotDef functionInstanceSlot) {
        var op = switch (mode) {
            case Invoke -> Invoke.invoke_v;
            case Spawn -> Invoke.spawn_v;
            case Fork -> Invoke.fork_v;
            case Await -> Invoke.await_v;
        };
        ls.addInt(op);
        slot(functionInstanceSlot);
    }

    public void invokeAsyncViaContext(InvokeMode mode, SlotDef functionInstanceSlot, SlotDef resultSlot, SlotDef forkContext) {
        var op = switch (mode){
            case Spawn -> Invoke.spawnc_vvo;
            case Fork -> Invoke.forkc_vvo;
            default -> throw new IllegalStateException("Unexpected value: " + mode);
        };
        ls.addInt(op);
        slot(functionInstanceSlot);
        slot(resultSlot);
        slot(forkContext);
    }

    public void invokeAsyncViaContext(InvokeMode mode, SlotDef functionInstanceSlot, SlotDef forkContext) {
        var op = switch (mode) {
            case Spawn -> Invoke.spawnc_vo;
            case Fork -> Invoke.forkc_vo;
            case Await -> Invoke.awaitc_vo;
            default -> throw new IllegalStateException("Unexpected value: " + mode);
        };
        ls.addInt(op);
        slot(functionInstanceSlot);
        slot(forkContext);
    }

    public void return_v(SlotDef slot) {
        ls.addInt(Return.return_v | (slot.getTypeCode().getValue() << 16));
        slot(slot);
    }

    public void return_c(Literal literalExpressionResult) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        int additionSize = 0;
        var typeCode = literalExpressionResult.inferType().getTypeCode();
        if (typeCode == TypeCode.DOUBLE || typeCode == TypeCode.LONG) {
            additionSize = 1;
        } else if (typeCode == TypeCode.VOID || typeCode == TypeCode.NULL) {
            additionSize = -1;
        }
        if (literalExpressionResult instanceof NullLiteral) {
            ls.addInt(Return.return_n);
        } else {
            ls.addInt(Return.KIND_RETURN | (literalExpressionResult.inferType().getTypeCode().getValue() << 16) | (0x0101 + additionSize));
            literal(literalExpressionResult);
        }
        sizeVerifier.verify();
    }

    public void accept(SlotDef slot) {
        ls.addInt(Accept.accept_v | (slot.getTypeCode().getValue() << 16));
        slot(slot);
    }

    public void acceptAny(SlotDef slot) {
        ls.addInt(Accept.accept_any_v);
        slot(slot);
    }

    public void cast(TypeCode srcType, SlotDef srcSlot, TypeCode toType, SlotDef targetSlot) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        if(srcType == toType) {
            throw new IllegalArgumentException("types are same, needn't cast");
        }
        int opCode = Cast.KIND_CAST | (srcType.getValue() << 16) | (toType.getValue() << 8) | 0x02;
        ls.addInt(opCode);
        ls.addInt(targetSlot.getIndex());
        ls.addInt(srcSlot.getIndex());

        sizeVerifier.verify();
    }

    public SizeVerifier sizeVerifier(){
        return new SizeVerifier(this);
    }

    public void loadScope(int offset, SlotDef target) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        if(offset == 1){
            ls.addInt(Load.loadscope_v);
            slot(target);
        } else {
            ls.addInt(Load.loadscope_vc);
            slot(target);
            ls.addInt(offset);
        }
        sizeVerifier.verify();
    }

    public void loadClassOfScope(int offset, SlotDef target) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        if(offset == 1) {
            ls.addInt(Load.loadcls_scope_v);
            slot(target);
        } else {
            ls.addInt(Load.loadcls_scope_vc);
            slot(target);
            ls.addInt(offset);
        }
        sizeVerifier.verify();
    }

    public void loadMetaClassOfScope(int offset, int metaLevel, SlotDef target) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        if(offset == 1) {
            ls.addInt(metaLevel == 1 ? Load.loadcls_scope_v : Load.loadcls2_scope_v);
            slot(target);
        } else {
            ls.addInt(metaLevel == 1 ? Load.loadcls_scope_vc : Load.loadcls2_scope_vc);
            slot(target);
            ls.addInt(offset);
        }
        sizeVerifier.verify();
    }

    public void loadClassOfInstance(int metaLevel, SlotDef target, SlotDef instance) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(metaLevel == 1 ? Load.loadcls_vo : Load.loadcls2_vo);
        slot(target);
        slot(instance);

        sizeVerifier.verify();
    }

    public void loadClass(SlotDef target, int className) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Load.loadcls_vC);
        slot(target);
        ls.addInt(className);
        sizeVerifier.verify();
    }


    public void bindClassUnderInstance(SlotDef target, int className, SlotDef instance) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Load.bindcls_vCo);
        slot(target);
        ls.addInt(className);
        slot(instance);
        sizeVerifier.verify();
    }

    public void bindClassUnderScope(SlotDef target, int subClassName, int scopeDepth) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Load.bindcls_scope_vCc);
        slot(target);
        ls.addInt(subClassName);
        ls.addInt(scopeDepth);
        sizeVerifier.verify();
    }

    public void castScopeBoundClassToClassInterval(SlotDef target, int parameterizedScopedClassIntervalClass, SlotDef scopeBoundClass){
        ls.addInt(Cast.C2sbr_oCC);
        slot(target);
        ls.addInt(parameterizedScopedClassIntervalClass);
        slot(scopeBoundClass);
    }

    public void castScopedClassIntervalToBoundClass(SlotDef target, SlotDef parameterizedScopedClassIntervalInstance, SlotDef boxedClassField) {
        ls.addInt(Cast.sbr2C_Cov);
        slot(target);
        slot(parameterizedScopedClassIntervalInstance);
        slot(boxedClassField);
    }

    public void array_get(SlotDef target, SlotDef array, int index, TypeCode elementType) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Array.array_get_vac | (elementType.getValue() << 16));
        slot(target);
        slot(array);
        ls.addInt(index);
        sizeVerifier.verify();
    }

    public void array_get(SlotDef target, SlotDef array, SlotDef index, TypeCode elementType) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Array.array_get_vav | (elementType.getValue() << 16));
        slot(target);
        slot(array);
        slot(index);
        sizeVerifier.verify();
    }

    public void array_put(SlotDef array, int index, Literal<?> value) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt((Array.array_put_acc | (value.getTypeCode().getValue() << 16)) + additionSizeOf(value.classDef.getTypeCode()));
        slot(array);
        ls.addInt(index);
        literal(value);
        sizeVerifier.verify();
    }

    public void array_put(SlotDef array, SlotDef index, Literal<?> value) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt((Array.array_put_avc | (value.getTypeCode().getValue() << 16)) + additionSizeOf(value.classDef.getTypeCode()));
        slot(array);
        slot(index);
        literal(value);
        sizeVerifier.verify();
    }

    public void array_put(SlotDef array, int index, SlotDef value) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Array.array_put_acv | (value.getTypeCode().getValue() << 16));
        slot(array);
        ls.addInt(index);
        slot(value);
        sizeVerifier.verify();
    }

    public void array_put(SlotDef array, SlotDef index, SlotDef value) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Array.array_put_avv | (value.getTypeCode().getValue() << 16));
        slot(array);
        slot(index);
        slot(value);
        sizeVerifier.verify();
    }


    public void fill_array(SlotDef target, TypeCode elementTypeCode, int size, int blobId) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Array.array_fill_acL | (elementTypeCode.getValue() << 8));
        slot(target);
        ls.addInt(size);
        ls.addInt(blobId);
        sizeVerifier.verify();
    }

    public void new_array(SlotDef target, int arrayClassName, TypeCode elementTypeCode, int length, boolean genericInstantiateRequired) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        if(genericInstantiateRequired){
            ls.addInt(Array.array_create_g_vCc);
        } else {
            ls.addInt(Array.array_create_vCc | (elementTypeCode.getValue() << 16));
        }
        slot(target);
        ls.addInt(arrayClassName);
        ls.addInt(length);
        sizeVerifier.verify();
    }

    public void new_array(SlotDef target, int arrayClassname, TypeCode elementTypeCode, SlotDef length, boolean genericInstantiateRequired) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        if(genericInstantiateRequired){
            ls.addInt(Array.array_create_g_vCv);
        } else {
            ls.addInt(Array.array_create_vCv | (elementTypeCode.getValue() << 16));
        }
        slot(target);
        ls.addInt(arrayClassname);
        slot(length);
        sizeVerifier.verify();
    }

//    public void new_object_array(SlotDef target, int arrayClassname, int classname, SlotDef length) {
//        SizeVerifier sizeVerifier = this.sizeVerifier();
//        ls.addInt(Array.array_create_o_vCcv);
//        slot(target);
//        ls.addInt(arrayClassname);
//        ls.addInt(classname);
//        slot(length);
//        sizeVerifier.verify();
//    }

    public void box(SlotDef target, Literal<?> literal) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt((Box.box_vc | (literal.getTypeCode().getValue() << 16)) + additionSizeOf(literal.getTypeCode()));
        slot(target);
        literal(literal);
        sizeVerifier.verify();
    }

    public void force_box(SlotDef variableSlot, Literal<?> literal) {

    }

    public void box(SlotDef target, SlotDef src) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Box.box_vv | (src.getClassDef().getTypeCode().getValue() << 16));
        slot(target);
        slot(src);
        sizeVerifier.verify();
    }

    public void box_classref(SlotDef target, Literal<?> literal, int expectedClass) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Box.box_C_vCC);
        slot(target);
        literal(literal);
        ls.addInt(expectedClass);
        sizeVerifier.verify();
    }

    public void box_classref(SlotDef target, SlotDef src, int expectedClass) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Box.box_C_vvC);
        slot(target);
        slot(src);
        ls.addInt(expectedClass);
        sizeVerifier.verify();
    }

    public void box_any(SlotDef target, SlotDef src) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Box.box_o_vv);
        slot(target);
        slot(src);
        sizeVerifier.verify();
    }

    public void unbox(SlotDef target, SlotDef src) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(Box.unbox_vo | (target.getTypeCode().getValue() << 16));
        slot(target);
        slot(src);
        sizeVerifier.verify();
    }

    public void put_block_code(int[] stmt) {
        for (int i : stmt) {
            ls.addInt(i);
        }
    }

    public void jump(Label label) {
        ls.addInt(Jump.jump_c);
        putLabel(label);
    }

    public void jnz(SlotDef slotDef) {
        ls.addInt(Jump.jnz_v);
        slot(slotDef);
    }

    private void putLabel(Label label){
        if(!label.isAddressDetermined()){
            label.registerPos(ls.size());
            ls.addInt(label.getIndex());
        } else {
            ls.addInt(label.getResolvedAddress());
        }
    }

    public void updateLabelAddress(int pos, int resolvedAddress) {
        ls.setInt(pos, resolvedAddress);
    }

    public int pos() {
        return this.ls.size();
    }

    public void jumpIfNot(SlotDef condition, Label label) {
        ls.addInt(Jump.jump_f_vc | (condition.getTypeCode().getValue() << 16));
        this.slot(condition);
        putLabel(label);
    }

    public void jumpIf(SlotDef condition, Label label) {
        ls.addInt(Jump.jump_t_vc | (condition.getTypeCode().getValue() << 16));
        this.slot(condition);
        putLabel(label);
    }

    public void setFinalExit(SlotDef finalExit, Label label) {
        ls.addInt(TryCatch.set_final_exit_vc);
        this.slot(finalExit);
        putLabel(label);
    }

    public void neg(SlotDef target, SlotDef src) {
        ls.addInt(Neg.neg_vv | (src.getTypeCode().getValue() << 16));
        this.slot(target);
        this.slot(src);
    }

    public void inc(SlotDef siteObject, SlotDef siteField, Literal<?> literal) {
        SizeVerifier sizeVerifier = this.sizeVerifier();
        ls.addInt(IncDec.inc_ovc | (literal.getTypeCode().getValue() << 16) + additionSizeOf(literal.getTypeCode()));
        this.slot(siteObject);
        this.slot(siteField);
        this.literal(literal);
        sizeVerifier.verify();
    }

    public void inc(SlotDef siteObject, SlotDef siteField, SlotDef change) {
        ls.addInt(IncDec.inc_ovv | (change.getTypeCode().getValue() << 16));
        this.slot(siteObject);
        this.slot(siteField);
        this.slot(change);
    }

    public void not(SlotDef target, SlotDef expr) {
        if(target == expr){
            ls.addInt(Not.not_v);
            this.slot(target);
        } else {
            ls.addInt(Not.not_vv);
            this.slot(target);
            this.slot(expr);
        }
    }

    public void bitnot(SlotDef target, SlotDef expr) {
        ls.addInt(BitNot.bnot_vv | (target.getTypeCode().getValue() << 16));
        this.slot(target);
        this.slot(expr);
    }


    public void jumpByDenseSwitchTable(SlotDef variableSlot, int switchTableId) {
        ls.addInt(Jump.switch_dense_cv);
        ls.addInt(switchTableId);
        slot(variableSlot);
    }

    public void jumpBySparseSwitchTable(SlotDef variableSlot, int switchTableId) {
        ls.addInt(Jump.switch_sparse_cv);
        ls.addInt(switchTableId);
        slot(variableSlot);
    }

    public void store_exception(SlotDef variableSlot) {
        ls.addInt(TryCatch.except_store_v);
        slot(variableSlot);
    }

    public void throw_(SlotDef exceptionSlot) {
        ls.addInt(TryCatch.except_throw_v);
        slot(exceptionSlot);
    }

    public void throw_if_exists(SlotDef maybeExceptionSlot) {
        ls.addInt(TryCatch.except_throw_if_v);
        slot(maybeExceptionSlot);
    }

    public void equalsNull(SlotDef target, SlotDef variableSlot) {
        ls.addInt(Equals.equals_o_vvn);
        slot(target);
        slot(variableSlot);
    }

    public void notEqualsNull(SlotDef target, SlotDef variableSlot) {
        ls.addInt(NotEquals.ne_o_vvn);
        slot(target);
        slot(variableSlot);
    }

    public void instanceOf(SlotDef target, SlotDef value, TypeCode primitiveTypeCode) {
        ls.add((InstanceOf.instanceof_g_vvC & OpCode.DTYPE_MASK_NEG)  | (primitiveTypeCode.getValue() << 16));
        slot(target);
        slot(value);
        ls.addInt(0);
    }

    public void instanceOf(SlotDef target, SlotDef value, int className) {
        ls.add(InstanceOf.instanceof_o_vvC);
        slot(target);
        slot(value);
        ls.addInt(className);
    }

    public void instanceOf_primitive(SlotDef target, SlotDef value, int className) {
        ls.add(InstanceOf.instanceof_p_vvC);
        slot(target);
        slot(value);
        ls.addInt(className);
    }

    public void cast_object(SlotDef src, SlotDef targetSlot, TypeCode typeCode, int className) {
        ls.addInt(Cast.cast_object_vvtC);
        ls.addInt(targetSlot.getIndex());
        ls.addInt(src.getIndex());
        ls.addInt(typeCode.getValue());
        ls.addInt(className);
    }

    public void cast_to_any(SlotDef src, TypeCode srcTypeCode, int srcClassName,
                                SlotDef targetSlot, TypeCode targetTypeCode, int targetClassName) {
        ls.addInt(Cast.cast_to_any_vtCvtC);
        ls.addInt(targetSlot.getIndex());
        ls.addInt(targetTypeCode.getValue());
        ls.addInt(targetClassName);
        ls.addInt(src.getIndex());
        ls.addInt(srcTypeCode.getValue());
        ls.addInt(srcClassName);
    }

    // unbox from lang.Object
    public void force_unbox(SlotDef target, SlotDef src, TypeCode targetTypeCode) {
        ls.addInt(Box.unbox_force_vot);
        ls.addInt(target.getIndex());
        ls.addInt(src.getIndex());
        ls.addInt(targetTypeCode.getValue());
    }

    public void pause() {
        ls.addInt(Pause.pause);
    }


    private static class SizeVerifier{
        final CodeBuffer thisBuff;
        final int start;
        SizeVerifier(CodeBuffer thisBuff){
            this.thisBuff = thisBuff;
            start = thisBuff.ls.size();
        }
        void verify(){
            int size = this.thisBuff.ls.getInt(start) & 0xff;
            int realSize = thisBuff.ls.size() - start - 1;
            if(size != realSize){
                throw new RuntimeException("code size is " + size + " however it wrotes " + realSize);
            }
        }
    }

}
