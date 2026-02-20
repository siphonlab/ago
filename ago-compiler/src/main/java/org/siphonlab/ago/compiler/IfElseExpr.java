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
        this.elsePart = elsePart.setParent(this).transform();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        var type1 = this.ifPart.inferType();
        var type2 = this.elsePart.inferType();
        if(type1 != type2) {
            var commonType = ClassDef.findCommonType(type1, type2);
            if(commonType == null){
                throw new TypeMismatchError("no common type found for '%s' and '%s'".formatted(type1, type2), this.getSourceLocation());
            }
            if((type1 != commonType) || (type2 != commonType)) {
                var ip = commonType != type1 ? new Cast(ifPart, commonType) : ifPart;
                var ep = commonType != type2 ? new Cast(elsePart, commonType) : elsePart;
                return new IfElseExpr(ip, condition, ep);
            }
        }
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
