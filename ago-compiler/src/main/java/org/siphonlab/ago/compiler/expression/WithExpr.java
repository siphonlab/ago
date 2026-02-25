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
package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.statement.Statement;

public class WithExpr extends ExpressionInFunctionBody{

    private final CurrWithExpression expression;
    private final Statement statement;

    public WithExpr(FunctionDef ownerFunction, CurrWithExpression expression, Statement statement) throws CompilationError {
        super(ownerFunction);
        this.expression = expression.transform();
        this.statement = statement.transform();
        statement.setParent(this);
        expression.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return expression.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            expression.outputToLocalVar(localVar, blockCompiler);
            blockCompiler.enterWith(expression);
            this.statement.termVisit(blockCompiler);
            blockCompiler.leaveWith(expression);
            blockCompiler.leave(this);
        } catch (CompilationError e) {
            throw e;
        }
    }

    @Override
    public ExpressionBase setSourceLocation(SourceLocation sourceLocation) {
        return super.setSourceLocation(sourceLocation);
    }

    @Override
    public String toString() {
        return "(WITH %s %s)".formatted(expression, statement);
    }
}
