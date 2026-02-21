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
package org.siphonlab.ago.compiler;

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;
import org.siphonlab.ago.compiler.statement.Label;

public class IfElseExpr extends ExpressionBase {


    private final Expression ifPart;
    private final Expression condition;
    private final Expression elsePart;

    public IfElseExpr(Expression ifPart, Expression condition, Expression elsePart) throws CompilationError {
        this.ifPart = ifPart.setParent(this).transform();
        this.condition = condition.setParent(this).transform();
        this.elsePart = new Cast(elsePart, this.ifPart.inferType()).transform();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(this.condition instanceof Literal<?> literal){
            if(BooleanLiteral.isTrue(literal)){
                return ifPart;
            } else {
                return elsePart;
            }
        }
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return this.ifPart.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        var term = this.condition.transform().visit(blockCompiler);
        try {
            blockCompiler.enter(this);

            if(term instanceof Literal<?> literal){
                if(BooleanLiteral.isTrue(literal)){
                    ifPart.outputToLocalVar(localVar,blockCompiler);
                } else {
                    elsePart.outputToLocalVar(localVar,blockCompiler);
                }
            } else {
                CodeBuffer code = blockCompiler.getCode();
                Label elseEntrance = blockCompiler.createLabel();
                Label exit = blockCompiler.createLabel();
                code.jumpIfNot(((Var.LocalVar)term).getVariableSlot(), elseEntrance);
                Assign.to(localVar, ifPart).setSourceLocation(ifPart.getSourceLocation()).termVisit(blockCompiler);
                code.jump(exit);
                elseEntrance.here();
                Assign.to(localVar,elsePart).setSourceLocation(elsePart.getSourceLocation()).termVisit(blockCompiler);
                exit.here();
            }
        } catch (CompilationError e){
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

}
