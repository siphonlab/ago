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
package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;

import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;
import org.siphonlab.ago.compiler.expression.logic.Not;

public class DoWhileStmt extends LoopStmt {

    private Expression condition;
    private final Statement body;

    private boolean conditionNeg = false;

    public DoWhileStmt(FunctionDef ownerFunction, String label, Expression condition, Statement body) throws CompilationError {
        super(ownerFunction, label);
        this.condition = condition.setParent(this);
        this.body = body.setParent(this).transform();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        while(condition instanceof Not not){
            conditionNeg = !conditionNeg;
            this.condition = not.getValue();
        }
        this.condition = condition.transform();

        if(this.condition.inferType() instanceof NullableClassDef && !(this.condition instanceof NullableValue)){
            this.condition = new NullableValue(ownerFunction, this.condition);
        }

        if(this.condition instanceof Literal<?> literal){
            if(BooleanLiteral.isFalse(literal)){    // always false
                return this.body;
            }
        }
        return this;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();
            exitLabel = blockCompiler.createLabel();
            if (this.condition instanceof Literal<?> literal && BooleanLiteral.isTrue(literal)) {
                continueLabel = blockCompiler.createLabel().here();
                body.termVisit(blockCompiler);
                code.jump(continueLabel);
                exitLabel.here();
                return;
            }

            var bodyBegin = continueLabel = blockCompiler.createLabel().here();
            this.body.termVisit(blockCompiler);

            if (condition instanceof LiteralResultExpression literalResultExpression) {
                var tempVar = blockCompiler.acquireTempVar(literalResultExpression);
                ownerFunction.assign(tempVar, condition).setSourceLocation(condition.getSourceLocation()).visit(blockCompiler);
                code.jumpIf(tempVar.getVariableSlot(), bodyBegin);
            } else {
                Var.LocalVar condResult = (Var.LocalVar) condition.visit(blockCompiler);
                boolean checkCondResult = true;

                if(condition instanceof NullableValue nullableValue){
                    var isNull = nullableValue.isNull().visit(blockCompiler);
                    blockCompiler.releaseRegister(condResult);
                    if(!conditionNeg) {
                        code.jumpIf(isNull.getVariableSlot(), exitLabel);       // if is null, exit
                    } else {
                        code.jumpIf(isNull.getVariableSlot(), bodyBegin);
                    }
                    NullableValue.NonNullValue nonNullValue = nullableValue.nonNullValue();
                    ClassDef nonNullType = nonNullValue.inferType();
                    if(nonNullType.isPrimitiveBoxed()){
                        condResult = nonNullValue.visit(blockCompiler);
                        condResult = (Var.LocalVar) ownerFunction.unbox(condResult).transform().visit(blockCompiler);
                    } else if(nonNullType.getTypeCode() == TypeCode.OBJECT){
                        checkCondResult = false;
                    } else {
                        condResult = nonNullValue.visit(blockCompiler);
                    }
                }
                if(checkCondResult) {
                    if (!conditionNeg) {
                        code.jumpIf(condResult.getVariableSlot(), bodyBegin);
                    } else {
                        code.jumpIfNot(condResult.getVariableSlot(), bodyBegin);
                    }
                } else {
                    if (!conditionNeg) {
                        code.jump(bodyBegin);
                    } else {
                        code.jump(exitLabel);
                    }
                }
            }
            exitLabel.here();
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public DoWhileStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "do\n" + body + "\nwhile(%s)".formatted(condition);
    }
}
