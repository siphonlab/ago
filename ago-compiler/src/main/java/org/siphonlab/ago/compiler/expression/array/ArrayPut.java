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
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;

import org.siphonlab.ago.compiler.expression.literal.IntLiteral;

public class ArrayPut extends ExpressionInFunctionBody {

    private final Expression array;
    private Expression indexExpr;
    private Expression value;
    private final ClassDef elementType;

    public ArrayPut(FunctionDef ownerFunction, Expression array, Expression indexExpr, Expression value) throws CompilationError {
        super(ownerFunction);
        array = array.transform().setParent(this);
        //if(!(array.inferType() instanceof ArrayClassDef)){
        ClassDef arrayType = array.inferType();
        if(!arrayType.getRoot().getAnyArrayClass().isThatOrSuperOfThat(arrayType)){
            throw new TypeMismatchError("'%s' is not an array".formatted(array), array.getSourceLocation());
        }
        this.array = array;
        this.elementType = arrayType instanceof ArrayClassDef arrayClassDef? arrayClassDef.getElementType() : arrayType.getGenericSource().instantiationArguments().getTypeArgumentsArray()[0].getClassDefValue();
        this.indexExpr = indexExpr;
        this.value = value;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.indexExpr = ownerFunction.cast(indexExpr, PrimitiveClassDef.INT).transform().setParent(this);
        this.value = ownerFunction.cast(value, this.elementType).setParent(this).transform();
        return this;
    }

    public ArrayPut(FunctionDef ownerFunction, ArrayElement arrayElement, Expression value) throws CompilationError {
        this(ownerFunction, arrayElement.getArray(), arrayElement.getIndexExpr(), value);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return elementType;
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

            Var.LocalVar array = (Var.LocalVar) this.array.visit(blockCompiler);
            blockCompiler.lockRegister(array);

            var indexExpr = this.indexExpr.visit(blockCompiler);
            blockCompiler.lockRegister(indexExpr);

            var value = this.value.visit(blockCompiler);
            blockCompiler.lockRegister(value);

            CodeBuffer code = blockCompiler.getCode();
            if (indexExpr instanceof Literal<?> literal) {
                assert indexExpr instanceof IntLiteral;
                int index = ((IntLiteral) literal).value;
                if (value instanceof Literal<?> literalValue) {
                    code.array_put(array.getVariableSlot(), index, literalValue);
                } else {
                    code.array_put(array.getVariableSlot(), index, ((Var.LocalVar) value).getVariableSlot());
                }
            } else {
                Var.LocalVar index = (Var.LocalVar) indexExpr;
                if (value instanceof Literal<?> literalValue) {
                    code.array_put(array.getVariableSlot(), index.getVariableSlot(), literalValue);
                } else {
                    code.array_put(array.getVariableSlot(), index.getVariableSlot(), ((Var.LocalVar) value).getVariableSlot());
                }
            }

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
    public ArrayPut setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(ArrayPut %s[%s] = %s)".formatted(this.array, this.indexExpr, this.value);
    }
}
