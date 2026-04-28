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
package org.siphonlab.ago.compiler.expression.logic;

import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;

import org.siphonlab.ago.compiler.expression.*;

import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;

import java.util.Objects;

/**
 * `not` expression always return boolean result
 */
public class Not extends UnaryExpression {

    public Not(FunctionDef ownerFunction, Expression value) throws CompilationError {
        super(ownerFunction, value);
    }

    public Expression getValue(){
        return value;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(value instanceof Not not){
            return not.getValue();
        }
        if(value instanceof Equals equals){
            return equals.neg();
        }

        this.value = this.value.transform();

        var t = this.value.inferType();
        if(t instanceof NullableClassDef n){
            NullableValue v;
            if(!(value instanceof NullableValue nullableValue)){
                v = new NullableValue(ownerFunction, value);
            } else {
                v = nullableValue;
            }
            return new Not(ownerFunction, new AndExpr(ownerFunction,
                        v.isNotNull(),
                        ownerFunction.cast(v.nonNullValue(), getRoot().BOOLEAN()).transform()
                    ));
        }

        var v = ownerFunction.cast(this.value, getRoot().BOOLEAN()).transform();
        if(v instanceof Literal<?> literal){
            return getRoot().createBooleanLiteral(!BooleanLiteral.isTrue(literal)).setParent(this.getParent()).setSourceLocation(this.getSourceLocation());
        }
        if(v != this.value) {
            return new Not(ownerFunction, v).setParent(this.getParent()).setSourceLocation(this.sourceLocation);
        }
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return getRoot().BOOLEAN();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();
            var v = ownerFunction.cast(this.value, getRoot().BOOLEAN()).transform().visit(blockCompiler);
            if (v instanceof Literal<?> literal) {
                code.assignLiteral(localVar.getVariableSlot(), BooleanLiteral.isFalse(literal) ? getRoot().createBooleanLiteral( true) : getRoot().createBooleanLiteral( false));
            } else {
                code.not(localVar.getVariableSlot(), ((Var.LocalVar) v).getVariableSlot());
                // not_vov not implemented
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Not n && Objects.equals(n.value, this.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "(Not %s)".formatted(this.value);
    }
}
