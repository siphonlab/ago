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
import org.siphonlab.ago.compiler.expression.array.CollectionElement;

import java.util.Objects;

/**
 * Wrap OutputLocalVarExpression to a temp var
 */
public class PipeToTempVar extends ExpressionInFunctionBody implements LocalVarResultExpression{

    private final Expression baseExpression;
    private final boolean locked;

    private boolean visited = false;
    private Var.LocalVar tempVar;

    public PipeToTempVar(FunctionDef ownerFunction, Expression baseExpression){
        this(ownerFunction, baseExpression, false);
    }

    public PipeToTempVar(FunctionDef ownerFunction, Expression baseExpression, boolean locked){
        super(ownerFunction);
        this.baseExpression = baseExpression;
        this.locked = locked;
    }

    public Var.LocalVar getTempVar() {
        return tempVar;
    }

    public boolean isLocked() {
        return locked;
    }

    @Override
    public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
        if(visited){
            return tempVar;
        } else {
            if(baseExpression instanceof Var.LocalVar){
                this.tempVar = (Var.LocalVar)baseExpression;
                if(locked){
                    blockCompiler.lockRegister(this.tempVar);
                }
            } else {
                this.tempVar = blockCompiler.acquireTempVar(this.baseExpression);
                baseExpression.outputToLocalVar(tempVar, blockCompiler);
                if(locked){
                    blockCompiler.lockRegister(this.tempVar);
                }
            }
            visited = true;
            return tempVar;
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        baseExpression.visit(blockCompiler);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return baseExpression.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        if(this.locked) blockCompiler.lockRegister(localVar);
        var t = this.visit(blockCompiler);
        if(this.locked) blockCompiler.releaseRegister(localVar);
        t.outputToLocalVar(localVar, blockCompiler);
    }

    @Override
    public String toString() {
        return "(PipeToTemp %s)".formatted(baseExpression);
    }

    @Override
    public PipeToTempVar setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PipeToTempVar that)) return false;
        return Objects.equals(baseExpression, that.baseExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(baseExpression);
    }

}
