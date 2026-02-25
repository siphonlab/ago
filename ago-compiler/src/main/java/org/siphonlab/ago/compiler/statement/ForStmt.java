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

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;

public class ForStmt extends LoopStmt{

    private final Statement init;
    private final Expression condition;
    private final Statement updateStatement;
    private final Statement body;

    public ForStmt(FunctionDef ownerFunction, String label, Statement init, Expression condition, Statement updateStatement, Statement body) throws CompilationError {
        super(ownerFunction, label);
        this.init = (init != null) ? init.setParent(this).transform(): null;
        Expression c ;
        if(condition != null) {
            condition.setParent(this);
            c = ownerFunction.cast(condition, PrimitiveClassDef.BOOLEAN).transform();
        } else {
            c = null;
        }
        this.condition = c;
        this.updateStatement = updateStatement != null ? updateStatement.transform().setParent(this) : null;
        this.body = body.transform().setParent(this);
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();
            if (this.init != null) {
                this.init.termVisit(blockCompiler);
            }
            var beginLabel = blockCompiler.createLabel().here();
            this.exitLabel = blockCompiler.createLabel();
            this.continueLabel = blockCompiler.createLabel();
            if (this.condition != null) {
                if (this.condition instanceof Literal<?> literal) {
                    if (BooleanLiteral.isFalse(literal)) {    // always false
                        return;
                    } // otherwise always true
                } else if (this.condition instanceof LiteralResultExpression literalResultExpression) {
                    // condition already evaluated, and entranceLabel is no the condition evaluation
                    var tempVar = blockCompiler.acquireTempVar(this.condition).setSourceLocation(this.condition.getSourceLocation());
                    ownerFunction.assign(tempVar, this.condition).visit(blockCompiler);
                    code.jumpIfNot(tempVar.getVariableSlot(), exitLabel);
                } else {
                    Var.LocalVar r = (Var.LocalVar) this.condition.visit(blockCompiler);
                    code.jumpIfNot(r.getVariableSlot(), exitLabel);
                }
            }  // otherwise always true

            this.body.termVisit(blockCompiler);
            continueLabel.here();       // from i++ to continue
            if (this.updateStatement != null) {
                this.updateStatement.termVisit(blockCompiler);
            }
            code.jump(beginLabel);
            exitLabel.here();
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }
}
