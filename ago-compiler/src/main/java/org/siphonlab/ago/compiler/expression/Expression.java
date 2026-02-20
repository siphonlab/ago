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
import org.siphonlab.ago.compiler.exception.CompilationError;

public interface Expression {

    /**
     * for LiteralExpression, return itself
     * for LocalVarResultExpression, return a LocalVar
     * for other expression, its `visit` invoke `outputToLocalVar(tempVar)`
     * @param blockCompiler
     * @return
     */
    TermExpression visit(BlockCompiler blockCompiler) throws CompilationError;

    Expression transform() throws CompilationError;

    ClassDef inferType() throws CompilationError;

    /**
     * every expression must support output to localVar
     * @param localVar
     * @param blockCompiler
     */
    void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError;

    void termVisit(BlockCompiler blockCompiler) throws CompilationError;

    Expression setSourceLocation(SourceLocation sourceLocation);

    SourceLocation getSourceLocation();

    Expression setParent(Expression expression);

    Expression getParent();
}

