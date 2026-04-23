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


import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;

/**
 */
public class IsNull extends Equals{

    private final Var.LocalVar nonNullValueReceiver;

    public IsNull(FunctionDef ownerFunction, Expression nullableExpression, Type type, Var.LocalVar nonNullValueReceiver) throws CompilationError {
        super(ownerFunction, nullableExpression, ownerFunction.getRoot().nullLiteral(), type);
        this.nonNullValueReceiver = nonNullValueReceiver;
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        Var.LocalVar tempVar = blockCompiler.acquireTempVar(this);
        this.outputToLocalVar(tempVar, blockCompiler);
        return tempVar;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        CodeBuffer code = blockCompiler.getCode();
        try {
            blockCompiler.enter(this);

            NullableClassDef nullableClassDef = (NullableClassDef) this.left.inferType();
            var result = (Var.LocalVar)this.left.visit(blockCompiler);
            blockCompiler.lockRegister(localVar);

            if(type == Type.Equals) {
                code.equalsNull(localVar.getVariableSlot(), result.getVariableSlot());
            } else {
                code.notEqualsNull(localVar.getVariableSlot(), result.getVariableSlot());
            }

            if(this.nonNullValueReceiver != null){
                var exit = blockCompiler.createLabel();
                if(type == Type.Equals) {
                    code.jumpIf(localVar.getVariableSlot(), exit);
                } else {
                    code.jumpIfNot(localVar.getVariableSlot(), exit);
                }
                ownerFunction.cast(result, nullableClassDef.getBaseClass()).transform().outputToLocalVar(nonNullValueReceiver, blockCompiler);
                exit.here();
            }

            blockCompiler.releaseRegister(localVar);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    public Var.LocalVar getNonNullValueReceiver() {
        return nonNullValueReceiver;
    }
}
