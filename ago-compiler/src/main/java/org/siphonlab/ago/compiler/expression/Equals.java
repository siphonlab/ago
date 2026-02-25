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


import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.literal.*;
import org.siphonlab.ago.compiler.generic.ScopedClassIntervalClassDef;

import java.util.Objects;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.opcode.compare.Equals.KIND_EQUALS;
import static org.siphonlab.ago.opcode.compare.NotEquals.KIND_NOT_EQUALS;

/**
 */
public class Equals extends BiExpression{

    private final Type type;

    public enum Type{
        Equals(KIND_EQUALS),
        NotEquals(KIND_NOT_EQUALS),
        // ExactEquals
        ;

        public final int op;

        Type(int op) {
            this.op = op;
        }
    }

    public Equals(FunctionDef ownerFunction, Expression left, Expression right, Type type) throws CompilationError {
        super(ownerFunction, left, right);
        this.type = type;
    }

    @Override
    public Expression transformInner() throws CompilationError {
        var p = transformScopeBoundClass(left, right);
        if(p.getLeft() != left || p.getRight() != right){
            this.left = p.getLeft();
            this.right = p.getRight();
        }
        return super.transformInner();
    }

    // according org.siphonlab.ago.compile.expression.Assign.processBoundClass
    private Pair<Expression, Expression> transformScopeBoundClass(Expression left, Expression right) throws CompilationError {
        if (right instanceof ClassOf || right instanceof ClassUnder || right instanceof ConstClass) {
            right = new ClassRefLiteral(getClassOf(right));
            var leftType = left.inferType();
            if (leftType instanceof ScopedClassIntervalClassDef scopedClassIntervalClassDef) {
                left = new Unbox(this.ownerFunction, left).transformInner();
                return Pair.of(left, right);
            } else if (left instanceof ClassOf || left instanceof ClassUnder || left instanceof ConstClass) {
                left = new ClassRefLiteral(getClassOf(left));
            } else {
                throw new TypeMismatchError("class interval or classref expected", this.getSourceLocation());
            }
            return Pair.of(left, right);
        }
        if (left instanceof ClassOf || left instanceof ClassUnder || left instanceof ConstClass){
            var r = transformScopeBoundClass(right, left);
            return Pair.of(r.getRight(), r.getLeft());
        }
        return Pair.of(left, right);
    }

    private ClassDef getClassOf(Expression classExpression) throws CompilationError {
        if (classExpression instanceof ClassOf.ClassOfScope classOf) {
            return classOf.getClassDef();
        } else if(classExpression instanceof ClassOf.ClassOfInstance classOfInstance){
            return classOfInstance.getClassDef();
        } else if(classExpression instanceof ClassOf.ClassOfScopedClassInterval classOfScopedClassInterval){
            return classOfScopedClassInterval.getClassDef();
        } else if(classExpression instanceof ClassUnder classUnder){
            return classUnder.getClassDef();
        } else if(classExpression instanceof  ConstClass constClass){
            return constClass.getClassDef();
        }
        return null;
    }

    @Override
    protected Expression transformUnboxed(Expression left, Expression right) throws CompilationError {
        return new Equals(this.ownerFunction, left,right,this.type);
    }

    @Override
    protected void processTwoVariables(Var.LocalVar result, Var.LocalVar left, Var.LocalVar right, BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.getCode().compareVariables(type.op, left.inferType().getTypeCode(), left.getVariableSlot(), right.getVariableSlot(), result.getVariableSlot());
    }

    @Override
    protected void processRightLiteral(Var.LocalVar result, Var.LocalVar left, Literal<?> literal, BlockCompiler blockCompiler) throws CompilationError {
        if(literal instanceof NullLiteral){
            if(type == Type.Equals) {
                blockCompiler.getCode().equalsNull(result.getVariableSlot(), left.getVariableSlot());
            } else {
                blockCompiler.getCode().notEqualsNull(result.getVariableSlot(), left.getVariableSlot());
            }
            return;
        }
        blockCompiler.getCode().biOperateVariableLiteral(type.op, left.inferType().getTypeCode(), left.getVariableSlot(), literal, result.getVariableSlot());
    }

    @Override
    protected void processLeftLiteral(Var.LocalVar result, Literal<?> literal, Var.LocalVar right, BlockCompiler blockCompiler) throws CompilationError {
        processRightLiteral(result, right, literal, blockCompiler);     // symmetrical
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
                (((IntLiteral) this.left).value == ((IntLiteral) this.right).value);
            case DOUBLE_VALUE ->
                (((DoubleLiteral) this.left).value == ((DoubleLiteral) this.right).value);
            case BYTE_VALUE->
                (((ByteLiteral) this.left).value == ((ByteLiteral) this.right).value);
            case CHAR_VALUE ->
                (((CharLiteral) this.left).value.charValue() == ((CharLiteral) this.right).value.charValue());
            case SHORT_VALUE ->
                (((ShortLiteral) this.left).value == ((ShortLiteral) this.right).value);
            case FLOAT_VALUE ->
                (((FloatLiteral) this.left).value == ((FloatLiteral) this.right).value);
            case BOOLEAN_VALUE ->
                (((BooleanLiteral) this.left).value == ((BooleanLiteral) this.right).value);
            case LONG_VALUE ->
                    (((LongLiteral) this.left).value == ((LongLiteral) this.right).value);
            case NULL_VALUE ->  true;
            case STRING_VALUE ->
                    (Objects.equals(((StringLiteral) this.left).value, ((StringLiteral) this.right).value));
            case CLASS_REF_VALUE ->
                    (((ClassRefLiteral) this.left).value == ((ClassRefLiteral) this.right).value);
            default->
                throw new TypeMismatchError( String.format("cannot apply '==' on '%s' and '%s'", this.left.inferType(), this.right.inferType()), sourceLocation);
        };
        r = switch (type){
            case Equals -> r;
            case NotEquals -> !r;
            default -> throw new UnsupportedOperationException("TODO");
        };
        return new BooleanLiteral(r).setSourceLocation(this.getSourceLocation());
    }

    @Override
    public String toString() {
        return "(%s %s %s)".formatted(type, left, right);
    }

    @Override
    public Equals setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Equals that)) return false;
        return type == that.type && Objects.equals(this.left, that.left) && Objects.equals(this.right,that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, left, right);
    }

    public static boolean isLiteralEquals(Expression expression1, Expression expression2) throws CompilationError {
        var eq = new Equals(null, expression1,expression2,Type.Equals).transform();
        if(eq instanceof BooleanLiteral booleanLiteral){
            return booleanLiteral.value;
        }
        return false;
    }
}
