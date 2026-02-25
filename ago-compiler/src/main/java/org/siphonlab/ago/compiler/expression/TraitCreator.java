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
import org.siphonlab.ago.compiler.ConstructorDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

import java.util.ArrayList;

public class TraitCreator extends Creator{

    private final Expression bindPermit;

    public TraitCreator(FunctionDef ownerFunction,  Expression traitField, Expression bindPermit, SourceLocation sourceLocation) throws CompilationError {
        super(ownerFunction, traitField, new ArrayList<>(), sourceLocation);
        this.bindPermit = bindPermit;
    }

    @Override
    protected void validate(ClassDef classDef) throws TypeMismatchError {
        if(!classDef.isTrait()) {
            throw new TypeMismatchError("'%s' is not a trait".formatted(classDef.getFullname()), this.sourceLocation);
        }
    }

    @Override
    public TraitCreator setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    void beforeInvokeConstructor(Var.LocalVar instanceVar, BlockCompiler blockCompiler) throws CompilationError {
        if(bindPermit != null){
            blockCompiler.lockRegister(instanceVar);
            bindPermit.termVisit(blockCompiler);
            blockCompiler.releaseRegister(instanceVar);
        }
    }

    @Override
    protected Expression makeConstructorInvocation(Var.LocalVar localVar, ConstructorDef constructor) throws CompilationError {
        var c = ClassUnder.create(ownerFunction, localVar, constructor);
        c.setCandidates(null);
        var constructorInvocation = ownerFunction.invoke(Invoke.InvokeMode.Invoke, c, this.arguments, this.sourceLocation).setSourceLocation(this.getSourceLocation());
        return constructorInvocation.transform();
    }
}
