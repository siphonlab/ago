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
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Invoke;
import org.siphonlab.ago.compiler.expression.Var;

public class AsyncInvokeFunctorStmt extends Statement{

    private final Expression functor;
    private final Invoke.InvokeMode invokeMode;

    public AsyncInvokeFunctorStmt(FunctionDef ownerFunction, Invoke.InvokeMode invokeMode, Expression functor) {
        super(ownerFunction);
        this.invokeMode = invokeMode;
        this.functor = functor;
        functor.setParent(this);
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);
            Var.LocalVar instance = (Var.LocalVar) functor.visit(blockCompiler);

            blockCompiler.getCode().invoke(invokeMode, instance.getVariableSlot());

        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public String toString() {
        return "(%s %s)".formatted(invokeMode, functor);
    }
}
