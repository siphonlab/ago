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
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.Collections;
import java.util.Objects;

public class ToString extends ExpressionInFunctionBody{

    private final Expression expression;
    private final ClassDef exprClass;

    public ToString(FunctionDef ownerFunction, Expression expression, ClassDef exprClass) throws CompilationError {
        super(ownerFunction);
        this.expression = expression.transform();
        this.setParent(expression.getParent());
        expression.setParent(this);
        this.exprClass = exprClass;
    }

    public ToString(FunctionDef ownerFunction,Expression expression) throws CompilationError {
        this(ownerFunction, expression, expression.inferType());
    }


    @Override
    public ClassDef inferType() throws CompilationError {
        return PrimitiveClassDef.STRING;
    }

    @Override
    public String toString() {
        return "(ToString %s)".formatted(expression);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(expression instanceof Literal<?> literal){
            return ownerFunction.cast(literal, PrimitiveClassDef.STRING).transform();
        } else {
            return ownerFunction.invoke(Invoke.InvokeMode.Invoke, new ClassUnder.ClassUnderInstance(ownerFunction, expression, exprClass.findMethod("toString#")), Collections.emptyList(), this.getSourceLocation());
        }
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        throw new RuntimeException("already transformed to Invoke");
    }

    @Override
    public ToString setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ToString toString)) return false;
        return Objects.equals(expression, toString.expression) && Objects.equals(exprClass, toString.exprClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, exprClass);
    }
}
