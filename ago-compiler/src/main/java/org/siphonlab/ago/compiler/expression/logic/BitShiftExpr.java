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
package org.siphonlab.ago.compiler.expression.logic;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.IllegalExpressionError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;


import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.*;


import org.siphonlab.ago.opcode.logic.BitShiftLeft;
import org.siphonlab.ago.opcode.logic.BitShiftRight;
import org.siphonlab.ago.opcode.logic.BitUnsignedRight;

import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.INT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;

public class BitShiftExpr extends ExpressionInFunctionBody {

    private final Type type;
    private Expression right;
    private Expression left;

    public enum Type{
        LShift(BitShiftLeft.KIND_BIT_LSHIFT),
        RShift(BitShiftRight.KIND_BIT_RSHIFT),
        URShift(BitUnsignedRight.KIND_BIT_URSHIFT);

        private final int opCode;
        Type(int opCode) {
            this.opCode = opCode;
        }
    }

    public BitShiftExpr(FunctionDef ownerFunction, Type type, Expression left, Expression right) throws CompilationError {
        super(ownerFunction);
        this.type = type;
        this.left = left.transform().setParent(this);
        this.right = ownerFunction.cast(right, PrimitiveClassDef.INT).transform().setParent(this);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        ClassDef type = this.left.inferType();
        if(!type.getUnboxedTypeCode().isIntFamily()){
            throw new TypeMismatchError("int family expression expected",this.getSourceLocation());
        }
        if(type.getTypeCode().isObject()){
            this.left = ownerFunction.unbox(this.left).setParent(this);
        }
        if(!right.inferType().getTypeCode().isIntFamily()){
            throw new TypeMismatchError("int family value expected", right.getSourceLocation());
        }
        right = ownerFunction.cast(right, PrimitiveClassDef.INT).transform();
        if(right instanceof IntLiteral r){
            if(r.value == 0) return left;
            if(r.value < 0) throw new IllegalExpressionError("illegal bits value", right.getSourceLocation());
        }
        if(left instanceof Literal<?> l && right instanceof IntLiteral r){
            return processTwoLiterals(l, r);
        }

        return this;
    }

    private Expression processTwoLiterals(Literal<?> l, IntLiteral r) throws CompilationError {
        int change = r.value;
        switch (this.type){
            case LShift:
                return switch (this.left.inferType().getTypeCode().value) {
                    case INT_VALUE -> new IntLiteral((((IntLiteral) l).value) << change);
                    case BYTE_VALUE -> new ByteLiteral((byte) (((ByteLiteral) l).value << change));
                    case CHAR_VALUE -> new CharLiteral((char) ((((CharLiteral) l).value) << change));
                    case SHORT_VALUE -> new ShortLiteral((short) ((((ShortLiteral) l).value) << change));
                    case LONG_VALUE -> new LongLiteral((((LongLiteral) l).value) << change);
                    default ->
                            throw new TypeMismatchError(String.format("cannot apply '<<' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
                };
            case RShift:
                return switch (this.left.inferType().getTypeCode().value) {
                    case INT_VALUE -> new IntLiteral((((IntLiteral) l).value) >> change);
                    case BYTE_VALUE -> new ByteLiteral((byte) (((ByteLiteral) l).value >> change));
                    case CHAR_VALUE -> new CharLiteral((char) ((((CharLiteral) l).value) >> change));
                    case SHORT_VALUE -> new ShortLiteral((short) ((((ShortLiteral) l).value) >> change));
                    case LONG_VALUE -> new LongLiteral((((LongLiteral) l).value) >> change);
                    default ->
                            throw new TypeMismatchError(String.format("cannot apply '>>' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
                };
            case URShift:
                return switch (this.left.inferType().getTypeCode().value) {
                    case INT_VALUE -> new IntLiteral((((IntLiteral) l).value) >>> change);
                    case BYTE_VALUE -> new ByteLiteral((byte) (((ByteLiteral) l).value >>> change));
                    case CHAR_VALUE -> new CharLiteral((char) ((((CharLiteral) l).value) >>> change));
                    case SHORT_VALUE -> new ShortLiteral((short) ((((ShortLiteral) l).value) >>> change));
                    case LONG_VALUE -> new LongLiteral((((LongLiteral) l).value) >>> change);
                    default ->
                            throw new TypeMismatchError(String.format("cannot apply '>>>' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
                };
            default:
                throw new UnsupportedOperationException("unsupported type");
        }
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return this.left.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            TermExpression left = this.left.visit(blockCompiler);
            blockCompiler.lockRegister(left);
            TermExpression right = this.right.visit(blockCompiler);
            blockCompiler.releaseRegister(left);

            if (left instanceof Literal<?> literal1 && right instanceof Literal<?> literal2) {
                processTwoLiterals(literal1, (IntLiteral) literal2).outputToLocalVar(localVar, blockCompiler);
            } else {
                if (left instanceof Literal<?> literal) {
                    blockCompiler.getCode().biOperateLiteralVariable(type.opCode, literal.getTypeCode(), literal, ((Var.LocalVar) right).getVariableSlot(), localVar.getVariableSlot());
                } else if (right instanceof Literal<?> literal) {
                    blockCompiler.getCode().biOperateVariableLiteral(type.opCode, left.inferType().getTypeCode(), ((Var.LocalVar) left).getVariableSlot(), literal, localVar.getVariableSlot());
                } else {
                    blockCompiler.getCode().biOperate(type.opCode, left.inferType().getTypeCode(), ((Var.LocalVar) left).getVariableSlot(), ((Var.LocalVar) right).getVariableSlot(), localVar.getVariableSlot());
                }
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.left.termVisit(blockCompiler);
        this.right.termVisit(blockCompiler);
    }

    @Override
    public String toString() {
        return "(%s %s %s)".formatted(this.type, left, right);
    }

    @Override
    public BitShiftExpr setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
