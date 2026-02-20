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
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

public class CurrWithExpression extends ExpressionBase implements LocalVarResultExpression {

    private final Expression base;

    public CurrWithExpression(Expression base) throws CompilationError {
        this.base = base.transform();
        this.sourceLocation = base.getSourceLocation();
        this.setParent(base.getParent());
        base.setParent(this);
    }

    @Override
    public CurrWithExpression transform() throws CompilationError {
        ClassDef classDef = base.inferType();
        if(!classDef.getRoot().getObjectClass().isThatOrSuperOfThat(classDef)){
            throw new TypeMismatchError("with expression must be object", this.getSourceLocation());
        }
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return base.inferType();
    }

    public Var.LocalVar getLocalVar() {
        return localVar;
    }

    private Var.LocalVar localVar;

    @Override
    public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
        if(this.localVar == null){
            if(this.base instanceof Var.LocalVar localVar){
                return this.localVar = localVar;
            }
            var t = super.visit(blockCompiler);
            if(t instanceof Var.LocalVar l){
                return this.localVar = l;
            } else {
                var tempVar = blockCompiler.acquireTempVar(this.base);
                Assign.to(tempVar,t).termVisit(blockCompiler);
                return this.localVar = tempVar;
            }
        } else {
            return localVar;
        }
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        if (this.localVar == null) {
            blockCompiler.enter(this);
            base.outputToLocalVar(localVar, blockCompiler);
            this.localVar = localVar;
            blockCompiler.leave(this);
        }
    }

    @Override
    public String toString() {
        return "(NowWith %s)".formatted(this.base);
    }
}
