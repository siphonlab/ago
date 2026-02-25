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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.generic.GenericInstantiationPlaceHolder;
import org.siphonlab.ago.compiler.generic.GenericTypeCode;
import org.siphonlab.ago.compiler.generic.ScopedClassIntervalClassDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Creator extends ExpressionInFunctionBody{

    private final Expression typeExpr;
    protected final List<Expression> arguments;
    private final Expression scope;
    private final ClassDef classDef;
    private boolean invokeConstructor = true;
    private final String constructorName;

    public Creator(FunctionDef ownerFunction, Expression typeExpr, List<Expression> arguments, SourceLocation sourceLocation) throws CompilationError {
        this(ownerFunction,typeExpr, arguments, sourceLocation, null);
    }
    public Creator(FunctionDef ownerFunction, Expression typeExpr, List<Expression> arguments, SourceLocation sourceLocation, String constructorName) throws CompilationError {
        super(ownerFunction);
        this.sourceLocation = sourceLocation;
        List<Expression> transformedArguments = new ArrayList<>(arguments.size());
        for (Expression argument : arguments) {
            transformedArguments.add(argument.transform().setParent(this));
        }
        this.arguments = transformedArguments;
        this.constructorName = constructorName;

        ClassDef classDef;
        if(typeExpr.inferType() instanceof ScopedClassIntervalClassDef scopedClassIntervalClassDef){
            ClassDef lBoundClass = scopedClassIntervalClassDef.getLBoundClass();
            if(lBoundClass != scopedClassIntervalClassDef.getUBoundClass() || lBoundClass == scopedClassIntervalClassDef.getRoot().getAnyClass()){
                throw new TypeMismatchError("for creator the scope must limit to single class, that means lbound equals ubound, and `any` was denied", sourceLocation);
            } else if(lBoundClass.getTypeCode() != TypeCode.OBJECT){
                throw new TypeMismatchError("object type expected", sourceLocation);
            }
            MetaClassDef metaClassDef = ScopedClassIntervalClassDef.getMetaOfLBoundClass(scopedClassIntervalClassDef);
            this.typeExpr = new ClassOf.ClassOfScopedClassInterval(typeExpr, metaClassDef).setParent(this);
            classDef = lBoundClass;
            scope = null;
        } else {
            var p = extractScopeAndClass(typeExpr, sourceLocation);
            scope = p.getLeft();
            classDef = p.getRight();
            this.typeExpr = typeExpr.transform().setParent(this);
        }

        if(classDef instanceof GenericInstantiationPlaceHolder genericInstantiationPlaceHolder){
            this.classDef = genericInstantiationPlaceHolder.resolve(ownerFunction, this.arguments);
        } else {
            this.classDef = classDef;
        }

        validate(classDef);

    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(this.classDef instanceof FunctionDef functionDef){
            if(this.typeExpr instanceof MaybeFunction maybeFunction){
                return new FunctionCreator(ownerFunction, maybeFunction,this.arguments,this.getSourceLocation()).transform();
            } else {
                throw new TypeMismatchError("illegal function instance creator",this.getSourceLocation());
            }
        }
        return this;
    }

    protected void validate(ClassDef classDef) throws TypeMismatchError {
        if(classDef.isInterfaceOrTrait()){
            throw new TypeMismatchError("'%s' is an interface, cannot create instance".formatted(classDef.getFullname()), this.sourceLocation);
        } else if(classDef.isAbstract()){
            throw new TypeMismatchError("'%s' is an abstract class".formatted(classDef.getFullname()), this.sourceLocation);
        }
    }

    public static Pair<Expression, ClassDef> extractScopeAndClass(Expression typeExpr, SourceLocation sourceLocation) throws SyntaxError {
        Expression scope;
        ClassDef classDef;
        if(typeExpr instanceof ConstClass constClass){
            scope = null;
            classDef = constClass.getClassDef();
            if(!classDef.isTop()){
                throw new SyntaxError("'%s' is not allowed to create in current scope".formatted(classDef.getFullname()), sourceLocation);
            }
        } else if(typeExpr instanceof ClassOf.ClassOfScope classOfScope){
            if(classOfScope.metaLevel > 1) {
                scope = null;       // for metaclass, it equals top classes
            } else {
                scope = classOfScope.getScope().getParentScope();
            }
            classDef = classOfScope.getClassDef();
        } else if(typeExpr instanceof ClassOf.ClassOfInstance classOfInstance){
            throw new UnsupportedOperationException("unsupported creator for " + typeExpr);
        } else if(typeExpr instanceof ClassUnder.ClassUnderScope classUnderScope){
            scope = classUnderScope.getScope();
            classDef = classUnderScope.getClassDef();
        } else if(typeExpr instanceof ClassUnder.ClassUnderInstance classUnderInstance) {
            scope = classUnderInstance.getScope();
            classDef = classUnderInstance.getClassDef();
        } else {
            throw new UnsupportedOperationException("unsupported creator for " + typeExpr);
        }
        return Pair.of(scope, classDef);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return classDef;
    }

    void beforeInvokeConstructor(Var.LocalVar instanceVar, BlockCompiler blockCompiler) throws CompilationError {
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        CodeBuffer code = blockCompiler.getCode();
        var fun = blockCompiler.getFunctionDef();

        SlotDef objectSlot = localVar.getVariableSlot();
        try {
            blockCompiler.enter(this);

            if(typeExpr instanceof ClassOf.ClassOfScopedClassInterval classOfScopedClassInterval){
                Var.LocalVar r = (Var.LocalVar) classOfScopedClassInterval.getScopedClassIntervalInstance().visit(blockCompiler);
                blockCompiler.lockRegister(r);
                code.new_bound_class(localVar.getVariableSlot(), r.getVariableSlot());
                blockCompiler.releaseRegister(r);
            } else {
                var n = NewProps.resolve(fun, classDef);
                if (this.scope instanceof Scope scope) {
                    code.new_scope_child(objectSlot, scope.getDepth(), n);
                } else if (this.scope == null) {
                    code.new_(objectSlot, n);
                } else {
                    var term = this.scope.visit(blockCompiler);
                    code.new_(objectSlot, ((Var.LocalVar) term).getVariableSlot(), n);
                }
            }
            if(invokeConstructor) {
                ConstructorDef constructor;
                if(constructorName != null){
                    constructor = (ConstructorDef) this.classDef.getChild(constructorName);
                    if(constructor == null){
                        throw new ResolveError("constructor '%s' not found".formatted(constructorName), typeExpr.getSourceLocation());
                    }
                } else {
                    constructor = this.classDef.getConstructor();
                }
                beforeInvokeConstructor(localVar, blockCompiler);
                if (constructor != null) {
                    blockCompiler.lockRegister(localVar);
                    var constructorInvocation = makeConstructorInvocation(localVar, constructor);
                    constructorInvocation.termVisit(blockCompiler);
                    blockCompiler.releaseRegister(localVar);
                } else if (CollectionUtils.isNotEmpty(this.arguments)) {
                    throw new ResolveError("no constructor found", typeExpr.getSourceLocation());
                } else {
                    // no constructor
                }
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    protected Expression makeConstructorInvocation(Var.LocalVar localVar, ConstructorDef constructor) throws CompilationError {
        var c = ClassUnder.create(ownerFunction, localVar, constructor);
        if(this.constructorName == null) {
            c.setCandidates(this.classDef.getConstructors());
        } else {
            c.setCandidates(Collections.singleton(constructor));
        }
        var constructorInvocation = ownerFunction.invoke(Invoke.InvokeMode.Invoke, c, this.arguments, this.sourceLocation).setSourceLocation(this.getSourceLocation()).transform();
        return constructorInvocation;
    }

    public Creator setInvokeConstructor(boolean invokeConstructor) {
        this.invokeConstructor = invokeConstructor;
        return this;
    }

    public boolean isInvokeConstructor() {
        return invokeConstructor;
    }

    public static final class NewProps {
        private int className;
        private boolean isGenericCode;
        private boolean forGenericInstantiation;
        private boolean interfaceInvoke;
        private boolean traitInScope;
        private boolean isNative;

        private NewProps(int className, boolean isGenericCode,
                        boolean forGenericInstantiation, boolean isNative) {
            this.className = className;
            this.isGenericCode = isGenericCode;
            this.forGenericInstantiation = forGenericInstantiation;
            this.isNative = isNative;
        }

        static NewProps resolve(ClassDef scopeFun, ClassDef classToNew) {
            boolean isGenericCode;
            int className;
            boolean forGenericInstantiation = false;
            if (classToNew instanceof GenericTypeCode.GenericCodeAvatarClassDef genericCodeAvatarClassDef) {
                isGenericCode = true;
                className = genericCodeAvatarClassDef.getTypeCode().getValue();
            } else {
                isGenericCode = false;
                className = scopeFun.idOfClass(classToNew);
                forGenericInstantiation = classToNew.isGenericInstantiateRequiredForNew();
            }
            return new NewProps(className, isGenericCode, forGenericInstantiation, classToNew.isNative());
        }

        public NewProps setForGenericInstantiation(boolean v) {
            this.forGenericInstantiation = v;
            return this;
        }

        public void isInterfaceInvoke(boolean interfaceInvoke) {
            this.interfaceInvoke = interfaceInvoke;
        }

        public int className() {
            return className;
        }

        public boolean isGenericCode() {
            return isGenericCode;
        }

        public boolean forGenericInstantiation() {
            return forGenericInstantiation;
        }

        public boolean isInterfaceInvoke() {
            return interfaceInvoke;
        }

        public boolean isNative(){return isNative;}

        @Override
        public String toString() {
            return "NewProps{" +
                    "className=" + className +
                    ", isGenericCode=" + isGenericCode +
                    ", forGenericInstantiation=" + forGenericInstantiation +
                    ", interfaceInvoke=" + interfaceInvoke +
                    ", traitInScope=" + traitInScope +
                    '}';
        }

        public boolean isTraitInScope() {
            return traitInScope;
        }

        public void setTraitInScope(boolean traitInScope) {
            this.traitInScope = traitInScope;
        }

        public void setClassName(int className) {
            this.className = className;
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);
    }

    @Override
    public String toString() {
        if(this.scope == null){
            return "(New %s %s)".formatted(this.typeExpr, this.arguments);
        } else {
            return "(New %s.%s %s)".formatted(this.scope, this.typeExpr, this.arguments);
        }
    }

    @Override
    public Creator setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

}
