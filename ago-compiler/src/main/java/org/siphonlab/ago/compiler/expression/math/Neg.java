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
package org.siphonlab.ago.compiler.expression.math;




import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.*;



import java.util.Objects;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;

public class Neg extends UnaryArithmetic {

    public Neg(FunctionDef ownerFunction, Expression value) throws CompilationError {
        super(ownerFunction, value);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return value.inferType();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        ClassDef type = this.value.inferType();

        if(type.getTypeCode() == STRING){   // auto cast to double like js does
            return new Neg(ownerFunction, ownerFunction.cast(this.value, PrimitiveClassDef.DOUBLE).setSourceLocation(this.getSourceLocation())).setParent(this.getParent()).transform();
        }
        if(type.getTypeCode() == CHAR){     // char cast to int
            return new Neg(ownerFunction, ownerFunction.cast(this.value, PrimitiveClassDef.INT).setSourceLocation(this.getSourceLocation())).setParent(this).transform();
        }
        if(type.getTypeCode() == OBJECT && type.isPrimitiveOrBoxed()){
            return new Neg(ownerFunction, ownerFunction.unbox(this.value).transform()).setParent(this).transform();
        }
        if(!type.isPrimitiveNumberFamily()){
            type.isPrimitiveNumberFamily();
            throw new TypeMismatchError("illegal type '%' for negative, number or number string required".formatted(type.getFullname()), this.getSourceLocation());
        }
        if(this.value instanceof Literal<?> literal) {
            return neg(literal).setParent(this);
        }
        return this;
    }

    protected Literal<?> neg(Literal<?> literal) throws TypeMismatchError {
        return switch (literal.getTypeCode().value) {
            case INT_VALUE -> new IntLiteral(-((IntLiteral) literal).value);
            case DOUBLE_VALUE -> new DoubleLiteral(-((DoubleLiteral) literal).value);
            case BYTE_VALUE -> new ByteLiteral((byte) -((ByteLiteral) literal).value);
            case SHORT_VALUE -> new ShortLiteral((short) -((ShortLiteral) literal).value);
            case FLOAT_VALUE -> new FloatLiteral(-((FloatLiteral) literal).value);
            case LONG_VALUE -> new LongLiteral(-((LongLiteral) literal).value);
            default ->
                    throw new TypeMismatchError(String.format("cannot apply '-' on '%s'", literal.inferType()), sourceLocation);
        };
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var term = this.value.visit(blockCompiler);
            CodeBuffer code = blockCompiler.getCode();
            if (term instanceof Literal<?> literal) {
                code.assignLiteral(localVar.getVariableSlot(), neg(literal));
            } else {
                assert localVar.inferType() == this.inferType();
                if (!term.inferType().isPrimitiveNumberFamily()) {
                    throw new TypeMismatchError("illegal type '%' for negative, number or number string required", this.getSourceLocation());
                }
                code.neg(localVar.getVariableSlot(), ((Var.LocalVar) term).getVariableSlot());
            }
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

            var term = this.value.visit(blockCompiler);
            CodeBuffer code = blockCompiler.getCode();
            if (term instanceof Literal<?> literal) {
                return neg(literal);
            } else {
                if (!term.inferType().isPrimitiveNumberFamily()) {
                    throw new TypeMismatchError("illegal type '%' for negative, number required", this.getSourceLocation());
                }
                var localVar = blockCompiler.acquireTempVar(term);
                code.neg(localVar.getVariableSlot(), ((Var.LocalVar) term).getVariableSlot());
                return localVar;
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public Neg setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(Neg %s)".formatted(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Neg n && Objects.equals(n.value, this.value);
    }
}
