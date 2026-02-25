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
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.TermExpression;

public class ExpressionStmt extends Statement {

    protected Expression expression;

    public ExpressionStmt(FunctionDef ownerFunction, Expression expression) throws CompilationError {
        super(ownerFunction);
        this.expression = expression.transform().setParent(this);
        this.setSourceLocation(expression.getSourceLocation());
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        return expression.visit(blockCompiler);
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.expression.termVisit(blockCompiler);
    }

    @Override
    public String toString() {
        return expression.toString();
    }

    @Override
    public ExpressionStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
