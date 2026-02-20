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
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

@Deprecated
public class ExtractScopeFromScopedClassInterval extends ExpressionBase{

    private final Expression scopedClassIntervalInstance;
    private final ClassDef ObjectType;

    public ExtractScopeFromScopedClassInterval(Expression scopedClassIntervalInstance, ClassDef ObjectType){
        this.scopedClassIntervalInstance = scopedClassIntervalInstance;
        this.ObjectType = ObjectType;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return ObjectType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        var classDef = blockCompiler.getFunctionDef();
        ClassDef classInterval = classDef.getRoot().getScopedClassInterval();
        if(!this.scopedClassIntervalInstance.inferType().isDeriveFrom(classInterval)){
            throw new TypeMismatchError("a ClassInterval expression expected", this.getSourceLocation());
        }

        var fld = new Var.Field(scopedClassIntervalInstance, classInterval.getVariable("scope"));

        try {
            blockCompiler.enter(this);

            fld.setSourceLocation(this.getSourceLocation()).outputToLocalVar(localVar, blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }
}
