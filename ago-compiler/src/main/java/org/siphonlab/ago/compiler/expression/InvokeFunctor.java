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

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;

public class InvokeFunctor extends ExpressionBase{

    private final Invoke.InvokeMode invokeMode;

    private final Expression functor;
    private final Expression forkContext;

    @Override
    public ClassDef inferType() throws CompilationError {
        if(invokeMode.isAsync()){
            return functor.inferType();
        } else {
            // Function<R>
            return functor.inferType().getGenericSource().instantiationArguments().getTypeArgumentsArray()[0].getClassDefValue();
        }
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);
            blockCompiler.lockRegister(localVar);
            Var.LocalVar instance = (Var.LocalVar) functor.visit(blockCompiler);
            blockCompiler.lockRegister(instance);

            Var.LocalVar forkContextVar = null;
            if(forkContext != null){
                forkContextVar = (Var.LocalVar) this.forkContext.visit(blockCompiler);
            }
            if (invokeMode.isAsync()) {
                if(forkContext != null){
                    blockCompiler.getCode().invokeAsyncViaContext(invokeMode, instance.getVariableSlot(), localVar.getVariableSlot(), forkContextVar.getVariableSlot());
                } else {
                    blockCompiler.getCode().invokeAsync(invokeMode, instance.getVariableSlot(), localVar.getVariableSlot());
                }
            } else {
                if(invokeMode == Invoke.InvokeMode.Await && forkContext != null){
                    blockCompiler.getCode().invokeAsyncViaContext(invokeMode, instance.getVariableSlot(), forkContextVar.getVariableSlot());
                } else {
                    blockCompiler.getCode().invoke(invokeMode, instance.getVariableSlot());
                }

                if (localVar.getVariableSlot().getTypeCode() == TypeCode.OBJECT
                        && localVar.getVariableSlot().getClassDef() == blockCompiler.getFunctionDef().getRoot().getAnyClass()) {
                    blockCompiler.getCode().acceptAny(localVar.getVariableSlot());
                } else {
                    blockCompiler.getCode().accept(localVar.getVariableSlot());
                }
            }
            if (instance.varMode == Var.LocalVar.VarMode.Temp) {
                // release the register after invoke if it's a temp var
                Assign.to(instance, new NullLiteral(functor.inferType())).termVisit(blockCompiler);
            }
            blockCompiler.releaseRegister(instance);
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
            blockCompiler.lockRegister(instance);

            Var.LocalVar forkContextVar = null;
            if(forkContext != null){
                forkContextVar = (Var.LocalVar) this.forkContext.visit(blockCompiler);
            }

            if(forkContext != null){
                blockCompiler.getCode().invokeAsyncViaContext(invokeMode, instance.getVariableSlot(), forkContextVar.getVariableSlot());
            } else {
                blockCompiler.getCode().invoke(invokeMode, instance.getVariableSlot());
            }

            if (instance.varMode == Var.LocalVar.VarMode.Temp) {
                // release the register after invoke if it's a temp var
                Assign.to(instance, new NullLiteral(functor.inferType())).termVisit(blockCompiler);
            }
            blockCompiler.releaseRegister(instance);
        } catch(CompilationError e){
            throw e;
        } finally{
            blockCompiler.leave(this);
        }
    }

    public InvokeFunctor(Invoke.InvokeMode invokeMode, Expression functor, Expression forkContext){
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
