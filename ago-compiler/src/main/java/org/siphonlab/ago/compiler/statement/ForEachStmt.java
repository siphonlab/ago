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
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.array.ArrayElement;
import org.siphonlab.ago.compiler.expression.array.ArrayLength;
import org.siphonlab.ago.compiler.expression.invoke.Invoke;
import org.siphonlab.ago.compiler.expression.invoke.InvokeCallFrame;
import org.siphonlab.ago.compiler.expression.math.SelfArithmetic;

import java.util.Collections;

import static org.siphonlab.ago.compiler.expression.invoke.Invoke.invokeCallFrame;

public class ForEachStmt extends LoopStmt{


    private final Var.LocalVar iterVar;
    private Expression expression;
    private final Statement body;
    private final Mode mode;
    private final SourceLocation enhanceControlPartSourceLocation;

    public enum Mode{
        Iterable,
        Iterator,
        Array,
        Generator
    }

    public ForEachStmt(FunctionDef ownerFunction, String label, Var.LocalVar iterVar, Expression expression, Statement body,
                       Mode mode, SourceLocation enhanceControlPartSourceLocation) throws CompilationError {
        super(ownerFunction, label);
        this.iterVar = iterVar;
        this.expression = expression.transform().setParent(this);
        this.body = body.transform().setParent(this);
        this.mode = mode;
        this.enhanceControlPartSourceLocation = enhanceControlPartSourceLocation;
    }

