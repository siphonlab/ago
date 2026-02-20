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
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.ExpressionBase;
import org.siphonlab.ago.compiler.expression.TermExpression;
import org.siphonlab.ago.compiler.expression.Var;

public abstract class Statement extends ExpressionBase {

    @Override
    public ClassDef inferType() throws CompilationError {
        throw new UnsupportedOperationException("statement not support inferType");
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        throw new UnsupportedOperationException("statement not support visit");
    }

    @Override
    public abstract void termVisit(BlockCompiler blockCompiler) throws CompilationError ;


    @Override
    public Statement transform() throws CompilationError {
        var r = (Statement)super.transform();
        return r;
    }

    @Override
    public Statement setParent(Expression expression) {
        super.setParent(expression);
        return this;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        throw new UnsupportedOperationException("statement not support outputToLocalVar");
    }

    @Override
    public Statement setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
