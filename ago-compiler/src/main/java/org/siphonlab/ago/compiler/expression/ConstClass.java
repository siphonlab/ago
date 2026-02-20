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


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.Collection;
import java.util.Objects;

public class ConstClass extends ExpressionBase implements MaybeFunction {

    private final ClassDef classDef;

    public ConstClass(ClassDef classDef) {
        this.classDef = classDef;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        var m = classDef.getMetaClassDef();
        if (m == null) {
            return new PhantomMetaClassDef(classDef);
        }
        return m;
    }

    public ClassDef getClassDef() {
        return classDef;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        if (localVar instanceof Var.ReusingLocalVar reusingLocalVar && reusingLocalVar.isOutputted())
            return;

        blockCompiler.enter(this);
        blockCompiler.getCode().loadClass(localVar.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(this.getClassDef()));
        blockCompiler.leave(this);

        if (localVar instanceof Var.ReusingLocalVar reusingLocalVar) {
            reusingLocalVar.setOutputted(true);
        }
    }

    @Override
    public String toString() {
        return "(Class %s)".formatted(getClassDef().getFullname());
    }

    @Override
    public ConstClass setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean isFunction() {
        return getClassDef() instanceof FunctionDef;
    }

    @Override
    public FunctionDef getFunction() {
        return isFunction() ? (FunctionDef) getClassDef() : null;
    }

    private Collection<FunctionDef> candidateFunctions;

    @Override
    public void setCandidates(Collection<FunctionDef> candidates) {
        this.candidateFunctions = candidates;
    }

    @Override
    public Collection<FunctionDef> getCandidates() {
        return candidateFunctions;
    }

    @Override
    public Expression getScopeOfFunction() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ConstClass that = (ConstClass) o;
        return classDef == that.classDef;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ConstClass.class, classDef);
    }
}
