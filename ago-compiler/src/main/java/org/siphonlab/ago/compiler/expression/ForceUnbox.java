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

class ForceUnbox extends ExpressionBase{

    private final Expression expression;
    private final ClassDef toType;

    public ForceUnbox(Expression expression, ClassDef implicitOrExplicitPrimaryClass){
        this.expression = expression;
        this.toType = implicitOrExplicitPrimaryClass;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return toType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);
            Var.LocalVar result = (Var.LocalVar) this.expression.visit(blockCompiler);
            blockCompiler.getCode().force_unbox(localVar.getVariableSlot(), result.getVariableSlot(), toType.getTypeCode());
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public String toString() {
        return "(ForceUnbox %s to %s)".formatted(expression, toType);
    }
}
