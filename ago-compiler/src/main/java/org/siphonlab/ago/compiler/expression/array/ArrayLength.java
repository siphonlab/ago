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
package org.siphonlab.ago.compiler.expression.array;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.ExpressionBase;
import org.siphonlab.ago.compiler.expression.Var;

public class ArrayLength extends ExpressionBase {

    private final Expression array;

    public ArrayLength(Expression array){
        this.array = array;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return PrimitiveClassDef.INT;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        ClassDef classDef = array.inferType();
        var lengthField = classDef.getFields().get("length");
        Var.of(array,lengthField).setSourceLocation(this.getSourceLocation()).outputToLocalVar(localVar,blockCompiler);
    }

    @Override
    public String toString() {
        return "(Length %s)".formatted(array);
    }
}
