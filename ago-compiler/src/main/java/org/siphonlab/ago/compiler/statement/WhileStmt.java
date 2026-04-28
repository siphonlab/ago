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

public class WhileStmt extends LoopStmt {

    private Expression condition;
    private final Statement body;

    private boolean conditionNeg = false;

    public WhileStmt(FunctionDef ownerFunction, String label, Expression condition, Statement body) throws CompilationError {
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
                return new EmptyStmt(ownerFunction).setSourceLocation(this.getSourceLocation());
            }
        }
        return this;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();
            this.exitLabel = blockCompiler.createLabel();

            if (this.condition instanceof Literal<?> literal && BooleanLiteral.isTrue(literal)) {
                var continueLabel = blockCompiler.createLabel().here();
                this.body.termVisit(blockCompiler);
                code.jump(continueLabel);
                this.exitLabel.here();
                return;
            }

            this.continueLabel = blockCompiler.createLabel().here();

            Label loopBodyLabel = blockCompiler.createLabel();
            if (condition instanceof LiteralResultExpression literalResultExpression) {
                var initValue = literalResultExpression.visit(blockCompiler);   // i.e. while(var i = 0){}
                if (BooleanLiteral.isFalse(initValue)) {
                    return;     // only condition evaluated once
                }
            } else {
                Var.LocalVar condResult = (Var.LocalVar) condition.visit(blockCompiler);
                boolean checkCondResult = true;
                if(condition instanceof NullableValue nullableValue){
                    var isNull = nullableValue.isNull().visit(blockCompiler);
                    if(!conditionNeg) {
                        code.jumpIf(isNull.getVariableSlot(), exitLabel);       // if is null, exit
                    } else {
                        code.jumpIf(isNull.getVariableSlot(), loopBodyLabel);
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
                        code.jumpIfNot(condResult.getVariableSlot(), exitLabel);
                    } else {
                        code.jumpIf(condResult.getVariableSlot(), exitLabel);
                    }
                }
            }
            loopBodyLabel.here();
            this.body.termVisit(blockCompiler);
            code.jump(continueLabel);       // evaluate condition again
            exitLabel.here();
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public WhileStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "while(%s) \n".formatted(condition) + body;
    }
}
