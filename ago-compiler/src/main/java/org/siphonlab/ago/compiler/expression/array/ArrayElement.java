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

import org.siphonlab.ago.compiler.expression.literal.IntLiteral;
import org.siphonlab.ago.compiler.expression.math.ArithmeticExpr;

import java.util.Objects;

public class ArrayElement extends ExpressionBase implements Assign.Assignee, CollectionElement {

    private final Expression array;
    private Expression indexExpr;
    private final ClassDef arrayType;
    private final ClassDef elementType;
    private Var.LocalVar processedArray;
    private TermExpression processedIndex;

    public ArrayElement(Expression array, Expression indexExpr) throws CompilationError {
        this.array = array.transform().setParent(this);
        ClassDef arrayType = array.inferType();
        if(!arrayType.getRoot().getAnyArrayClass().isThatOrSuperOfThat(arrayType)){
            throw new SyntaxError("array expected", array.getSourceLocation());
        }
        this.arrayType = arrayType;
        if(arrayType instanceof ArrayClassDef arrayClassDef){
            elementType = arrayClassDef.getElementType();
        } else {
            elementType = arrayType.getGenericSource().instantiationArguments().getTypeArgumentsArray()[0].getClassDefValue();
        }
        this.indexExpr = indexExpr;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        this.indexExpr = new Cast(indexExpr.setParent(this).transform(), PrimitiveClassDef.INT).transform();
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return elementType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        Var.LocalVar array = (Var.LocalVar) this.array.visit(blockCompiler);
        blockCompiler.lockRegister(array);
        var indexExpr = this.indexExpr.visit(blockCompiler);
        this.processedIndex = indexExpr;

        CodeBuffer code = blockCompiler.getCode();

        blockCompiler.enter(this);
        if (indexExpr instanceof Literal<?> literal) {
            assert indexExpr instanceof IntLiteral;
            int index = ((IntLiteral) literal).value;
            code.array_get(localVar.getVariableSlot(), array.getVariableSlot(), index, elementType.getTypeCode());
        } else {
            Var.LocalVar index = (Var.LocalVar) indexExpr;
            code.array_get(localVar.getVariableSlot(), array.getVariableSlot(), index.getVariableSlot(), elementType.getTypeCode());
        }
        blockCompiler.leave(this);


        blockCompiler.releaseRegister(array);
        this.processedArray = array;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);
    }

    @Override
    public ArrayElement setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    public Expression getArray() {
        return array;
    }

    public Expression getIndexExpr() {
        return indexExpr;
    }

    @Override
    public String toString() {
        return "(ArrayElement %s[%s])".formatted(this.array, this.indexExpr);
    }

    public Var.LocalVar getProcessedCollection() {
        return processedArray;
    }

    public TermExpression getProcessedIndex() {
        return processedIndex;
    }

    @Override
    public Expression toPutElement(Expression processedCollection, TermExpression processedIndex, Expression value) throws CompilationError {
        return new ArrayPut(processedCollection, processedIndex, value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ArrayElement that)) return false;
        return Objects.equals(arrayType, that.arrayType) && Objects.equals(array, that.array) && Objects.equals(indexExpr, that.indexExpr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(array, indexExpr, arrayType);
    }
}
