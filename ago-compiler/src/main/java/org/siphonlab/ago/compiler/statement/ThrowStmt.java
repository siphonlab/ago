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
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Var;

public class ThrowStmt extends Statement{

    private final Expression expression;

    public ThrowStmt(Expression expression) throws CompilationError {
        this.expression = expression.transform();
        expression.setParent(this);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        ClassDef classDef = this.expression.inferType();
        if(!classDef.isThatOrDerivedFromThat(classDef.getRoot().getThrowableClass())){
            throw new ResolveError("'%s' is not throwable".formatted(classDef.getFullname()), this.getSourceLocation());
        }
        return this;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.validateThrowException(this.expression.inferType(), this);

        try {
            blockCompiler.enter(this);

            Var.LocalVar v = (Var.LocalVar) this.expression.visit(blockCompiler);
            blockCompiler.getCode().throw_(v.getVariableSlot());
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public ThrowStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "throw %s".formatted(expression);
    }
}
