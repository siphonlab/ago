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
package org.siphonlab.ago.compiler.expression.math;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;


import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.*;
import org.siphonlab.ago.opcode.OpCode;
import org.siphonlab.ago.opcode.arithmetic.Multiply;
import org.siphonlab.ago.opcode.arithmetic.Subtract;



import java.util.Objects;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.opcode.arithmetic.Add.KIND_ADD;
import static org.siphonlab.ago.opcode.arithmetic.Div.KIND_DIV;
import static org.siphonlab.ago.opcode.arithmetic.Mod.KIND_MOD;

/**
 * Add.visit is not available,
 * for Add(target, a, b), it's Assign(target, Add(a, b)), then outputToLocalVar invoked
 * for Add(a, b), it will be transformed to PipeToTempVar(Add(a, b)), invoke outputToLocalVar too
 */
public class ArithmeticExpr extends BiExpression {

    private final Type type;

    public enum Type{
        Add(KIND_ADD),
        Substract(Subtract.KIND_SUBTRACT),
        Multi(Multiply.KIND_MULTIPLY),
        Div(KIND_DIV),
        Mod(KIND_MOD),
        ;

        public final int op;

        Type(int op) {
            this.op = op;
        }

        public static Type of(int op) {
            return switch (op){
                case KIND_ADD -> Add;
                case Subtract.KIND_SUBTRACT -> Substract;
                case Multiply.KIND_MULTIPLY -> Multi;
                case KIND_DIV -> Div;
                case KIND_MOD -> Mod;
                default -> throw new RuntimeException();
            };
        }
    }

    public ArithmeticExpr(FunctionDef ownerFunction, Type type, Expression left, Expression right) throws CompilationError {
        super(ownerFunction, left, right);
        this.type = type;
    }

    @Override
    public Expression transformInner() throws CompilationError {
        if(this.type == Type.Add &&
                (left.inferType().getUnboxedTypeCode() == TypeCode.STRING
                || right.inferType().getUnboxedTypeCode() == TypeCode.STRING)){
            return ownerFunction.concat(left, right).setSourceLocation(this.getSourceLocation()).transformInner();
        }
        return super.transformInner();
    }

    @Override
    protected Expression transformUnboxed(Expression left, Expression right) throws CompilationError {
        return new ArithmeticExpr(ownerFunction, this.type, left, right).setSourceLocation(this.getSourceLocation()).setParent(this.getParent()).transform();
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
        if(right.value instanceof Number number && number.doubleValue() == 0){
            return left;
        }
        return this;
    }

    @Override
    protected Expression processLeftLiteral(Literal<?> left, Expression right) {
        if(left.value instanceof Number number && number.doubleValue() == 0){
            return right;
        }
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        assert left.inferType() == right.inferType();
        return left.inferType();
    }

