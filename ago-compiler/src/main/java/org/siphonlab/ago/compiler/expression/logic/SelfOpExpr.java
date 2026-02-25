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
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.array.CollectionElement;


public abstract class SelfOpExpr extends ExpressionInFunctionBody {

    protected final Expression site;
    protected final Expression value;

    public SelfOpExpr(FunctionDef ownerFunction, Expression site, Expression value){
        super(ownerFunction);
        this.site = site;
        this.value = value;
    }

    abstract Expression expr(Expression left, Expression right) throws CompilationError;


    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            if (site instanceof Var.Field field) {
                field.simplify(blockCompiler);
                blockCompiler.lockRegister(field.getBaseVar());
                var temp = localVar != null ? localVar : blockCompiler.acquireTempVar(this);
                expr(field, value).outputToLocalVar(temp, blockCompiler);
                ownerFunction.assign(field, temp).termVisit(blockCompiler);
                blockCompiler.releaseRegister(field.getBaseVar());
            } else if (site instanceof Var.LocalVar var) {
                expr(var, value).outputToLocalVar(var, blockCompiler);
                if (localVar != null && localVar != var) {
                    ownerFunction.assign(localVar, var).termVisit(blockCompiler);
                }
            } else if (site instanceof CollectionElement collectionElement) {
                var old = collectionElement.visit(blockCompiler);
                var arr = collectionElement.getProcessedCollection();
                blockCompiler.lockRegister(arr);
                var index = collectionElement.getProcessedIndex();
                blockCompiler.lockRegister(index);
                var expr = expr(old, value);
                var r = expr.setSourceLocation(this.sourceLocation).visit(blockCompiler);
                collectionElement.toPutElement(arr, index, r, ownerFunction).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
                blockCompiler.releaseRegister(arr);
                blockCompiler.releaseRegister(index);
                if (localVar != null) {
                    ownerFunction.assign(localVar, r).termVisit(blockCompiler);
                }
            } else if (site instanceof Attribute attribute) {
                var got = attribute.visit(blockCompiler);
                blockCompiler.lockRegister(attribute.getProcessedScope());
                var expr = expr(got, value).visit(blockCompiler);
                var r = expr.setSourceLocation(this.sourceLocation).visit(blockCompiler);
                attribute.setValue(ownerFunction, r).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
                blockCompiler.releaseRegister(attribute.getProcessedScope());
                if (localVar != null) {
                    ownerFunction.assign(localVar, r).termVisit(blockCompiler);
                }
            } else {
                throw new UnsupportedOperationException("not supported yet");
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.outputToLocalVar(null, blockCompiler);
    }

}
