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
import org.siphonlab.ago.compiler.expression.*;

import java.util.List;
import java.util.Objects;

public class ListElement extends ExpressionInFunctionBody implements Assign.Assignee, CollectionElement {

    private final Expression list;
    private Expression indexExpr;
    private final ClassDef listType;
    private final ClassDef elementType;
    private final FunctionDef accessor;
    private Var.LocalVar processedList;
    private TermExpression processedIndex;

    public ListElement(FunctionDef ownerFunction,Expression list, Expression indexExpr) throws CompilationError {
        super(ownerFunction);
        this.list = list.transform().setParent(this);
        ClassDef listType = list.inferType();
        Root root = listType.getRoot();
        if(!(root.getAnyReadonlyList().isThatOrSuperOfThat(listType) || root.getAnyReadwriteList().isThatOrSuperOfThat(listType))){
            throw new SyntaxError("list expected", list.getSourceLocation());
        }
        this.listType = listType;
        elementType = listType.getGenericSource().instantiationArguments().getTypeArgumentsArray()[0].getClassDefValue();
        this.indexExpr = indexExpr;
        this.accessor = this.listType.findMethod("get#index");
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.indexExpr = ownerFunction.cast(indexExpr.setParent(this).transform(), PrimitiveClassDef.INT).transform();
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return elementType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        Var.LocalVar list = (Var.LocalVar) this.list.visit(blockCompiler);
        blockCompiler.lockRegister(list);
        var indexExpr = this.indexExpr.visit(blockCompiler);

        this.processedIndex = indexExpr;

        blockCompiler.enter(this);

        var invoke = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ownerFunction.classUnder(list, accessor), List.of(indexExpr), this.getSourceLocation());
        invoke.outputToLocalVar(localVar, blockCompiler);

        blockCompiler.leave(this);

        blockCompiler.releaseRegister(list);
        this.processedList = list;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);
    }

    @Override
    public ListElement setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    public Expression getList() {
        return list;
    }

    public Expression getIndexExpr() {
        return indexExpr;
    }

    @Override
    public String toString() {
        return "(ListElement %s[%s])".formatted(this.list, this.indexExpr);
    }

    public Var.LocalVar getProcessedCollection() {
        return processedList;
    }

    public TermExpression getProcessedIndex() {
        return processedIndex;
    }

    @Override
    public Expression toPutElement(Expression processedCollection, TermExpression processedIndex, Expression value, FunctionDef ownerFunction) throws CompilationError {
        return new ListPut(ownerFunction, processedCollection, processedIndex, value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ListElement that)) return false;
        return Objects.equals(listType, that.listType) && Objects.equals(list, that.list) && Objects.equals(indexExpr, that.indexExpr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list, indexExpr, listType);
    }
}
