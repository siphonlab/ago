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

import java.util.Objects;

public class Unbox extends ExpressionInFunctionBody{
    private final Expression expression;

    public Unbox(FunctionDef ownerFunction,Expression expression){
        super(ownerFunction);
        this.expression = expression;

        this.setParent(expression.getParent());
        expression.setParent(this);
        this.setSourceLocation(expression.getSourceLocation());
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(this.expression.inferType().isPrimitiveFamily()) {
            return this.expression;
        }
        if(this.expression instanceof EnumValue enumValue){
            return enumValue.toLiteral();
        }
        if(this.expression instanceof ConstValue constValue){
            return constValue.toLiteral();
        }
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        var t = expression.inferType();
        return PrimitiveClassDef.fromBoxedType(t);
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var c = this.expression.visit(blockCompiler);
            CodeBuffer code = blockCompiler.getCode();
            if (c instanceof Literal<?> literal) {
                code.assignLiteral(localVar.getVariableSlot(), literal);
            } else {
                var v = (Var.LocalVar) c;
                code.unbox(localVar.getVariableSlot(), v.getVariableSlot());
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public String toString() {
        return "(Unbox %s)".formatted(expression);
    }

    @Override
    public Unbox setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Unbox unbox)) return false;
        return Objects.equals(expression, unbox.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expression);
    }
}
