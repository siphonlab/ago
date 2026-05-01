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
package org.siphonlab.ago.compiler.expression.invoke;

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.ExpressionInFunctionBody;
import org.siphonlab.ago.compiler.expression.Var;
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;

import static org.siphonlab.ago.compiler.expression.invoke.Invoke.invokeCallFrame;

/**
 * Functor is a created call frame, InvokeFunctor is to run the call frame
 */
public class InvokeCallFrame extends ExpressionInFunctionBody {

    private final Invoke.InvokeMode invokeMode;

    private final Expression functor;
    private final Expression forkContext;

    @Override
    public ClassDef inferType() throws CompilationError {
        if(invokeMode.isAsync()){
            return functor.inferType();
        } else {
            if(functor.inferType() instanceof FunctionDef f){
                return f.getResultType();
            } else {
                // Function<R>
                return functor.inferType().getGenericSource().typeArguments()[0].getClassDefValue();
            }
        }
    }

    public Expression getForkContext() {
        return forkContext;
    }

    public Invoke.InvokeMode getInvokeMode() {
        return invokeMode;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);
            Var.LocalVar instance = (Var.LocalVar) functor.visit(blockCompiler);

            blockCompiler.lockRegister(localVar);
            invokeCallFrame(blockCompiler, invokeMode, instance, localVar, forkContext);
            blockCompiler.releaseRegister(localVar);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);
            Var.LocalVar instance = (Var.LocalVar) functor.visit(blockCompiler);

            invokeCallFrame(blockCompiler, invokeMode, instance, forkContext);

        } catch(CompilationError e){
            throw e;
        } finally{
            blockCompiler.leave(this);
        }
    }

    public InvokeCallFrame(FunctionDef ownerFunction, Invoke.InvokeMode invokeMode, Expression functor, Expression forkContext){
        super(ownerFunction);
        this.invokeMode = invokeMode;
        this.functor = functor;
        this.forkContext = forkContext;
        functor.setParent(this);
    }

    @Override
    public String toString() {
        return "(%s %s)".formatted(invokeMode, functor);
    }
}
