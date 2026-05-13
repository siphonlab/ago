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
package org.siphonlab.ago.compiler.expression.dynamic;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.ExpressionInFunctionBody;
import org.siphonlab.ago.compiler.expression.MaybeFunction;
import org.siphonlab.ago.compiler.expression.Var;
import org.siphonlab.ago.compiler.expression.invoke.Invoke;

import java.util.List;

import static org.siphonlab.ago.compiler.expression.invoke.Invoke.invokeCallFrame;

public class DynamicInvoke extends ExpressionInFunctionBody {

    private final Invoke.InvokeMode invokeMode;

    private Expression functor;
    private final List<Expression> arguments;
    private final Expression forkContext;

    public DynamicInvoke(FunctionDef ownerFunction, Invoke.InvokeMode invokeMode, Expression functor, List<Expression> arguments, Expression forkContext) throws CompilationError {
        super(ownerFunction);
        this.invokeMode = invokeMode;
        this.functor = functor.setParent(this);
        this.arguments = arguments;
        for (Expression argument : arguments) {
            argument.setParent(this);
        }

        this.forkContext = forkContext;
        functor.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        if (invokeMode.isAsync()) {
            return getRoot().getFunctionBaseOfAnyClass();
        } else {
            return getRoot().getAnyClass();
        }
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.functor = this.functor.transform();
        if(this.functor instanceof MaybeFunction maybeFunction){
            var r = ownerFunction.cast(new Invoke(ownerFunction, invokeMode, maybeFunction, arguments, getSourceLocation()).setParent(this.getParent()).transform(), inferType(), true);
            return r;
        }
        List<Expression> expressions = this.arguments;
        for (int i = 0; i < expressions.size(); i++) {
            Expression argument = expressions.get(i);
            arguments.set(i, argument.transform());
        }
        return this;
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
            Var.LocalVar instance;
            if(arguments.isEmpty()){
                // the functor maybe call frame or a no-args function
                instance = (Var.LocalVar) functor.visit(blockCompiler);

            } else {
                // the functor must be not call frame, call frame needn't argument
                // creator result won't be null, so the type is Object
                instance = (Var.LocalVar) new DynamicCreator(ownerFunction, functor, arguments).transform().visit(blockCompiler);
            }

            blockCompiler.lockRegister(instance);
            blockCompiler.getCode().ensureInvocable(instance.getVariableSlot());

            var r = blockCompiler.acquireTempVar(getRoot().getAnyClass());
            Invoke.invokeCallFrame(blockCompiler, this.invokeMode, instance, r, this.forkContext);
            blockCompiler.releaseRegister(instance);

            ownerFunction.cast(r, localVar.inferType(),true).transform().outputToLocalVar(localVar, blockCompiler);

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
            Var.LocalVar instance;
            if(arguments.isEmpty()){
                instance = (Var.LocalVar) functor.visit(blockCompiler);

            } else {
                instance = (Var.LocalVar) new DynamicCreator(ownerFunction, functor, arguments).transform().visit(blockCompiler);
            }

            blockCompiler.lockRegister(instance);
            blockCompiler.getCode().ensureInvocable(instance.getVariableSlot());

            Invoke.invokeCallFrame(blockCompiler, this.invokeMode, instance, this.forkContext);

            blockCompiler.releaseRegister(instance);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }    }

    @Override
    public String toString() {
        return "(DynamicInvoke %s %s %s)".formatted(invokeMode, functor, arguments);
    }
}
