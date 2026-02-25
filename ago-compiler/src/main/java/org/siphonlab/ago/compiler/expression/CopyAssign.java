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

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class CopyAssign extends ExpressionInFunctionBody{

    private final Expression assignee;
    private final Expression value;
    private final ClassDef commonType;

    public CopyAssign(FunctionDef ownerFunction, Expression assignee, Expression value, ClassDef commonType) throws CompilationError {
        super(ownerFunction);
        this.assignee = assignee.transform();
        this.value = value.transform();
        this.commonType = commonType;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        return super.transformInner();
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return assignee.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Var.LocalVar a = (Var.LocalVar) assignee.visit(blockCompiler);
            Var.LocalVar v = (Var.LocalVar) value.visit(blockCompiler);
            blockCompiler.getCode().copyAssign(a.getVariableSlot(), v.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(commonType));

            ownerFunction.assign(localVar,a).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Var.LocalVar a = (Var.LocalVar) assignee.visit(blockCompiler);
            Var.LocalVar v = (Var.LocalVar) value.visit(blockCompiler);
            blockCompiler.getCode().copyAssign(a.getVariableSlot(), v.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(commonType));

        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public CopyAssign setParent(Expression expression) {
        super.setParent(expression);
        return this;
    }

    @Override
    public CopyAssign setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(CopyAssign %s %s)".formatted(assignee, value);
    }
}
