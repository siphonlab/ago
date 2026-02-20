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



import org.siphonlab.ago.compiler.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.array.ArrayElement;
import org.siphonlab.ago.compiler.expression.array.ArrayLength;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;
import org.siphonlab.ago.compiler.expression.math.SelfArithmetic;

import java.util.Collections;

public class ForEachStmt extends LoopStmt{


    private final Var.LocalVar iterVar;
    private final Expression expression;
    private final Statement body;
    private final Mode mode;
    private final SourceLocation enhanceControlPartSourceLocation;

    public enum Mode{
        Iterable,
        Iterator,
        Array
    }

    public ForEachStmt(String label, Var.LocalVar iterVar, Expression expression, Statement body,
                       Mode mode, SourceLocation enhanceControlPartSourceLocation) throws CompilationError {
        super(label);
        this.iterVar = iterVar;
        this.expression = expression.transform().setParent(this);
        this.body = body.transform().setParent(this);
        this.mode = mode;
        this.enhanceControlPartSourceLocation = enhanceControlPartSourceLocation;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        if(mode == Mode.Array){
            iterateArray(blockCompiler);
            return;
        }
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();
            this.exitLabel = blockCompiler.createLabel();
            this.continueLabel = blockCompiler.createLabel();

            FunctionDef functionDef = blockCompiler.getFunctionDef();

            Var.LocalVar iteratorValue;
            ClassDef iteratorType;
            if(mode == Mode.Iterable) {
                // iterable
                ClassDef iterableType = expression.inferType();
                ClassUnder iteratorFun = (ClassUnder) ClassUnder.create(expression, iterableType.getChild("iterator#")).setSourceLocation(expression.getSourceLocation()).setParent(this);
                var invokeIteratorFun = new Invoke(Invoke.InvokeMode.Invoke, functionDef, iteratorFun, Collections.emptyList(), expression.getSourceLocation());
                iteratorType = invokeIteratorFun.inferType();

                iteratorValue = (Var.LocalVar) invokeIteratorFun.visit(blockCompiler);
                blockCompiler.lockRegister(iteratorValue);
            } else {
                iteratorValue = (Var.LocalVar) expression.visit(blockCompiler);
                iteratorType = iteratorValue.inferType();
            }

            code.jumpIfNot(iteratorValue.getVariableSlot(), exitLabel);

            this.continueLabel.here();      // test hasNext() and fetch next();

            ClassUnder hasNextFun = (ClassUnder) ClassUnder.create(iteratorValue, iteratorType.getChild("hasNext#")).setSourceLocation(expression.getSourceLocation()).setParent(this);
            var invokeHasNext = new Invoke(Invoke.InvokeMode.Invoke, functionDef, hasNextFun, Collections.emptyList(), expression.getSourceLocation());
            Var.LocalVar hasNextValue = (Var.LocalVar) invokeHasNext.visit(blockCompiler);
            code.jumpIfNot(hasNextValue.getVariableSlot(), exitLabel);

            ClassUnder nextFun = (ClassUnder) ClassUnder.create(iteratorValue, iteratorType.getChild("next#")).setSourceLocation(expression.getSourceLocation()).setParent(this);
            var invokeNext = new Invoke(Invoke.InvokeMode.Invoke, functionDef, nextFun, Collections.emptyList(), expression.getSourceLocation());
            Assign.to(iterVar, invokeNext).setSourceLocation(enhanceControlPartSourceLocation).termVisit(blockCompiler);

            this.body.termVisit(blockCompiler);

            blockCompiler.releaseRegister(iteratorValue);

            code.jump(continueLabel);
            exitLabel.here();
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    private void iterateArray(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();

            Var.LocalVar i = blockCompiler.acquireTempVar(new IntLiteral(0));
            blockCompiler.lockRegister(i);

            Var.LocalVar array = (Var.LocalVar) expression.visit(blockCompiler);
            blockCompiler.lockRegister(array);

            Assign.to(i, new IntLiteral(0)).termVisit(blockCompiler);
            Var.LocalVar length = (Var.LocalVar) new ArrayLength(array).visit(blockCompiler);
            blockCompiler.lockRegister(length);

            this.continueLabel = blockCompiler.createLabel().here();
            this.exitLabel = blockCompiler.createLabel();
            Var.LocalVar r = (Var.LocalVar) new Compare(i, length, Compare.Type.LT).visit(blockCompiler);
            code.jumpIfNot(r.getVariableSlot(), exitLabel);
            Assign.to(iterVar, new ArrayElement(array, i)).setSourceLocation(enhanceControlPartSourceLocation).termVisit(blockCompiler);

            this.body.termVisit(blockCompiler);

            new SelfArithmetic(i, new IntLiteral(1), SelfArithmetic.Type.Inc).setSourceLocation(enhanceControlPartSourceLocation).termVisit(blockCompiler);
            code.jump(continueLabel);

            exitLabel.here();

            blockCompiler.releaseRegister(length);
            blockCompiler.releaseRegister(array);
            blockCompiler.releaseRegister(i);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public String toString() {
        return "for(%s in %s) ".formatted(this.iterVar, this.expression, this.body);
    }
}
