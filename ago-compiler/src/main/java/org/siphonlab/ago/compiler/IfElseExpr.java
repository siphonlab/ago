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
package org.siphonlab.ago.compiler;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;
import org.siphonlab.ago.compiler.expression.logic.AndExpr;
import org.siphonlab.ago.compiler.expression.logic.Not;
import org.siphonlab.ago.compiler.statement.Label;

import java.util.Objects;

public class IfElseExpr extends ExpressionInFunctionBody {


    private final Expression ifPart;
    private Expression condition;
    private final Expression elsePart;

    private boolean conditionNeg = false;

    public IfElseExpr(FunctionDef ownerFunction, Expression ifPart, Expression condition, Expression elsePart) throws CompilationError {
        super(ownerFunction);
        this.ifPart = ifPart.setParent(this).transform();
        this.condition = condition.setParent(this);
        this.elsePart = ownerFunction.cast(elsePart, this.ifPart.inferType()).transform();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        while(condition instanceof Not not){
            conditionNeg = !conditionNeg;
            this.condition = not.getValue();
        }
        this.condition = this.condition.transform();

        if(this.condition instanceof Literal<?> literal){
            if(BooleanLiteral.isTrue(literal)){
                return ifPart;
            } else {
                return elsePart;
            }
        }
        var t = this.condition.inferType();
        if(t instanceof NullableClassDef n){
            var baseClass = n.getBaseClass();
            PipeToTempVar maybeNull;
            var nonNull = ownerFunction.cast(maybeNull = new PipeToTempVar(ownerFunction, condition,true), baseClass).transform();
            this.condition = new AndExpr(ownerFunction,
                    new Equals(ownerFunction, maybeNull, getRoot().nullLiteral(), Equals.Type.NotEquals),
                    ownerFunction.cast(nonNull, getRoot().BOOLEAN()).transform()
            ).usingTempVariable(maybeNull).transform();
        }
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return this.ifPart.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        var term = this.condition.transform().visit(blockCompiler);
        try {
            blockCompiler.enter(this);

            if(term instanceof Literal<?> literal){
                if(BooleanLiteral.isTrue(literal)){
                    ifPart.outputToLocalVar(localVar,blockCompiler);
                } else {
                    elsePart.outputToLocalVar(localVar,blockCompiler);
                }
            } else {
                Var.LocalVar condResult = (Var.LocalVar) term;
                CodeBuffer code = blockCompiler.getCode();
                Label elseLabel = blockCompiler.createLabel();
                Label trueLabel = blockCompiler.createLabel();
                Label exit = blockCompiler.createLabel();
                if(condResult.inferType() instanceof NullableClassDef nullableClassDef){
                    blockCompiler.lockRegister(condResult);
                    var isNull = (Var.LocalVar)new Equals(ownerFunction, condResult, getRoot().nullLiteral(), Equals.Type.Equals).visit(blockCompiler);
                    blockCompiler.releaseRegister(condResult);
                    if(!conditionNeg) {
                        code.jumpIf(isNull.getVariableSlot(), elseLabel);       // if is null, goto else
                    } else {
                        code.jumpIf(isNull.getVariableSlot(), trueLabel);
                    }
                    condResult = (Var.LocalVar) ownerFunction.cast(condResult, nullableClassDef.getBaseClass()).transform().visit(blockCompiler);
                }
                if(!conditionNeg) {
                    code.jumpIfNot(condResult.getVariableSlot(), elseLabel);
                } else {
                    code.jumpIf(condResult.getVariableSlot(), elseLabel);
                }
                trueLabel.here();
                ownerFunction.assign(localVar, ifPart).setSourceLocation(ifPart.getSourceLocation()).termVisit(blockCompiler);
                code.jump(exit);
                elseLabel.here();
                ownerFunction.assign(localVar,elsePart).setSourceLocation(elsePart.getSourceLocation()).termVisit(blockCompiler);
                exit.here();
            }
        } catch (CompilationError e){
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public String toString() {
        return "(%s If %s Else %s)".formatted(ifPart, condition, elsePart);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IfElseExpr that)) return false;
        return Objects.equals(ifPart, that.ifPart) && Objects.equals(condition, that.condition) && Objects.equals(elsePart, that.elsePart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ifPart, condition, elsePart);
    }

    @Override
    public IfElseExpr setParent(Expression expression) {
        super.setParent(expression);
        return this;
    }

    @Override
    public IfElseExpr setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
