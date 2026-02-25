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

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.List;

public class FunctionCreator extends Invoke{

    public FunctionCreator(FunctionDef ownerFunction, ClassDef scopeClass, MaybeFunction maybeFunction, List<Expression> arguments, SourceLocation sourceLocation) throws CompilationError {
        super(ownerFunction, InvokeMode.Invoke, maybeFunction, arguments, sourceLocation);
    }

    public FunctionCreator(FunctionDef ownerFunction, MaybeFunction maybeFunction, List<Expression> arguments, SourceLocation sourceLocation) throws CompilationError {
        super(ownerFunction, InvokeMode.Invoke, maybeFunction, arguments, sourceLocation);
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            this.prepareInvocation(blockCompiler, localVar);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return resolvedFunctionDef;
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            return this.prepareInvocation(blockCompiler, null);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public String toString() {
        if(this.scope == null){
            return "(FunctionInstance %s [%s] %s)".formatted(resolvedFunctionDef.getFullnameWithoutPackage(), StringUtils.join(arguments, ","), this.maybeFunction);
        } else {
            return "(FunctionInstance %s::%s [%s] %s)".formatted(this.scope, resolvedFunctionDef.getName(), StringUtils.join(arguments, ","), this.maybeFunction);
        }
    }

    @Override
    public FunctionCreator setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
