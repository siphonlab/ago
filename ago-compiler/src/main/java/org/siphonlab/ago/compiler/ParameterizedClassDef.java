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
package org.siphonlab.ago.compiler;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.CastStrategy;
import org.siphonlab.ago.compiler.expression.EnumValue;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.resolvepath.NamePathResolver;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ParameterizedClassDef extends ClassDef implements ConcreteType{

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterizedClassDef.class);

    protected final ClassDef baseClass;
    protected final ConstructorDef parameterizedConstructor;
    protected final Literal<?>[] arguments;

    public static abstract class ReferencingObject{

    }

    public static abstract class RefViaClass extends ReferencingObject{
        final ClassDef classDef;
        public RefViaClass(ClassDef classDef){
            this.classDef = classDef;
        }
    }

    public static class RefViaSuperClass extends RefViaClass {
        public RefViaSuperClass(ClassDef classDef) {
            super(classDef);
        }
    }

    public static class RefViaInterface extends RefViaClass {
        public RefViaInterface(ClassDef classDef) {
            super(classDef);
        }
    }

    public static class RefViaPermitClass extends RefViaClass {
        public RefViaPermitClass(ClassDef classDef) {
            super(classDef);
        }
    }

    public static class RefViaFunctionResult extends RefViaClass {
        public RefViaFunctionResult(FunctionDef classDef) {
            super(classDef);
        }
    }

    public static class RefViaVariable extends ReferencingObject {
        private final Variable variable;

        public RefViaVariable(Variable variable) {
            this.variable = variable;
        }
    }

    public static class PlaceHolder extends ClassDef{

        private final ClassDef baseClass;
        private final AgoParser.ClassCreatorArgumentsContext classCreatorArguments;
        private final SourceLocation sourceLocation;
        private final ClassDef scopeClass;

        private final List<ReferencingObject> referencingObjects = new ArrayList<>();

        public PlaceHolder(ClassDef baseClass, AgoParser.ClassCreatorArgumentsContext classCreatorArguments, SourceLocation sourceLocation, ClassDef scopeClass) {
            super(baseClass.root, baseClass.getFullname());
            this.baseClass = baseClass;
            this.classCreatorArguments = classCreatorArguments;
            this.sourceLocation = sourceLocation;
            this.scopeClass = scopeClass;

            this.setClassType(baseClass.getClassType());
            this.setUnit(scopeClass.getUnit());
            this.setModifiers(baseClass.getModifiers());
            this.setSuperClass(baseClass.getSuperClass());
            this.setInterfaces(baseClass.getInterfaces());
            this.setCompilingStage(baseClass.getCompilingStage());
            this.parent = baseClass.parent;
        }

        @Override
        public AgoParser.ClassBodyContext getClassBody() {
            return baseClass.getClassBody();
        }

        public ClassDef getBaseClass() {
            return baseClass;
        }

        public void registerReference(ReferencingObject referencingObject){
            this.referencingObjects.add(referencingObject);
        }

        public ParameterizedClassDef resolve() throws CompilationError {
            if(baseClass instanceof FunctionDef){
                throw new TypeMismatchError("function cannot parameterized", sourceLocation);
            }
            var expressionList = classCreatorArguments.arguments().expressionList();
            Literal<?>[] literalArguments;
            if(expressionList == null){
                throw new SyntaxError("no arguments provided", sourceLocation);
            } else {
                var expressions = expressionList.expression();
                literalArguments = new Literal[expressions.size()];
                for (int i = 0; i < expressions.size(); i++) {
                    AgoParser.ExpressionContext expression = expressions.get(i);
                    boolean isLiteral = false;
                    if(expression instanceof AgoParser.PrimaryExprContext primaryExpr){
                        if(primaryExpr.primaryExpression() instanceof AgoParser.LiteralExprContext literalExpr){
                            isLiteral = true;
                            AgoParser.LiteralContext literalContext = literalExpr.literal();
                            literalArguments[i] = Literal.parse(literalContext, getRoot(), unit.sourceLocation(literalExpr));
                        } else if (primaryExpr.primaryExpression() instanceof AgoParser.NamePathExprContext namePath) {
                            var v = unit.resolveNamePath(null, this, namePath.namePath(), NamePathResolver.ResolveMode.ForValue);
                            if(v instanceof EnumValue enumValue) {
                                isLiteral = true;
                                literalArguments[i] = enumValue.toLiteral();
                            }
                        }
                    }
                    if(!isLiteral){
                        throw new TypeMismatchError("parameterized class only accept literal", unit.sourceLocation(expression));
                    }
                }

                ConstructorDef metaConstructorDef = null;
                if(baseClass.getMetaClassDef() == null)
                    throw new ResolveError("no metaclass for '%s'".formatted(baseClass.getFullname()), sourceLocation);

                for (FunctionDef constructor : baseClass.getMetaClassDef().getConstructors()) {
                    if(constructor.isInGenericInstantiation()){
                        if(constructor.getCompilingStage().lte(CompilingStage.InheritsFields)){
                            Compiler.processClassTillStage(constructor,CompilingStage.InheritsFields);
                        }
                    } else if (constructor.getCompilingStage() == CompilingStage.ParseFields) {
                        if (!constructor.parseFields()) {
                            throw new RuntimeException("'%s' depended on '%s', and it cannot parse fields now".formatted(this.getFullname(), constructor.getFullname()));
                        }
                    }
                    var parameters = constructor.getParameters();
                    if(parameters.size() == literalArguments.length){
                        var castedArgs = new ArrayList<Literal<?>>();
                        for (int i = 0; i < literalArguments.length; i++) {
                            Literal<?> argument = literalArguments[i];
                            try {
                                ClassDef type = parameters.get(i).getType();
                                if(type.isEnum()){
                                    castedArgs.add(argument);
                                } else {
                                    var l = CastStrategy.castLiteral(argument, (PrimitiveClassDef) type, argument.getSourceLocation());
                                    castedArgs.add((Literal<?>) l);
                                }
                            } catch (CompilationError e) {
                                break;
                            }
                        }
                        if(castedArgs.size() == parameters.size()){
                            metaConstructorDef = (ConstructorDef) constructor;
                            for (int i = 0; i < literalArguments.length; i++) {
                                literalArguments[i] = castedArgs.get(i);
                            }
                        }
                    }
                }
                for (Literal<?> literalArgument : literalArguments) {
                    if (literalArgument instanceof StringLiteral stringLiteral) {
                        scopeClass.idOfConstString(stringLiteral.getString());
                    }
                }
                if(metaConstructorDef == null)
                    throw scopeClass.unit.resolveError(classCreatorArguments, "no constructor in '%s' matched given arguments".formatted(baseClass.getFullname()));

                var pc = ((ClassContainer) baseClass.getParent()).getOrCreateParameterizedClass(baseClass, metaConstructorDef, literalArguments, null);
                scopeClass.registerConcreteType((ConcreteType) pc);

                for (ReferencingObject referencingObject : this.referencingObjects) {
                    if(referencingObject instanceof RefViaSuperClass refViaSuperClass){
                        refViaSuperClass.classDef.setSuperClass(pc);
                    } else if(referencingObject instanceof RefViaInterface refViaInterface) {
                        List<ClassDef> interfaces = refViaInterface.classDef.getInterfaces();
                        interfaces.set(interfaces.indexOf(this), pc);
                        refViaInterface.classDef.registerConcreteType((ConcreteType) pc);
                    } else if (referencingObject instanceof RefViaPermitClass refViaPermitClass) {
                        refViaPermitClass.classDef.setPermitClass(pc);
                        refViaPermitClass.classDef.registerConcreteType((ConcreteType) pc);
                    } else if(referencingObject instanceof RefViaVariable refViaVariable){
                        refViaVariable.variable.setType(pc);
                        refViaVariable.variable.ownerClass.registerConcreteType((ConcreteType) pc);
                    } else if(referencingObject instanceof RefViaFunctionResult refViaFunctionResult){
                        ((FunctionDef)refViaFunctionResult.classDef).setResultType(pc);
                    }
                }

                return pc;  // for use immediately
            }
        }

        @Override
        public boolean parseFields() throws CompilationError {
            if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
            if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;
            if(this.baseClass.compilingStage == CompilingStage.ParseFields){
                if(!this.baseClass.parseFields()) return false;
            }
            this.nextCompilingStage(CompilingStage.ValidateHierarchy);
            return true;
        }

        @Override
        public void inheritsFields() throws CompilationError {
            if (this.compilingStage != CompilingStage.InheritsFields) return;

            if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: inherits fields".formatted(this));
            if (baseClass != null) {
                if (baseClass.compilingStage == CompilingStage.InheritsFields) {
                    baseClass.inheritsFields();
                }
                this.inheritsFields(baseClass.getFields(), baseClass);
                if (baseClass.isInterfaceOrTrait()) {
                    this.setFieldForPermitClass(baseClass.getFieldForPermitClass());
                }
            }
            this.nextCompilingStage(CompilingStage.ValidateNewFunctions);
        }

        @Override
        public void resolveHierarchicalClasses() throws CompilationError {
            if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;
            if(baseClass != null){
                if(baseClass.compilingStage == CompilingStage.ResolveHierarchicalClasses){
                    baseClass.resolveHierarchicalClasses();
                }
                if (baseClass.isInterfaceOrTrait()) {
                    this.setPermitClass(baseClass.getPermitClass());
                    this.setFieldForPermitClass(baseClass.getFieldForPermitClass());
                }
                this.setInterfaces(new ArrayList<>(baseClass.getInterfaces()));
                this.setSuperClass(baseClass.getSuperClass());
                this.setInterfaces(new ArrayList<>(baseClass.getInterfaces()));
                this.setCompilingStage(CompilingStage.ParseFields);
            }
        }

        @Override
        public MetaClassDef resolveMetaclass() throws CompilationError {
            return baseClass.resolveMetaclass();
        }

        @Override
        public void inheritsChildClasses() throws CompilationError {
            if(this.compilingStage != CompilingStage.InheritsInnerClasses) return;

            if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: inherit child classes".formatted(this));

            if(this.baseClass.compilingStage == CompilingStage.InheritsInnerClasses){
                this.baseClass.inheritsChildClasses();
            }
            this.inheritsAllChildClasses(baseClass.getUniqueChildren());
            this.nextCompilingStage(CompilingStage.ValidateMembers);
        }

        @Override
        public void setCompilingStage(CompilingStage compilingStage) {
            super.setCompilingStage(compilingStage);
        }
    }

    public ParameterizedClassDef(ClassDef baseClass, ConstructorDef parameterizedConstructor, Literal<?>[] arguments) {
        super(baseClass.root, composeName(baseClass, arguments));
        Objects.requireNonNull(baseClass);
        Objects.requireNonNull(parameterizedConstructor);

        this.baseClass = baseClass;
        this.parameterizedConstructor = parameterizedConstructor;
        this.arguments = arguments;
        this.classType = baseClass.classType;

        this.setMetaClassDef(baseClass.getMetaClassDef());
        this.setSuperClass(baseClass.getSuperClass());
        this.setModifiers(baseClass.getModifiers());
        if(baseClass.isInterfaceOrTrait()) {
            this.setPermitClass(baseClass.getPermitClass());
        }
        this.setInterfaces(new ArrayList<>(baseClass.getInterfaces()));
        this.registerConcreteType(baseClass);

        this.setConstructor(baseClass.getConstructor());

        this.registerConcreteType(baseClass);
        for (Literal<?> argument : arguments) {
            if(argument instanceof ClassRefLiteral classRefLiteral){
                this.registerConcreteType(classRefLiteral.getClassDefValue());
            }
        }

        if(baseClass.getCompilingStage() == CompilingStage.ResolveHierarchicalClasses){
            this.compilingStage = CompilingStage.ResolveHierarchicalClasses;
        } else if(baseClass.getCompilingStage().getValue() >= CompilingStage.InheritsFields.getValue())
            this.compilingStage = CompilingStage.ParseFields;
        else
            this.compilingStage = CompilingStage.InheritsFields;
    }

    @Override
    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;
        if(this.baseClass.compilingStage == CompilingStage.ParseFields){
            if(!this.baseClass.parseFields()) return false;
        }
        this.nextCompilingStage(CompilingStage.ValidateHierarchy);
        return true;
    }

    @Override
    public void inheritsFields() throws CompilationError {
        if (this.compilingStage != CompilingStage.InheritsFields) return;

        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: inherits fields".formatted(this));
        if (baseClass != null) {
            if (baseClass.compilingStage == CompilingStage.InheritsFields) {
                baseClass.inheritsFields();
            }
            this.inheritsFields(baseClass.getFields(), baseClass);
            if (baseClass.isInterfaceOrTrait()) {
                this.setFieldForPermitClass(baseClass.getFieldForPermitClass());
            }
        }
        this.nextCompilingStage(CompilingStage.ValidateNewFunctions);
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;
        if(baseClass != null){
            if(baseClass.compilingStage == CompilingStage.ResolveHierarchicalClasses){
                baseClass.resolveHierarchicalClasses();
            }
            if (baseClass.isInterfaceOrTrait()) {
                this.setPermitClass(baseClass.getPermitClass());
                this.setFieldForPermitClass(baseClass.getFieldForPermitClass());
            }
            this.setInterfaces(new ArrayList<>(baseClass.getInterfaces()));
            this.setSuperClass(baseClass.getSuperClass());
            this.setInterfaces(new ArrayList<>(baseClass.getInterfaces()));
            this.setCompilingStage(CompilingStage.ParseFields);
        }
    }

    @Override
    public void inheritsChildClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.InheritsInnerClasses) return;

        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: inherit child classes".formatted(this));

        if(this.baseClass.compilingStage == CompilingStage.InheritsInnerClasses){
            this.baseClass.inheritsChildClasses();
        }
        this.inheritsAllChildClasses(baseClass.getUniqueChildren());
        this.nextCompilingStage(CompilingStage.ValidateMembers);
    }

    @Override
    public boolean isDeriveFrom(ClassDef maybeSuperClass) {
        return baseClass == maybeSuperClass || baseClass.isDeriveFrom(maybeSuperClass);
    }

    public ClassDef getBaseClass() {
        return baseClass;
    }

    public ConstructorDef getParameterizedConstructor(){
        return parameterizedConstructor;
    }

    public Literal<?>[] getArguments() {
        return arguments;
    }

    public static String composeName(ClassDef baseClass, Literal<?>[] arguments) {
        return baseClass.getName() + "::(" + Arrays.stream(arguments).map(Literal::getId).collect(Collectors.joining(",")) + ")";
    }

    @Override
    public List<ClassDef> getConcreteDependencyClasses() {
        if(this.baseClass instanceof ConcreteType c){
            return ListUtils.union(List.of(this.baseClass), c.getConcreteDependencyClasses());
        }
        return List.of(this.baseClass);
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments, Set<ClassDef> visited) {
        if(this.baseClass.isAffectedByTypeArguments(instantiationArguments, visited)) return true;
        for (var arg : this.arguments) {
            if(arg instanceof ClassRefLiteral typeArgument) {
                if (typeArgument.getClassDefValue().isAffectedByTypeArguments(instantiationArguments, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isGenericTerminated(Set<ClassDef> visited) {
        if(!this.baseClass.isGenericTerminated(visited)) return false;
        for (var arg : this.arguments) {
            if(arg instanceof ClassRefLiteral typeArgument) {
                if (!typeArgument.getClassDefValue().isGenericTerminated(visited)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, ClassContainer parent, MutableBoolean returnExisted) throws CompilationError {
        ParameterizedClassDef c = null;
        try {
            var n = baseClass.instantiateAsReferenceClass(instantiationArguments, returnExisted);
            c = ((ClassContainer)this.getParent()).getOrCreateParameterizedClass(n, constructor, mapArguments(instantiationArguments), returnExisted);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    protected Literal<?>[] mapArguments(InstantiationArguments instantiationArguments) throws CompilationError {
        Literal<?>[] args = new Literal[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            Literal<?> a = arguments[i];
            args[i] = a;
            if (a instanceof ClassRefLiteral classRefLiteral) {
                var a2 = classRefLiteral.getClassDefValue().instantiate(instantiationArguments, null);
                if (a2 != classRefLiteral.getClassDefValue()) {
                    args[i] = a2.toClassRefLiteral();
                }
            }
        }
        return args;
    }

    @Override
    public void acceptRegisterConcreteType(ClassDef hostClass) {
        for (Literal<?> argument : this.arguments) {
            if(argument instanceof ClassRefLiteral classRefLiteral){
               hostClass.idOfClass(classRefLiteral.getClassDefValue());
            } else if(argument instanceof StringLiteral stringLiteral){
                hostClass.idOfConstString(stringLiteral.getString());
            }
        }
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited, MutableInt depth) {
        if(anotherClass == this) return this;
        if(anotherClass instanceof ParameterizedClassDef anotherParameterizedClassDef){
            if(anotherParameterizedClassDef.baseClass == this.baseClass){
                return null;        // the arguments must be same, if same it matched (anotherClass == this)
            }
        }
        // my base class is super of another class means nothing, i.e. VarChar::(200) and NVarChar::(200)
        return super.asThatOrSuperOfThat(anotherClass, visited, depth);
    }
}
