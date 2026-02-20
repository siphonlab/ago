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

public class MapPut extends ExpressionBase {

    private final Expression map;
    private Expression keyExpr;
    private Expression value;
    private final FunctionDef accessor;
    private final ClassDef keyType;
    private final ClassDef valueType;

    public MapPut(Expression map, Expression keyExpr, Expression value) throws CompilationError {
        map = map.transform().setParent(this);
        ClassDef mapType = map.inferType();
        Root root = mapType.getRoot();
        if(!(root.getAnyReadwriteMap().isThatOrSuperOfThat(mapType))){
            throw new SyntaxError("writable map expected", map.getSourceLocation());
        }
        this.map = map;

        ClassRefLiteral[] typeArgumentsArray = mapType.getGenericSource().instantiationArguments().getTypeArgumentsArray();
        this.keyType = typeArgumentsArray[0].getClassDefValue();
        this.valueType = typeArgumentsArray[1].getClassDefValue();

        this.keyExpr = keyExpr;
        this.value = value;
        this.accessor = mapType.findMethod("put");
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.keyExpr = new Cast(keyExpr, this.keyType).setParent(this).transform();
        this.value = new Cast(value, this.valueType).setParent(this).transform();
        return this;
    }

    public MapPut(MapValue mapValue, Expression value) throws CompilationError {
        this(mapValue.getMap(), mapValue.getIndexExpr(), value);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return valueType;
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

            Var.LocalVar map = (Var.LocalVar) this.map.visit(blockCompiler);
            blockCompiler.lockRegister(map);

            var keyExpr = this.keyExpr.visit(blockCompiler);
            blockCompiler.lockRegister(keyExpr);

            var value = this.value.visit(blockCompiler);
            blockCompiler.lockRegister(value);

            var invoke = new Invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(this.map, accessor), List.of(keyExpr, value), this.getSourceLocation());
            invoke.termVisit(blockCompiler);

            blockCompiler.releaseRegister(map);
            blockCompiler.releaseRegister(keyExpr);
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
    public MapPut setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(MapPut %s[%s] = %s)".formatted(this.map, this.keyExpr, this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MapPut that)) return false;
        return Objects.equals(map, that.map) && Objects.equals(keyExpr, that.keyExpr) &&  Objects.equals(value, that.value) ;
    }
}
