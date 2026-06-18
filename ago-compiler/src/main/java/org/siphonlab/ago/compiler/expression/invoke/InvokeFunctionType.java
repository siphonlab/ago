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
package org.siphonlab.ago.compiler.expression.invoke;


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;
import org.siphonlab.ago.compiler.generic.ClassIntervalClassDef;
import org.siphonlab.ago.compiler.generic.ScopedClassIntervalClassDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.join;
import static org.siphonlab.ago.compiler.expression.invoke.Invoke.invokeCallFrame;

// for `var v as [SomeFunction] = f; f()`, the expression is scopedFunctionExpr
public class InvokeFunctionType extends ExpressionInFunctionBody {

    private final List<Expression> arguments;
    private final Invoke.InvokeMode invokeMode;
    private final Expression forkContext;
    private final Expression scopedFunctionExpr;

    private ClassDef resultType;
    private final ClassDef accordingFunction;
    private List<ClassDef> parameterTypes;

    public InvokeFunctionType(FunctionDef ownerFunction, Invoke.InvokeMode invokeMode, Expression scopedFunctionExpr, List<Expression> arguments, Expression forkContext, SourceLocation sourceLocation) throws CompilationError {
        super(ownerFunction);
        this.invokeMode = invokeMode;
        this.forkContext = forkContext;
        this.sourceLocation = sourceLocation;
        List<Expression> transformedArguments = new ArrayList<>(arguments.size());
        for (Expression argument : arguments) {
            transformedArguments.add(argument.transform().setParent(this));
        }
        this.arguments = transformedArguments;

        if(scopedFunctionExpr.inferType() instanceof ClassIntervalClassDef classIntervalClassDef) {
            var lBound = classIntervalClassDef.getLBoundClass();
            if (!lBound.isThatOrDerivedFromThat(lBound.getRoot().getFunctionBaseOfAnyClass())) {
                throw new ResolveError("function value expected", scopedFunctionExpr.getSourceLocation());
            } else {
                this.accordingFunction = lBound;
            }
            var expr = new ClassOf.ClassOfScopedClassInterval(scopedFunctionExpr, ScopedClassIntervalClassDef.getMetaOfLBoundClass(classIntervalClassDef));
            this.scopedFunctionExpr = expr.setParent(this).transform();
        } else {
            throw new ResolveError("class interval expected", scopedFunctionExpr.getSourceLocation());
        }
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        if(invokeMode.isAsync()){
            return this.accordingFunction;
        } else {
            return this.resultType;
        }
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        var genericSource = accordingFunction.getGenericSource();
        var typeArguments = genericSource.typeArguments();
        this.resultType = typeArguments[0].getClassDefValue();
        if(this.arguments.size() != typeArguments.length - 1){
            throw new ResolveError("'%d' arguments expected, but '%d' provided".formatted(typeArguments.length - 1, arguments.size()), sourceLocation);
        }
        List<ClassDef> parameterTypes = new ArrayList<>();
        for (int i = 1; i < typeArguments.length; i++) {
            ClassRefLiteral typeArgument = typeArguments[i];
            Expression element = arguments.get(i - 1);
            arguments.set(i-1, ownerFunction.cast(element, typeArgument.getClassDefValue()).transform());
            parameterTypes.add(typeArgument.getClassDefValue());
        }
        this.parameterTypes = parameterTypes;
        return this;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        if(localVar.inferType().getTypeCode() == TypeCode.VOID){
            throw new SyntaxError("'void' variable not allowed", localVar.getSourceLocation());
        }
        try {
            blockCompiler.enter(this);

            blockCompiler.lockRegister(localVar);

            var instance = prepareInvocation(blockCompiler);

            invokeCallFrame(blockCompiler, invokeMode, instance, localVar, forkContext);

            blockCompiler.releaseRegister(localVar);
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

            var instance = prepareInvocation(blockCompiler);

            invokeCallFrame(blockCompiler, invokeMode, instance, forkContext);

        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        if(this.inferType().isVoid()){
            this.termVisit(blockCompiler);
            return getRoot().createVoidLiteral().setSourceLocation(this.getSourceLocation());
        } else {
            return super.visit(blockCompiler);
        }
    }


    private Var.LocalVar createInstance(BlockCompiler blockCompiler) throws CompilationError {
        var temp = blockCompiler.acquireTempVar(new SomeInstance(ownerFunction, this.scopedFunctionExpr.inferType()).setSourceLocation(scopedFunctionExpr.getSourceLocation()));
        SlotDef resultSlot = temp.getVariableSlot();
        if(this.scopedFunctionExpr instanceof ClassOf.ClassOfScopedClassInterval scopedClassInterval){
            Var.LocalVar r = (Var.LocalVar) scopedClassInterval.getScopedClassIntervalInstance().visit(blockCompiler);
            blockCompiler.lockRegister(r);
            blockCompiler.getCode().new_bound_class(temp.getVariableSlot(), r.getVariableSlot());
            blockCompiler.releaseRegister(r);
        } else {
            var instanceVar = (Var.LocalVar) this.scopedFunctionExpr.visit(blockCompiler);        // stored the class instance(function)
            blockCompiler.getCode().new_bound_class(resultSlot, instanceVar.getVariableSlot());
        }
        return temp;
    }

    private Var.LocalVar prepareInvocation(BlockCompiler blockCompiler) throws CompilationError {
        var instance = createInstance(blockCompiler);
        blockCompiler.lockRegister(instance);
        for (int i = 0; i < this.arguments.size(); i++) {
            Variable parameter = new Variable();
            parameter.setType(parameterTypes.get(i));
            parameter.setName("p_%d_%s".formatted(i, parameter.getType().getName()));
            SlotDef slot = new SlotDef();
            slot.setClassDef(parameter.getType());
            slot.setIndex(i);       // assert parameters are at the head
            parameter.setSlot(slot);

            Expression arg = arguments.get(i);
            Expression argValue;
            if(arg instanceof DefaultParameterValue defaultParameterValue){
                FunctionDef functionDef = blockCompiler.getFunctionDef();
                var invokeDefaultValue = new Invoke(functionDef, Invoke.InvokeMode.Invoke,
                        ClassUnder.create(functionDef, instance, defaultParameterValue.getResolvedParameter().getDefaultValueFun()), Collections.emptyList(), arg.getSourceLocation());
                argValue = invokeDefaultValue.transform().visit(blockCompiler);
            } else {
                argValue = arg.visit(blockCompiler);
            }
            ownerFunction.assign(new Var.Field(ownerFunction, instance, parameter), argValue)
                    .setSourceLocation(arg.getSourceLocation()).setParent(arg.getParent()).termVisit(blockCompiler);
        }
        blockCompiler.releaseRegister(instance);
        return instance;
    }

    @Override
    public String toString() {
        return "(InvokeExpr %s [%s] %s)".formatted(scopedFunctionExpr, join(arguments, ","), resultType);
    }

    @Override
    public InvokeFunctionType setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

}
