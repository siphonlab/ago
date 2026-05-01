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
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;

import static org.siphonlab.ago.compiler.statement.Return.findClosestTryCatchFinal;

public class Yield extends Statement {

    protected Expression value;

    public Yield(FunctionDef ownerFunction, Expression value) throws CompilationError {
        super(ownerFunction);
        this.value = value.transform().setParent(this);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(!this.ownerFunction.isGenerator()) {
            throw new TypeMismatchError("the `yield` statement only works within generator function", this.getSourceLocation());
        }

        this.value = ownerFunction.cast(this.value, ownerFunction.getResultType()).transform();

        return this;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();

            // each outer finally should execute before `return`
            for(var tryCatchFinallyStmt = findClosestTryCatchFinal(this); tryCatchFinallyStmt != null; tryCatchFinallyStmt = findClosestTryCatchFinal(tryCatchFinallyStmt)) {
                Label finalExitLabel = blockCompiler.createLabel();
                code.setFinalExit(tryCatchFinallyStmt.getFinalExit().getVariableSlot(), finalExitLabel);
                code.jump(tryCatchFinallyStmt.getFinalEntrance());
                finalExitLabel.here();      // after final executed, jump to here(return)
            }

            var term = value.visit(blockCompiler);
            if (term instanceof Literal<?> literal) {
                code.yield_c(literal);
            } else if (term instanceof Var.LocalVar localVar) {
                code.yield_v(localVar.getVariableSlot());
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public String toString() {
        return "(Yield %s)".formatted(value);
    }

    @Override
    public Yield setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
