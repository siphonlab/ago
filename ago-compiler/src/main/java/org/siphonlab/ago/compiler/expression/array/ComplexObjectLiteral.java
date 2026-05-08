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
import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;

import java.util.*;

import static org.siphonlab.ago.compiler.ClassDef.findCommonType;

public class ComplexObjectLiteral extends ExpressionInFunctionBody {

    private final Expression objectTypeExpr;
    private final List<ObjectLiteralKVDef> kvDefs;
    private final ClassDef objectType;

    public ComplexObjectLiteral(FunctionDef ownerFunction, Expression objectTypeExpr, List<ObjectLiteralKVDef> kvDefs) throws SyntaxError {
        super(ownerFunction);
        this.objectTypeExpr = objectTypeExpr;
        this.kvDefs = kvDefs;
        Pair<Expression, ClassDef> pair = Creator.extractScopeAndClass(objectTypeExpr, this.getSourceLocation());
        this.objectType = pair.getRight();
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        if (objectType == null) throw new IllegalStateException("type not prepared");
        return objectType;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        var root = ownerFunction.getRoot();
        for (ObjectLiteralKVDef kvDef : this.kvDefs) {
            if (kvDef instanceof KVCollectionExpandoDef expandoDef) {
                Expression expr = expandoDef.getExpression();
                var t = expr.inferType();
                if (root.getAnyMapClass().isThatOrSuperOfThat(t)) {
                    throw new TypeMismatchError("cannot copy a Map to an object", expr.getSourceLocation());
                } else {
                    if (objectType.isThatOrSuperOfThat(t) || t.isThatOrSuperOfThat(objectType)) {
                        //
                    } else {
                        throw new TypeMismatchError("cannot assign '%s' to '%s'".formatted(t.getFullname(), objectType.getFullname()), expr.getSourceLocation());
                    }
                }
            }
        }

        return this;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var fun = ownerFunction;

            new Creator(fun, this.objectTypeExpr, new ArrayList<>(), this.getSourceLocation()).transform()
                    .outputToLocalVar(localVar, blockCompiler);

            blockCompiler.lockRegister(localVar);

            var collectionElementDefs = this.kvDefs;
            for (int i = 0; i < collectionElementDefs.size(); i++) {
                var element = collectionElementDefs.get(i);

                if (element instanceof KVCollectionExpandoDef expandoDef) {
                    generateCodeForMaybeNull(blockCompiler, expandoDef.getExpression(), nonNullValue ->
                            assignAll(nonNullValue, localVar, blockCompiler, expandoDef.getKind(), expandoDef.getSourceLocation()));

                } else if (element instanceof KVPairDef pair) {
                    var key = fun.cast(pair.getKey(), getRoot().STRING(),true).setParent(this).transform();
                    if(key instanceof StringLiteral s){
                        String a = s.getString();
                        var attrPair = objectType.getAttribute(a);
                        if(attrPair != null) {
                            Attribute attr = new Attribute(fun, localVar, attrPair.getGetter(), attrPair.getSetter()).setSourceLocation(key.getSourceLocation());
                            fun.assign(attr, pair.getValue()).transform().termVisit(blockCompiler);
                        } else {
                            Field field = objectType.getFields().get(a);
                            if(field == null){
                                throw new ResolveError("'%s' is not attribute or field".formatted(a), pair.getKey().getSourceLocation());
                            } else if(!field.isPublic()){
                                throw new ResolveError("'%s' is not public field".formatted(a), pair.getKey().getSourceLocation());
                            }
                            Var.Field fieldOfDest = fun.field(localVar, field);
                            var assign = fun.assign(fieldOfDest, pair.getValue()).setSourceLocation(pair.getSourceLocation());
                            assign.transform().termVisit(blockCompiler);
                        }
                    } else {    // for expression
                        throw new SyntaxError("dynamic key can only apply to Object", pair.getSourceLocation());
                    }
                }
            }

            blockCompiler.releaseRegister(localVar);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    private void assignAll(TermExpression expression, Var.LocalVar instance, BlockCompiler blockCompiler, KVCollectionExpandoDef.Kind kind, SourceLocation sourceLocation) throws CompilationError {
        var functionDef = blockCompiler.getFunctionDef();
        var objectType = expression.inferType();
        var root = blockCompiler.getRoot();
        switch (kind) {
            case ReadOnlyMap:
                throw new TypeMismatchError("cannot copy a Map to attributes of an object", sourceLocation);
            case KeyValuePairIterable, KeyValuePairIterator:
                throw new TypeMismatchError("cannot copy Iterable/Iterator to attributes of an object", sourceLocation);

            case Object:
                var assign = new CopyAssign(functionDef, instance, expression, findCommonType(objectType, objectType));
                assign.termVisit(blockCompiler);
                break;

            default:
                throw new IllegalArgumentException("unknown kind " + kind);

        }
    }

    @Override
    public ComplexObjectLiteral setParent(Expression expression) {
        super.setParent(expression);
        return this;
    }

    @Override
    public ComplexObjectLiteral setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "[%s| %s]".formatted(this.objectType.getFullname(), StringUtils.join(this.kvDefs, ','));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComplexObjectLiteral that)) return false;
        return Objects.equals(this.objectTypeExpr, that.objectTypeExpr) && Objects.equals(kvDefs, that.kvDefs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectTypeExpr, kvDefs);
    }

}
