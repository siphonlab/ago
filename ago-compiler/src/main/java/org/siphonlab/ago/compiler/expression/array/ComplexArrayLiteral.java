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
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;
import org.siphonlab.ago.compiler.expression.math.SelfArithmetic;
import org.siphonlab.ago.compiler.statement.ForEachStmt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ComplexArrayLiteral extends ExpressionInFunctionBody {

    private final ArrayClassDef arrayType;
    private List<CollectionElementDef> elements;

    public ComplexArrayLiteral(FunctionDef functionDef, ArrayClassDef arrayType, List<CollectionElementDef> elements) {
        super(functionDef);
        this.arrayType = arrayType;
        this.elements = elements;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return arrayType;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        Root root = ownerFunction.getRoot();
        for(var el : elements) {
            if(el.isExpando()) {
                ClassDef expandoType = el.getExpression().inferType();
                if (!(expandoType instanceof ArrayClassDef) && !root.getAnyCollectionClass().isThatOrSuperOfThat(expandoType)) {
                    throw new TypeMismatchError("lang.Collection or array expected", el.getExpression().getSourceLocation());
                }
            }  else {
                el.setExpression(ownerFunction.cast(el.getExpression(), arrayType.getElementType()).setParent(this).transform());
            }
        }
        return this;
    }

    record GroupResult(Expression expression, Var.LocalVar result, TermExpression size){}

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            if(this.elements.size() == 1){
                outputSingleItem(localVar, blockCompiler);
                return;
            }

            var length = blockCompiler.acquireTempVar(getRoot().INT());
            blockCompiler.lockRegister(length);

            boolean lengthHasValue = false;
            List<CollectionElementDef> collectionElementDefs = this.elements;
            List<GroupResult> groups = new ArrayList<>();      // result, length
            for (int i = 0; i < collectionElementDefs.size(); i++) {
                CollectionElementDef element = collectionElementDefs.get(i);
                Expression expression = element.getExpression();
                if (element.isExpando()) {
                    Var.LocalVar p = (Var.LocalVar) expression.visit(blockCompiler);
                    blockCompiler.lockRegister(p);

                    ClassDef classDef = p.inferType();
                    TermExpression expandoSize;
                    if (classDef instanceof ArrayClassDef) {
                        expandoSize = new ArrayLength(ownerFunction, expression).visit(blockCompiler);
                    } else {
                        assert ownerFunction.getRoot().getAnyCollectionClass().isThatOrSuperOfThat(classDef);
                        expandoSize = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ownerFunction.classUnder(p, classDef.findMethod("size#get")),
                                            Collections.emptyList(), expression.getSourceLocation()).visit(blockCompiler);
                    }
                    blockCompiler.lockRegister(expandoSize);
                    groups.add(new GroupResult(expression, p, expandoSize));

                    if (!lengthHasValue) {
                        ownerFunction.assign((Var.LocalVar)length, expandoSize).termVisit(blockCompiler);
                        lengthHasValue = true;
                    } else {
                        new SelfArithmetic(ownerFunction, length, expandoSize, SelfArithmetic.Type.Inc).termVisit(blockCompiler);
                    }
                } else {
                    var arrayPart = new ArrayList<Expression>();
                    for(var j =i; j < collectionElementDefs.size(); j++){
                        var el = collectionElementDefs.get(j);
                        if(!el.isExpando()){
                            arrayPart.add(el.getExpression());
                        } else {
                            break;
                        }
                    }
                    var arr = new ArrayLiteral(ownerFunction, this.arrayType, arrayPart)
                            .setSourceLocation(arrayPart.getFirst().getSourceLocation().extend(arrayPart.getLast().getSourceLocation()))
                            .transform();
                    Var.LocalVar r = (Var.LocalVar) arr.visit(blockCompiler);
                    groups.add(new GroupResult(arr, r, getRoot().createIntLiteral(arrayPart.size())));
                    blockCompiler.lockRegister(r);

                    if (!lengthHasValue) {
                        ownerFunction.assign((Var.LocalVar)length, getRoot().createIntLiteral(arrayPart.size())).termVisit(blockCompiler);
                        lengthHasValue = true;
                    } else {
                        new SelfArithmetic(ownerFunction, length, getRoot().createIntLiteral(arrayPart.size()), SelfArithmetic.Type.Inc).termVisit(blockCompiler);
                    }
                    i+= arrayPart.size() - 1;
                }
            }

            new ArrayCreate(ownerFunction, arrayType, length).setSourceLocation(this.getSourceLocation()).outputToLocalVar(localVar, blockCompiler);

            var destIndex = blockCompiler.acquireTempVar(getRoot().INT());
            blockCompiler.lockRegister(destIndex);
            getRoot().createIntLiteral(0).outputToLocalVar(destIndex, blockCompiler);   // set to 0
            for (var group : groups) {
                var groupType = group.result.inferType();
                boolean solved = false;
                if(groupType instanceof ArrayClassDef) {
                    if(groupType.equals(this.arrayType)) {
                        // Array.copy
                        ownerFunction.invoke(Invoke.InvokeMode.Invoke, ownerFunction.classUnder(new ConstClass(arrayType), arrayType.getMetaClassDef().findMethod("copy#")),
                                List.of(group.result, getRoot().createIntLiteral(0), localVar, destIndex, group.size), group.expression().getSourceLocation()).termVisit(blockCompiler);
                        new SelfArithmetic(ownerFunction, destIndex, group.size, SelfArithmetic.Type.Inc).termVisit(blockCompiler);
                        solved = true;
                    }
                } else {
                    // copyTo#offset_length
                    var collectionClass = ownerFunction.getRoot().getAnyCollectionClass().asThatOrSuperOfThat(groupType);
                    if(collectionClass.getGenericSource().typeArguments()[0].getClassDefValue() == this.arrayType.getElementType()) {
                        ownerFunction.invoke(Invoke.InvokeMode.Invoke, ownerFunction.classUnder(group.result, collectionClass.findMethod("copyTo#offset_length")),
                                        List.of(localVar, destIndex, group.size),
                                        group.expression.getSourceLocation())
                                .termVisit(blockCompiler);
                        new SelfArithmetic(ownerFunction, destIndex, group.size, SelfArithmetic.Type.Inc).termVisit(blockCompiler);
                        solved = true;
                    }
                }
                if(!solved){
                    iterateCopyToArray(localVar, blockCompiler, group, destIndex);
                }
                blockCompiler.releaseRegister(group.result);
                blockCompiler.releaseRegister(group.size);
            }
            blockCompiler.releaseRegister(destIndex);
            blockCompiler.releaseRegister(length);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    private void outputSingleItem(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        var el = this.elements.getFirst();
        assert el.isExpando();
        Expression expression = el.getExpression();
        Var.LocalVar p = (Var.LocalVar) expression.visit(blockCompiler);
        blockCompiler.lockRegister(p);

        ClassDef classDef = p.inferType();
        TermExpression expandoSize;
        if (classDef instanceof ArrayClassDef) {
            expandoSize = new ArrayLength(ownerFunction, expression).visit(blockCompiler);
            if(classDef == this.arrayType) {
                new ArrayCreate(ownerFunction, arrayType, expandoSize).setSourceLocation(this.getSourceLocation()).outputToLocalVar(localVar, blockCompiler);
                ownerFunction.invoke(Invoke.InvokeMode.Invoke, ownerFunction.classUnder(new ConstClass(arrayType), arrayType.getMetaClassDef().findMethod("copy#")),
                        List.of(p, getRoot().createIntLiteral(0), localVar, getRoot().createIntLiteral(0), expandoSize), expandoSize.getSourceLocation()).termVisit(blockCompiler);

                blockCompiler.releaseRegister(p);
                return;
            }
        } else {
            var collectionClass = ownerFunction.getRoot().getAnyCollectionClass().asThatOrSuperOfThat(classDef);
            expandoSize = ownerFunction.invoke(Invoke.InvokeMode.Invoke, ownerFunction.classUnder(p, classDef.findMethod("size#get")), Collections.emptyList(), expression.getSourceLocation()).visit(blockCompiler);
            if(collectionClass.getGenericSource().typeArguments()[0].getClassDefValue() == this.arrayType.getElementType()) {
                ownerFunction.invoke(Invoke.InvokeMode.Invoke, ownerFunction.classUnder(p, collectionClass.findMethod("toArray#")), Collections.emptyList(), expression.getSourceLocation())
                        .outputToLocalVar(localVar, blockCompiler);
                return;
            }
        }
        new ArrayCreate(ownerFunction, arrayType, expandoSize).setSourceLocation(this.getSourceLocation()).outputToLocalVar(localVar, blockCompiler);
        blockCompiler.lockRegister(expandoSize);
        var arrayIndex = blockCompiler.acquireTempVar(getRoot().INT());
        blockCompiler.lockRegister(arrayIndex);
        iterateCopyToArray(localVar, blockCompiler, new GroupResult(expression, p, expandoSize), arrayIndex);
        blockCompiler.releaseRegister(arrayIndex);
        blockCompiler.releaseRegister(p);
    }

    // for each assign
    private void iterateCopyToArray(Var.LocalVar destArray, BlockCompiler blockCompiler, GroupResult group, Var.LocalVar destIndex) throws CompilationError {
        ClassDef t;
        ForEachStmt.Mode mode;
        var groupType = group.result.inferType();
        var r = blockCompiler.extractCollectionElementType(groupType);
        t = r.elementType();
        mode = switch (r.type()){
            case Array -> ForEachStmt.Mode.Array;
            case Iterator -> ForEachStmt.Mode.Iterator;
            case Iterable, Collection, List -> ForEachStmt.Mode.Iterable;
        };
        assert t != null;
        var it = blockCompiler.acquireTempVar(t);
        blockCompiler.lockRegister(it);
        var forEach = new ForEachStmt(ownerFunction, null, it, group.result,
                ownerFunction.expressionStmt(
                        ownerFunction.assign(new ArrayElement(ownerFunction, destArray, new SelfArithmetic(ownerFunction, destIndex, getRoot().createIntLiteral(1), SelfArithmetic.Type.IncPost)),
                                                ownerFunction.cast(it, arrayType.getElementType()).transform())
                ), mode, group.expression.getSourceLocation());
        forEach.termVisit(blockCompiler);
        blockCompiler.releaseRegister(it);
    }

    @Override
    public ComplexArrayLiteral setParent(Expression expression) {
        super.setParent(expression);
        return this;
    }

    @Override
    public ComplexArrayLiteral setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "[%s| %s]".formatted(this.arrayType.getFullname(), StringUtils.join(this.elements, ','));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComplexArrayLiteral that)) return false;
        return Objects.equals(arrayType, that.arrayType) && Objects.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrayType, elements);
    }
}
