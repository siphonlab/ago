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

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

import org.siphonlab.ago.compiler.expression.*;

import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;
import org.siphonlab.ago.compiler.statement.Label;

import java.util.Objects;

import static org.siphonlab.ago.opcode.logic.And.KIND_AND;
import static org.siphonlab.ago.opcode.logic.Or.KIND_OR;

public class OrExpr extends ExpressionInFunctionBody {
    public Expression left;
    public Expression right;
    private NullableValue originalNullableLeft;
    private NullableValue originalNullableRight;
    private ClassDef resultType;

    public OrExpr(FunctionDef ownerFunction, Expression left, Expression right) throws CompilationError {
        super(ownerFunction);
        this.left = left.transform();
        this.right = right.transform();
        this.left.setParent(this);
        this.right.setParent(this);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        boolean leftIsNullable = false, rightIsNullable = false;
        var left = this.left;
        var right = this.right;

        ClassDef leftType = left.inferType();
        if (leftType instanceof NullableClassDef) {
            if (!(left instanceof NullableValue)) {
                left = new NullableValue(ownerFunction, left);
            }
            leftIsNullable = true;
            originalNullableLeft = (NullableValue) left;
            left = originalNullableLeft.nonNullValue();
            leftType = left.inferType();
        }

        ClassDef rightType = right.inferType();
        if (rightType instanceof NullableClassDef) {
            if (!(right instanceof NullableValue)) {
                right = new NullableValue(ownerFunction, right);
            }
            rightIsNullable = true;
            originalNullableRight = (NullableValue) right;
            right = originalNullableRight.nonNullValue();
            rightType = right.inferType();
        }

        if(!leftIsNullable && !rightIsNullable) {
            if (leftType.getTypeCode() == TypeCode.BOOLEAN || rightType.getTypeCode() == TypeCode.BOOLEAN ||
                    leftType.getUnboxedTypeCode() == TypeCode.BOOLEAN || rightType.getUnboxedTypeCode() == TypeCode.BOOLEAN) {
                Expression l, r;
                l = ownerFunction.cast(left, getRoot().BOOLEAN()).transform();
                r = ownerFunction.cast(right, getRoot().BOOLEAN()).transform();
                if (l == this.left && r == this.right) {
                    resultType = this.left.inferType();
                    return this;
                }
                return new OrExpr(ownerFunction, l, r).transform();
            }
        }

        CastStrategy.UnifyTypeResult result = new CastStrategy(ownerFunction, this.getSourceLocation(), false).unifyTypes(left, right);

        if(result.changed() || result.left() != left || result.right() != right) {
            left = result.left();
            right = result.right();
        }
        ClassDef finalType ;
        if(leftIsNullable || rightIsNullable) {
            finalType = ownerFunction.getOrCreateNullableType(result.resultType(), null);
            if (this.left.inferType() != finalType) {
                this.left = ownerFunction.cast(left, result.resultType()).transform();        // maybe int? cast to double?, now the left is (double)nonNullValue
            } else {
                this.left = originalNullableLeft;
            }
            if(this.right.inferType() != finalType) {
                this.right = ownerFunction.cast(right, result.resultType()).transform();
            } else {
                this.right = originalNullableRight;
            }
            resultType = finalType;
        } else {
            this.left = left;
            this.right = right;
            resultType = this.left.inferType();
        }

        if(this.left instanceof Literal<?> l){
            return BooleanLiteral.isTrue(l) ? l : this.right;
        }

        return this;
    }


    @Override
    public ClassDef inferType() throws CompilationError {
        return Objects.requireNonNull(resultType);
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        CodeBuffer code = blockCompiler.getCode();
        try {
            blockCompiler.enter(this);

            boolean returnsNullable = this.originalNullableLeft != null || this.originalNullableRight != null;

            if(!returnsNullable){
                if (this.left instanceof LiteralResultExpression lre) {
                    Literal<?> literal = lre.visit(blockCompiler);
                    if (BooleanLiteral.isFalse(literal)) {
                        ownerFunction.assign(localVar, this.right).setSourceLocation(this.getSourceLocation()).visit(blockCompiler);
                    } else {
                        ownerFunction.assign(localVar, literal).visit(blockCompiler);
                    }
                    return;
                } else {
                    this.left.outputToLocalVar(localVar, blockCompiler);
                }
                if (this.right instanceof Var var) {
                    if (this.left.inferType().isBoolean()) {
                        var v1 = localVar;
                        Var.LocalVar v2 = (Var.LocalVar) var.visit(blockCompiler);
                        blockCompiler.getCode().biOperate(KIND_OR, left.inferType().getTypeCode(), v1.getVariableSlot(), v2.getVariableSlot(), localVar.getVariableSlot());
                        return;
                    }
                }

                // make shortcut
                Label skip = blockCompiler.createLabel();
                blockCompiler.getCode().jumpIfNot(localVar.getVariableSlot(), skip);
                right.outputToLocalVar(localVar, blockCompiler);
                skip.here();

                return;
            }

            Label setNull = blockCompiler.createLabel();
            var exit = blockCompiler.createLabel();
            var setRight = blockCompiler.createLabel();

            if (this.originalNullableLeft != null) {    // that means, this.originalNullableLeft != this.left, and left based on the nonNullValue of it
                this.originalNullableLeft.outputToLocalVar(localVar, blockCompiler);        // visit at first, to make nonNullValue of left has value
                code.jumpIf(originalNullableLeft.isNull().visit(blockCompiler).getVariableSlot(), setRight);
            }
            if(this.left.inferType() instanceof NullableClassDef) {
                NullableValue n = this.left instanceof NullableValue ? (NullableValue) this.left : new NullableValue(ownerFunction, this.left);
                this.left.outputToLocalVar(localVar, blockCompiler);
                code.jumpIf(n.isNull().visit(blockCompiler).getVariableSlot(), setRight);       // already is null
                code.jumpIf(n.nonNullValue().visit(blockCompiler).getVariableSlot(), exit);
            } else {
                var leftResult = this.left.visit(blockCompiler);
                if(leftResult instanceof Literal<?> literal){
                    if (BooleanLiteral.isTrue(literal)) {
                        ownerFunction.cast(literal, resultType).transform().termVisit(blockCompiler);
                        code.jump(exit);
                    } // otherwise decide by the right value
                } else {
                    code.jumpIfNot(((Var.LocalVar)leftResult).getVariableSlot(), setRight);        // setRight
                    ownerFunction.cast(leftResult, resultType).transform().termVisit(blockCompiler);
                    code.jump(exit);
                }
            }

            setRight.here();

            if (this.originalNullableRight != null) {
                this.originalNullableRight.outputToLocalVar(localVar, blockCompiler);
                code.jumpIf(originalNullableRight.isNull().visit(blockCompiler).getVariableSlot(), exit);
            }
            if(this.right.inferType() instanceof NullableClassDef) {
                this.right.outputToLocalVar(localVar, blockCompiler);
            } else {
                ownerFunction.cast(this.right, resultType).transform().outputToLocalVar(localVar, blockCompiler);
            }
            code.jump(exit);

            setNull.here();
            code.assignLiteral(localVar.getVariableSlot(), getRoot().nullLiteral());
            exit.here();
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public String toString() {
        return "(Or %s %s)".formatted(left, right);
    }

    @Override
    public OrExpr setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OrExpr orExpr)) return false;
        return Objects.equals(left, orExpr.left) && Objects.equals(right, orExpr.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}
