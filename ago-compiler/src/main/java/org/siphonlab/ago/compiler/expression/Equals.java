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
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.literal.*;
import org.siphonlab.ago.compiler.expression.logic.Not;
import org.siphonlab.ago.compiler.generic.ScopedClassIntervalClassDef;
import org.siphonlab.ago.compiler.narrowtype.NarrowTyper;

import java.util.Objects;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.opcode.compare.Equals.KIND_EQUALS;
import static org.siphonlab.ago.opcode.compare.NotEquals.KIND_NOT_EQUALS;

/**
 */
public class Equals extends BiExpression{

    protected final Type type;
    private final BlockCompiler blockCompiler;

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

    public Equals(BlockCompiler blockCompiler, Expression left, Expression right, Type type) throws CompilationError {
        super(blockCompiler.getFunctionDef(), left, right);
        this.type = type;
        this.blockCompiler = blockCompiler;
    }

    public Equals(FunctionDef functionDef, Expression left, Expression right, Type type) throws CompilationError {
        super(functionDef, left, right);
        this.type = type;
        this.blockCompiler = null;
    }

    @Override
    public Expression transform() throws CompilationError {
        if(this.transformed) return this;

        if(this.left.equals(this.right)){
            return getRoot().createBooleanLiteral(true);
        }

        if(this.left instanceof BooleanLiteral b && this.right.inferType().isBooleanOrBoxed()){
            return transformBooleanLiteral(BooleanLiteral.isTrue(b), this.right).transform();
        } else if(this.right instanceof BooleanLiteral b && this.left.inferType().isBooleanOrBoxed()){
            return transformBooleanLiteral(BooleanLiteral.isTrue(b), this.left).transform();
        }

        return super.transform();
    }

