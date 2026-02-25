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
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;


import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.statement.Label;

import java.util.Objects;

import static org.siphonlab.ago.compiler.PrimitiveClassDef.BOOLEAN;
import static org.siphonlab.ago.opcode.logic.And.KIND_AND;

public class AndExpr extends ExpressionInFunctionBody {
    public Expression left;
    public Expression right;

    public AndExpr(FunctionDef ownerFunction, Expression left, Expression right) throws CompilationError {
        super(ownerFunction);
        this.left = left.transform();
        this.right = right.transform();
        this.left.setParent(this);
        this.right.setParent(this);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        ClassDef leftType = left.inferType();
        ClassDef rightType = right.inferType();
        if(leftType.getTypeCode() == TypeCode.BOOLEAN || leftType.getUnboxedTypeCode() == TypeCode.BOOLEAN){
            if(!(rightType.getTypeCode() == TypeCode.BOOLEAN
                    || rightType.getUnboxedTypeCode() == TypeCode.BOOLEAN)){
                return new AndExpr(ownerFunction, left, ownerFunction.cast(right, BOOLEAN, false).transform()).transform();
            }
        } else if(rightType.getTypeCode() == TypeCode.BOOLEAN
                || rightType.getUnboxedTypeCode() == TypeCode.BOOLEAN){
            return new AndExpr(ownerFunction, ownerFunction.cast(left, BOOLEAN, false).transform(), right).transform();
        }

        CastStrategy.UnifyTypeResult result = new CastStrategy(ownerFunction, this.getSourceLocation(), false).unifyTypes(this.left, this.right);
        if(result.changed() || result.left() != this.left || result.right() != this.right){
            this.left = result.left();
            this.right = result.right();
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
        try {
            blockCompiler.enter(this);

            if (this.left instanceof LiteralResultExpression lre) {
                Literal<?> literal = lre.visit(blockCompiler);
                if (BooleanLiteral.isTrue(literal)) {
                    ownerFunction.assign(localVar, this.right).setSourceLocation(this.getSourceLocation()).visit(blockCompiler);
                } else {
                    ownerFunction.assign(localVar, literal).visit(blockCompiler);
                }
            } else {
                this.left.outputToLocalVar(localVar, blockCompiler);
            }
            if (this.right instanceof Var var) {
                if (this.left.inferType() == BOOLEAN) {
                    var v1 = localVar;
                    Var.LocalVar v2 = (Var.LocalVar) var.visit(blockCompiler);
                    blockCompiler.getCode().biOperate(KIND_AND, left.inferType().getTypeCode(), v1.getVariableSlot(), v2.getVariableSlot(), localVar.getVariableSlot());
                    return;
                }
            }

            // make shortcut
            Label skip = blockCompiler.createLabel();
            blockCompiler.getCode().jumpIfNot(localVar.getVariableSlot(), skip);
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
