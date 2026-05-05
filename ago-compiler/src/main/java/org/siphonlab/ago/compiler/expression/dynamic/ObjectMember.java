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
package org.siphonlab.ago.compiler.expression.dynamic;


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.NullableClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.array.CollectionElement;
import org.siphonlab.ago.compiler.expression.array.MapPut;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;
import org.siphonlab.ago.compiler.resolvepath.NamePathResolver;

import java.util.Objects;

public class ObjectMember extends ExpressionInFunctionBody implements Assign.Assignee, CollectionElement {

    private Expression object;
    private Expression indexExpr;
    private Var.LocalVar processedObject;
    private TermExpression processedIndex;

    public ObjectMember(FunctionDef ownerFunction, Expression object, Expression indexExpr) throws CompilationError {
        super(ownerFunction);
        this.object = object.transform().setParent(this);
        this.indexExpr = indexExpr.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return getRoot().getAnyClass();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(this.object.inferType() instanceof NullableClassDef nullableClassDef){
            return BlockCompiler.nullableIfThenExpr(ownerFunction, this.object, nonNullExpression -> new ObjectMember(ownerFunction, nonNullExpression, indexExpr));
        }
        this.object = ownerFunction.cast(object, getRoot().getObjectClass()).transform();
        this.indexExpr = ownerFunction.cast(indexExpr.setParent(this).transform(), getRoot().STRING()).transform();
//        if(this.indexExpr instanceof Literal<?> literal){
//            StringLiteral index = (StringLiteral) literal;
//            var resolver = new NamePathResolver(NamePathResolver.ResolveMode.ForVariable, ownerFunction.getUnit(), ownerFunction, object, index);
//            try{
//                return resolver.resolve();
//            } catch (ResolveError _){
//                resolver = new NamePathResolver(NamePathResolver.ResolveMode.ForInvokable, ownerFunction.getUnit(), ownerFunction, object, index);
//            }
//            try{
//                return resolver.resolve();
//            } catch (ResolveError _){
//
//            }
//        }
        return this;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        Var.LocalVar object = (Var.LocalVar) this.object.visit(blockCompiler);
        blockCompiler.lockRegister(object);
        var indexExpr = (Var.LocalVar)this.indexExpr.visit(blockCompiler);

        this.processedIndex = indexExpr;

        blockCompiler.enter(this);

        blockCompiler.getCode().getDynamicMember(localVar.getVariableSlot(), object.getVariableSlot(), indexExpr.getVariableSlot());

        blockCompiler.leave(this);

        blockCompiler.releaseRegister(object);
        this.processedObject = object;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);
    }

    @Override
    public ObjectMember setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    public Expression getObject() {
        return object;
    }

    public Expression getIndexExpr() {
        return indexExpr;
    }

    @Override
    public String toString() {
        return "(ObjectMember %s[%s])".formatted(this.object, this.indexExpr);
    }

    public Var.LocalVar getProcessedCollection() {
        return processedObject;
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
        if (!(o instanceof ObjectMember that)) return false;
        return Objects.equals(object, that.object) && Objects.equals(indexExpr, that.indexExpr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object, indexExpr);
    }
}
