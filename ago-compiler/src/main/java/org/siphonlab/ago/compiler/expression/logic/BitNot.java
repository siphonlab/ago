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

import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.UnaryExpression;
import org.siphonlab.ago.compiler.expression.Var;
import org.siphonlab.ago.compiler.expression.literal.ByteLiteral;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;
import org.siphonlab.ago.compiler.expression.literal.LongLiteral;
import org.siphonlab.ago.compiler.expression.literal.ShortLiteral;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

import java.util.Objects;

public class BitNot extends UnaryExpression {

    public BitNot(Expression value) throws CompilationError {
        super(value);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        var type = this.value.inferType();
        if(!type.getUnboxedTypeCode().isIntFamily()){
            throw new TypeMismatchError("int family value expected",this.getSourceLocation());
        }
        if(value instanceof Literal<?> literal){
            return bitNot(literal);
        }
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return this.value.inferType();
    }

    Literal<?> bitNot(Literal<?> literal) throws TypeMismatchError {
        if (literal instanceof  IntLiteral intLiteral){
            return new IntLiteral(~intLiteral.value).setSourceLocation(this.getSourceLocation());
        } else if(literal instanceof ByteLiteral byteLiteral){
            return new ByteLiteral((byte) ~byteLiteral.value).setSourceLocation(this.getSourceLocation());
        } else if(literal instanceof ShortLiteral shortLiteral){
            return new ShortLiteral((short) ~shortLiteral.value).setSourceLocation(this.getSourceLocation());
        } else if(literal instanceof LongLiteral longLiteral){
            return new LongLiteral(~longLiteral.value).setSourceLocation(this.getSourceLocation());
        }
        throw new TypeMismatchError("int family value expected", this.getSourceLocation());
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();
            var v = this.value.visit(blockCompiler);
            if (v instanceof Literal<?> literal) {
                code.assignLiteral(localVar.getVariableSlot(), bitNot(literal));
            } else {
                code.bitnot(localVar.getVariableSlot(), ((Var.LocalVar) v).getVariableSlot());
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BitNot n && Objects.equals(n.value, this.value);
    }
}
