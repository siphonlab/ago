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
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.LiteralResultExpression;
import org.siphonlab.ago.compiler.expression.Var;
import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;

public class WhileStmt extends LoopStmt {

    private Expression condition;
    private final Statement body;

    public WhileStmt(String label, Expression condition, Statement body) throws CompilationError {
        super(label);
        this.condition = condition.setParent(this).transform();
        this.body = body.setParent(this).transform();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(this.condition instanceof Literal<?> literal){
            if(BooleanLiteral.isFalse(literal)){    // always false
                return new EmptyStmt().setSourceLocation(this.getSourceLocation());
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
                var bodyBeginLabel = blockCompiler.createLabel().here();
                this.continueLabel = bodyBeginLabel;
                this.body.termVisit(blockCompiler);
                this.exitLabel.here();
                code.jump(bodyBeginLabel);
                return;
            }

            this.continueLabel = blockCompiler.createLabel().here();

            if (condition instanceof LiteralResultExpression literalResultExpression) {
                var initValue = literalResultExpression.visit(blockCompiler);   // i.e. while(var i = 0){}
                if (BooleanLiteral.isFalse(initValue)) {
                    return;     // only condition evaluated once
                }
            } else {
                Var.LocalVar r = (Var.LocalVar) condition.visit(blockCompiler);
                code.jumpIfNot(r.getVariableSlot(), exitLabel);
            }
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
