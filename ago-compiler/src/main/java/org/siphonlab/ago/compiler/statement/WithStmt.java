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
import org.siphonlab.ago.compiler.NullableClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.CurrWithExpression;
import org.siphonlab.ago.compiler.expression.Expression;

public class WithStmt extends Statement{


    private final CurrWithExpression withExpression;
    private final Statement statement;

    public WithStmt(FunctionDef ownerFunction, CurrWithExpression withExpression, Statement statement) throws CompilationError {
        super(ownerFunction);
        this.withExpression = withExpression.transform();
        this.statement = statement.transform();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        return super.transformInner();
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            withExpression.visit(blockCompiler);

            blockCompiler.enterWith(withExpression);
            statement.termVisit(blockCompiler);
            blockCompiler.leaveWith(withExpression);
            blockCompiler.leave(this);
        } catch (CompilationError e) {
            throw e;
        }

    }

    @Override
    public WithStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "with(%s) %s".formatted(withExpression, statement);
    }
}