    @Override
    public Statement transform() throws CompilationError {
        if(this.transformed) return this;
        if(this.expression.inferType() instanceof NullableClassDef && !(this.expression instanceof NullableValue)){
            this.expression = new NullableValue(ownerFunction, this.expression);
        }
        return super.transform();
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        CodeBuffer code = blockCompiler.getCode();

        if(mode == Mode.Array){
            iterateArray(blockCompiler);
            return;
        }
        if(mode == Mode.Generator){
            iterateGenerator(blockCompiler);
            return;
        }
        try {
            blockCompiler.enter(this);

            this.exitLabel = blockCompiler.createLabel();
            this.continueLabel = blockCompiler.createLabel();

            Var.LocalVar iteratorValue;
            if(this.expression instanceof NullableValue nullableValue) {
                nullableValue.visit(blockCompiler);
                var isNull = nullableValue.isNull().visit(blockCompiler);
                code.jumpIf(isNull.getVariableSlot(), this.exitLabel);
                iteratorValue = nullableValue.nonNullValue().visit(blockCompiler);
            } else {
                iteratorValue = (Var.LocalVar) expression.visit(blockCompiler);
            }

            ClassDef iteratorType;
            if(mode == Mode.Iterable) {
                // iterable
                ClassDef iterableType = expression.inferType();
                ClassUnder iteratorFun = (ClassUnder) ClassUnder.create(ownerFunction, iteratorValue, iterableType.getChild("iterator#")).setSourceLocation(expression.getSourceLocation()).setParent(this);
                var invokeIteratorFun = ownerFunction.invoke(Invoke.InvokeMode.Invoke, iteratorFun, Collections.emptyList(), expression.getSourceLocation()).transform();
                iteratorType = invokeIteratorFun.inferType();

                iteratorValue = (Var.LocalVar) invokeIteratorFun.visit(blockCompiler);
            } else {
                iteratorType = iteratorValue.inferType();
            }
            blockCompiler.lockRegister(iteratorValue);

            this.continueLabel.here();      // test hasNext() and fetch next();

            ClassUnder hasNextFun = (ClassUnder) ClassUnder.create(ownerFunction, iteratorValue, iteratorType.getChild("hasNext#")).setSourceLocation(expression.getSourceLocation()).setParent(this);
            var invokeHasNext = ownerFunction.invoke(Invoke.InvokeMode.Invoke, hasNextFun, Collections.emptyList(), expression.getSourceLocation()).transform();
            Var.LocalVar hasNextValue = (Var.LocalVar) invokeHasNext.visit(blockCompiler);
            code.jumpIfNot(hasNextValue.getVariableSlot(), exitLabel);

            ClassUnder nextFun = (ClassUnder) ClassUnder.create(ownerFunction, iteratorValue, iteratorType.getChild("next#")).setSourceLocation(expression.getSourceLocation()).setParent(this);
            var invokeNext = ownerFunction.invoke(Invoke.InvokeMode.Invoke, nextFun, Collections.emptyList(), expression.getSourceLocation()).transform();
            ownerFunction.assign(iterVar, invokeNext).setSourceLocation(enhanceControlPartSourceLocation).termVisit(blockCompiler);

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

            blockCompiler.lockRegister(iterVar);

            Var.LocalVar i = blockCompiler.acquireTempVar(getRoot().createIntLiteral(0));
            blockCompiler.lockRegister(i);

            this.exitLabel = blockCompiler.createLabel();

            Var.LocalVar array;
            if(this.expression instanceof NullableValue nullableValue) {
                nullableValue.visit(blockCompiler);
                var isNull = nullableValue.isNull().visit(blockCompiler);
                code.jumpIf(isNull.getVariableSlot(), this.exitLabel);
                array = nullableValue.nonNullValue().visit(blockCompiler);
            } else {
                array = (Var.LocalVar) expression.visit(blockCompiler);
            }
            blockCompiler.lockRegister(array);

            ownerFunction.assign(i, getRoot().createIntLiteral(0)).termVisit(blockCompiler);
            Var.LocalVar length = (Var.LocalVar) new ArrayLength(ownerFunction, array).visit(blockCompiler);
            this.continueLabel = blockCompiler.createLabel().here();
            blockCompiler.lockRegister(length);

            Var.LocalVar r = (Var.LocalVar) new Compare(ownerFunction, i, length, Compare.Type.LT).visit(blockCompiler);
            code.jumpIfNot(r.getVariableSlot(), exitLabel);
            ownerFunction.assign(iterVar, new ArrayElement(ownerFunction, array, i)).setSourceLocation(enhanceControlPartSourceLocation).termVisit(blockCompiler);

            this.body.termVisit(blockCompiler);

            new SelfArithmetic(ownerFunction, i, getRoot().createIntLiteral(1), SelfArithmetic.Type.Inc).setSourceLocation(enhanceControlPartSourceLocation).termVisit(blockCompiler);
            code.jump(continueLabel);

            exitLabel.here();

            blockCompiler.releaseRegister(iterVar);
            blockCompiler.releaseRegister(length);
            blockCompiler.releaseRegister(array);
            blockCompiler.releaseRegister(i);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    private void iterateGenerator(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();

            this.exitLabel = blockCompiler.createLabel();

            Var.LocalVar generator;
            if(this.expression instanceof NullableValue nullableValue) {
                nullableValue.visit(blockCompiler);
                var isNull = nullableValue.isNull().visit(blockCompiler);
                code.jumpIf(isNull.getVariableSlot(), this.exitLabel);
                generator = nullableValue.nonNullValue().visit(blockCompiler);
            } else {
                generator = (Var.LocalVar) expression.visit(blockCompiler);
            }
            blockCompiler.lockRegister(generator);

            var genClass = getRoot().getGeneratorOfAnyClass().asThatOrSuperOfThat(generator.inferType());
            if(genClass == null){
                throw new TypeMismatchError("generator function expected", this.expression.getSourceLocation());
            }

            this.continueLabel = blockCompiler.createLabel();

            Invoke.InvokeMode invokeMode;
            Expression forkContext;
            if(expression instanceof Invoke invoke){
                invokeMode = invoke.getInvokeMode();
                if(invokeMode == Invoke.InvokeMode.Await){
                    invokeMode = Invoke.InvokeMode.Fork;
                }
                forkContext = invoke.getForkContext();
            } else if(expression instanceof InvokeCallFrame invoke){
                invokeMode = invoke.getInvokeMode();
                if(invokeMode == Invoke.InvokeMode.Await){
                    invokeMode = Invoke.InvokeMode.Fork;
                }
                forkContext = invoke.getForkContext();
            } else {
                forkContext = null;
                invokeMode = Invoke.InvokeMode.Invoke;
            }

            if(invokeMode.isAsync()) {
                invokeCallFrame(blockCompiler, invokeMode, generator, forkContext);     // spawn for first time

                code.await();       // wait result

                Label test = blockCompiler.createLabel().here();
                var done = (Var.LocalVar)ownerFunction.field(generator, genClass.getVariable("done")).visit(blockCompiler);
                code.jumpIf(done.getVariableSlot(), exitLabel);

                code.accept(iterVar.getVariableSlot());

                this.body.termVisit(blockCompiler);

                continueLabel.here();
                code.resume(generator.getVariableSlot());       // resume after the second time
                code.await();
                code.jump(test);

                exitLabel.here();
            } else {
                continueLabel.here();
                invokeCallFrame(blockCompiler, invokeMode, generator, forkContext);

                var done = (Var.LocalVar)ownerFunction.field(generator, genClass.getVariable("done")).visit(blockCompiler);
                code.jumpIf(done.getVariableSlot(), exitLabel);

                code.accept(iterVar.getVariableSlot());

                this.body.termVisit(blockCompiler);

                code.jump(continueLabel);

                exitLabel.here();
            }


            if (generator.varMode == Var.LocalVar.VarMode.Temp) {
                // release the register after invoke if it's a temp var
                ownerFunction.assign(generator, getRoot().nullLiteral()).termVisit(blockCompiler);
            }

            blockCompiler.releaseRegister(generator);
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
