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
import org.siphonlab.ago.compiler.Variable;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class ConstValue extends ExpressionBase{


    private final Variable finalVar;
    private final Literal<?> constLiteralValue;

    public ConstValue(Variable finalVar) {
        this.finalVar = finalVar;
        this.constLiteralValue = finalVar.getConstLiteralValue();
        assert constLiteralValue != null;
    }

    @Override
    public Expression transformInner() throws CompilationError {
        return this.toLiteral();
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return finalVar.getType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Assign.to(localVar, constLiteralValue).termVisit(blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public ConstValue setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(Const %s %s)".formatted(finalVar.getName(), constLiteralValue);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Literal<?> literal){
            return this.constLiteralValue.equals(literal);
        } else if(obj instanceof ConstValue c2){
            return this == c2 || this.finalVar.equals(c2.finalVar);
        }
        return false;
    }

    public Literal<?> toLiteral() {
        return this.constLiteralValue;
    }
}
