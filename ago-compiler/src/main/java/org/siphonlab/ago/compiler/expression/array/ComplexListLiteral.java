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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.invoke.Invoke;
import org.siphonlab.ago.compiler.statement.ForEachStmt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ComplexListLiteral extends ExpressionInFunctionBody {

    private final Expression listTypeExpr;
    private final ClassDef elementType;
    private final List<CollectionElementDef> elements;
    private ClassDef listType;

    public ComplexListLiteral(FunctionDef ownerFunction, Expression listTypeExpr, ClassDef elementType, List<CollectionElementDef> elements) {
        super(ownerFunction);
        this.listTypeExpr = listTypeExpr;
        this.elementType = elementType;
        this.elements = elements;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        if(listType == null) throw new IllegalStateException("type not prepared");
        return listType;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        Pair<Expression, ClassDef> pair = Creator.extractScopeAndClass(listTypeExpr, this.getSourceLocation());
        this.listType = pair.getRight();
        Root root = ownerFunction.getRoot();
        for(var el : elements) {
            if(el.isExpando()) {
                ClassDef expandoType = el.getExpression().inferType();
                if (!(expandoType instanceof ArrayClassDef)
                        && !root.getAnyIterableInterface().isThatOrSuperOfThat(expandoType)
                        && !root.getAnyIteratorInterface().isThatOrSuperOfThat(expandoType)
                        && !root.getGeneratorOfAnyClass().isThatOrSuperOfThat(expandoType)
                ) {
                    throw new TypeMismatchError("array, lang.Iterable, lang.Iterator, or generator expected", el.getExpression().getSourceLocation());
                }
            } else {
                el.setExpression(ownerFunction.cast(el.getExpression(), elementType).setParent(this).transform());
            }
        }
        return this;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            boolean listCreated = false;
            List<CollectionElementDef> collectionElementDefs = this.elements;
            for (int i = 0; i < collectionElementDefs.size(); i++) {
                CollectionElementDef element = collectionElementDefs.get(i);
                Expression expression = element.getExpression();

                Var.LocalVar part;
                if (element.isExpando()) {
                    var bListCreated = new MutableBoolean(listCreated);
                    generateCodeForMaybeNull(blockCompiler, expression, nonNullValue ->{
                        var v = (Var.LocalVar) nonNullValue;
                        addAll(v, localVar, bListCreated, blockCompiler);
                    });
                    listCreated = bListCreated.get();
                } else {
                    var arrayPart = new ArrayList<Expression>();
                    boolean allAreLiterals = true;
                    for(var j =i; j < collectionElementDefs.size(); j++){
                        var el = collectionElementDefs.get(j);
                        if(!el.isExpando()){
                            arrayPart.add(el.getExpression());
                            if(allAreLiterals && !(el.getExpression() instanceof Literal<?>)){
                                allAreLiterals = false;
                            }
                        } else {
                            break;
                        }
                    }
                    if(!allAreLiterals){
                        if(!listCreated) {
                            new Creator(ownerFunction, listTypeExpr, Collections.emptyList(), expression.getSourceLocation(), "new#").outputToLocalVar(localVar, blockCompiler);
                            listCreated = true;
                        }
                        ownerFunction.invoke(Invoke.InvokeMode.Invoke, ownerFunction.classUnder(localVar, this.inferType().findMethod("add#")), Collections.singletonList(expression), expression.getSourceLocation())
                                .transform().termVisit(blockCompiler);
                        continue;
                    } else {
                        MutableBoolean returnExisted = new MutableBoolean();
                        var arrayType = ownerFunction.getOrCreateArrayType(elementType, returnExisted);
                        if (returnExisted.isFalse()) Compiler.processClassTillStage(arrayType, CompilingStage.AllocateSlots);
                        var arr = new ArrayLiteral(ownerFunction, arrayType, arrayPart)
                                .setSourceLocation(arrayPart.getFirst().getSourceLocation().extend(arrayPart.getLast().getSourceLocation()))
                                .transform();
                        part = (Var.LocalVar) arr.visit(blockCompiler);
                        i += arrayPart.size() - 1;
                    }
                    addAll(part, localVar, new MutableBoolean(listCreated), blockCompiler);
                }
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    private void addAll(Var.LocalVar part, Var.LocalVar listInstance, MutableBoolean listCreated, BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.lockRegister(part);
        ClassDef classDef = part.inferType();
        if (listCreated.isFalse()) {
            if(classDef instanceof ArrayClassDef arrayClassDef && arrayClassDef.getElementType() == this.elementType) {
                new Creator(ownerFunction, listTypeExpr, Collections.singletonList(part), part.getSourceLocation(), "new#array")
                            .outputToLocalVar(listInstance, blockCompiler);
            } else {
                new Creator(ownerFunction, listTypeExpr, Collections.emptyList(), part.getSourceLocation(), "new#")
                        .outputToLocalVar(listInstance, blockCompiler);
            }
            listCreated.setTrue();
            blockCompiler.releaseRegister(part);
            return;
        }
        var t = blockCompiler.extractCollectionElementType(classDef);
        var method = switch (t.type()){
            case Iterable -> "addAll#iterable";
            case Iterator -> "addAll#iterator";
            case Array -> "addAll#array";
            case Collection, List -> "addAll#collection";
            case Generator -> "addAll#generator";
        };
        ownerFunction.invoke(Invoke.InvokeMode.Invoke,
                ownerFunction.classUnder(listInstance, this.inferType().findMethod(method)), List.of(part),
                part.getSourceLocation()).transform().termVisit(blockCompiler);
//            iterateCopy(listInstance, blockCompiler, part, t, part.getSourceLocation());
        blockCompiler.releaseRegister(part);
    }

    private void iterateCopy(Var.LocalVar destList, BlockCompiler blockCompiler, Var.LocalVar srcCollection, BlockCompiler.CollectionElementType collectionElementType, SourceLocation sourceLocation) throws CompilationError {
        var mode = switch (collectionElementType.type()){
            case Array -> ForEachStmt.Mode.Array;
            case Iterator -> ForEachStmt.Mode.Iterator;
            case Iterable, Collection, List -> ForEachStmt.Mode.Iterable;
            case Generator -> ForEachStmt.Mode.Generator;
        };
        var it = blockCompiler.acquireTempVar(collectionElementType.elementType());
        blockCompiler.lockRegister(it);
        var forEach = new ForEachStmt(ownerFunction, null, it, srcCollection,
                ownerFunction.expressionStmt(
                        ownerFunction.invoke(Invoke.InvokeMode.Invoke, ownerFunction.classUnder(destList, destList.inferType().findMethod("add#")),
                                List.of(ownerFunction.cast(it, elementType)), sourceLocation)
                ), mode, sourceLocation);
        forEach.termVisit(blockCompiler);
        blockCompiler.releaseRegister(it);
    }

    @Override
    public ComplexListLiteral setParent(Expression expression) {
        super.setParent(expression);
        return this;
    }

    @Override
    public ComplexListLiteral setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "[%s| %s]".formatted(this.listType.getFullname(), StringUtils.join(this.elements, ','));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComplexListLiteral that)) return false;
        return Objects.equals(this.listTypeExpr, that.listTypeExpr) && Objects.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listTypeExpr, elements);
    }

}
