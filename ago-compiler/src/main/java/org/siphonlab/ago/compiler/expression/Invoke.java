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


import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.array.ArrayLiteral;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;
import org.siphonlab.ago.compiler.expression.literal.VoidLiteral;
import org.siphonlab.ago.compiler.generic.TypeParamsContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Invoke extends ExpressionBase{

    private Expression forkContext;

    public void setForkContext(Expression forkContext) {
        this.forkContext = forkContext;
    }

    public Expression getForkContext() {
        return forkContext;
    }

    public enum InvokeMode {
        Invoke, Fork, Spawn, Await;
        public boolean isAsync(){
            return this == Spawn || this == Fork;
        }
    }

    protected final MaybeFunction maybeFunction;
    protected final FunctionDef resolvedFunctionDef;
    protected final Expression scope;
    protected List<Expression> arguments;
    private final ClassDef scopeClass;
    private final InvokeMode invokeMode;

    public Invoke(InvokeMode invokeMode, ClassDef scopeClass, MaybeFunction maybeFunction, List<Expression> arguments, SourceLocation sourceLocation) throws CompilationError {
        this.scopeClass = scopeClass;
        this.sourceLocation = sourceLocation;
        this.maybeFunction = maybeFunction;
        this.invokeMode = invokeMode;
        this.scope = maybeFunction.getScopeOfFunction();
        if(maybeFunction instanceof ConstClass constClass && !constClass.getClassDef().isTop()){
            throw new SyntaxError("'%s' is not allowed to create in current scope".formatted(constClass), sourceLocation);
        }
        List<Expression> transformedArguments = new ArrayList<>(arguments.size());
        for (Expression argument : arguments) {
            transformedArguments.add(argument.transform().setParent(this));
        }
        this.arguments = transformedArguments;
        this.resolvedFunctionDef = findBestPolymorphismMethod(maybeFunction.getFunction(), maybeFunction.getCandidates(), transformedArguments, sourceLocation);
        if(resolvedFunctionDef == null){
            findBestPolymorphismMethod(maybeFunction.getFunction(), maybeFunction.getCandidates(), transformedArguments, sourceLocation);
            throw new SyntaxError("no function found for '%s'".formatted(maybeFunction.getFunction()), sourceLocation);
        }
    }

    public Invoke(InvokeMode invokeMode, MaybeFunction maybeFunction, List<Expression> arguments, SourceLocation sourceLocation) throws CompilationError {
        this(invokeMode, null, maybeFunction, arguments, sourceLocation);
    }
    @Override
    protected Expression transformInner() throws CompilationError {
        List<Expression> expressions = this.arguments;
        List<Parameter> parameters = resolvedFunctionDef.getParameters();
        for (int i = 0; i < expressions.size(); i++) {
            var parameter = parameters.get(i);
            Expression arg;
            if (parameter.isVarArgs()) {
                List<Expression> elements = new ArrayList<>();
                for (var j = i ; j < expressions.size(); j++) {
                    elements.add(expressions.get(j));
                }
                arg = new ArrayLiteral((ArrayClassDef) parameter.getType(),elements).transform();
                arguments.set(i, arg);
                this.arguments = arguments.subList(0,i + 1);
                break;
            } else{
                arg = expressions.get(i);
                arg = new Cast(arg, parameter.getType()).setSourceLocation(arg.getSourceLocation()).setParent(arg.getParent()).transform();
                arguments.set(i, arg);
            }
        }
        return this;
    }

    public InvokeMode getInvokeMode() {
        return invokeMode;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        if(invokeMode.isAsync()){
            return resolvedFunctionDef;
        } else {
            return resolvedFunctionDef.getResultType();
        }
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        if(localVar.inferType().getTypeCode() == TypeCode.VOID){
            throw new SyntaxError("'void' variable not allowed", localVar.getSourceLocation());
        }

        blockCompiler.validateThrowException(this.resolvedFunctionDef.getThrowsExceptions(), this);

        try {
            blockCompiler.lockRegister(localVar);

            blockCompiler.enter(this);

            var instance = prepareInvocation(blockCompiler);

            if (instance == null)
                return;

            blockCompiler.lockRegister(instance);
            Var.LocalVar forkContextVar = null;
            if(forkContext != null){
                forkContextVar = (Var.LocalVar) this.forkContext.visit(blockCompiler);
            }
            if (invokeMode.isAsync()) {
                if(forkContext != null){
                    blockCompiler.getCode().invokeAsyncViaContext(invokeMode, instance.getVariableSlot(), localVar.getVariableSlot(), forkContextVar.getVariableSlot());
                } else {
                    blockCompiler.getCode().invokeAsync(invokeMode, instance.getVariableSlot(), localVar.getVariableSlot());
                }
            } else {
                if(invokeMode == InvokeMode.Await && forkContext != null){
                    blockCompiler.getCode().invokeAsyncViaContext(invokeMode, instance.getVariableSlot(), forkContextVar.getVariableSlot());
                } else {
                    blockCompiler.getCode().invoke(invokeMode, instance.getVariableSlot());
                }

                if (localVar.getVariableSlot().getTypeCode() == TypeCode.OBJECT
                        && localVar.getVariableSlot().getClassDef() == resolvedFunctionDef.getRoot().getAnyClass()) {
                    blockCompiler.getCode().acceptAny(localVar.getVariableSlot());
                } else {
                    blockCompiler.getCode().accept(localVar.getVariableSlot());
                }
            }
            if(instance.varMode == Var.LocalVar.VarMode.Temp){
                // release the register after invoke if it's a temp var
                Assign.to(instance,new NullLiteral(resolvedFunctionDef)).termVisit(blockCompiler);
            }
            blockCompiler.releaseRegister(instance);
            blockCompiler.releaseRegister(localVar);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.validateThrowException(this.resolvedFunctionDef.getThrowsExceptions(), this);

        try {
            blockCompiler.enter(this);

            var instance = prepareInvocation(blockCompiler);
            if (instance == null)
                return;

            blockCompiler.lockRegister(instance);

            Var.LocalVar forkContextVar = null;
            if(forkContext != null){
                forkContextVar = (Var.LocalVar) this.forkContext.visit(blockCompiler);
            }

            if(forkContext != null){
                blockCompiler.getCode().invokeAsyncViaContext(invokeMode, instance.getVariableSlot(), forkContextVar.getVariableSlot());
            } else {
                blockCompiler.getCode().invoke(invokeMode, instance.getVariableSlot());
            }
            blockCompiler.releaseRegister(instance);

            if (instance.varMode == Var.LocalVar.VarMode.Temp) {
                // release the register after invoke if it's a temp var
                Assign.to(instance, new NullLiteral(resolvedFunctionDef)).termVisit(blockCompiler);
            }

        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.validateThrowException(this.resolvedFunctionDef.getThrowsExceptions(), this);

        if(this.inferType() == PrimitiveClassDef.VOID){
            this.termVisit(blockCompiler);
            return new VoidLiteral().setSourceLocation(this.getSourceLocation());
        } else {
            return super.visit(blockCompiler);
        }
    }

    private Var.LocalVar createInstance(BlockCompiler blockCompiler, Var.LocalVar receiverVar) throws CompilationError {
        var code = blockCompiler.getCode();

        if(receiverVar == null) receiverVar = blockCompiler.acquireTempVar(new SomeInstance(resolvedFunctionDef).setSourceLocation(((Expression)maybeFunction).getSourceLocation()));
        SlotDef resultSlot = receiverVar.getVariableSlot();
        var fun = blockCompiler.getFunctionDef();
        boolean resolvedFunctionDefGenericInstantiateRequired = resolvedFunctionDef.isGenericInstantiateRequiredForNew();
        if(maybeFunction instanceof ConstClass constClass){
            var n = Creator.NewProps.resolve(fun, resolvedFunctionDef);
            code.new_(resultSlot, n.setForGenericInstantiation(n.forGenericInstantiation() || resolvedFunctionDefGenericInstantiateRequired));
        } else if(maybeFunction instanceof ClassOf.ClassOfScope classOfScope){
            assert (classOfScope.metaLevel == 1); // if it's metaclass it won't be Function
            Scope scope = (Scope) this.scope;
            //TODO why this happen?
            //TODO handle resolvedFunctionDefGenericInstantiateRequired
            var n = Creator.NewProps.resolve(fun, scope.getClassDef());
            code.new_scope_method(resultSlot, scope.getDepth(),  false, fun.simpleNameOfFunction(resolvedFunctionDef),
                    n.setForGenericInstantiation(n.forGenericInstantiation() || resolvedFunctionDefGenericInstantiateRequired));
        } else if(maybeFunction instanceof ClassUnder.ClassUnderScope classUnderScope){
            Scope scope = (Scope) this.scope;
            boolean isSuper = false;
            if(scope.isPronoun()){
                // super lock the class of method, however `this` won't lock
                if(scope.getPronounType().isSuper()) {
                    isSuper = true;
                }
            }
            var n = Creator.NewProps.resolve(fun, scope.getClassDef());
            code.new_scope_method(resultSlot, scope.getDepth(), isSuper, fun.simpleNameOfFunction(resolvedFunctionDef),
                    n.setForGenericInstantiation(n.forGenericInstantiation() || resolvedFunctionDefGenericInstantiateRequired));
        } else if(maybeFunction instanceof ClassUnder.ClassUnderInstance classUnderInstance) {
            var instance = this.scope;
            if (instance instanceof ConstClass constClass) {
                var n = Creator.NewProps.resolve(fun, constClass.getClassDef());
                code.new_method_static(resultSlot, fun.simpleNameOfFunction(resolvedFunctionDef),
                        n.setForGenericInstantiation(n.forGenericInstantiation() || resolvedFunctionDefGenericInstantiateRequired));
            } else {
                //if(instance instanceof ClassOf || instance instanceof ClassUnder){
                // it will be loaded into a temp var, works like below
                //}
                var result = (Var.LocalVar) instance.visit(blockCompiler);
                ClassDef scopeClass = scope.inferType();
                var n = Creator.NewProps.resolve(fun, scopeClass);
                n.isInterfaceInvoke(isInterfaceInvoke(scopeClass));
                if(isInterfaceInvoke(scopeClass)){
                    if(scopeClass.getGenericSource() != null){
                        // for Interface.method, we use the template interface as scope class
                        n.setClassName(fun.idOfClass(scopeClass.getTemplateClass()));
                    }
                }
                n.setTraitInScope(scopeClass instanceof TraitDefInScope);
                code.new_method(resultSlot, result.getVariableSlot(), fun.simpleNameOfFunction(resolvedFunctionDef),
                        n.setForGenericInstantiation(n.forGenericInstantiation() || resolvedFunctionDefGenericInstantiateRequired));
            }
        } else if(maybeFunction instanceof Scope scope){
            assert scope.isPronoun();
            var n = Creator.NewProps.resolve(fun, scope.getClassDef());
            code.new_scope_method(resultSlot, scope.getDepth(), scope.getPronounType().isSuper(), fun.simpleNameOfFunction(resolvedFunctionDef),
                    n.setForGenericInstantiation(n.forGenericInstantiation()  || resolvedFunctionDefGenericInstantiateRequired));
        } else {
            throw new UnsupportedOperationException();
        }
        return receiverVar;
    }

    private boolean isInterfaceInvoke(ClassDef scopeClass) {
        boolean interfaceInvoke;
        if(scopeClass.isInterface()){
            interfaceInvoke = true;
        } else if(scopeClass.isTrait()){
            interfaceInvoke = !(scopeClass instanceof TraitDefInScope);
        } else {
            interfaceInvoke = false;
        }
        return interfaceInvoke;
    }

    private Var.LocalVar prepareInvocation(BlockCompiler blockCompiler) throws CompilationError {
        return prepareInvocation(blockCompiler, null);
    }

    public Var.LocalVar prepareInvocation(BlockCompiler blockCompiler, Var.LocalVar receiverVar) throws CompilationError {
        var args = processArgs(arguments, blockCompiler);
        // for empty constructor, needn't invoke, but for empty method must invoke for it may be overrided by descendants
        if(args.isEmpty() && (resolvedFunctionDef.isEmptyMethod() && resolvedFunctionDef instanceof ConstructorDef)){
            return null;
        }
        // ensure slots prepared
        if(resolvedFunctionDef.getCompilingStage().lte(CompilingStage.AllocateSlots))
            Compiler.processClassTillStage( resolvedFunctionDef, CompilingStage.AllocateSlots);

        var instance = createInstance(blockCompiler, receiverVar);
        List<Parameter> parameters = resolvedFunctionDef.getParameters();
        for (int i = 0; i < args.size(); i++) {
            Parameter parameter = parameters.get(i);
            var arg = args.get(i);
            Assign.to(new Var.Field(instance, parameter), arg)
                        .setSourceLocation(arg.getSourceLocation()).setParent(arg.getParent()).termVisit(blockCompiler);
        }
        for (TermExpression arg : args) {
            blockCompiler.releaseRegister(arg);
        }
        return instance;
    }

    static List<TermExpression> processArgs(List<Expression> arguments, BlockCompiler blockCompiler) throws CompilationError {
        List<TermExpression> args = new ArrayList<>(arguments.size());
        for (Expression argument : arguments) {
            TermExpression arg = argument.visit(blockCompiler);     // local var still local var, literal too, other expr become a temp local var
            blockCompiler.lockRegister(arg);
            args.add(arg);
        }
        return args;
    }

    @Override
    public String toString() {
        if(this.scope == null){
            return "(Invoke %s [%s] %s)".formatted(resolvedFunctionDef.getFullnameWithoutPackage(), StringUtils.join(arguments, ","), this.maybeFunction);
        } else {
            return "(Invoke %s::%s [%s] %s)".formatted(this.scope, resolvedFunctionDef.getName(), StringUtils.join(arguments, ","), this.maybeFunction);
        }
    }

    @Override
    public Invoke setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    public FunctionDef findBestPolymorphismMethod(FunctionDef method, Collection<FunctionDef> candidates, List<Expression> values, SourceLocation sourceLocation) throws CompilationError {
        // ensure the parameters parsed
        if(method.getCompilingStage().lte(CompilingStage.InheritsFields)){
            Compiler.processClassTillStage(method,CompilingStage.InheritsFields);
        }
        if(candidates != null){
            for (FunctionDef candidate : candidates) {
                if(candidate.getCompilingStage().lte(CompilingStage.InheritsFields)){
                    Compiler.processClassTillStage(candidate, CompilingStage.InheritsFields);
                }
            }
        }
        // resolve
        var resolver = new FunctionInvocationResolver(method, candidates, values, sourceLocation);
        var r = resolver.resolve(resolveResult -> {
            TypeParamsContext paramsContext = resolveResult.functionDef.getTypeParamsContext();
            if(paramsContext != null){
                if (!resolveResult.allFound(paramsContext)) {
                    resolveResult.error = new ResolveError("not all generic type params provided concrete argument, expected:%d provided:'%d'".formatted(paramsContext.size(), resolveResult.providedArguments.size()), sourceLocation);
                }
            }
        });
        TypeParamsContext paramsContext = r.functionDef.getTypeParamsContext();
        if(paramsContext != null) {
            ClassRefLiteral[] typeArgs = r.toTypeArgs(paramsContext);
            var pc = scopeClass.getOrCreateGenericInstantiationClassDef(r.functionDef, typeArgs, null);
            scopeClass.registerConcreteType(pc);
            scopeClass.idOfClass(r.functionDef);
            return (FunctionDef) pc;        // GenericInstantiationFunctionDef
        } else {
            return r.functionDef;
        }
    }

}
