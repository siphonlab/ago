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


import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.array.ArrayLiteral;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;
import org.siphonlab.ago.compiler.generic.TypeParamsContext;
import org.siphonlab.ago.compiler.statement.ExpressionStmt;
import org.siphonlab.ago.compiler.statement.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Invoke extends ExpressionInFunctionBody {

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

    protected MaybeFunction maybeFunction;
    protected FunctionDef resolvedFunctionDef;
    protected Expression scope;
    protected List<Expression> arguments;
    private final InvokeMode invokeMode;

    private Expression preparedVisitorForNullable;
    private Statement preparedTermVisitorForNullable;

    public Invoke(FunctionDef ownerFunction, InvokeMode invokeMode, MaybeFunction maybeFunction, List<Expression> arguments, SourceLocation sourceLocation) throws CompilationError {
        super(ownerFunction);
        this.sourceLocation = sourceLocation;
        this.invokeMode = invokeMode;
        this.scope = maybeFunction.getScopeOfFunction();
        if(maybeFunction instanceof ConstClass constClass && !constClass.getClassDef().isTop()){
            throw new SyntaxError("'%s' is not allowed to create in current scope".formatted(constClass), sourceLocation);
        }
        this.maybeFunction = maybeFunction;
        this.arguments = arguments;
    }

    @Override
    public Invoke transform() throws CompilationError {
        return (Invoke) super.transform();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        List<Expression> transformedArguments = new ArrayList<>(arguments.size());
        if(maybeFunction instanceof BindExtensionMethod bindExtensionMethod){
            Expression firstArg = bindExtensionMethod.setParent(this).getTarget().transform();
            if(firstArg instanceof NullableValue.NonNullPlaceHolder nc){
                this.preparedVisitorForNullable = BlockCompiler.nullableIfThenExpr(this.ownerFunction, nc.getNullableValue(), baseOfFirstArg -> {
                        var b = new BindExtensionMethod(ownerFunction, baseOfFirstArg, bindExtensionMethod.getFunction());
                        return new Invoke(ownerFunction, invokeMode, b, new ArrayList<>(arguments), getSourceLocation()).transform();
                    }
                );
                this.preparedTermVisitorForNullable = BlockCompiler.nullableIfThenStmt(ownerFunction, nc.getNullableValue(), baseOfFirstArg ->{
                    var b = new BindExtensionMethod(ownerFunction, baseOfFirstArg, bindExtensionMethod.getFunction());
                    return new ExpressionStmt(ownerFunction, new Invoke(ownerFunction, invokeMode, b, new ArrayList<>(arguments), getSourceLocation()).transform());
                });
                return this;
            }
            transformedArguments.add(firstArg);
        }

        if(this.scope instanceof NullableValue.NonNullPlaceHolder nc){
            this.preparedVisitorForNullable = BlockCompiler.nullableIfThenExpr(this.ownerFunction, nc.getNullableValue(), baseOfScope -> {
                    var r = new Invoke(ownerFunction, invokeMode, maybeFunction, this.arguments, getSourceLocation());
                    r.setScope(baseOfScope);
                    return r.transform();
                }
            );
            this.preparedTermVisitorForNullable = BlockCompiler.nullableIfThenStmt(ownerFunction, nc.getNullableValue(), baseOfScope ->{
                var r = new Invoke(ownerFunction, invokeMode, maybeFunction, this.arguments, getSourceLocation());
                r.setScope(baseOfScope);
                return new ExpressionStmt(ownerFunction, r.transform());
            });
            return this;
        }

        for (Expression argument : arguments) {
            transformedArguments.add(argument.transform().setParent(this));
        }
        this.resolvedFunctionDef = findBestPolymorphismMethod(maybeFunction.getFunction(), maybeFunction.getCandidates(), transformedArguments, sourceLocation);
        if(maybeFunction instanceof BindExtensionMethod bindExtensionMethod){
            maybeFunction = new ConstClass(bindExtensionMethod.getFunction()).setSourceLocation(bindExtensionMethod.getSourceLocation());
            maybeFunction.setCandidates(bindExtensionMethod.getCandidates());
        }
        if(resolvedFunctionDef == null){
            findBestPolymorphismMethod(maybeFunction.getFunction(), maybeFunction.getCandidates(), transformedArguments, sourceLocation);
            throw new SyntaxError("no function found for '%s'".formatted(maybeFunction.getFunction()), sourceLocation);
        }

        this.arguments = transformedArguments;
        List<Expression> expressions = this.arguments;
        List<Parameter> parameters = resolvedFunctionDef.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            var parameter = parameters.get(i);
            Expression arg;
            if (parameter.isVarArgs()) {
                List<Expression> elements = new ArrayList<>();
                for (var j = i ; j < expressions.size(); j++) {
                    elements.add(expressions.get(j));
                }
                arg = new ArrayLiteral(ownerFunction, (ArrayClassDef) parameter.getType(),elements).transform();
                if(i < arguments.size()) {
                    arguments.set(i, arg);
                } else if(i == arguments.size()){
                    arguments.add(arg);
                } else {
                    throw new IllegalStateException("unexpected arguments size '%d' for parameter size '%d'".formatted(arguments.size(), parameters.size()));
                }
                this.arguments = arguments.subList(0,i + 1);
                break;
            } else{
                arg = expressions.get(i);
                arg = ownerFunction.cast(arg, parameter.getType()).setSourceLocation(arg.getSourceLocation()).setParent(arg.getParent()).transform();
                arguments.set(i, arg);
            }
        }
        return this;
    }

    private void setScope(Expression scope) {
        this.scope = scope;
    }

    public InvokeMode getInvokeMode() {
        return invokeMode;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        this.transform();
        if(this.preparedVisitorForNullable != null){
            return this.preparedVisitorForNullable.inferType();
        }
        if (invokeMode.isAsync() || resolvedFunctionDef.isGenerator()) {
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

        if(preparedVisitorForNullable != null) {
            preparedVisitorForNullable.outputToLocalVar(localVar, blockCompiler);       //TODO it will spend a register for temp result, not the best performance way
            return;
        }

        blockCompiler.validateThrowException(this.resolvedFunctionDef.getThrowsExceptions(), this);
        try {
            blockCompiler.enter(this);

            // for generator function, the instance is its result
            if(resolvedFunctionDef.isGenerator()){
                prepareInvocation(blockCompiler, localVar);
                return;
            }
            var instance = prepareInvocation(blockCompiler);

            if (instance == null) {
                throw new IllegalStateException("create function instance for '%s' failed".formatted(this));
            }

            blockCompiler.lockRegister(localVar);
            invokeCallFrame(blockCompiler, invokeMode, instance, localVar, forkContext);
            blockCompiler.releaseRegister(localVar);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    public static void invokeCallFrame(BlockCompiler blockCompiler, InvokeMode invokeMode, Var.LocalVar callFrameInstance, Var.LocalVar result, Expression forkContext) throws CompilationError {
        blockCompiler.lockRegister(callFrameInstance);

        CodeBuffer code = blockCompiler.getCode();
        Var.LocalVar forkContextVar = null;
        if(forkContext != null){
            forkContextVar = (Var.LocalVar) forkContext.visit(blockCompiler);
        }

        Root root = result.inferType().getRoot();
        if (invokeMode.isAsync()) {
            if(forkContext != null){
                code.invokeAsyncViaContext(invokeMode, callFrameInstance.getVariableSlot(), result.getVariableSlot(), forkContextVar.getVariableSlot());
            } else {
                code.invokeAsync(invokeMode, callFrameInstance.getVariableSlot(), result.getVariableSlot());
            }
        } else {
            if(invokeMode == InvokeMode.Await && forkContext != null){
                code.invokeAsyncViaContext(invokeMode, callFrameInstance.getVariableSlot(), forkContextVar.getVariableSlot());
            } else {
                code.invoke(invokeMode, callFrameInstance.getVariableSlot());
            }

            if (result.getVariableSlot().getClassDef() instanceof AnyClassDef) {
                code.acceptAny(result.getVariableSlot());
            } else {
                code.accept(result.getVariableSlot());
            }
        }
        blockCompiler.releaseRegister(callFrameInstance);
        if (callFrameInstance.varMode == Var.LocalVar.VarMode.Temp) {
            // release the register after invoke if it's a temp var
            blockCompiler.getFunctionDef().assign(callFrameInstance, root.nullLiteral()).termVisit(blockCompiler);
        }
    }

    public static void invokeCallFrame(BlockCompiler blockCompiler, InvokeMode invokeMode, Var.LocalVar callFrameInstance, Expression forkContext) throws CompilationError {
        blockCompiler.lockRegister(callFrameInstance);

        CodeBuffer code = blockCompiler.getCode();
        Var.LocalVar forkContextVar = null;
        if(forkContext != null){
            forkContextVar = (Var.LocalVar) forkContext.visit(blockCompiler);
        }

        FunctionDef ownerFunction = blockCompiler.getFunctionDef();
        Root root = ownerFunction.getRoot();
        if (invokeMode.isAsync()) {
            if(forkContext != null){
                code.invokeAsyncViaContext(invokeMode, callFrameInstance.getVariableSlot(), forkContextVar.getVariableSlot());
            } else {
                code.invoke(invokeMode, callFrameInstance.getVariableSlot());
            }
        } else {
            if(invokeMode == InvokeMode.Await && forkContext != null){
                code.invokeAsyncViaContext(invokeMode, callFrameInstance.getVariableSlot(), forkContextVar.getVariableSlot());
            } else {
                code.invoke(invokeMode, callFrameInstance.getVariableSlot());
            }
        }
        blockCompiler.releaseRegister(callFrameInstance);

        if(!root.getGeneratorOfAnyClass().isThatOrSuperOfThat(callFrameInstance.inferType())){      // generator release temp var by itself
            if (callFrameInstance.varMode == Var.LocalVar.VarMode.Temp) {
                // release the register after invoke if it's a temp var
                ownerFunction.assign(callFrameInstance, root.nullLiteral()).termVisit(blockCompiler);
            }
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        if(this.preparedTermVisitorForNullable !=null){
            this.preparedTermVisitorForNullable.termVisit(blockCompiler);
            return;
        }

        blockCompiler.validateThrowException(this.resolvedFunctionDef.getThrowsExceptions(), this);

        try {
            blockCompiler.enter(this);

            var instance = prepareInvocation(blockCompiler);
            if (instance == null) {
                throw new IllegalStateException("create function instance for '%s' failed".formatted(this));
            }

            if(this.resolvedFunctionDef.isGenerator()){
                return;
            }

            invokeCallFrame(blockCompiler, invokeMode, instance, forkContext);

        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        if(this.preparedVisitorForNullable !=null){
            return this.preparedVisitorForNullable.visit(blockCompiler);
        }

        blockCompiler.validateThrowException(this.resolvedFunctionDef.getThrowsExceptions(), this);

        if(this.inferType().isVoid()){
            this.termVisit(blockCompiler);
            return getRoot().createVoidLiteral().setSourceLocation(this.getSourceLocation());
        } else {
            return super.visit(blockCompiler);
        }
    }

    private Var.LocalVar createInstance(BlockCompiler blockCompiler, Var.LocalVar receiverVar) throws CompilationError {
        var code = blockCompiler.getCode();

        if(receiverVar == null) receiverVar = blockCompiler.acquireTempVar(new SomeInstance(ownerFunction, resolvedFunctionDef).setSourceLocation(((Expression)maybeFunction).getSourceLocation()));
        SlotDef resultSlot = receiverVar.getVariableSlot();
        var fun = blockCompiler.getFunctionDef();
        boolean resolvedFunctionDefGenericInstantiateRequired = !resolvedFunctionDef.isGenericTerminated();
        if(maybeFunction instanceof ConstClass constClass){
            var n = Creator.NewProps.resolve(fun, resolvedFunctionDef);
            code.new_(resultSlot, n.setForGenericInstantiation(n.forGenericInstantiation() || resolvedFunctionDefGenericInstantiateRequired));
        } else if(maybeFunction instanceof ClassOf.ClassOfScope classOfScope){
            assert (classOfScope.getMetaLevel() == 1); // if it's metaclass it won't be Function
            Scope scope = classOfScope.getScope();
            if(resolvedFunctionDef != ownerFunction){  // an overload function of me
                scope = (Scope) this.scope;       // here parent is already classOfScope.scope.parentScope, see org.siphonlab.ago.compiler.expression.ClassOf.ClassOfScope.getScopeOfFunction
                if(scope == null){          // for top-level fun will cause this
                    ClassDef classDef = classOfScope.getClassDef();
                    var n = Creator.NewProps.resolve(fun, classDef);
                    code.new_(resultSlot, n.setForGenericInstantiation(n.forGenericInstantiation() || resolvedFunctionDefGenericInstantiateRequired));
                } else {
                    var n = Creator.NewProps.resolve(fun, scope.getClassDef());
                    code.new_scope_method(resultSlot, scope.getDepth(), false, fun.simpleNameOfFunction(resolvedFunctionDef),
                            n.setForGenericInstantiation(n.forGenericInstantiation() || resolvedFunctionDefGenericInstantiateRequired));
                }
            } else if(scope.getDepth() == 0){
                code.new_scope(resultSlot, scope.getDepth());
            }
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
            ownerFunction.assign(ownerFunction.field(instance, parameter), arg)
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
        var resolver = new FunctionInvocationResolver(ownerFunction, method, candidates, values, sourceLocation);
        var r = resolver.resolve(resolveResult -> {
            TypeParamsContext paramsContext = resolveResult.functionDef.getTypeParamsContext();
            if(paramsContext != null){
                if (!resolveResult.allFound(paramsContext)) {
                    resolveResult.error = new ResolveError("not all generic type params provided concrete argument, expected:%d provided:'%d'".formatted(paramsContext.size(), resolveResult.providedArguments.size()), sourceLocation);
                }
            }
        });
        TypeParamsContext paramsContext = r.functionDef.getTypeParamsContext();
        // TODO ensure it's ok, it seems duplicated with org.siphonlab.ago.compiler.resolvepath.NamePathResolver.resolveTypeArgsListFromAssigneeAST
        if(paramsContext != null && !r.functionDef.isGenericTerminated()) {
            ClassRefLiteral[] typeArgs = r.toTypeArgs(paramsContext);
            var pc = ownerFunction.getOrCreateGenericInstantiationClassDef(r.functionDef, typeArgs, null);
            if(pc instanceof ConcreteType c) ownerFunction.registerConcreteType(c);
            ownerFunction.idOfClass(r.functionDef);
            return (FunctionDef) pc;        // GenericInstantiationFunctionDef
        } else {
            return r.functionDef;
        }
    }

}
