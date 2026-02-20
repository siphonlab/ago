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
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.Objects;

public class SomeInstance extends ExpressionBase{

    private final ClassDef classDef;

    public SomeInstance(ClassDef classDef){
        this.classDef = classDef;
    }
    @Override
    public ClassDef inferType() throws CompilationError {
        return classDef;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        throw new UnsupportedOperationException("AnInstance is a placeholder expression");
    }

    @Override
    public String toString() {
        return "(SomeInstance %s)".formatted(classDef);
    }

    @Override
    public ExpressionBase setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SomeInstance that)) return false;
        return Objects.equals(classDef, that.classDef);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(classDef);
    }
}
