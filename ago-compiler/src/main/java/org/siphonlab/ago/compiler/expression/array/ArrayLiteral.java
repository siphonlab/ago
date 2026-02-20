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

import org.siphonlab.ago.compiler.ArrayClassDef;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;

import org.apache.commons.collections4.ListUtils;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArrayLiteral extends ExpressionBase {

    private final List<Expression> elements;
    private final ArrayClassDef arrayType;

    public ArrayLiteral(ArrayClassDef arrayType, List<Expression> elements) throws CompilationError {
        this.arrayType = arrayType;
        List<Expression> els = new ArrayList<>();
        for (Expression element : elements) {
            els.add(new Cast(element.setParent(this).transform(), arrayType.getElementType()).transform());
        }
        this.elements = els;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return arrayType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        int size = elements.size();

        new ArrayCreate(arrayType,  new IntLiteral(size)).setSourceLocation(this.getSourceLocation()).outputToLocalVar(localVar, blockCompiler);

        if(elements.isEmpty()) return;

        try {
            blockCompiler.enter(this);

            // all elements are literal, that means it's need store in const area
            if (elements.stream().allMatch(el -> el instanceof Literal<?>)) {
                int blobId = blockCompiler.getFunctionDef().getOrCreateBLOB(elements.stream().map(l -> (Literal<?>) l).toList(), this);
                blockCompiler.getCode().fill_array(localVar.getVariableSlot(), arrayType.getElementType().getTypeCode(), size, blobId);
            } else {
                blockCompiler.lockRegister(localVar);
                for (int i = 0; i < size; i++) {
                    Expression element = elements.get(i);
                    new ArrayPut(localVar, new IntLiteral(i), element).setSourceLocation(element.getSourceLocation()).visit(blockCompiler);
                }
                blockCompiler.releaseRegister(localVar);
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public ArrayLiteral setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(Array %s %s)".formatted(arrayType, elements);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ArrayLiteral that)) return false;
        return Objects.equals(arrayType, that.arrayType) && ListUtils.isEqualList(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements, arrayType);
    }
}
