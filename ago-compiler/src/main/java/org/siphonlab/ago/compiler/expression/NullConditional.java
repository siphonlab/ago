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
import org.siphonlab.ago.compiler.NullableClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

import java.util.Objects;

public class NullConditional extends ExpressionInFunctionBody{

    private Expression expression;
    private ClassDef type;

    public NullConditional(FunctionDef ownerFunction, Expression expression){
        super(ownerFunction);
        this.expression = expression;
        expression.setParent(this);
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.expression = this.expression.transform();
        var exprType = expression.inferType();
        if(!(exprType instanceof NullableClassDef)){
            throw new TypeMismatchError("nullable class expected", this.expression.getSourceLocation());
        }
        NullableClassDef nullableClassDef = (NullableClassDef)exprType;
        this.type = nullableClassDef.getBaseClass();
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return type;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        new Cast(ownerFunction, this.expression, this.type).setSourceLocation(this.getSourceLocation()).transform().outputToLocalVar(localVar, blockCompiler);
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        return new Cast(ownerFunction, this.expression, this.type).setSourceLocation(this.getSourceLocation()).transform().visit(blockCompiler);
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.expression.termVisit(blockCompiler);
    }

    @Override
    public String toString() {
        return this.expression + "?";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NullConditional that)) return false;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, this.getClass());
    }

}
