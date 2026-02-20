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
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;


import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.*;
import org.siphonlab.ago.opcode.OpCode;

import java.util.Objects;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.opcode.logic.BitAnd.KIND_BITAND;
import static org.siphonlab.ago.opcode.logic.BitOr.KIND_BITOR;
import static org.siphonlab.ago.opcode.logic.BitXor.KIND_BITXOR;

/**
 */
public class BitOpExpr extends BiExpression {

    private final Type type;

    public enum Type{
        BitAnd(KIND_BITAND),
        BitOr(KIND_BITOR),
        BitXor(KIND_BITXOR),
        ;

        public final int op;

        Type(int op) {
            this.op = op;
        }

        public static Type of(int op) {
            return switch (op){
                case KIND_BITAND -> BitAnd;
                case KIND_BITOR -> BitOr;
                case KIND_BITXOR -> BitXor;
                default -> throw new RuntimeException();
            };
        }
    }

    public BitOpExpr(Type type, Expression left, Expression right) throws CompilationError {
        super(left, right);
        this.type = type;
    }

    @Override
    public Expression transformInner() throws CompilationError {
        CastStrategy.UnifyTypeResult result = new CastStrategy(this.getSourceLocation(), false).unifyTypes(this.left, this.right);
        if (result.changed() || result.left() != this.left || result.right() != this.right) {
            this.left = result.left();
            this.right = result.right();
        }
        var type = this.left.inferType();
        if(!type.getUnboxedTypeCode().isIntFamily()){
            throw new TypeMismatchError("int family expression expected",this.getSourceLocation());
        }
        if(type.getTypeCode().isObject()){
            this.left = new Unbox(this.left).transform();
            this.right = new Unbox(this.left).transform();
        }
        return processLiterals();
    }

    @Override
    protected Expression transformUnboxed(Expression left, Expression right) throws CompilationError {
        return new BitOpExpr(this.type, left, right).setSourceLocation(this.getSourceLocation()).setParent(this.getParent()).transform();
    }

    @Override
    protected void processTwoVariables(Var.LocalVar result, Var.LocalVar left, Var.LocalVar right, BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.getCode().biOperate(type.op, left.inferType().getTypeCode(), left.getVariableSlot(), right.getVariableSlot(), result.getVariableSlot());
    }

    @Override
    protected void processRightLiteral(Var.LocalVar result, Var.LocalVar left, Literal<?> literal, BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.getCode().biOperateVariableLiteral(type.op, literal.inferType().getTypeCode(), left.getVariableSlot(), literal, result.getVariableSlot());
    }

    @Override
    protected void processLeftLiteral(Var.LocalVar result, Literal<?> literal, Var.LocalVar right, BlockCompiler blockCompiler) throws CompilationError {
        if(OpCode.isSymmetrical(type.op)){      // add_vcv to add_vvc
            processRightLiteral(result, right, literal, blockCompiler);
            return;
        }
        blockCompiler.getCode().biOperateLiteralVariable(type.op, literal.inferType().getTypeCode(), literal, right.getVariableSlot(), result.getVariableSlot());
    }

    @Override
    protected Expression processRightLiteral(Expression left, Literal<?> right) {
        switch (this.type) {
            case BitAnd:
                if (right.value instanceof Number number && number.doubleValue() == 0) {
                    return right;
                }
                break;
            case BitOr:
                if (right.value instanceof Number number && number.doubleValue() == 0) {
                    return left;
                }
                break;
            case BitXor:
                break;
        }
        return this;
    }

    @Override
    protected Expression processLeftLiteral(Literal<?> left, Expression right) {
        return processRightLiteral(right, left);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        assert left.inferType() == right.inferType();
        return left.inferType();
    }

    @Override
    protected Literal<?> processTwoLiteralsInner(Literal<?> left, Literal<?> right) throws CompilationError {
        switch (this.type){
            case BitAnd:
                return switch (this.left.inferType().getTypeCode().value){
                    case INT_VALUE ->
                            new IntLiteral(((IntLiteral) this.left).value & ((IntLiteral) this.right).value);
                    case BYTE_VALUE  ->
                            new ByteLiteral((byte) (((ByteLiteral) this.left).value & ((ByteLiteral) this.right).value));
                    case CHAR_VALUE  ->
                            new CharLiteral((char) (((CharLiteral) this.left).value.charValue() & ((CharLiteral) this.right).value.charValue()));
                    case SHORT_VALUE ->
                            new ShortLiteral((short) (((ShortLiteral) this.left).value & ((ShortLiteral) this.right).value));
                    case LONG_VALUE ->
                            new LongLiteral(((LongLiteral) this.left).value & ((LongLiteral) this.right).value);
                    default ->
                            throw new TypeMismatchError( String.format("cannot apply 'band' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
                };
            case BitOr:
                return switch (this.left.inferType().getTypeCode().value){
                    case INT_VALUE ->
                            new IntLiteral(((IntLiteral) this.left).value | ((IntLiteral) this.right).value);
                    case BYTE_VALUE  ->
                            new ByteLiteral((byte) (((ByteLiteral) this.left).value | ((ByteLiteral) this.right).value));
                    case CHAR_VALUE  ->
                            new CharLiteral((char) (((CharLiteral) this.left).value.charValue() | ((CharLiteral) this.right).value.charValue()));
                    case SHORT_VALUE ->
                            new ShortLiteral((short) (((ShortLiteral) this.left).value | ((ShortLiteral) this.right).value));
                    case LONG_VALUE ->
                            new LongLiteral(((LongLiteral) this.left).value | ((LongLiteral) this.right).value);
                    default ->
                            throw new TypeMismatchError( String.format("cannot apply 'bor' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
                };
            case BitXor:
                return switch (this.left.inferType().getTypeCode().value){
                    case INT_VALUE ->
                            new IntLiteral(((IntLiteral) this.left).value ^ ((IntLiteral) this.right).value);
                    case BYTE_VALUE  ->
                            new ByteLiteral((byte) (((ByteLiteral) this.left).value ^ ((ByteLiteral) this.right).value));
                    case CHAR_VALUE  ->
                            new CharLiteral((char) (((CharLiteral) this.left).value.charValue() ^ ((CharLiteral) this.right).value.charValue()));
                    case SHORT_VALUE ->
                            new ShortLiteral((short) (((ShortLiteral) this.left).value ^ ((ShortLiteral) this.right).value));
                    case LONG_VALUE ->
                            new LongLiteral(((LongLiteral) this.left).value ^ ((LongLiteral) this.right).value);
                    default ->
                            throw new TypeMismatchError( String.format("cannot apply 'bxor' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
                };
            default:
                throw new UnsupportedOperationException("unsupported type");
        }
    }

    @Override
    public String toString() {
        return "(%s %s %s)".formatted(type, left, right);
    }

    @Override
    public BitOpExpr setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BitOpExpr that)) return false;
        return type == that.type && Objects.equals(that.left, left) && Objects.equals(that.right,right);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }
}
