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


import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.statement.Label;

import java.util.Objects;

import static org.siphonlab.ago.opcode.logic.And.KIND_AND;

public class AndExpr extends ExpressionInFunctionBody {
    public Expression left;
    public Expression right;

    private NullableValue originalNullableLeft;
    private NullableValue originalNullableRight;

    public AndExpr(FunctionDef ownerFunction, Expression left, Expression right) throws CompilationError {
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

        if(leftType.getTypeCode() == TypeCode.BOOLEAN || rightType.getTypeCode() == TypeCode.BOOLEAN ||
                leftType.getUnboxedTypeCode() == TypeCode.BOOLEAN || rightType.getUnboxedTypeCode() == TypeCode.BOOLEAN){
            Expression l, r;
            if(leftIsNullable){
                l = new AndExpr(ownerFunction, originalNullableLeft.isNotNull(),
                             ownerFunction.cast(left, getRoot().BOOLEAN()).transform());
            } else {
                l = ownerFunction.cast(left, getRoot().BOOLEAN()).transform();
            }
            if(rightIsNullable){
                r = new AndExpr(ownerFunction, originalNullableRight.isNotNull(),
                        ownerFunction.cast(right, getRoot().BOOLEAN()).transform());
            } else {
                r = ownerFunction.cast(right, getRoot().BOOLEAN()).transform();
            }
            if(l == this.left && r == this.right) return this;
            return new AndExpr(ownerFunction, l, r).transform();
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
                this.left = ownerFunction.cast(left, finalType).transform();
            } else {
                this.left = originalNullableLeft;
            }
            if(this.right.inferType() != finalType) {
                this.right = ownerFunction.cast(right, finalType).transform();
            } else {
                this.right = originalNullableRight;
            }
        } else {
            this.left = left;
            this.right = right;
        }

        if(this.left instanceof Literal<?> l){
            return BooleanLiteral.isTrue(l) ? this.right : l;
        }
        if(this.right instanceof Literal<?> r){
            if(BooleanLiteral.isFalse(r)) return r;     // the left need evaluate if right means true, that is, after left evaluated return right value
        }

        return this;
    }


    @Override
    public ClassDef inferType() throws CompilationError {
        return this.left.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        CodeBuffer code = blockCompiler.getCode();
        try {
            blockCompiler.enter(this);
            // make shortcut
            boolean returnsNullable = this.originalNullableLeft != null || this.originalNullableRight != null;
            Label skip = blockCompiler.createLabel();

            if (this.left instanceof LiteralResultExpression lre) {
                Literal<?> literal = lre.visit(blockCompiler);
                if (BooleanLiteral.isTrue(literal)) {
                    ownerFunction.assign(localVar, this.right).setSourceLocation(this.getSourceLocation()).visit(blockCompiler);
                } else {
                    ownerFunction.assign(localVar, literal).visit(blockCompiler);
                }
                code.jumpIfNot(localVar.getVariableSlot(), skip);
            } else {
                if (this.originalNullableLeft == null) {
                    assert !(this.left.inferType() instanceof NullableClassDef);
                    if(this.originalNullableRight == null) {
                        this.left.outputToLocalVar(localVar, blockCompiler);

                        if (!returnsNullable) {
                            if (this.right instanceof Var var) {
                                if (this.left.inferType().isBoolean()) {
                                    var v1 = localVar;
                                    Var.LocalVar v2 = (Var.LocalVar) var.visit(blockCompiler);
                                    code.biOperate(KIND_AND, left.inferType().getTypeCode(), v1.getVariableSlot(), v2.getVariableSlot(), localVar.getVariableSlot());
                                    return;
                                }
                            }
                        }
                        code.jumpIfNot(localVar.getVariableSlot(), skip);
                    } else {
                        Var.LocalVar leftResult = (Var.LocalVar) this.left.visit(blockCompiler);
                        code.jumpIfNot(leftResult.getVariableSlot(), skip);
                    }
                } else {
                    this.originalNullableLeft.outputToLocalVar(localVar, blockCompiler);
                    code.jumpIf(originalNullableLeft.isNull().visit(blockCompiler).getVariableSlot(), skip);

                    Var.LocalVar leftResult;
                    if (this.left != this.originalNullableLeft) {
                        assert !(this.left.inferType() instanceof NullableClassDef);
                        leftResult = (Var.LocalVar)this.left.visit(blockCompiler);
                    } else {
                        leftResult = this.originalNullableLeft.nonNullValue().visit(blockCompiler);
                    }
                    code.jumpIfNot(leftResult.getVariableSlot(), skip);
                }
            }

            if(this.originalNullableRight != null) {
                this.originalNullableRight.outputToLocalVar(localVar, blockCompiler);
                if(this.right != originalNullableRight){
                    this.right.outputToLocalVar(localVar, blockCompiler);
                }
            } else {
                this.right.outputToLocalVar(localVar, blockCompiler);
            }
            var exit = blockCompiler.createLabel();
            if(returnsNullable){
                code.jump(exit);
                skip.here();
                code.assignLiteral(localVar.getVariableSlot(), getRoot().nullLiteral());
                exit.here();
            } else {
                skip.here();
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public String toString() {
        return "(And %s %s)".formatted(left, right);
    }

    @Override
    public AndExpr setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AndExpr andExpr)) return false;
        return Objects.equals(left, andExpr.left) && Objects.equals(right, andExpr.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}
