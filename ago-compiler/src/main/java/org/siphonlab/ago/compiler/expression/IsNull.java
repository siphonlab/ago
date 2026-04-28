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

    public IsNull(FunctionDef ownerFunction, NullableValue nullableExpression, Type type) throws CompilationError {
        super(ownerFunction, nullableExpression, ownerFunction.getRoot().nullLiteral(), type);
    }

    @Override
    public Expression transformInner() throws CompilationError {
        this.left = this.left.transform();
        return this;
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        NullableValue nullableValue = (NullableValue) this.left;
        Var.LocalVar tempVar;
        if(nullableValue.hasReceiver() && ((NullableClassDef)nullableValue.inferType()).getBaseClass() == getRoot().BOOLEAN()){
            blockCompiler.lockRegister(nullableValue.getNonNullValueReceiver());
            tempVar = blockCompiler.acquireTempVar(this);
            blockCompiler.releaseRegister(nullableValue.getNonNullValueReceiver());
        } else {
            tempVar = blockCompiler.acquireTempVar(this);
        }

        this.outputToLocalVar(tempVar, blockCompiler);
        return tempVar;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            NullableValue nullableValue = (NullableValue) this.left;
            nullableValue.visit(blockCompiler);
            if(this.type == Type.Equals) {
                nullableValue.isNull().outputToLocalVar(localVar, blockCompiler);
            } else {
                nullableValue.isNotNull().outputToLocalVar(localVar, blockCompiler);
            }
            if(nullableValue.hasReceiver()){
                var code = blockCompiler.getCode();
                var exit = blockCompiler.createLabel();
                if(type == Type.Equals) {
                    code.jumpIf(localVar.getVariableSlot(), exit);
                } else {
                    code.jumpIfNot(localVar.getVariableSlot(), exit);
                }
                nullableValue.nonNullValue().visit(blockCompiler);
                exit.here();
            } else {
                nullableValue.releaseResult(blockCompiler);
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

}
