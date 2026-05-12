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
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.invoke.Invoke;
import org.siphonlab.ago.compiler.statement.*;

import java.util.*;

public class ComplexMapLiteral extends ExpressionInFunctionBody {

    private final Expression mapTypeExpr;
    private final ClassDef keyType;
    private final ClassDef valueType;
    private final List<ObjectLiteralKVDef> kvDefs;
    private final ClassDef mapType;

    public ComplexMapLiteral(FunctionDef ownerFunction, Expression mapTypeExpr, ClassDef keyType, ClassDef valueType, List<ObjectLiteralKVDef> kvDefs) throws SyntaxError {
        super(ownerFunction);
        this.mapTypeExpr = mapTypeExpr;
        this.keyType = keyType;
        this.valueType = valueType;
        this.kvDefs = kvDefs;

        Pair<Expression, ClassDef> pair = Creator.extractScopeAndClass(mapTypeExpr, this.getSourceLocation(), true);
        this.mapType = pair.getRight();
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        if(mapType == null) throw new IllegalStateException("type not prepared");
        return mapType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var fun = ownerFunction;

            new Creator(fun, this.mapTypeExpr, new ArrayList<>(), this.getSourceLocation()).transform()
                .outputToLocalVar(localVar, blockCompiler);

            blockCompiler.lockRegister(localVar);

            var collectionElementDefs = this.kvDefs;
            for (int i = 0; i < collectionElementDefs.size(); i++) {
                var element = collectionElementDefs.get(i);

                if (element instanceof KVCollectionExpandoDef expandoDef) {
                    generateCodeForMaybeNull(blockCompiler, expandoDef.getExpression(), nonNullValue ->
                            putAll(nonNullValue, localVar, blockCompiler, expandoDef.getKind(), expandoDef.getSourceLocation()));

                } else if(element instanceof KVPairDef pair) {
                    var key = fun.cast(pair.getKey(), this.keyType, true).setParent(this).transform();
                    var value = fun.cast(pair.getValue(), this.valueType, true).setParent(this).transform();

                    var put = new MapPut(fun, localVar, key, value).setSourceLocation(this.getSourceLocation()).transform();
                    put.termVisit(blockCompiler);
                }
            }

            blockCompiler.releaseRegister(localVar);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    private void putAll(Expression expando, Var.LocalVar mapInstance, BlockCompiler blockCompiler, KVCollectionExpandoDef.Kind kind, SourceLocation sourceLocation) throws CompilationError {
        var functionDef = blockCompiler.getFunctionDef();
        var objectType = expando.inferType();
        var root = blockCompiler.getRoot();
        switch (kind){
        case ReadOnlyMap:
            var putAll = functionDef.invoke(Invoke.InvokeMode.Invoke, functionDef.classUnder(mapInstance, objectType.findMethod("putAll#")), List.of(expando), sourceLocation);
            putAll.transform().termVisit(blockCompiler);
            break;
        case KeyValuePairIterable, KeyValuePairIterator: {
            ClassDef iteratorType;
            ForEachStmt.Mode forEachMode;
            if (kind == KVCollectionExpandoDef.Kind.KeyValuePairIterable) {
                iteratorType = objectType.findMethod("iterator#").getResultType();
                forEachMode = ForEachStmt.Mode.Iterable;
            } else {
                iteratorType = objectType;
                forEachMode = ForEachStmt.Mode.Iterator;
            }

            var keyValuePairType = iteratorType.findMethod("next#").getResultType();
            Var.LocalVar iterVar = blockCompiler.acquireTempVar(keyValuePairType);
            var keyOfIter = functionDef.field(iterVar, keyValuePairType.getVariable("key"));
            var valueOfIter = functionDef.field(iterVar, keyValuePairType.getVariable("value"));
            var put = new MapPut(functionDef, mapInstance, keyOfIter, valueOfIter).setSourceLocation(sourceLocation).transform();

            ForEachStmt forEachStmt = new ForEachStmt(functionDef, null, iterVar, expando, functionDef.expressionStmt(put), forEachMode, sourceLocation);
            forEachStmt.termVisit(blockCompiler);
            break;
        }

        case Object: {
            Set<String> visited = new HashSet<>();
            var obj = (Var.LocalVar) expando.visit(blockCompiler);
            blockCompiler.lockRegister(obj);
            if (objectType.getAttributes() != null) {
                for (var k : objectType.getAttributes().keySet()) {
                    GetterSetterPair attribute = objectType.getAttribute(k);
                    var attr = new Attribute(functionDef, obj, attribute.getGetter(), attribute.getSetter());
                    var put = new MapPut(functionDef, mapInstance, getRoot().createStringLiteral(k), attr).setSourceLocation(sourceLocation).transform();
                    put.termVisit(blockCompiler);
                    visited.add(k);
                }
            }
            Map<String, Field> fields = objectType.getFields();
            for (var k : fields.keySet()) {
                var fld = fields.get(k);
                if (fld.isPublic() && !visited.contains(k)) {
                    var put = new MapPut(functionDef, mapInstance, getRoot().createStringLiteral(k), functionDef.field(obj, fld)).setSourceLocation(sourceLocation).transform();
                    put.termVisit(blockCompiler);
                    visited.add(k);
                }
            }
            break;
        }

            default:
                throw new IllegalArgumentException("unknown kind " + kind);

        }
    }

    @Override
    public ComplexMapLiteral setParent(Expression expression) {
        super.setParent(expression);
        return this;
    }

    @Override
    public ComplexMapLiteral setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "[%s| %s]".formatted(this.mapType.getFullname(), StringUtils.join(this.kvDefs, ','));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComplexMapLiteral that)) return false;
        return Objects.equals(this.mapTypeExpr, that.mapTypeExpr) && Objects.equals(kvDefs, that.kvDefs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapTypeExpr, kvDefs);
    }

}
