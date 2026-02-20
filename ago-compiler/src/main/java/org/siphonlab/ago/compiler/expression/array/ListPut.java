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


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;

import java.util.List;
import java.util.Objects;

public class ListPut extends ExpressionBase {

    private final Expression list;
    private Expression indexExpr;
    private Expression value;
    private final FunctionDef accessor;
    private final ClassDef elementType;

    public ListPut(Expression list, Expression indexExpr, Expression value) throws CompilationError {
        list = list.transform().setParent(this);
        ClassDef listType = list.inferType();
        Root root = listType.getRoot();
        if(!(root.getAnyReadwriteList().isThatOrSuperOfThat(listType))){
            throw new SyntaxError("writable list expected", list.getSourceLocation());
        }
        this.list = list;
        this.elementType = listType.getGenericSource().instantiationArguments().getTypeArgumentsArray()[0].getClassDefValue();
        this.indexExpr = indexExpr;
        this.value = value;
        this.accessor = listType.findMethod("set#index");
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.indexExpr = new Cast(indexExpr, PrimitiveClassDef.INT).transform().setParent(this);
        this.value = new Cast(value, this.elementType).setParent(this).transform();
        return this;
    }

    public ListPut(ListElement listElement, Expression value) throws CompilationError {
        this(listElement.getList(), listElement.getIndexExpr(), value);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return this.elementType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var r = this.visit(blockCompiler);
            r.outputToLocalVar(localVar, blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Var.LocalVar array = (Var.LocalVar) this.list.visit(blockCompiler);
            blockCompiler.lockRegister(array);

            var indexExpr = this.indexExpr.visit(blockCompiler);
            blockCompiler.lockRegister(indexExpr);

            var value = this.value.visit(blockCompiler);
            blockCompiler.lockRegister(value);

            var invoke = new Invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(list, accessor), List.of(indexExpr, value), this.getSourceLocation());
            invoke.termVisit(blockCompiler);

            blockCompiler.releaseRegister(array);
            blockCompiler.releaseRegister(indexExpr);
            blockCompiler.releaseRegister(value);
            return value;
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);
    }

    @Override
    public ListPut setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(ListPut %s[%s] = %s)".formatted(this.list, this.indexExpr, this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ListPut that)) return false;
        return Objects.equals(list, that.list) && Objects.equals(indexExpr, that.indexExpr) &&  Objects.equals(value, that.value) ;
    }
}
