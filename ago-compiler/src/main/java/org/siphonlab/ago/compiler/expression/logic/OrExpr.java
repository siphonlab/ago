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

import static org.siphonlab.ago.opcode.logic.Or.KIND_OR;

public class OrExpr extends ExpressionInFunctionBody {
    public Expression left;
    public Expression right;

    public OrExpr(FunctionDef ownerFunction, Expression left, Expression right) throws CompilationError {
        super(ownerFunction);
        this.left = left.transform();
        this.right = right.transform();
        this.left.setParent(this);
        this.right.setParent(this);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        boolean leftIsNullable = false,  rightIsNullable = false;
        var left = this.left;
        var right = this.right;
        Expression maybeNullLeft = null;
        Expression maybeNullRight = null;

        ClassDef leftType = left.inferType();
        if(leftType instanceof NullableClassDef n){
            leftIsNullable = true;
            leftType = n.getBaseClass();
            left = ownerFunction.cast(maybeNullLeft = new PipeToTempVar(ownerFunction, left), leftType).transform();
        }
        ClassDef rightType = right.inferType();
        if(rightType instanceof NullableClassDef n){
            rightIsNullable = true;
            rightType = n.getBaseClass();
            right = ownerFunction.cast(maybeNullRight = new PipeToTempVar(ownerFunction, right), rightType).transform();
        }

        if(leftType.getTypeCode() == TypeCode.BOOLEAN || rightType.getTypeCode() == TypeCode.BOOLEAN ||
                leftType.getUnboxedTypeCode() == TypeCode.BOOLEAN || rightType.getUnboxedTypeCode() == TypeCode.BOOLEAN){
            Expression l, r;
            if(leftIsNullable){
                l = new AndExpr(ownerFunction, new Equals(ownerFunction, maybeNullLeft, getRoot().nullLiteral(), Equals.Type.NotEquals),
                        ownerFunction.cast(left, getRoot().BOOLEAN()).transform());
            } else {
                l = ownerFunction.cast(left, getRoot().BOOLEAN()).transform();
            }
            if(rightIsNullable){
                r = new AndExpr(ownerFunction, new Equals(ownerFunction, maybeNullRight, getRoot().nullLiteral(), Equals.Type.NotEquals),
                        ownerFunction.cast(right, getRoot().BOOLEAN()).transform());
            } else {
                r = ownerFunction.cast(right, getRoot().BOOLEAN()).transform();
            }
            if(l == this.left && r == this.right) return this;
            return new OrExpr(ownerFunction, l, r).transform();
        }

        CastStrategy.UnifyTypeResult result = new CastStrategy(ownerFunction, this.getSourceLocation(), false).unifyTypes(left, right);

        if(result.changed() || result.left() != left || result.right() != right) {
            left = result.left();
            right = result.right();
        }
        ClassDef finalType;
        if(leftIsNullable || rightIsNullable) {
            finalType = ownerFunction.getOrCreateNullableType(result.resultType(), null);
            left = ownerFunction.cast(left, finalType).transform();
            right = ownerFunction.cast(right, finalType).transform();
        }

        if(leftIsNullable){
            this.left = new IfElseExpr(ownerFunction, left,
                    new Equals(ownerFunction, maybeNullLeft, getRoot().nullLiteral(), Equals.Type.NotEquals), getRoot().nullLiteral());
        }  else {
            this.left = left;
        }
        if(rightIsNullable){
            this.right = new IfElseExpr(ownerFunction, right,
                    new Equals(ownerFunction, maybeNullRight, getRoot().nullLiteral(), Equals.Type.NotEquals), getRoot().nullLiteral());
        } else {
            this.right = right;
        }

        if(this.left instanceof Literal<?> l){
            return BooleanLiteral.isTrue(l) ? l : this.right;
        }
        return this;
    }


    @Override
    public ClassDef inferType() throws CompilationError {
        return this.left.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            if (this.left instanceof LiteralResultExpression lre) {
                Literal<?> literal = lre.visit(blockCompiler);
                if (BooleanLiteral.isFalse(literal)) {
                    ownerFunction.assign(localVar, this.right).setSourceLocation(this.getSourceLocation()).visit(blockCompiler);
                } else {
                    ownerFunction.assign(localVar, literal).visit(blockCompiler);
                }
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
            blockCompiler.getCode().jumpIf(localVar.getVariableSlot(), skip);
            right.outputToLocalVar(localVar, blockCompiler);
            skip.here();
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