    @Override
    protected Root getRoot() {
        Root root;
        if(this.ownerFunction != null) return super.getRoot();
        try {
            root = this.left.inferType().getRoot();
            if(root == null) root = this.right.inferType().getRoot();
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        return Objects.requireNonNull(root);
    }

    @Override
    public Expression transformInner() throws CompilationError {
        boolean nullableFound = false;
        if(left.inferType() instanceof NullableClassDef n){
            if(!(right.inferType() instanceof NullableClassDef)) {
                if(left instanceof Var.LocalVar localVar){
                    left = narrowTyping(localVar, right);
                }
            }
            if(!(left instanceof NullableValue)) {
                left = new NullableValue(ownerFunction, left);
            }
            if(right.inferType() instanceof NullClassDef){
                return new IsNull(ownerFunction, (NullableValue) left, type);
            }
            left = ((NullableValue) left).nonNullPlaceHolder();
            nullableFound = true;
        }
        if(right.inferType() instanceof NullableClassDef n){
            if(right instanceof Var.LocalVar localVar){
                right = narrowTyping(localVar, left);
            }
            if(!(right instanceof NullableValue)) {
                right = new NullableValue(ownerFunction, right);
            }
            if(left.inferType() instanceof NullClassDef){
                return new IsNull(ownerFunction, (NullableValue) right, type);
            }
            right = ((NullableValue) right).nonNullPlaceHolder();
            nullableFound = true;
        }

        if(!nullableFound){
            if(this.left instanceof BooleanLiteral b){
                Cast rightToBoolean = ownerFunction.cast(this.right, getRoot().BOOLEAN(),true);
                if((b.value && type == Type.Equals) || (!b.value && type == Type.NotEquals)){
                    return rightToBoolean;
                } else {
                    return new Not(ownerFunction, rightToBoolean);
                }
            } else if(this.right instanceof BooleanLiteral b){
                Cast leftToBoolean = ownerFunction.cast(this.left, getRoot().BOOLEAN(),true);
                if((b.value && type == Type.Equals) || (!b.value && type == Type.NotEquals)){
                    return leftToBoolean;
                } else {
                    return new Not(ownerFunction, leftToBoolean);
                }
            }
            var p = transformScopeBoundClass(left, right);
            if(p.getLeft() != left || p.getRight() != right){
                this.left = p.getLeft();
                this.right = p.getRight();
            }
        }
        return super.transformInner();
    }

    private Expression transformBooleanLiteral(boolean b, Expression expression) throws CompilationError {
        if((b && type == Type.Equals) || (!b && type == Type.NotEquals)){
            return expression;
        } else {
            return new Not(ownerFunction, expression);
        }
    }

    private Expression narrowTyping(Var.LocalVar localVar, Expression value) throws CompilationError {
        if(blockCompiler == null) return localVar;

        NarrowTyper narrowTyper = blockCompiler.getNarrowTyper();
        var variable = localVar.variable;
        var nullableClass = (NullableClassDef)variable.getType();
        if(narrowTyper.isCollecting()) {
            var nonNullVar = blockCompiler.acquireNarrowTypingVar(variable, nullableClass.getNullableBaseClass());
            var nullVar = blockCompiler.acquireNarrowTypingVar(variable, getRoot().NULL());
            if(type == Type.Equals && value.inferType() instanceof NullClassDef) {
                narrowTyper.collectNarrowVar(nullVar, nonNullVar);
            } else {
                narrowTyper.collectNarrowVar(nonNullVar, nullVar);
            }
            return new NullableValue(ownerFunction, localVar, nonNullVar);
        } else {
            return localVar;
        }
    }

    @Override
    protected void outputToLocalVar(Var.LocalVar localVar, TermExpression evaluatedLeft, TermExpression evaluatedRight, BlockCompiler blockCompiler) throws CompilationError {

        CodeBuffer code = blockCompiler.getCode();

        try {
            blockCompiler.enter(this);
            var exitLabel = blockCompiler.createLabel();
            var returnFalse = blockCompiler.createLabel();
            if (this.left instanceof NullableValue.NonNullPlaceHolder leftPlaceHolder) {
                NullableValue leftNullableValue = leftPlaceHolder.getNullableValue();
                if (this.right instanceof NullableValue.NonNullPlaceHolder rightNonNull) {

                    blockCompiler.lockRegister(localVar);
                    var lIsNull = leftNullableValue.isNull().visit(blockCompiler);
                    NullableValue rightNullableValue = rightNonNull.getNullableValue();
                    blockCompiler.lockRegister(lIsNull);
                    var rIsNull = rightNullableValue.isNull().visit(blockCompiler);
                    blockCompiler.lockRegister(rIsNull);

                    // test both null
                    code.and(localVar.getVariableSlot(), lIsNull.getVariableSlot(), rIsNull.getVariableSlot());
                    if(type == Type.Equals)
                        code.jumpIf(localVar.getVariableSlot(), exitLabel);      // both null, result is true
                    else
                        code.jumpIf(localVar.getVariableSlot(), returnFalse);

                    // test both nonnull
                    new Equals(ownerFunction, lIsNull, rIsNull, Equals.Type.Equals).outputToLocalVar(localVar, blockCompiler);      // localVar = (!lIsNull && !rIsNull)
                    if(type == Type.Equals)
                        code.jumpIfNot(localVar.getVariableSlot(), returnFalse);    // if failed return false, otherwise they are both nonnull, go ahead
                    else
                        code.jumpIfNot(localVar.getVariableSlot(), exitLabel);

                    blockCompiler.releaseRegister(localVar);
                    blockCompiler.releaseRegister(lIsNull);
                    blockCompiler.releaseRegister(rIsNull);

                    evaluatedLeft = leftNullableValue.nonNullValue().visit(blockCompiler);
                    blockCompiler.lockRegister(evaluatedLeft);
                    evaluatedRight = rightNullableValue.nonNullValue().visit(blockCompiler);

                    super.outputToLocalVar(localVar, evaluatedLeft, evaluatedRight, blockCompiler);

                    code.jump(exitLabel);
                    returnFalse.here();
                    ownerFunction.assign(localVar, getRoot().createBooleanLiteral(false)).termVisit(blockCompiler);

                    blockCompiler.releaseRegister(evaluatedLeft);
                    exitLabel.here();
                } else {
                    var isNull = leftNullableValue.isNull().visit(blockCompiler);
                    var trueLabel = blockCompiler.createLabel();

                    code.jumpIfNot(isNull.getVariableSlot(), trueLabel);
                    ownerFunction.assign(localVar, getRoot().createBooleanLiteral(false)).termVisit(blockCompiler);
                    code.jump(exitLabel);

                    trueLabel.here();
                    super.outputToLocalVar(localVar, leftNullableValue.nonNullValue().visit(blockCompiler), evaluatedRight, blockCompiler);
                    exitLabel.here();
                }
            } else if (this.right instanceof NullableValue.NonNullPlaceHolder rightPlaceHolder) {
                NullableValue rightNullableValue = rightPlaceHolder.getNullableValue();
                var isNull = rightNullableValue.isNull().visit(blockCompiler);
                var trueLabel = blockCompiler.createLabel();

                code.jumpIfNot(isNull.getVariableSlot(), trueLabel);
                ownerFunction.assign(localVar, getRoot().createBooleanLiteral(false)).termVisit(blockCompiler);
                code.jump(exitLabel);

                trueLabel.here();
                super.outputToLocalVar(localVar, evaluatedLeft, rightNullableValue.nonNullValue().visit(blockCompiler), blockCompiler);
                exitLabel.here();
            } else {
                super.outputToLocalVar(localVar, evaluatedLeft, evaluatedRight, blockCompiler);
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    // according org.siphonlab.ago.compile.expression.Assign.processBoundClass
    private Pair<Expression, Expression> transformScopeBoundClass(Expression left, Expression right) throws CompilationError {
        if (right instanceof ClassOf || right instanceof ClassUnder || right instanceof ConstClass) {
            right = getClassOf(right).toClassRefLiteral();
            var leftType = left.inferType();
            if (leftType instanceof ScopedClassIntervalClassDef scopedClassIntervalClassDef) {
                left = new Unbox(this.ownerFunction, left).transformInner();
                return Pair.of(left, right);
            } else if (left instanceof ClassOf || left instanceof ClassUnder || left instanceof ConstClass) {
                left = getClassOf(left).toClassRefLiteral();
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
        return new Equals(this.blockCompiler, left,right,this.type);
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
        return getRoot().BOOLEAN();
    }

    @Override
    protected Literal<?> processTwoLiteralsInner(Literal<?> left, Literal<?> right) throws CompilationError {
        var r = switch (this.left.inferType().getTypeCode().value){
            case INT_VALUE->
                (((IntLiteral) this.left).value == ((IntLiteral) this.right).value);
            case DOUBLE_VALUE ->
                (((DoubleLiteral) this.left).value == ((DoubleLiteral) this.right).value);
            case DECIMAL_VALUE ->
                (((DecimalLiteral) this.left).value.equals(((DecimalLiteral) this.right).value));
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
        // when come from `static isLiteralEquals`, getOwnerFunction().getRoot() not works
        return left.getClassDef().getRoot().createBooleanLiteral( r).setSourceLocation(this.getSourceLocation());
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
        var eq = new Equals((FunctionDef) null, expression1,expression2,Type.Equals).transform();
        if(eq instanceof BooleanLiteral booleanLiteral){
            return booleanLiteral.value;
        }
        return false;
    }

    public Type getType() {
        return type;
    }

    public Equals neg() throws CompilationError {
        if(blockCompiler != null){
            return new Equals(blockCompiler, left, right, type == Type.Equals ? Type.NotEquals : Type.Equals);
        } else {
            return new Equals(ownerFunction, left, right, type == Type.Equals ? Type.NotEquals : Type.Equals);
        }
    }

}