    @Override
    protected Literal<?> processTwoLiteralsInner(Literal<?> left, Literal<?> right) throws CompilationError {
        switch (this.type){
            case Add:
                return switch (this.left.inferType().getTypeCode().value){
                    case INT_VALUE ->
                            new IntLiteral(((IntLiteral) this.left).value + ((IntLiteral) this.right).value);
                    case DOUBLE_VALUE  ->
                            new DoubleLiteral(((DoubleLiteral) this.left).value + ((DoubleLiteral) this.right).value);
                    case BYTE_VALUE  ->
                            new ByteLiteral((byte) (((ByteLiteral) this.left).value + ((ByteLiteral) this.right).value));
                    case CHAR_VALUE  ->
                            new CharLiteral((char) (((CharLiteral) this.left).value.charValue() + ((CharLiteral) this.right).value.charValue()));
                    case SHORT_VALUE ->
                            new ShortLiteral((short) (((ShortLiteral) this.left).value + ((ShortLiteral) this.right).value));
                    case FLOAT_VALUE ->
                            new FloatLiteral(((FloatLiteral) this.left).value + ((FloatLiteral) this.right).value);
                    case LONG_VALUE ->
                            new LongLiteral(((LongLiteral) this.left).value + ((LongLiteral) this.right).value);
                    default ->
                            throw new TypeMismatchError( String.format("cannot apply '+' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
                };
            case Substract:
                return switch (this.left.inferType().getTypeCode().value){
                    case INT_VALUE ->
                            new IntLiteral(((IntLiteral) this.left).value - ((IntLiteral) this.right).value);
                    case DOUBLE_VALUE  ->
                            new DoubleLiteral(((DoubleLiteral) this.left).value - ((DoubleLiteral) this.right).value);
                    case BYTE_VALUE  ->
                            new ByteLiteral((byte) (((ByteLiteral) this.left).value - ((ByteLiteral) this.right).value));
                    case CHAR_VALUE  ->
                            new CharLiteral((char) (((CharLiteral) this.left).value.charValue() - ((CharLiteral) this.right).value.charValue()));
                    case SHORT_VALUE ->
                            new ShortLiteral((short) (((ShortLiteral) this.left).value - ((ShortLiteral) this.right).value));
                    case FLOAT_VALUE ->
                            new FloatLiteral(((FloatLiteral) this.left).value - ((FloatLiteral) this.right).value);
                    case LONG_VALUE ->
                            new LongLiteral(((LongLiteral) this.left).value - ((LongLiteral) this.right).value);
                    default ->
                            throw new TypeMismatchError( String.format("cannot apply '-' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
                };
            case Multi:
                return switch (this.left.inferType().getTypeCode().value){
                    case INT_VALUE ->
                            new IntLiteral(((IntLiteral) this.left).value * ((IntLiteral) this.right).value);
                    case DOUBLE_VALUE  ->
                            new DoubleLiteral(((DoubleLiteral) this.left).value * ((DoubleLiteral) this.right).value);
                    case BYTE_VALUE  ->
                            new ByteLiteral((byte) (((ByteLiteral) this.left).value * ((ByteLiteral) this.right).value));
                    case CHAR_VALUE  ->
                            new CharLiteral((char) (((CharLiteral) this.left).value.charValue() * ((CharLiteral) this.right).value.charValue()));
                    case SHORT_VALUE ->
                            new ShortLiteral((short) (((ShortLiteral) this.left).value * ((ShortLiteral) this.right).value));
                    case FLOAT_VALUE ->
                            new FloatLiteral(((FloatLiteral) this.left).value * ((FloatLiteral) this.right).value);
                    case LONG_VALUE ->
                            new LongLiteral(((LongLiteral) this.left).value * ((LongLiteral) this.right).value);
                    default ->
                            throw new TypeMismatchError( String.format("cannot apply '*' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
                };
            case Div:
                return switch (this.left.inferType().getTypeCode().value){
                    case INT_VALUE ->
                            new IntLiteral(((IntLiteral) this.left).value / ((IntLiteral) this.right).value);
                    case DOUBLE_VALUE  ->
                            new DoubleLiteral(((DoubleLiteral) this.left).value / ((DoubleLiteral) this.right).value);
                    case BYTE_VALUE  ->
                            new ByteLiteral((byte) (((ByteLiteral) this.left).value / ((ByteLiteral) this.right).value));
                    case CHAR_VALUE  ->
                            new CharLiteral((char) (((CharLiteral) this.left).value.charValue() / ((CharLiteral) this.right).value.charValue()));
                    case SHORT_VALUE ->
                            new ShortLiteral((short) (((ShortLiteral) this.left).value / ((ShortLiteral) this.right).value));
                    case FLOAT_VALUE ->
                            new FloatLiteral(((FloatLiteral) this.left).value / ((FloatLiteral) this.right).value);
                    case LONG_VALUE ->
                            new LongLiteral(((LongLiteral) this.left).value / ((LongLiteral) this.right).value);
                    default ->
                            throw new TypeMismatchError( String.format("cannot apply '/' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
                };
            case Mod:
                return switch (this.left.inferType().getTypeCode().value){
                    case INT_VALUE ->
                            new IntLiteral(((IntLiteral) this.left).value % ((IntLiteral) this.right).value);
                    case DOUBLE_VALUE  ->
                            new DoubleLiteral(((DoubleLiteral) this.left).value % ((DoubleLiteral) this.right).value);
                    case BYTE_VALUE  ->
                            new ByteLiteral((byte) (((ByteLiteral) this.left).value % ((ByteLiteral) this.right).value));
                    case CHAR_VALUE  ->
                            new CharLiteral((char) (((CharLiteral) this.left).value.charValue() % ((CharLiteral) this.right).value.charValue()));
                    case SHORT_VALUE ->
                            new ShortLiteral((short) (((ShortLiteral) this.left).value % ((ShortLiteral) this.right).value));
                    case FLOAT_VALUE ->
                            new FloatLiteral(((FloatLiteral) this.left).value % ((FloatLiteral) this.right).value);
                    case LONG_VALUE ->
                            new LongLiteral(((LongLiteral) this.left).value % ((LongLiteral) this.right).value);
                    default ->
                            throw new TypeMismatchError( String.format("cannot apply '%' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
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
    public ArithmeticExpr setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ArithmeticExpr that)) return false;
        return type == that.type && Objects.equals(that.left, left) && Objects.equals(that.right,right);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }
}
