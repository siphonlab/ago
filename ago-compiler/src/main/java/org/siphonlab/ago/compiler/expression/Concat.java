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
package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;

import java.util.Objects;

import static org.siphonlab.ago.opcode.Concat.KIND_CONCAT;

public class Concat extends BiExpression{

    public Concat(Expression left, Expression right) throws CompilationError {
        super(left.transform(), right.transform());
    }

    @Override
    public Expression transformInner() throws CompilationError {
        var left = this.left;
        var right = this.right;

        if(left.inferType().getUnboxedTypeCode() != TypeCode.STRING){
            left = new Cast(this.left, PrimitiveClassDef.STRING);
            if(right.inferType().getUnboxedTypeCode() != TypeCode.STRING){
                right = new Cast(this.right, PrimitiveClassDef.STRING);
            }
            return new Concat(left, right).setSourceLocation(this.getSourceLocation()).transform();
        } else {
            if(right.inferType().getUnboxedTypeCode() != TypeCode.STRING){
                right = new Cast(right, PrimitiveClassDef.STRING);
                return new Concat(left, right).setSourceLocation(this.getSourceLocation()).transform();
            }
        }
        return super.transformInner();
    }

    @Override
    protected Expression transformUnboxed(Expression left, Expression right) throws CompilationError {
        return new Concat(left, right).setSourceLocation(this.getSourceLocation()).setParent(this.getParent()).transform();
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return PrimitiveClassDef.STRING;
    }

    @Override
    protected void processTwoVariables(Var.LocalVar result, Var.LocalVar left, Var.LocalVar right, BlockCompiler blockCompiler) throws CompilationError {
        assert left.inferType().getTypeCode() == TypeCode.STRING;
        assert right.inferType().getTypeCode() == TypeCode.STRING;
        blockCompiler.getCode().biOperate(KIND_CONCAT, left.inferType().getTypeCode(), left.getVariableSlot(), right.getVariableSlot(), result.getVariableSlot());
    }

    @Override
    protected void processRightLiteral(Var.LocalVar result, Var.LocalVar left, Literal<?> literal, BlockCompiler blockCompiler) throws CompilationError {
        assert left.inferType().getTypeCode() == TypeCode.STRING;
        assert right.inferType().getTypeCode() == TypeCode.STRING;
        blockCompiler.getCode().biOperateVariableLiteral(KIND_CONCAT, literal.inferType().getTypeCode(), left.getVariableSlot(), literal, result.getVariableSlot());
    }

    @Override
    protected void processLeftLiteral(Var.LocalVar result, Literal<?> literal, Var.LocalVar right, BlockCompiler blockCompiler) throws CompilationError {
        assert left.inferType().getTypeCode() == TypeCode.STRING;
        assert right.inferType().getTypeCode() == TypeCode.STRING;
        blockCompiler.getCode().biOperateLiteralVariable(KIND_CONCAT, literal.inferType().getTypeCode(), literal, right.getVariableSlot(), result.getVariableSlot());
    }

    @Override
    protected Expression processRightLiteral(Expression left, Literal<?> right) {
        if(right instanceof StringLiteral stringLiteral && stringLiteral.getString().isEmpty()){
            return left;
        }
        return this;
    }

    @Override
    protected Expression processLeftLiteral(Literal<?> left, Expression right) {
        if(left instanceof StringLiteral stringLiteral && stringLiteral.getString().isEmpty()){
            return right;
        }
        return this;
    }

    @Override
    protected Literal<?> processTwoLiteralsInner(Literal<?> left, Literal<?> right) throws CompilationError {
        assert left instanceof StringLiteral && right instanceof StringLiteral;

        StringLiteral leftStr = (StringLiteral) left;
        StringLiteral rightStr = (StringLiteral) right;
        if (leftStr.getString().isEmpty()) {
            return right;
        }
        if (rightStr.getString().isEmpty()) {
            return left;
        }
        return new StringLiteral(leftStr.getString() + rightStr.getString());
    }

    @Override
    public String toString() {
        return "(Concat %s %s)".formatted(left, right);
    }

    @Override
    public Concat setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Concat that)) return false;
        return Objects.equals(this.left, that.left) && Objects.equals(this.right, that.right);
    }
}
