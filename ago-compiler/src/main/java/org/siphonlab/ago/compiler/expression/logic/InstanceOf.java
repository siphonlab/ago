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

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

import org.siphonlab.ago.compiler.expression.*;

import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;

public class InstanceOf extends ExpressionBase {

    private final Expression expression;
    private final ClassDef type;
    private final Var.LocalVar receiverVar;

    public InstanceOf(Expression expression, ClassDef right, Var.LocalVar receiverVar) throws CompilationError {
        this.expression = expression.transform().setParent(this);
        this.type = right;
        this.receiverVar = receiverVar;
        if(receiverVar!=null) receiverVar.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return PrimitiveClassDef.BOOLEAN;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(expression instanceof Literal<?> literal){
            var r = handleLiteral(literal);
            if(r != null) return r;
        }
        return this;
    }

    private Literal<Boolean> handleLiteral(Literal<?> literal){
        if (type.isPrimitiveFamily()) {
            return new BooleanLiteral(literal.getTypeCode() == type.getTypeCode());
        } else if (!type.getTypeCode().isGeneric()) {
            return new BooleanLiteral(false);
        }
        return null;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        CodeBuffer code = blockCompiler.getCode();
        var t = this.expression.visit(blockCompiler);
        Var.LocalVar expressionResult;
        try {
            blockCompiler.enter(this);

            if (t instanceof Literal<?> literal) {
                var r = handleLiteral(literal);
                if (r != null) {
                    code.assignLiteral(localVar.getVariableSlot(), r);
                    if (receiverVar != null) {
                        code.assignLiteral(receiverVar.getVariableSlot(), r);
                    }
                    return;
                } else {
                    expressionResult = blockCompiler.acquireTempVar(literal);
                    code.assignLiteral(expressionResult.getVariableSlot(), literal);
                }
            } else {
                expressionResult = (Var.LocalVar) t;
            }
            if (type.isPrimitive()) {
                code.instanceOf(localVar.getVariableSlot(), expressionResult.getVariableSlot(), type.getTypeCode());
            } else if(type.isPrimitiveFamily()) {
                code.instanceOf_primitive(localVar.getVariableSlot(), expressionResult.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(type));
            } else {
                code.instanceOf(localVar.getVariableSlot(), expressionResult.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(type));
            }
            if (this.receiverVar != null) {
                var exitLabel = blockCompiler.createLabel();
                /*
                in `if(x instanceof Class v)`, it will generate 2 jump
                    4	jump_f_vc	3,10
                    7	move_o_vv	1,0         // v = expression result
                    10	jump_f_vc	3,40
                 */
                code.jumpIfNot(localVar.getVariableSlot(), exitLabel);
                Assign.to(receiverVar, expressionResult).termVisit(blockCompiler);
                exitLabel.here();
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

}
