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
package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.VoidLiteral;

public class Return extends Statement {

    protected Expression value;

    public Return(FunctionDef ownerFunction, Expression value) throws CompilationError {
        super(ownerFunction);
        this.value = value.transform().setParent(this);
    }

    public Return(FunctionDef ownerFunction) throws CompilationError {
        super(ownerFunction);
        this.value = ownerFunction.getRoot().createVoidLiteral();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(!this.ownerFunction.isGenerator()) {
            this.value = ownerFunction.cast(this.value, ownerFunction.getResultType()).transform();
        } else {
            if(this.value != null){
                throw new TypeMismatchError("the `return` statement of generator function shouldn't carry result.", this.getSourceLocation());
            }
        }
        return this;
    }

    protected static TryCatchFinallyStmt findClosestTryCatchFinal(Expression from) {
        for (Expression p = from.getParent(); p != null; p = p.getParent()) {
            if (p instanceof TryCatchFinallyStmt tryCatchFinallyStmt && tryCatchFinallyStmt.getFinalExit() != null) {
                return tryCatchFinallyStmt;
            }
        }
        return null;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();

            ClassDef genClass;
            if(ownerFunction.isGenerator()) {
                genClass = getRoot().getGeneratorOfAnyClass().asThatOrSuperOfThat(ownerFunction);
            } else {
                genClass = null;
            }

            // each outer finally should execute before `return`
            for(var tryCatchFinallyStmt = findClosestTryCatchFinal(this); tryCatchFinallyStmt != null; tryCatchFinallyStmt = findClosestTryCatchFinal(tryCatchFinallyStmt)) {
                Label finalExitLabel = blockCompiler.createLabel();
                code.setFinalExit(tryCatchFinallyStmt.getFinalExit().getVariableSlot(), finalExitLabel);
                if(ownerFunction.isGenerator()) {
                    ownerFunction.assign(ownerFunction.field(new Scope.Local(ownerFunction), genClass.getVariable("done")), getRoot().createBooleanLiteral(true)).termVisit(blockCompiler);
                }
                code.jump(tryCatchFinallyStmt.getFinalEntrance());
                finalExitLabel.here();      // after final executed, jump to here(return)
            }

            if(ownerFunction.isGenerator()) {
                ownerFunction.assign(ownerFunction.field(new Scope.Local(ownerFunction), genClass.getVariable("done")), getRoot().createBooleanLiteral(true)).termVisit(blockCompiler);
            }
            if(value != null) {     // for there is a VoidLiteral, the `else` branch won't enter
                var term = value.visit(blockCompiler);
                if (term instanceof Literal<?> literal) {
                    code.return_c(literal);
                } else if (term instanceof Var.LocalVar localVar) {
                    code.return_v(localVar.getVariableSlot());
                } else {
                    throw new UnsupportedOperationException();
                }
            } else {
                code.return_void();
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public String toString() {
        return "(Return %s)".formatted(value);
    }

    @Override
    public Return setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
