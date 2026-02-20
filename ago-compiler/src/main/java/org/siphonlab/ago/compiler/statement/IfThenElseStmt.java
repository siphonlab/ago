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
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.LiteralResultExpression;
import org.siphonlab.ago.compiler.expression.Var;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;
import org.siphonlab.ago.compiler.expression.logic.Not;

public class IfThenElseStmt extends Statement {


    private Expression condition;
    private final Statement trueBranch;
    private final Statement falseBranch;

    private boolean conditionNeg = false;

    public IfThenElseStmt(Expression condition, Statement trueBranch, Statement falseBranch) throws CompilationError {
        this.condition = condition.setParent(this);
        this.trueBranch = trueBranch.setParent(this);
        this.falseBranch = falseBranch == null ? null : falseBranch.setParent(this);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.condition = condition.transform();
        while(condition instanceof Not not){
            conditionNeg = !conditionNeg;
            this.condition = not.getValue();
        }
        if(trueBranch instanceof EmptyStmt){
            if(falseBranch != null && !(falseBranch instanceof EmptyStmt)){
                return new IfThenElseStmt(new Not(condition), falseBranch, null).setSourceLocation(this.getSourceLocation()).transform();
            } else {
                // only evaluate the condition
                return new ExpressionStmt(condition);
            }
        }
        if(this.condition instanceof Literal<?> literal){
            if(BooleanLiteral.isTrue(literal)){
                return trueBranch;
            } else {
                return falseBranch != null ? falseBranch : new EmptyStmt().setSourceLocation(this.getSourceLocation());
            }
        }
        return this;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            if (condition instanceof LiteralResultExpression literalResultExpression) {
                var literal = literalResultExpression.visit(blockCompiler);
                if (BooleanLiteral.isTrue(literal)) {
                    if(!conditionNeg) {
                        trueBranch.termVisit(blockCompiler);
                    } else {
                        falseBranch.termVisit(blockCompiler);
                    }
                } else if (falseBranch != null) {
                    if(!conditionNeg) {
                        falseBranch.termVisit(blockCompiler);
                    } else {
                        trueBranch.termVisit(blockCompiler);
                    }
                }
                return;
            }
            Var.LocalVar condResult = (Var.LocalVar) condition.visit(blockCompiler);
            CodeBuffer code = blockCompiler.getCode();
            var exitLabel = blockCompiler.createLabel();
            Label elseLabel = blockCompiler.createLabel();
            if(!conditionNeg) {
                code.jumpIfNot(condResult.getVariableSlot(), elseLabel);
            } else {
                code.jumpIf(condResult.getVariableSlot(), elseLabel);
            }
            this.trueBranch.termVisit(blockCompiler);
            if (this.falseBranch != null) {
                code.jump(exitLabel);
                elseLabel.here();
                this.falseBranch.termVisit(blockCompiler);
            } else {
                elseLabel.here();
            }
            exitLabel.here();
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public IfThenElseStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        var s =  "if(%s) \n".formatted(condition) + trueBranch;
        if(this.falseBranch != null){
            s += "\nelse\n" + this.falseBranch;
        }
        return s;
    }
}
