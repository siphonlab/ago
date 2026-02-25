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
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.List;
import java.util.Objects;

public class MapValue extends ExpressionInFunctionBody implements Assign.Assignee, CollectionElement {

    private final Expression map;
    private Expression indexExpr;
    private final ClassDef mapType;
    private final ClassDef keyType;
    private final ClassDef valueType;
    private final FunctionDef accessor;
    private Var.LocalVar processedMap;
    private TermExpression processedIndex;

    public MapValue(FunctionDef ownerFunction, Expression map, Expression indexExpr) throws CompilationError {
        super(ownerFunction);
        this.map = map.transform().setParent(this);
        ClassDef mapType = map.inferType();
        Root root = mapType.getRoot();
        if(!(root.getAnyReadonlyMap().isThatOrSuperOfThat(mapType) || root.getAnyReadwriteMap().isThatOrSuperOfThat(mapType))){
            throw new SyntaxError("map expected", map.getSourceLocation());
        }
        this.mapType = mapType;
        ClassRefLiteral[] typeArgumentsArray = mapType.getGenericSource().instantiationArguments().getTypeArgumentsArray();
        this.keyType = typeArgumentsArray[0].getClassDefValue();
        this.valueType = typeArgumentsArray[1].getClassDefValue();
        this.indexExpr = indexExpr;
        this.accessor = this.mapType.findMethod("get#key");
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return valueType;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.indexExpr = ownerFunction.cast(indexExpr.setParent(this).transform(), this.keyType).transform();
        return this;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        Var.LocalVar map = (Var.LocalVar) this.map.visit(blockCompiler);
        blockCompiler.lockRegister(map);
        var indexExpr = this.indexExpr.visit(blockCompiler);

        this.processedIndex = indexExpr;

        blockCompiler.enter(this);

        var invoke = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ownerFunction.classUnder(map, accessor), List.of(indexExpr), this.getSourceLocation());
        invoke.outputToLocalVar(localVar, blockCompiler);

        blockCompiler.leave(this);

        blockCompiler.releaseRegister(map);
        this.processedMap = map;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);
    }

    @Override
    public MapValue setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    public Expression getMap() {
        return map;
    }

    public Expression getIndexExpr() {
        return indexExpr;
    }

    @Override
    public String toString() {
        return "(MapValue %s[%s])".formatted(this.map, this.indexExpr);
    }

    public Var.LocalVar getProcessedCollection() {
        return processedMap;
    }

    public TermExpression getProcessedIndex() {
        return processedIndex;
    }

    @Override
    public Expression toPutElement(Expression processedCollection, TermExpression processedIndex, Expression value, FunctionDef ownerFunction) throws CompilationError {
        return new MapPut(ownerFunction, processedCollection, processedIndex, value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MapValue that)) return false;
        return Objects.equals(mapType, that.mapType) && Objects.equals(map, that.map) && Objects.equals(indexExpr, that.indexExpr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map, indexExpr, mapType);
    }
}
