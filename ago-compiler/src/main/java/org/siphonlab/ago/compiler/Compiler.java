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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.LiteralParser;
import org.siphonlab.ago.compiler.expression.literal.ByteLiteral;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.generic.SharedGenericTypeParameterClassDef;
import org.siphonlab.ago.Variance;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Compiler {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClassDef.class);

    private Root root = new Root();

    protected List<String> names = new ArrayList<>();
    protected Map<String, Integer> namesIndex = new HashMap<>();

    public Unit[] compile(File[] files) throws IOException, CompilationError {
        return compile(files, null);
    }

    public Unit[] compile(UnitSource[] unitSources, ClassDef[] rtClasses) throws IOException, CompilationError{
        Unit[] units = new Unit[unitSources.length];
        for (int i = 0; i < unitSources.length; i++) {
            UnitSource unitSource = unitSources[i];
            var unit = new Unit(unitSource.getFileName(), CharStreams.fromReader(unitSource.getReader()), root);
            if (rtClasses != null) {
                for (ClassDef rtClass : rtClasses) {
                    unit.importClass(rtClass);
                }
            }
            units[i] = unit;
        }

        for (var unit : units) {
            unit.packageDecl();
        }

        for (var unit : units) {
            unit.importFixedClassNames();
        }

        // parse classes and member function declarations
        for (var unit : units) {
            unit.classNames();
        }
        root.getAndCleanNewFoundClasses();      // skip these new-found classes

        for (var unit : units) {
            unit.solveRemainImports();
            if (!unit.getUnsolvedImports().isEmpty()) {
                for (Unit.UnsolvedImport unsolvedImport : unit.getUnsolvedImports()) {
                    throw unit.resolveError(unsolvedImport.importDeclaration(), unsolvedImport.classFullName() + " not found");
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
                    throw new RuntimeException("'%s' not compiled".formatted(classDef));
                }
            }
        }
        root.setCompilingStage(CompilingStage.Compiled);

        return units;
    }

    public Unit[] compile(File[] files, ClassDef[] rtClasses) throws IOException, CompilationError {
        UnitSource[] unitSources = new UnitSource[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            unitSources[i] = new UnitSource(file.getPath(), new FileReader(file, StandardCharsets.UTF_8));
        }
        return compile(unitSources, rtClasses);
    }

    private void setupBoxTypes() {
        PrimitiveClassDef.BYTE.setBoxedType(root.getByteClass());
        PrimitiveClassDef.SHORT.setBoxedType(root.getShortClass());
        PrimitiveClassDef.CHAR.setBoxedType(root.getCharClass());
        PrimitiveClassDef.INT.setBoxedType(root.getIntegerClass());
        PrimitiveClassDef.FLOAT.setBoxedType(root.getFloatClass());
        PrimitiveClassDef.LONG.setBoxedType(root.getLongClass());
        PrimitiveClassDef.DOUBLE.setBoxedType(root.getDoubleClass());
        PrimitiveClassDef.STRING.setBoxedType(root.getStringClass());
        PrimitiveClassDef.BOOLEAN.setBoxedType(root.getBooleanClass());
        PrimitiveClassDef.CLASS_REF.setBoxedType(root.getClassRefClass());

        PrimitiveClassDef.BYTE.setRoot(root);
        PrimitiveClassDef.SHORT.setRoot(root);
        PrimitiveClassDef.CHAR.setRoot(root);
        PrimitiveClassDef.INT.setRoot(root);
        PrimitiveClassDef.FLOAT.setRoot(root);
        PrimitiveClassDef.LONG.setRoot(root);
        PrimitiveClassDef.DOUBLE.setRoot(root);
        PrimitiveClassDef.STRING.setRoot(root);
        PrimitiveClassDef.BOOLEAN.setRoot(root);
        PrimitiveClassDef.CLASS_REF.setRoot(root);

        PrimitiveClassDef.BYTE.setPrimitiveInterface(root.getPrimitiveNumberTypeInterface());
        PrimitiveClassDef.SHORT.setPrimitiveInterface(root.getPrimitiveNumberTypeInterface());
        PrimitiveClassDef.INT.setPrimitiveInterface(root.getPrimitiveNumberTypeInterface());
        PrimitiveClassDef.FLOAT.setPrimitiveInterface(root.getPrimitiveNumberTypeInterface());
        PrimitiveClassDef.LONG.setPrimitiveInterface(root.getPrimitiveNumberTypeInterface());
        PrimitiveClassDef.DOUBLE.setPrimitiveInterface(root.getPrimitiveNumberTypeInterface());

        PrimitiveClassDef.CHAR.setPrimitiveInterface(root.getPrimitiveTypeInterface());
        PrimitiveClassDef.STRING.setPrimitiveInterface(root.getPrimitiveTypeInterface());
        PrimitiveClassDef.BOOLEAN.setPrimitiveInterface(root.getPrimitiveTypeInterface());
        PrimitiveClassDef.CLASS_REF.setPrimitiveInterface(root.getPrimitiveTypeInterface());
    }

    public static String parseStringLiteral(TerminalNode stringLiteral){
        return LiteralParser.parseJsStringLiteral(stringLiteral.getText());
    }

    void parseFields() throws CompilationError {
        var q = new LinkedList<ClassDef>();
        boolean resort = false;
        for (var it = root.getSortedClassesAndFunctions().iterator(); it.hasNext(); ) {
            ClassDef classDef = it.next();
            if(!classDef.parseFields()){
                q.add(classDef);
            }
        }
        while(!q.isEmpty()){
            var classDef = q.removeFirst();
            if(classDef.getCompilingStage().lt(CompilingStage.ParseFields))
                processClassTillStage(classDef, CompilingStage.ParseFields);
            if(!classDef.parseFields()) q.add(classDef);
        }
        if(resort) root.sortClasses();
    }

    public void validateHierarchy() throws CompilationError {
        for (var it = root.getSortedClassesAndFunctions().iterator(); it.hasNext(); ) {
            ClassDef classDef = it.next();
            if (classDef.getCompilingStage() != CompilingStage.ValidateHierarchy)
                continue;

            if(classDef.unit != null)
                classDef.unit.validateHierarchy(classDef);
            else
                classDef.nextCompilingStage(CompilingStage.InheritsFields);     // i.e. lang.ScopedClassInterval::Clang$Function2<int|int|int>|Clang$Any
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
                System.out.println(1);
                // metaclass can be involved by its class in generic, but has no generic type param itself
            } else {
                genericTypeParameters = scopeClass.getGenericTypeParametersContextAST();
            }
            if (genericTypeParameters != null) {
                var templClass = scopeClass;
                templClass.shiftToTemplate();

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
                    Literal<?>[] args;
                    if (typeOfGenericParam != null) {
                        args = scopeClass.unit.parseTypeRange(typeOfGenericParam.typeRange(), templClass);
                        args = ArrayUtils.add(args, new ByteLiteral(variance.byteValue()));
                    } else {
                        args = new Literal<?>[]{new ClassRefLiteral(root.getAnyClass()), new ClassRefLiteral(root.getAnyClass()), new ByteLiteral(Variance.Invariance.byteValue())};
                    }
                    var gt = root.getGenericTypeParameter();
                    SharedGenericTypeParameterClassDef pc = ((ClassContainer) gt.getParent()).getOrCreateGenericTypeParameter(gt, gt.getMetaClassDef().getConstructor(), args, null);
                    templClass.getTypeParamsContext().addGenericTypeParam(name, pc, genericTypeParameterContext);
                    if (pc.getUnit() == null)
                        pc.setUnit(templClass.getUnit());
                }
                //templClass.makePlaceHolderGenericSource();
            }
            scopeClass.nextCompilingStage(CompilingStage.ResolveHierarchicalClasses);    // to ExpandHierarchicalClasses
        }
    }


    void inheritsFields() throws CompilationError {
        for (ClassDef classDef : root.getSortedClassesAndFunctions()) {
            classDef.inheritsFields();
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
            validateFunction(classDef);
        }
    }

    private static void validateFunction(ClassDef classDef) throws CompilationError {
        for (ClassDef child : classDef.getDirectChildren()) {
            if (child instanceof FunctionDef functionDef) {
                classDef.validateNewFunction(functionDef);
            }
        }
        classDef.nextCompilingStage(CompilingStage.InheritsInnerClasses);
    }

    void inheritsChildClasses() throws CompilationError {
        for (ClassDef classDef : root.getSortedClassesAndFunctions()) {
            if (classDef.compilingStage == CompilingStage.InheritsInnerClasses) {
                classDef.inheritsChildClasses();
            }
        }
    }

    void validateMembers() throws SyntaxError, ResolveError {
        for (ClassDef classDef : root.getSortedClassesAndFunctions()) {
            if(classDef.compilingStage == CompilingStage.ValidateMembers) {
                validateMembers(classDef);
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
            classDef.compileBody();
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
                    classDef.parseFields();
                    break;
                case InheritsFields:
                    classDef.inheritsFields();
                    break;
                case ValidateNewFunctions:
                    validateFunction(classDef);
                    break;
                case InheritsInnerClasses:
                    classDef.inheritsChildClasses();
                    break;
                case ValidateMembers:
                    validateMembers(classDef);
                    break;
                case AllocateSlots:
                    classDef.allocateSlotsForFields();
                    break;
                case CompileMethodBody:
                    classDef.compileBody();
                    return;
                case Compiled:
                    return;
            }
        }
    }

    public Collection<ClassDef> load(AgoClassLoader classLoader) throws CompilationError {
        return new AgoClassParser(classLoader, this, this.root).load();
    }

    enum ModifierTarget {
        Variable,
        Field,
        Param,
        Class,
        Method,
        Constructor
    }

    static int variableModifiers(Unit unit, AgoParser.VariableModifiersContext variableModifier, ModifierTarget target) throws SyntaxError {
        List<AgoParser.VariableModifierContext> modifiers = variableModifier.variableModifier();
        return variableModifiers(unit, modifiers, target);
    }

    static int variableModifiers(Unit unit, List<AgoParser.VariableModifierContext> modifiers, ModifierTarget target) throws SyntaxError {
        int result = 0;
        if(modifiers != null){
            for (AgoParser.VariableModifierContext modifier : modifiers) {
                if(modifier.FINAL() != null){
                    if((result & AgoClass.FINAL) == AgoClass.FINAL) throw unit.syntaxError( modifier,"'final' duplicated");
                    result |= AgoClass.FINAL;
                } else if(modifier.FIELD() != null){
                    if(target != ModifierTarget.Param)
                        throw unit.syntaxError(modifier, "'field' can only apply on parameter");
                    if((result & AgoClass.FIELD_PARAM) == AgoClass.FIELD_PARAM) throw unit.syntaxError( modifier,"'field' duplicated");
                    result |= AgoClass.FIELD_PARAM;
                } else if(modifier.CHAN() != null){
                    throw new UnsupportedOperationException("chan TODO");
                } else if(modifier.THIS() != null){
                    if(target != ModifierTarget.Param)
                        throw unit.syntaxError(modifier, "'this' can only apply on parameter");
                    if((result & AgoClass.THIS_PARAM) == AgoClass.THIS_PARAM) throw unit.syntaxError( modifier,"'this' duplicated");
                    result |= AgoClass.THIS_PARAM;
                } else {
                    throw unit.syntaxError(modifier, "unexpected token '%s'".formatted(modifier.getText()));
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
                        throw unit.syntaxError(modifier, "'final' duplicated");
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
                        throw unit.syntaxError(modifier, "visibility duplicated");
                    }
                    result |= commonVisibility(unit, modifier.commonVisiblility(), target);
                    visibilityFound = true;
                } else {
                    throw unit.syntaxError(modifier, "unexpected token '%s'".formatted(modifier.getText()));
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
        if(methodStarter.OVERRIDE() != null){
            result = fieldModifiers(unit, methodStarter.fieldModifier(), ModifierTarget.Method);
            result |= AgoClass.OVERRIDE;
        } else {
            for (AgoParser.MethodModifierContext modifier : methodStarter.methodModifier()) {
                if(modifier.FINAL() != null){
                    if((result & AgoClass.FINAL) == AgoClass.FINAL) throw unit.syntaxError( modifier,"'final' duplicated");
                    result |= AgoClass.FINAL;
//                } else if(modifier.STATIC() != null) {
//                    if ((result & AgoClass.STATIC) == AgoClass.STATIC) throw unit.syntaxError(modifier, "'static' duplicated");
//                    result |= AgoClass.STATIC;
                } else if(modifier.commonVisiblility() != null){
                    if(visibilityFound){
                        throw unit.syntaxError(modifier, "visibility duplicated");
                    }
                    result |= commonVisibility(unit, modifier.commonVisiblility(), ModifierTarget.Method);
                    visibilityFound = true;
                } else if(modifier.ABSTRACT() != null){
                    if ((result & AgoClass.ABSTRACT) == AgoClass.ABSTRACT) throw unit.syntaxError(modifier, "'abstract' duplicated");
                    result |= AgoClass.ABSTRACT;
                } else if(modifier.OVERRIDE() != null){
                    if ((result & AgoClass.OVERRIDE) == AgoClass.OVERRIDE) throw unit.syntaxError(modifier, "'override' duplicated");
                    result |= AgoClass.OVERRIDE;
                } else {
                    throw unit.syntaxError(modifier, "unexpected token '%s'".formatted(modifier.getText()));
                }
            }
        }
        if(methodStarter.GETTER() != null){
            if ((result & AgoClass.GETTER) == AgoClass.GETTER) throw unit.syntaxError(methodStarter, "'get' duplicated");
            result |= AgoClass.GETTER;
        }
        if(methodStarter.SETTER() != null){
            if ((result & AgoClass.SETTER) == AgoClass.SETTER) throw unit.syntaxError(methodStarter, "'set' duplicated");
            result |= AgoClass.SETTER;
        }
        if(!visibilityFound){
            result |= commonVisibility(unit, null, ModifierTarget.Method);
        }
        return result;
    }

    static int constructorModifier(Unit unit, AgoParser.MethodStarterContext methodStarter) throws SyntaxError {
        int result = methodModifier(unit, methodStarter);
        if((result & AgoClass.ABSTRACT) == AgoClass.ABSTRACT){
            throw unit.syntaxError(methodStarter, "constructor cannot be abstract");
        }
        if((result & AgoClass.FINAL) == AgoClass.FINAL){
            throw unit.syntaxError(methodStarter, "constructor cannot be final");
        }
        if((result & AgoClass.OVERRIDE) == AgoClass.OVERRIDE){
            throw unit.syntaxError(methodStarter, "constructor needn't mark as 'override'");
        }
        if((result & AgoClass.GETTER) == AgoClass.GETTER){
            throw unit.syntaxError(methodStarter, "constructor needn't mark as 'override'");
        }
        if((result & AgoClass.SETTER) == AgoClass.SETTER){
            throw unit.syntaxError(methodStarter, "constructor needn't mark as 'override'");
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
              if((result & AgoClass.VISIBILITY_MASK) != 0) throw unit.syntaxError(commonVisibilility,"visibility duplicated");
              result |= AgoClass.PUBLIC;
        }
        if(commonVisibilility.PROTECTED() != null){
            if((result & AgoClass.VISIBILITY_MASK) != 0) throw unit.syntaxError(commonVisibilility,"visibility duplicated");
            result |= AgoClass.PROTECTED;
        }
        if(commonVisibilility.PRIVATE() != null){
            if((result & AgoClass.VISIBILITY_MASK) != 0) throw unit.syntaxError(commonVisibilility,"visibility duplicated");
            result |= AgoClass.PRIVATE;
        }
        return result;
    }

    static int classModifiers(Unit unit, List<AgoParser.ClassModifierContext> modifiers) throws SyntaxError {
        int result = 0;
        boolean visibilityFound = false;
        for (var modifier : modifiers) {
            if (modifier.FINAL() != null) {
                if ((result & AgoClass.FINAL) == AgoClass.FINAL) throw unit.syntaxError(modifier, "'final' duplicated");
                result |= AgoClass.FINAL;
//            } else if (modifier.STATIC() != null) {
//                if ((result & AgoClass.STATIC) == AgoClass.STATIC)
//                    throw unit.syntaxError(modifier, "'static' duplicated");
//                result |= AgoClass.STATIC;
            } else if (modifier.commonVisiblility() != null) {
                if (visibilityFound) {
                    throw unit.syntaxError(modifier, "visibility duplicated");
                }
                result |= commonVisibility(unit, modifier.commonVisiblility(), ModifierTarget.Class);
                visibilityFound = true;
            } else if (modifier.ABSTRACT() != null) {
                if ((result & AgoClass.ABSTRACT) == AgoClass.ABSTRACT)
                    throw unit.syntaxError(modifier, "'abstract' duplicated");
                result |= AgoClass.ABSTRACT;

            } else if(modifier.NATIVE() != null){
                if ((result & AgoClass.NATIVE) == AgoClass.NATIVE) throw unit.syntaxError(modifier, "'native' duplicated");
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
                if ((result & AgoClass.FINAL) == AgoClass.FINAL) throw unit.syntaxError(modifier, "'final' duplicated");
                result |= AgoClass.FINAL;
            } else if (modifier.commonVisiblility() != null) {
                if (visibilityFound) {
                    throw unit.syntaxError(modifier, "visibility duplicated");
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


}
