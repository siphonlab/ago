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

import org.antlr.v4.runtime.tree.TerminalNode;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.exception.*;
import org.siphonlab.ago.compiler.expression.LiteralParser;
import org.siphonlab.ago.Variance;
import org.siphonlab.ago.compiler.generic.TypeParamsContext;
import org.siphonlab.ago.compiler.module.Project;
import org.siphonlab.ago.compiler.parser.AgoLexer;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.siphonlab.collection.DuplicatedKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;


public class Compiler {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClassDef.class);
    private final Project project;

    private final Root root;

    protected List<String> names = new ArrayList<>();
    protected Map<String, Integer> namesIndex = new HashMap<>();

    public Compiler(Project project) {
        this.project = project;
        this.root = project.getRoot();
    }

    public Unit[] compile() throws IOException, CompilationError, CompilationErrorsException {
        Unit[] units = project.getUnits().toArray(new Unit[0]);

        for (var unit : units) {
            unit.packageDecl();
        }

        for (var unit : units) {
            unit.importFixedClassNames();
        }

        // parse classes and member function declarations
        for (var unit : units) {
            try {
                unit.classNames();
            } catch (DuplicatedKeyException e) {
                unit.appendError(new DuplicatedError(e.getMessage(), SourceLocation.UNKNOWN));
            }
        }
        throwErrorsIfExists(units);

        root.getAndCleanNewFoundClasses();      // skip these new-found classes
        root.resolveLangClasses();

        for (var unit : units) {
            unit.solveRemainImports();
            if (!unit.getUnsolvedImports().isEmpty()) {
                for (Unit.UnsolvedImport unsolvedImport : unit.getUnsolvedImports()) {
                    unit.appendError(unit.resolveError(unsolvedImport.importDeclaration(), unsolvedImport.classFullName() + " not found"));
                }
            }
        }

        setupBoxTypes();

        root.setCompilingStage(CompilingStage.ParseGenericParams);
        root.sortClasses();
        parseGenericParams();
        processNewFoundClasses();

        // only set superclass attribute, doesn't inherit fields and methods
        root.setCompilingStage(CompilingStage.ResolveHierarchicalClasses);
        resolveHierarchicalClasses();
        processNewFoundClasses();

        // inherit fields and parse own fields declaration
        root.setCompilingStage(CompilingStage.ParseFields);
        parseFields();
        processNewFoundClasses();

        root.setCompilingStage(CompilingStage.ValidateHierarchy);
        validateHierarchy();
        throwErrorsIfExists(units);
        root.sortClasses();

        root.setCompilingStage(CompilingStage.InheritsFields);
        inheritsFields();
        processNewFoundClasses();

        root.setCompilingStage(CompilingStage.ValidateNewFunctions);
        validateFunctions();

        // inherits visible methods & classes from super class
        root.setCompilingStage(CompilingStage.InheritsInnerClasses);
        inheritsChildClasses();
        processNewFoundClasses();

        root.setCompilingStage(CompilingStage.ValidateMembers);
        validateMembers();

        root.setCompilingStage(CompilingStage.AllocateSlots);
        resolveParameterizedClassDefPlaceHolders();
        processNewFoundClasses();
        allocateSlots();

        // compile body code
        root.setCompilingStage(CompilingStage.CompileMethodBody);
        compileMethodBodies();
        processNewFoundClasses();

        for (Namespace<?> n : root.getAllDescendants().getUniqueElements()) {
            if (n instanceof ClassDef classDef) {
                if (classDef.getCompilingStage() != CompilingStage.Compiled) {
                    throw new IllegalStateException("'%s' not compiled".formatted(classDef));
                }
            }
        }
        root.setCompilingStage(CompilingStage.Compiled);

        throwErrorsIfExists(units);

        return units;
    }

    private static void throwErrorsIfExists(Unit[] units) throws CompilationErrorsException {
        List<Exception> errors = new ArrayList<>();
        for (Unit unit : units) {
            if(unit.hasErrors()){
                errors.addAll(unit.getErrors());
            }
        }
        if(!errors.isEmpty()){
            throw new CompilationErrorsException(errors);
        }
    }

    private void setupBoxTypes() {
        root.BYTE().setBoxedType(root.getByteClass());
        root.SHORT().setBoxedType(root.getShortClass());
        root.CHAR().setBoxedType(root.getCharClass());
        root.INT().setBoxedType(root.getIntegerClass());
        root.FLOAT().setBoxedType(root.getFloatClass());
        root.LONG().setBoxedType(root.getLongClass());
        root.DOUBLE().setBoxedType(root.getDoubleClass());
        root.DECIMAL().setBoxedType(root.getDecimalClass());
        root.STRING().setBoxedType(root.getStringClass());
        root.BOOLEAN().setBoxedType(root.getBooleanClass());
        root.CLASSREF().setBoxedType(root.getClassRefClass());
    }

    public static String parseStringLiteral(TerminalNode stringLiteral){
        return LiteralParser.parseJsStringLiteral(stringLiteral.getText());
    }

    void parseFields(){
        var q = new LinkedList<ClassDef>();
        boolean resort = false;
        for (var it = root.getSortedClassesAndFunctions().iterator(); it.hasNext(); ) {
            ClassDef classDef = it.next();
            try {
                if(!classDef.parseFields()){
                    q.add(classDef);
                }
            } catch (CompilationError e) {
                classDef.unit.appendError(e);
            }
        }
        while(!q.isEmpty()){
            var classDef = q.removeFirst();
            try {
                if (classDef.getCompilingStage().lt(CompilingStage.ParseFields))
                    processClassTillStage(classDef, CompilingStage.ParseFields);
                if (!classDef.parseFields()) q.add(classDef);
            } catch (CompilationError e) {
                classDef.unit.appendError(e);
            }
        }
        if(resort) root.sortClasses();
    }

    public void validateHierarchy() throws CompilationError {
        for (var it = root.getSortedClassesAndFunctions().iterator(); it.hasNext(); ) {
            ClassDef classDef = it.next();
            if (classDef.getCompilingStage() != CompilingStage.ValidateHierarchy)
                continue;

            if(classDef.unit != null) {
                try {
                    classDef.unit.validateHierarchy(classDef);
                } catch (CompilationError e) {
                    classDef.unit.appendError(e);
                }
            } else {
                classDef.nextCompilingStage(CompilingStage.InheritsFields);     // i.e. lang.ScopedClassInterval::Clang$Function2<int|int|int>|Clang$Any
            }
        }
    }

    public void resolveHierarchicalClasses() throws CompilationError {
        for (var it = root.getSortedClassesAndFunctions().iterator(); it.hasNext(); ) {
            ClassDef classDef = it.next();
            classDef.resolveHierarchicalClasses();
        }
    }

    public void parseGenericParams() throws CompilationError {
        for (var it = root.getSortedClassesAndFunctions().iterator(); it.hasNext(); ) {
            ClassDef scopeClass = it.next();
            if (scopeClass.getCompilingStage() != CompilingStage.ParseGenericParams)
                continue;

            if (scopeClass.parent instanceof ClassDef p) {
                assert p.getCompilingStage() != CompilingStage.ParseGenericParams;      // already finish this stage
            }

            AgoParser.GenericTypeParametersContext genericTypeParameters = null;
            if (scopeClass instanceof MetaClassDef) {
//                throw new TypeMismatchError("metaclass cannot be generic template", scopeClass.getUnit().sourceLocation());
                // metaclass can be involved by its class in generic, but has no generic type param itself
            } else {
                genericTypeParameters = scopeClass.getGenericTypeParametersContextAST();
            }
            if (genericTypeParameters != null) {
                var templClass = scopeClass;
                templClass.shiftToTemplate();
                TypeParamsContext templClassTypeParamsContext = templClass.getTypeParamsContext();

                List<AgoParser.GenericTypeParameterContext> genericTypeParameter = genericTypeParameters.genericTypeParameter();
                for (int i = 0; i < genericTypeParameter.size(); i++) {
                    var genericTypeParameterContext = genericTypeParameter.get(i);
                    var identifier = genericTypeParameterContext.identifier();
                    var name = identifier.getText();
                    if (templClass.findGenericType(name) != null) {
                        throw scopeClass.unit.resolveError(identifier, "duplicated generic param id '%s'".formatted(name));
                    }
                    var variance = Variance.Invariance;
                    if (genericTypeParameterContext.ADD() != null) {
                        variance = Variance.Covariance;
                    } else if (genericTypeParameterContext.SUB() != null) {
                        variance = Variance.Contravariance;
                    }

                    var typeOfGenericParam = genericTypeParameterContext.typeOfGenericParam();
                    ClassDef[] bound;
                    if (typeOfGenericParam != null) {
                        bound = scopeClass.unit.parseTypeRange(typeOfGenericParam.typeRange(), templClass);
                    } else {
                        bound = new ClassDef[]{root.getAnyClass(), root.getAnyClass()};
                    }

                    var gt = root.getGenericTypeParameter();
                    var pc = ((ClassContainer) gt.getParent()).getOrCreateGenericTypeParameter(this.project, gt, gt.getMetaClassDef().getConstructor(), bound[0], bound[1], variance, null);
                    templClass.registerConcreteType((ConcreteType) pc);
                    templClass.getTypeParamsContext().createGenericTypeParam(null, name, pc, i);
                    if (pc.getUnit() == null) {
                        pc.setUnit(templClass.getUnit());
                        pc.setSourceLocation(templClass.getUnit().sourceLocation(typeOfGenericParam));
                    }
                }
                templClass.createTemplateDefaultGenericSource();
            }
            scopeClass.nextCompilingStage(CompilingStage.ResolveHierarchicalClasses);    // to ExpandHierarchicalClasses
        }
    }

    public Root getRoot() {
        return root;
    }

    void inheritsFields() throws CompilationError {
        for (ClassDef classDef : root.getSortedClassesAndFunctions()) {
            try {
                classDef.inheritsFields();
            } catch (CompilationError e) {
                if(classDef.unit != null) {
                    classDef.unit.appendError(e);
                    classDef.nextCompilingStage(CompilingStage.ValidateNewFunctions);
                } else {
                    throw e;
                }
            }
        }
    }

    private void resolveParameterizedClassDefPlaceHolders() throws CompilationError {
        for (ParameterizedClassDef.PlaceHolder parameterizedClassDefPlaceHolder : root.getParameterizedClassDefPlaceHolders()) {
            parameterizedClassDefPlaceHolder.resolve();
        }
        root.getParameterizedClassDefPlaceHolders().clear();
    }

    void allocateSlots() throws CompilationError {
        for (ClassDef classDef : root.getSortedClassesAndFunctions()) {
            classDef.allocateSlotsForFields();
        }
    }

    void validateFunctions() throws CompilationError {
        for (ClassDef classDef : root.getSortedClassesAndFunctions()) {
            if(classDef.getCompilingStage() != CompilingStage.ValidateNewFunctions) continue;
            try {
                validateFunction(classDef);
            } catch (CompilationError e) {
                if(classDef.unit != null) {
                    classDef.unit.appendError(e);
                    classDef.nextCompilingStage(CompilingStage.InheritsInnerClasses);
                } else {
                    throw e;
                }
            }
        }
    }

    static void validateFunction(ClassDef classDef) throws CompilationError {
        if(classDef.getCompilingStage() == CompilingStage.ValidateNewFunctions) {
            classDef.nextCompilingStage(CompilingStage.InheritsInnerClasses);
            for (ClassDef child : classDef.getDirectChildren()) {
                if (child instanceof FunctionDef functionDef) {
                    classDef.validateNewFunction(functionDef);
                }
            }
        }
    }

    void inheritsChildClasses() throws CompilationError {
        for (ClassDef classDef : root.getSortedClassesAndFunctions()) {
            if (classDef.compilingStage == CompilingStage.InheritsInnerClasses) {
                try {
                    classDef.inheritsChildClasses();
                } catch (CompilationError e){
                    if(classDef.unit != null) {
                        classDef.unit.appendError(e);
                        classDef.nextCompilingStage(CompilingStage.ValidateMembers);
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    void validateMembers() throws SyntaxError, ResolveError {
        for (ClassDef classDef : root.getSortedClassesAndFunctions()) {
            if(classDef.compilingStage == CompilingStage.ValidateMembers) {
                try {
                    validateMembers(classDef);
                } catch (CompilationError e){
                    if(classDef.unit != null) {
                        classDef.unit.appendError(e);
                        classDef.nextCompilingStage(CompilingStage.Compiled);
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    private static void validateMembers(ClassDef classDef) throws SyntaxError, ResolveError {
        classDef.verifyMembers();

        if(!classDef.isAbstract()) {
            var superClass = classDef.getSuperClass();
            if (superClass != null && superClass != classDef && superClass.isAbstract()) {
                classDef.verifyFunctionsImplemented(superClass, classDef.getBaseTypeDecl());
            }

            var interfaceDecls = classDef.getInterfaceDecls();
            if(interfaceDecls != null) {
                for (int i = 0; i < interfaceDecls.size(); i++) {
                    var interfaceDef = classDef.getInterfaces().get(i);
                    classDef.verifyFunctionsImplemented(interfaceDef, interfaceDecls.get(i));
                }
            }
        }
        classDef.setCompilingStage(CompilingStage.AllocateSlots);
    }

    void compileMethodBodies() throws CompilationError {
        for (ClassDef classDef : root.getSortedClassesAndFunctions()) {
            try {
                classDef.compileBody();
            } catch (Exception e) {
                if(classDef.unit != null) {
                    classDef.unit.appendError(e);
                    classDef.setCompilingStage(CompilingStage.Compiled);
                } else {
                    throw e;
                }
            }
        }
    }

    void processNewFoundClasses() throws CompilationError {
        var stage = root.getCompilingStage();
        boolean found = false;
        for(var newFoundClasses = root.getAndCleanNewFoundClasses(); !newFoundClasses.isEmpty(); newFoundClasses = root.getAndCleanNewFoundClasses()) {
            while (!newFoundClasses.isEmpty()) {
                found = true;
                ClassDef classDef = newFoundClasses.removeFirst();
                if(classDef.getCompilingStage().getValue() > stage.getValue()) continue;
                processClassTillStage(classDef, stage);
            }
        }
        if(found){
            root.sortClasses();
        }
    }

    public static void processClassTillStage(ClassDef classDef, CompilingStage stage) throws CompilationError {
        if(LOGGER.isDebugEnabled()) LOGGER.debug("process new found class '%s', compile to stage %s".formatted(classDef, stage));
        if(classDef == null) return;
        while(classDef.getCompilingStage().getValue() <= stage.getValue()){
            switch (classDef.getCompilingStage()){
                case ResolveHierarchicalClasses:
                    classDef.resolveHierarchicalClasses();
                    break;
                case ValidateHierarchy:
                    Unit unit = classDef.getUnit();
                    if(unit != null) {
                        unit.validateHierarchy(classDef);
                    } else {
                        classDef.nextCompilingStage(CompilingStage.InheritsFields);
                    }
                    break;
                case ParseFields:
                    try {
                        classDef.parseFields();
                    } catch (CompilationError e) {
                        classDef.unit.appendError(e);
                        classDef.nextCompilingStage(CompilingStage.InheritsFields);
                    }
                    break;
                case InheritsFields:
                    try {
                        classDef.inheritsFields();
                    } catch (CompilationError e) {
                        classDef.unit.appendError(e);
                        classDef.nextCompilingStage(CompilingStage.ValidateNewFunctions);
                    }
                    break;
                case ValidateNewFunctions:
                    try {
                        validateFunction(classDef);
                    } catch (CompilationError e) {
                        classDef.unit.appendError(e);
                        classDef.nextCompilingStage(CompilingStage.InheritsInnerClasses);
                    }
                    break;
                case InheritsInnerClasses:
                    try {
                        classDef.inheritsChildClasses();
                    } catch (CompilationError e) {
                        classDef.unit.appendError(e);
                        classDef.nextCompilingStage(CompilingStage.ValidateMembers);
                    }
                    break;
                case ValidateMembers:
                    try {
                        validateMembers(classDef);
                    } catch (CompilationError e) {
                        classDef.unit.appendError(e);
                        classDef.setCompilingStage(CompilingStage.Compiled);
                    }
                    break;
                case AllocateSlots:
                    try {
                        classDef.allocateSlotsForFields();
                    } catch (CompilationError e) {
                        classDef.unit.appendError(e);
                        classDef.setCompilingStage(CompilingStage.Compiled);
                    }
                    break;
                case CompileMethodBody:
                    try {
                        classDef.compileBody();
                    } catch (CompilationError e){
                        classDef.unit.appendError(e);
                        classDef.setCompilingStage(CompilingStage.Compiled);
                    }
                    return;
                case Compiled:
                    return;
            }
        }
    }

    public Collection<ClassDef> load(AgoClassLoader classLoader) throws CompilationError, IOException {      // TODO load class loader will lose the original module of AgoClass
        Root root = this.getRoot();
        root.setProject(null);
        var r = new AgoClassParser(classLoader, this, root).load();
        root.setProject(this.project);
        return r;
    }

    enum ModifierTarget {
        Variable,
        Field,
        Param,
        Class,
        Method,
        Constructor
    }

    static int variableModifiers(Unit unit, AgoParser.VariableModifiersContext variableModifier, ModifierTarget target) throws SyntaxError, UnsupportedExpressionError {
        List<AgoParser.VariableModifierContext> modifiers = variableModifier.variableModifier();
        return variableModifiers(unit, modifiers, target);
    }

    static int variableModifiers(Unit unit, List<AgoParser.VariableModifierContext> modifiers, ModifierTarget target) throws SyntaxError, UnsupportedExpressionError {
        int result = 0;
        if(modifiers != null){
            for (AgoParser.VariableModifierContext modifier : modifiers) {
                if(modifier.FINAL() != null){
                    if((result & AgoClass.FINAL) == AgoClass.FINAL) unit.appendError(unit.syntaxError( modifier,"'final' duplicated"));
                    result |= AgoClass.FINAL;
                } else if(modifier.FIELD() != null){
                    if(target != ModifierTarget.Param)
                        unit.appendError(unit.syntaxError(modifier, "'field' can only apply on parameter"));
                    if((result & AgoClass.FIELD_PARAM) == AgoClass.FIELD_PARAM) unit.appendError(unit.syntaxError( modifier,"'field' duplicated"));
                    result |= AgoClass.FIELD_PARAM;
                } else if(modifier.CHAN() != null){
                    throw new UnsupportedExpressionError("chan TODO", unit.sourceLocation(modifier.CHAN()));
                } else if(modifier.THIS() != null){
                    if(target != ModifierTarget.Param)
                        unit.appendError(unit.syntaxError(modifier, "'this' can only apply on parameter"));
                    if((result & AgoClass.THIS_PARAM) == AgoClass.THIS_PARAM) throw unit.syntaxError( modifier,"'this' duplicated");
                    result |= AgoClass.THIS_PARAM;
                } else {
                    unit.appendError(unit.syntaxError(modifier, "unexpected token '%s'".formatted(modifier.getText())));
                }
            }
        }
        return result;
    }

    static int fieldModifiers(Unit unit, List<AgoParser.FieldModifierContext> modifiers, ModifierTarget target) throws SyntaxError {
        int result = 0;
        boolean visibilityFound = false;
        if(modifiers != null){
            for (var modifier : modifiers) {
                if(modifier.FINAL() != null) {
                    if ((result & AgoClass.FINAL) == AgoClass.FINAL)
                        unit.appendError(unit.syntaxError(modifier, "'final' duplicated"));
                    result |= AgoClass.FINAL;
//                } else if(modifier.STATIC() != null) {
//                    if (target == ModifierTarget.Param)
//                        throw unit.syntaxError(modifier, "'static' cannot apply on parameter");
//                    if (target == ModifierTarget.Variable)
//                        throw unit.syntaxError(modifier, "'static' cannot apply on variable");
//                    if ((result & AgoClass.STATIC) == AgoClass.STATIC)
//                        throw unit.syntaxError(modifier, "'static' duplicated");
//                    result |= AgoClass.STATIC;
//                } else if(modifier.CHAN() != null){
//                    throw new UnsupportedOperationException("chan TODO");
                } else if(modifier.commonVisiblility() != null){
                    if(visibilityFound){
                        unit.appendError(unit.syntaxError(modifier, "visibility duplicated"));
                    }
                    result |= commonVisibility(unit, modifier.commonVisiblility(), target);
                    visibilityFound = true;
                } else {
                    unit.appendError(unit.syntaxError(modifier, "unexpected token '%s'".formatted(modifier.getText())));
                }
            }
        }
        if(!visibilityFound){
            result |= commonVisibility(unit, null, target);
        }
        return result;
    }

    static int methodModifier(Unit unit, AgoParser.MethodStarterContext methodStarter) throws SyntaxError {
        int result = 0;
        boolean visibilityFound = false;
        if(methodStarter.OVERRIDE() != null) {
            result = fieldModifiers(unit, methodStarter.fieldModifier(), ModifierTarget.Method);
            result |= AgoClass.OVERRIDE;
        } else {
            if(methodStarter.GENERATOR() != null) {
                result |= AgoClass.GENERATOR;
            }
            for (AgoParser.MethodModifierContext modifier : methodStarter.methodModifier()) {
                if(modifier.FINAL() != null){
                    if((result & AgoClass.FINAL) == AgoClass.FINAL) unit.appendError(unit.syntaxError( modifier,"'final' duplicated"));
                    result |= AgoClass.FINAL;
//                } else if(modifier.STATIC() != null) {
//                    if ((result & AgoClass.STATIC) == AgoClass.STATIC) throw unit.syntaxError(modifier, "'static' duplicated");
//                    result |= AgoClass.STATIC;
                } else if(modifier.commonVisiblility() != null){
                    if(visibilityFound){
                        unit.appendError(unit.syntaxError(modifier, "visibility duplicated"));
                    }
                    result |= commonVisibility(unit, modifier.commonVisiblility(), ModifierTarget.Method);
                    visibilityFound = true;
                } else if(modifier.ABSTRACT() != null){
                    if ((result & AgoClass.ABSTRACT) == AgoClass.ABSTRACT) unit.appendError(unit.syntaxError(modifier, "'abstract' duplicated"));
                    result |= AgoClass.ABSTRACT;
                } else if(modifier.OVERRIDE() != null){
                    if ((result & AgoClass.OVERRIDE) == AgoClass.OVERRIDE) unit.appendError(unit.syntaxError(modifier, "'override' duplicated"));
                    result |= AgoClass.OVERRIDE;
                } else if(modifier.GENERATOR() != null){
                    if ((result & AgoClass.GENERATOR) == AgoClass.GENERATOR) unit.appendError(unit.syntaxError(modifier, "'generator' duplicated"));
                    result |= AgoClass.GENERATOR;
                } else {
                    unit.appendError(unit.syntaxError(modifier, "unexpected token '%s'".formatted(modifier.getText())));
                }
            }
        }
        if(methodStarter.GETTER() != null){
            if ((result & AgoClass.GETTER) == AgoClass.GETTER) unit.appendError(unit.syntaxError(methodStarter, "'get' duplicated"));
            result |= AgoClass.GETTER;
        }
        if(methodStarter.SETTER() != null){
            if ((result & AgoClass.SETTER) == AgoClass.SETTER) unit.appendError(unit.syntaxError(methodStarter, "'set' duplicated"));
            result |= AgoClass.SETTER;
        }
        if(!visibilityFound){
            result |= commonVisibility(unit, null, ModifierTarget.Method);
        }
        return result;
    }

    static int constructorModifier(Unit unit, AgoParser.MethodStarterContext methodStarter) throws SyntaxError {
        int result = methodModifier(unit, methodStarter);
        if((result & AgoClass.FINAL) == AgoClass.FINAL){
            unit.appendError(unit.syntaxError(methodStarter, "constructor cannot be final"));
        }
        if((result & AgoClass.OVERRIDE) == AgoClass.OVERRIDE){
            unit.appendError(unit.syntaxError(methodStarter, "constructor needn't mark as 'override'"));
        }
        if((result & AgoClass.GETTER) == AgoClass.GETTER){
            unit.appendError(unit.syntaxError(methodStarter, "constructor needn't mark as 'override'"));
        }
        if((result & AgoClass.SETTER) == AgoClass.SETTER){
            unit.appendError(unit.syntaxError(methodStarter, "constructor needn't mark as 'override'"));
        }
        return result;
    }

    static int queryModifiers(Unit unit, List<AgoParser.QueryModifierContext> queryModifiers) throws SyntaxError {
        int result = 0;
        boolean visibilityFound = false;
        for (AgoParser.QueryModifierContext modifier : queryModifiers) {
            if(modifier.FINAL() != null){
                if((result & AgoClass.FINAL) == AgoClass.FINAL) unit.appendError(unit.syntaxError( modifier,"'final' duplicated"));
                result |= AgoClass.FINAL;
            } else if(modifier.commonVisiblility() != null){
                if(visibilityFound){
                    unit.appendError(unit.syntaxError(modifier, "visibility duplicated"));
                }
                result |= commonVisibility(unit, modifier.commonVisiblility(), ModifierTarget.Method);
                visibilityFound = true;
            } else if(modifier.OVERRIDE() != null){
                if ((result & AgoClass.OVERRIDE) == AgoClass.OVERRIDE) unit.appendError(unit.syntaxError(modifier, "'override' duplicated"));
                result |= AgoClass.OVERRIDE;
            } else {
                unit.appendError(unit.syntaxError(modifier, "unexpected token '%s'".formatted(modifier.getText())));
            }
            if(!visibilityFound){
                result |= commonVisibility(unit, null, ModifierTarget.Method);
            }
        }
        return result;
    }

    static int commonVisibility(Unit unit, AgoParser.CommonVisiblilityContext commonVisibilility, ModifierTarget target) throws SyntaxError{
        if(commonVisibilility == null) return switch (target){
            case Field -> AgoClass.PRIVATE;
            case Variable -> AgoClass.PRIVATE;
            case Param -> AgoClass.PRIVATE;
            case Class -> AgoClass.PUBLIC;
            case Method -> AgoClass.PUBLIC;
            case Constructor -> AgoClass.PUBLIC;
        };
        int result = 0;
        if(commonVisibilility.PUBLIC() != null){
              if((result & AgoClass.VISIBILITY_MASK) != 0) unit.appendError(unit.syntaxError(commonVisibilility,"visibility duplicated"));
              result |= AgoClass.PUBLIC;
        }
        if(commonVisibilility.PROTECTED() != null){
            if((result & AgoClass.VISIBILITY_MASK) != 0) unit.appendError(unit.syntaxError(commonVisibilility,"visibility duplicated"));
            result |= AgoClass.PROTECTED;
        }
        if(commonVisibilility.PRIVATE() != null){
            if((result & AgoClass.VISIBILITY_MASK) != 0) unit.appendError(unit.syntaxError(commonVisibilility,"visibility duplicated"));
            result |= AgoClass.PRIVATE;
        }
        return result;
    }

    static int classModifiers(Unit unit, List<AgoParser.ClassModifierContext> modifiers) throws SyntaxError {
        int result = 0;
        boolean visibilityFound = false;
        for (var modifier : modifiers) {
            if (modifier.FINAL() != null) {
                if ((result & AgoClass.FINAL) == AgoClass.FINAL) unit.appendError(unit.syntaxError(modifier, "'final' duplicated"));
                result |= AgoClass.FINAL;
//            } else if (modifier.STATIC() != null) {
//                if ((result & AgoClass.STATIC) == AgoClass.STATIC)
//                    throw unit.syntaxError(modifier, "'static' duplicated");
//                result |= AgoClass.STATIC;
            } else if (modifier.commonVisiblility() != null) {
                if (visibilityFound) {
                    unit.appendError(unit.syntaxError(modifier, "visibility duplicated"));
                }
                result |= commonVisibility(unit, modifier.commonVisiblility(), ModifierTarget.Class);
                visibilityFound = true;
            } else if (modifier.ABSTRACT() != null) {
                if ((result & AgoClass.ABSTRACT) == AgoClass.ABSTRACT)
                    unit.appendError(unit.syntaxError(modifier, "'abstract' duplicated"));
                result |= AgoClass.ABSTRACT;

            } else if(modifier.NATIVE() != null){
                if ((result & AgoClass.NATIVE) == AgoClass.NATIVE) unit.appendError(unit.syntaxError(modifier, "'native' duplicated"));
                result |= AgoClass.NATIVE;
            }
            if (!visibilityFound) {
                result |= commonVisibility(unit, null, ModifierTarget.Class);
            }
        }
        return result;
    }

    static int interfaceModifiers(Unit unit, List<AgoParser.InterfaceModifierContext> modifiers) throws SyntaxError {
        int result = AgoClass.ABSTRACT;
        boolean visibilityFound = false;
        for (var modifier : modifiers) {
            if (modifier.FINAL() != null) {
                if ((result & AgoClass.FINAL) == AgoClass.FINAL) unit.appendError(unit.syntaxError(modifier, "'final' duplicated"));
                result |= AgoClass.FINAL;
            } else if (modifier.commonVisiblility() != null) {
                if (visibilityFound) {
                    unit.appendError(unit.syntaxError(modifier, "visibility duplicated"));
                }
                result |= commonVisibility(unit, modifier.commonVisiblility(), ModifierTarget.Class);
                visibilityFound = true;
            }
            if (!visibilityFound) {
                result |= commonVisibility(unit, null, ModifierTarget.Class);
            }
        }
        return result;
    }

    public static TypeCode typeCodeFromPrimitiveTypeAst(AgoParser.PrimitiveTypeContext primitiveType){
        var type = switch (primitiveType.start.getType()) {
            case AgoLexer.BOOLEAN -> TypeCode.BOOLEAN;
            case AgoLexer.CHAR -> TypeCode.CHAR;
            case AgoLexer.SHORT -> TypeCode.SHORT;
            case AgoLexer.INT -> TypeCode.INT;
            case AgoLexer.LONG -> TypeCode.LONG;
            case AgoLexer.DOUBLE -> TypeCode.DOUBLE;
            case AgoLexer.DECIMAL -> TypeCode.DECIMAL;
            case AgoLexer.FLOAT -> TypeCode.FLOAT;
            case AgoLexer.STRING -> TypeCode.STRING;
            case AgoLexer.BYTE -> TypeCode.BYTE;
            case AgoLexer.VOID -> TypeCode.VOID;
            case AgoLexer.CLASSREF -> TypeCode.CLASS_REF;
            case AgoLexer.NULL_LITERAL -> TypeCode.NULL;
            default -> throw new IllegalStateException("not supported type " + primitiveType.getText());
        };
        return type;
    }

    public static PrimitiveClassDef fromPrimitiveTypeAst(Root root, AgoParser.PrimitiveTypeContext primitiveType) {
        return root.fromPrimitiveTypeCode(typeCodeFromPrimitiveTypeAst(primitiveType));
    }

}
