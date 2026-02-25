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
package org.siphonlab.ago.compiler.expression;


import org.apache.commons.lang3.ObjectUtils;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.literal.*;
import org.siphonlab.ago.opcode.compare.GreaterEquals;
import org.siphonlab.ago.opcode.compare.GreaterThan;
import org.siphonlab.ago.opcode.compare.LittleEquals;
import org.siphonlab.ago.opcode.compare.LittleThan;


import java.util.Objects;

import static org.siphonlab.ago.TypeCode.*;

/**
 */
public class Compare extends BiExpression{

    private final Type type;

    public enum Type{
        GT(GreaterThan.KIND_GREATER_THAN),
        LT(LittleThan.KIND_LITTLE_THAN),
        GE(GreaterEquals.KIND_GREATER_EQUALS),
        LE(LittleEquals.KIND_LITTLE_EQUALS)
        ;

        public final int op;

        Type(int op) {
            this.op = op;
        }
    }


    public Compare(FunctionDef ownerFunction, Expression left, Expression right, Compare.Type type) throws CompilationError {
        super(ownerFunction, left, right);
        this.type = type;
    }

    @Override
    protected Expression transformUnboxed(Expression left, Expression right) throws CompilationError {
        return new Compare(ownerFunction, left, right, this.type).setSourceLocation(this.getSourceLocation()).setParent(this.getParent()).transform();
    }

    @Override
    protected void processTwoVariables(Var.LocalVar result, Var.LocalVar left, Var.LocalVar right, BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.getCode().compareVariables(type.op, left.inferType().getTypeCode(), left.getVariableSlot(), right.getVariableSlot(), result.getVariableSlot());
    }

    @Override
    protected void processRightLiteral(Var.LocalVar result, Var.LocalVar left, Literal<?> literal, BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.getCode().compareVariableLiteral(type.op, literal.inferType().getTypeCode(), left.getVariableSlot(), literal, result.getVariableSlot());
    }

    @Override
    protected void processLeftLiteral(Var.LocalVar result, Literal<?> literal, Var.LocalVar right, BlockCompiler blockCompiler) throws CompilationError {
        // no `cmp_vcv`, reverse to `cmp_vvc`
        Type reverse = switch (type){
            case LT -> Type.GE;
            case GT -> Type.LE;
            case GE -> Type.LT;
            case LE -> Type.GT;
        };
        blockCompiler.getCode().compareVariableLiteral(reverse.op, literal.inferType().getTypeCode(), right.getVariableSlot(), literal, result.getVariableSlot());
    }

    @Override
    protected Expression processRightLiteral(Expression left, Literal<?> right) {
        return this;
    }

    @Override
    protected Expression processLeftLiteral(Literal<?> left, Expression right) {
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return PrimitiveClassDef.BOOLEAN;
    }

    @Override
    protected Literal<?> processTwoLiteralsInner(Literal<?> left, Literal<?> right) throws CompilationError {
        var r = switch (this.left.inferType().getTypeCode().value){
            case INT_VALUE->
                    ObjectUtils.compare(((IntLiteral) this.left).value , ((IntLiteral) this.right).value);
            case DOUBLE_VALUE ->
                    ObjectUtils.compare(((DoubleLiteral) this.left).value, ((DoubleLiteral) this.right).value);
            case BYTE_VALUE->
                    ObjectUtils.compare(((ByteLiteral) this.left).value, ((ByteLiteral) this.right).value);
            case CHAR_VALUE ->
                    ObjectUtils.compare(((CharLiteral) this.left).value.charValue(), ((CharLiteral) this.right).value.charValue());
            case SHORT_VALUE ->
                    ObjectUtils.compare(((ShortLiteral) this.left).value, ((ShortLiteral) this.right).value);
            case FLOAT_VALUE ->
                    ObjectUtils.compare(((FloatLiteral) this.left).value, ((FloatLiteral) this.right).value);
            case BOOLEAN_VALUE ->
                    ObjectUtils.compare(((BooleanLiteral) this.left).value, ((BooleanLiteral) this.right).value);
            case LONG_VALUE ->
                    ObjectUtils.compare(((LongLiteral) this.left).value, ((LongLiteral) this.right).value);
            case NULL_VALUE ->
                    0;
            case STRING_VALUE ->
                    ObjectUtils.compare(((StringLiteral) this.left).value, ((StringLiteral) this.right).value);
            case CLASS_REF_VALUE ->
                    ObjectUtils.compare(((ClassRefLiteral) this.left).value, ((ClassRefLiteral) this.right).value);
            default->
                    throw new TypeMismatchError( String.format("cannot apply '==' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
        };
        var b = switch (type){
            case GT -> r > 0;
            case GE -> r >= 0;
            case LT -> r < 0;
            case LE -> r <= 0;
        };
        return new BooleanLiteral(b).setSourceLocation(this.getSourceLocation());
    }

    @Override
    public String toString() {
        return "(%s %s %s)".formatted(type, left, right);
    }

    @Override
    public Compare setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Compare that)) return false;
        return type == that.type && Objects.equals(this.left, that.left) && Objects.equals(this.right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, left, right);
    }
}
