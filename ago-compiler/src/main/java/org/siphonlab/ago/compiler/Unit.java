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

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.generic.GenericInstantiationPlaceHolder;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.resolvepath.NamePathResolver;
import org.siphonlab.ago.compiler.parser.AgoLexer;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public class Unit {

    private final static Logger LOGGER = LoggerFactory.getLogger(Unit.class);

    private String filename;
    private final CharStream source;
    private final Root root;

    private List<CompilationError> errors = new ArrayList<>();

    private List<ClassDef> classes = new ArrayList<>();
    private List<ClassDef> topClasses = new ArrayList<>();

    private List<FunctionDef> functionDefs = new ArrayList<>();

    private Package pkg;

    private Map<String, ClassDef> importedClasses = new HashMap<>();
    private AgoParser.CompilationUnitContext compilationUnit;

    private List<UnsolvedImport> unsolvedImports = new ArrayList<>();

    private Set<ClassDef> solvedMetaClasses = new HashSet<ClassDef>();



    public Package getPackage() {
        return pkg;
    }

    public String getFilename() {
        return filename;
    }

    public Map<String, ClassDef> getImportedClasses() {
        return importedClasses;
    }

    public void importClass(ClassDef classDef) {
        this.importedClasses.put(classDef.name, classDef);
    }

    public Root getRoot() {
        return root;
    }

    public Unit(String filename, CharStream source, Root root) {
        this.filename = filename;
        this.source = source;
        //
        // quick sort
        this.root = root;
    }

    void packageDecl() {
        AgoLexer lexer = new AgoLexer(source);
        AgoParser parser = new AgoParser(new CommonTokenStream(lexer));
        this.compilationUnit = parser.compilationUnit();
        // 1st pass, compile all class names
        String packageName = compilationUnit.packageDeclaration() != null ? compilationUnit.packageDeclaration().qualifiedName().getText() : "";
        pkg = root.getChild(packageName);
        if (pkg == null) {
            pkg = root.createPackage(packageName);
        }
    }

    record UnsolvedImport(String classFullName, AgoParser.ImportDeclarationContext importDeclaration,
                          Package consumerPackage) {

    }

    public void importFixedClassNames() throws CompilationError {
        for (AgoParser.ImportDeclarationContext importDecl : compilationUnit.importDeclaration()) {
            if (importDecl.MUL() != null) {   // import all from package
                String importPkgName = fullName(importDecl.qualifiedNameAllowPostfix());
                Package pkg = root.getChild(importPkgName);
                if (pkg == null)
                    throw resolveError(importDecl.qualifiedNameAllowPostfix(), "package " + importPkgName + " not found");
                for (var c : pkg.getUniqueChildren()) {
                    this.importedClasses.put(c.getName(), c);
                }
                pkg.addClassDeclListener(decl -> this.importedClasses.put(decl.getFullname(), decl));
            } else {    // import class
                String className = fullName(importDecl.qualifiedNameAllowPostfix());
                ClassDef cls = root.findByFullname(className);
                if (cls != null) {
                    this.importedClasses.put(className, cls);
                } else {
                    // maybe not declared yet
                    this.unsolvedImports.add(new UnsolvedImport(className, importDecl, pkg));
                }
            }
        }
    }

    public List<UnsolvedImport> getUnsolvedImports() {
        return unsolvedImports;
    }

    public void classNames() throws CompilationError {
        for (AgoParser.TypeDeclarationContext typeDecl : compilationUnit.typeDeclaration()) {
            if (typeDecl instanceof AgoParser.ClassDeclContext c) {
                var classDeclaration = c.classDeclaration();
                var classDef = parseClassDef(classDeclaration, pkg);
                topClasses.add(classDef);
            } else if(typeDecl instanceof AgoParser.InterfaceDeclContext i) {
                var interfaceDeclaration = i.interfaceDeclaration();
                ClassDef classDef;
                classDef = parseInterfaceDef(interfaceDeclaration, pkg);
                topClasses.add(classDef);
            } else if (typeDecl instanceof AgoParser.TraitDeclContext c) {
                var classDeclaration = c.traitDeclaration();
                ClassDef classDef = parseTraitDef(classDeclaration, pkg);
                topClasses.add(classDef);
            } else if (typeDecl instanceof AgoParser.TopFunctionDeclContext methodDecl) {
                var fun = parseFunctionDef(methodDecl.methodDeclaration(), pkg);
                topClasses.add(fun);
            } else if(typeDecl instanceof AgoParser.EnumDeclContext enumDeclContext){
                EnumDef enumDef = parseEnumDef(enumDeclContext.enumDeclaration(), pkg);
                topClasses.add(enumDef);
            }
        }
    }

    private ClassDef parseInterfaceDef(AgoParser.InterfaceDeclarationContext interfaceDeclaration, ClassContainer parent) throws CompilationError {
        ClassDef classDef;
        parent.addChild(classDef = new InterfaceDef(interfaceDeclaration.interfaceName.getText(), interfaceDeclaration));
        classes.add(classDef);
        classDef.setUnit(this);
        classDef.setSourceLocation(this.sourceLocation(interfaceDeclaration));
        classDef.setModifiers(Compiler.interfaceModifiers(this, interfaceDeclaration.interfaceModifier()));
        classDef.nextCompilingStage(CompilingStage.ParseGenericParams);
        addChildClasses(classDef, interfaceDeclaration.classBody());
        return classDef;
    }

    private ClassDef parseTraitDef(AgoParser.TraitDeclarationContext traitDeclaration, ClassContainer parent) throws CompilationError {
        ClassDef classDef;
        parent.addChild(classDef = new TraitDef(traitDeclaration.className.getText(), traitDeclaration));
        classes.add(classDef);
        classDef.setUnit(this);
        classDef.setSourceLocation(this.sourceLocation(traitDeclaration));
        classDef.setModifiers(Compiler.classModifiers(this, traitDeclaration.classModifier()));
        classDef.nextCompilingStage(CompilingStage.ParseGenericParams);
        addChildClasses(classDef, traitDeclaration.classBody());
        return classDef;
    }

    private EnumDef parseEnumDef(AgoParser.EnumDeclarationContext enumDeclaration, ClassContainer parent) throws SyntaxError {
        EnumDef enumDef;
        parent.addChild(enumDef = new EnumDef(enumDeclaration.identifier().getText(), enumDeclaration));
        classes.add(enumDef);
        enumDef.setUnit(this);
        enumDef.setSourceLocation(sourceLocation(enumDeclaration));
        enumDef.setModifiers(Compiler.classModifiers(this, enumDeclaration.classModifier()) | AgoClass.FINAL);
        enumDef.setCompilingStage(CompilingStage.ResolveHierarchicalClasses);
        return enumDef;
    }

    private ClassDef parseClassDef(AgoParser.ClassDeclarationContext classDeclaration, ClassContainer parent) throws CompilationError {
        ClassDef classDef;
        parent.addChild(classDef = new ClassDef(classDeclaration.className.getText(), classDeclaration));
        classes.add(classDef);
        classDef.setUnit(this);
        classDef.setSourceLocation(this.sourceLocation(classDeclaration));
        classDef.setModifiers(Compiler.classModifiers(this, classDeclaration.classModifier()));
        classDef.nextCompilingStage(CompilingStage.ParseGenericParams);
        addChildClasses(classDef, classDeclaration.classBody());
        return classDef;
    }

    public void solveRemainImports() {
        Set<UnsolvedImport> solved = new HashSet<>();
        for (UnsolvedImport unsolvedImport : unsolvedImports) {
            var c = root.findByFullname(unsolvedImport.classFullName);
            if (c instanceof ClassDef classDef) {
                importedClasses.put(c.getName(), classDef);
                solved.add(unsolvedImport);
            }
        }
        unsolvedImports.removeAll(solved);
    }

    private void metaClass(ClassDef instanceClass, AgoParser.MetaclassDeclarationContext metaclassDecl) throws CompilationError {
        if (instanceClass.getMetaClassDef() != null) {
            throw syntaxError(metaclassDecl, "duplicated metaclass declaration");
        }
        int depth = 1;
        if (instanceClass instanceof MetaClassDef metaInstance) {
            if (metaInstance.getMetaLevel() == 2) {
                throw syntaxError(metaclassDecl, "metaclass too deep, only 2 tier nested allowed");
            }
            depth = 2;
        }
        var metaclass = new MetaClassDef(instanceClass, depth, metaclassDecl);
        instanceClass.setMetaClassDef(metaclass);
        metaclass.setUnit(this);
        metaclass.setSourceLocation(sourceLocation(metaclassDecl));
        metaclass.setSuperClass(root.getClassClass());  // let the super class of MetaClass be lang.Class
        metaclass.setCompilingStage(CompilingStage.ParseFields);      // direct jump to parse fields for metaclass
        pkg.addChild(metaclass);
        classes.add(metaclass);
        addChildClasses(metaclass, metaclassDecl.classBody());
    }


    public void resolveHierarchicalClasses(ClassDef classDef) throws CompilationError {
        AgoParser.DeclarationTypeContext baseType = classDef.getBaseTypeDecl();
        if (baseType != null) {
            var superClass = this.parseTypeName(classDef, baseType.namePath(), false);
            if (superClass == null)
                throw resolveError(baseType, "super class '%s' not found".formatted(baseType.namePath().getText()));
            if ((classDef instanceof FunctionDef) != (superClass instanceof FunctionDef)) {
                throw syntaxError(baseType, superClass.getFullname() + " is not a " + (classDef instanceof FunctionDef ? "function" : "class"));
            }
            if(classDef.isTrait() && !superClass.isTrait()){
                throw syntaxError(baseType, superClass.getFullname() + " is not a trait");
            }
            classDef.setSuperClass(superClass);
        } else {
            if (!classDef.isInterfaceOrTrait() && !classDef.isFunction()) {     // the superclass of Function is lang.Function
                classDef.setSuperClass(root.getObjectClass());
            }
        }

        // collect implemented interfaces
        var interfaceDecls = classDef.getInterfaceDecls();
        if(interfaceDecls != null){
            if(classDef instanceof ConstructorDef){
                throw syntaxError(interfaceDecls.getFirst(),"implement interfaces not allowed for constructor");
            }
            var interfaces = new ArrayList<ClassDef>();
            Map<ClassDef, AgoParser.IdentifierContext> wrappers = new LinkedHashMap<>();
            for (var interfaceDecl : interfaceDecls) {
                ClassDef interfaceDef;
                AgoParser.IdentifierContext wrapperField = null;
                AgoParser.DeclarationTypeContext interfaceRef;
                if(interfaceDecl instanceof AgoParser.SimpleInterfaceContext simpleInterface) {
                    interfaceRef = simpleInterface.declarationType();
                    interfaceDef = this.parseTypeName(classDef, interfaceRef.namePath(), false);
                } else if(interfaceDecl instanceof AgoParser.WrapperInterfaceContext wrapper){
                    if(classDef.isInterface()){
                        throw typeError(wrapper, "interface wrapper not applicable for interface");
                    }
                    interfaceRef = wrapper.declarationType();
                    interfaceDef = this.parseTypeName(classDef, interfaceRef.namePath(), false);
                    wrapperField = wrapper.identifier();
                } else {
                    throw new RuntimeException("impossible");
                }
                if (interfaceDef == null)
                    throw resolveError(interfaceDecl, "interface '%s' not found".formatted(interfaceRef.namePath().getText()));
                if(!interfaceDef.isInterfaceOrTrait())
                    throw syntaxError(interfaceDecl, "'%s' is not an interface nor a trait".formatted(interfaceDef));
                if(interfaces.contains(interfaceDef)){
                    throw resolveError(interfaceDecl, "duplicated interface '%s'".formatted(interfaceDef.getFullname()));
                }
                interfaces.add(interfaceDef);
                if(wrapperField != null) wrappers.put(interfaceDef, wrapperField);
                addDependency(classDef, interfaceDef, interfaceDecl);
            }
            classDef.setInterfaces(interfaces);
            if(!wrappers.isEmpty()){
                classDef.wrapperInterfaces.putAll(wrappers);
            }
        }
    }

    public void resolvePermitClass(ClassDef interfaceDef, AgoParser.DeclarationTypeContext permitTypeDecl) throws CompilationError {
        if (permitTypeDecl != null) {
            var permitClass = this.parseTypeName(interfaceDef, permitTypeDecl.namePath(), false);
            if (permitClass == null)
                throw resolveError(permitTypeDecl, "permit class '%s' not found".formatted(permitTypeDecl.namePath().getText()));
            interfaceDef.setPermitClass(permitClass);
        } else {
            var interfaces = interfaceDef.getInterfaces();
            for (var i : interfaces) {
                if (i.getPermitClass() == null && interfaceDef.getCompilingStage() == CompilingStage.ResolveHierarchicalClasses) {
                    if(i instanceof InterfaceDef in) {
                        i.getUnit().resolvePermitClass(in, in.getPermitTypeDecl());
                    } else if(i instanceof TraitDef t){
                        i.getUnit().resolvePermitClass(t, t.getPermitTypeDecl());
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(interfaces)) {
                var p = interfaces.getFirst().getPermitClass();
                if (interfaces.size() == 1 || interfaces.stream().allMatch(i -> i.getPermitClass() == p)) {
                    interfaceDef.setPermitClass(interfaces.getFirst().getPermitClass());
                } else {
                    var interfaceDecls = interfaceDef.getInterfaceDecls();
                    throw resolveError(interfaceDecls.get(1), "cannot predict permit class since interfaces have different permit class");
                }
            } else {
                interfaceDef.setPermitClass(root.getObjectClass());
            }
        }
    }

    protected void validateHierarchy(ClassDef classDef) throws CompilationError {
        if (classDef.isInterfaceOrTrait()) {
            validatePermitClassOfInterface(classDef);
        } else {
            validateClassMatchPermitClass(classDef);
        }
        for(var sp = classDef.getSuperClass(); sp != null; sp = sp.getSuperClass()){
            if(sp.isThatOrDerivedFromThat(classDef)){
                if(!classDef.getFullname().equals("lang.Object"))
                    throw resolveError(classDef.getBaseTypeDecl(), "recursive superclass '%s'".formatted(sp.getFullname()));
            }
            if(sp == sp.getSuperClass()) break;
        }
        ClassDef superClass = classDef.getSuperClass();
        if(superClass != null && classDef.getBaseTypeDecl() != null) {
            if(classDef.getClassType() == AgoClass.TYPE_CLASS || classDef.getClassType() == AgoClass.TYPE_TRAIT) {
                if (superClass.getClassType() != classDef.getClassType()) {
                    throw resolveError(classDef.getBaseTypeDecl(), "'%s' is not a %s".formatted(superClass.getFullname(), classDef.classType == AgoClass.TYPE_CLASS ? "class" : "trait"));
                }
                if (superClass.isFinal()) {
                    throw resolveError(classDef.getBaseTypeDecl(), "'%s' is a final class".formatted(superClass.getFullname()));
                }
            } else {
                throw typeError(classDef.getDeclarationAst(), "inheritantce only support class and trait");
            }
        }
        if(classDef instanceof InterfaceDef interfaceDef){
            for (int i = 0; i < interfaceDef.getInterfaces().size(); i++) {
                var baseInterface = interfaceDef.getInterfaces().get(i);
                if(baseInterface.isThatOrDerivedFromThat(interfaceDef)){
                    throw resolveError(interfaceDef.getInterfaceDecls().get(i), "recursive interface '%s'".formatted(baseInterface.getFullname()));
                }
            }
        }
        if(classDef instanceof TraitDef traitDef){
            if(!(traitDef.getParent() instanceof Package)){
                throw syntaxError(traitDef.getDeclarationName(), "trait cannot embedded in another type");
            }
        }
        List<ClassDef> interfaces = classDef.getInterfaces();
        for (int i = 0; i < interfaces.size(); i++) {
            var baseInterface = interfaces.get(i);
            for (int j = i + 1; j < interfaces.size(); j++) {
                var another = interfaces.get(j);
                if(another == baseInterface){
                    throw resolveError(classDef.getInterfaceDecls().get(j), "duplicated interface '%s' found".formatted(another.getFullname()));
                }
//                        if(another.isDerivedFrom(baseInterface)){
//                        // we don't handle this
//                        }
            }
        }
        classDef.nextCompilingStage(CompilingStage.InheritsFields);
    }

    private void validateClassMatchPermitClass(ClassDef classDef) throws SyntaxError {
        if (!classDef.isInterfaceOrTrait()) {
            // validate permit class capability
            var interfaces = classDef.getInterfaces();
            if (CollectionUtils.isNotEmpty(interfaces)) {
                for (int i = 0; i < interfaces.size(); i++) {
                    var interfaceDef = interfaces.get(i);
                    if (!classDef.isThatOrDerivedFromThat(interfaceDef.getPermitClass())) {    // TODO isAssignableFrom?
                        var interfaceDecls = classDef.getInterfaceDecls();
                        throw syntaxError(interfaceDecls.get(i), "interface '%s' not capable to '%s'".formatted(interfaceDef.getFullname(), classDef.getFullname()));
                    }
                }
            }
        }
    }

    private void validatePermitClassOfInterface(ClassDef interfaceDef) throws ResolveError {
        // validate permit class capability
        var interfaces = interfaceDef.getInterfaces();
        if (CollectionUtils.isNotEmpty(interfaces)) {
            for (int i = 0; i < interfaces.size(); i++) {
                var anInterface = interfaces.get(i);
                if (!interfaceDef.getPermitClass().isThatOrSuperOfThat(anInterface.getPermitClass())) {
                    var interfaceDecls = interfaceDef.getInterfaceDecls();
                    throw resolveError(interfaceDecls.get(i), "permit class '%s' of '%s' is not acceptable for '%s'".formatted(anInterface.getPermitClass(), anInterface, interfaceDef.getPermitClass()));
                }
            }
        }
    }

    @Deprecated
    public void verifyFunctionsImplemented(ClassDef classDef, Set<ClassDef> solved) throws ResolveError {
        if(solved.contains(classDef)) return;
        solved.add(classDef);

        if(classDef.isAbstract()) return;

        var superClass = classDef.getSuperClass();
        if(superClass != null && superClass != classDef  && superClass.isAbstract()){
            classDef.verifyFunctionsImplemented(superClass, classDef.getBaseTypeDecl());
        }

        var interfaces = classDef.getInterfaces();
        for (int i = 0; i < interfaces.size(); i++) {
            var interfaceDef = interfaces.get(i);
            classDef.verifyFunctionsImplemented(interfaceDef, classDef.getInterfaceDecls().get(i));
        }
    }

    private void addDependency(ClassDef classDef, ClassDef dependency, ParserRuleContext dependencyAst) throws ResolveError {
        if(classDef == dependency){
            throw resolveError(dependencyAst, "recursive dependency '%s' found".formatted(dependency));
        }
        if(classDef.dependencies.contains(dependency)){
            throw resolveError(dependencyAst, "duplicated dependency '%s' found".formatted(dependency));
        }
        if(classDef.isDependingOn(dependency)){
            throw resolveError(dependencyAst, "recursive dependency '%s' found".formatted(dependency));
        }
        classDef.addDependency(dependency);
    }

    protected void parseField(ClassDef ownerClass, AgoParser.FieldDeclarationContext fieldDeclaration) throws CompilationError {
        for (AgoParser.FieldVariableDeclaratorContext variableDeclarator : fieldDeclaration.fieldVariableDeclarators().fieldVariableDeclarator()) {
            Field field;
            if (variableDeclarator instanceof AgoParser.VarDeclExplicitTypeContext explicitType) {
                field = new Field(ownerClass, explicitType.identifier().getText(), variableDeclarator);
                var type = parseType(ownerClass, explicitType.typeOfVariable(),false);
                field.setType(type);
                field.setModifiers(Compiler.fieldModifiers(this, fieldDeclaration.fieldModifier(), Compiler.ModifierTarget.Field));
                field.setDeclaration(fieldDeclaration);
                field.setSourceLocation(sourceLocation(fieldDeclaration));
                var variableInitializer = explicitType.variableInitializer();
                if(variableInitializer != null){
                    field.setInitializer(variableInitializer.expression());
                }

                if (ownerClass.getMetaClassDef() != null && ownerClass.getMetaClassDef().fields.containsKey(field.name)) {
                    throw this.syntaxError(variableDeclarator, format("'%s' already existed in it's metaclass", field.name));
                } else if (ownerClass instanceof MetaClassDef metaOwner && metaOwner.getInstanceClassDef().fields.containsKey(field.name)) {
                    throw this.syntaxError(variableDeclarator, format("'%s' already existed in it's instance class", field.name));
                }
                ownerClass.addField(field);
            } else if (variableDeclarator instanceof AgoParser.VarDeclImplicitTypeContext varDeclImplicit) {
                throw new UnsupportedOperationException("implicit var type TODO"); // TODO
//                            field = new Field(ownerClass,varDeclImplicit.identifier().getText(), variableDeclarator);
//                            // varDeclImplicit.variableInitializer().expression()
//                            ownerClass.addField(field);
//                            fields.add(field);
//                            // TODO setType
            }
        }
    }

    private static String fullName(AgoParser.QualifiedNameAllowPostfixContext qualifiedNameContext) {
        StringBuffer sb = new StringBuffer();
        var identifier = qualifiedNameContext.identifierAllowPostfix();
        for (int i = 0; i < identifier.size(); i++) {
            var identifierContext = identifier.get(i);
            sb.append(StringUtils.deleteWhitespace(identifierContext.getText()));
            if (i < identifier.size() - 1) {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    private void addChildClasses(ClassDef classDef, AgoParser.ClassBodyContext classBody) throws CompilationError {
        if(classBody instanceof AgoParser.DefaultClassBodyContext defaultClassBodyContext) {
            for (AgoParser.ClassBodyDeclarationContext classBodyDeclaration : defaultClassBodyContext.classBodyDeclaration()) {
                addChildClasses(classDef, classBodyDeclaration.memberDeclaration());
            }
        }
    }

    private void addChildClasses(ClassDef classDef, AgoParser.MemberDeclarationContext memberDeclaration) throws CompilationError {
        if (memberDeclaration instanceof AgoParser.InnerClassDeclContext innerClassDecl) {
            parseClassDef(innerClassDecl.classDeclaration(),classDef);
        } else if (memberDeclaration instanceof AgoParser.MethodDeclContext methodDecl) {
            parseFunctionDef(methodDecl.methodDeclaration(), classDef);
        } else if (memberDeclaration instanceof AgoParser.ConstructorDeclContext constructorDecl) {
            parseConstructorDef(constructorDecl.constructorDeclaration(), classDef);
        } else if (memberDeclaration instanceof AgoParser.MetaclassDeclContext metaclassDecl) {
            AgoParser.MetaclassDeclarationContext metaclassDeclation = metaclassDecl.metaclassDeclaration();
            metaClass(classDef, metaclassDeclation);
        } else if(memberDeclaration instanceof AgoParser.FieldDeclContext fieldDeclContext){
            var variableDeclarators = fieldDeclContext.fieldDeclaration().fieldVariableDeclarators();
            for (AgoParser.FieldVariableDeclaratorContext fieldVariableDeclaratorContext : variableDeclarators.fieldVariableDeclarator()) {
                if(fieldVariableDeclaratorContext instanceof AgoParser.VarDeclExplicitTypeContext explicitTypeContext){
                    AgoParser.TypeOfVariableContext typeOfVariableContext = explicitTypeContext.typeOfVariable();
                    if(typeOfVariableContext instanceof AgoParser.AsClassDeclContext asClassDeclContext){
                        parseClassDef(asClassDeclContext.classDeclaration(),classDef);
                    }
                }
            }
        }
    }

    private FunctionDef parseFunctionDef(AgoParser.MethodDeclarationContext methodDecl, ClassContainer classContainer) throws CompilationError {
        var methodName = methodDecl.methodName();
        var qualifiedName = methodName.identifierAllowPostfix();

        String name = StringUtils.deleteWhitespace(qualifiedName.getText());
        if (classContainer instanceof ClassDef ownerClass) {
            if (ownerClass.getMetaClassDef() != null && ownerClass.getMetaClassDef().getChild(name) != null) {
                throw this.syntaxError(qualifiedName, format("'%s' already existed in it's metaclass", name));
            } else if (ownerClass instanceof MetaClassDef metaOwner && metaOwner.getInstanceClassDef().getChild(name) != null) {
                throw this.syntaxError(qualifiedName, format("'%s' already existed in it's instance class", name));
            }
        }
        int modifiers = Compiler.methodModifier(this, methodDecl.methodStarter());

        if((((modifiers & AgoClass.GETTER) != 0) || ((modifiers & AgoClass.SETTER) != 0)) && name.lastIndexOf('#') != -1){
            throw syntaxError(methodName, "getter and setter shouldn't provide postfix");
        }
        if((modifiers & AgoClass.GETTER) == 0 && name.endsWith("#get")){
            modifiers |= AgoClass.GETTER;
        }
        if((modifiers & AgoClass.SETTER) == 0 && name.endsWith("#set")){
           modifiers |= AgoClass.SETTER;
        }

        var fun = new FunctionDef(name, methodDecl, modifiers);
        fun.setUnit(this);
        fun.setSourceLocation(sourceLocation(methodDecl));
        if(classContainer instanceof ClassDef c && c.isInterface()){
            fun.setModifiers(fun.modifiers | AgoClass.ABSTRACT);
        }
        if (fun.isStatic()) {
            throw syntaxError(methodDecl.methodStarter(), "functions(include constructors) cannot be 'static'");
        }
        classContainer.addChild(fun);
        functionDefs.add(fun);
        fun.nextCompilingStage(CompilingStage.ParseGenericParams);

        AgoParser.MethodBodyContext methodBody = methodDecl.methodBody();
        if(methodBody instanceof AgoParser.MBBLockContext mbbLockContext) {
            scanTypesInBlock(fun, mbbLockContext.block());
        }
        return fun;
    }

    private void scanTypesInBlock(FunctionDef fun, AgoParser.BlockContext block) throws CompilationError {
        ArrayDeque<ParseTree> stack = new ArrayDeque<>();
        stack.addAll(block.blockStatement());
        while(!stack.isEmpty()){
            ParseTree blockStatement = stack.pop();
            if (blockStatement instanceof AgoParser.LocalTypeDeclContext localTypeDecl) {
                var localTypeDeclaration = localTypeDecl.localTypeDeclaration();
                if (localTypeDeclaration instanceof AgoParser.ClassDeclInBlockContext classDeclInBlockContext) {
                    parseClassDef(classDeclInBlockContext.classDeclaration(), fun);
                } else if (localTypeDeclaration instanceof AgoParser.MetaclassInBlockContext metaclassInBlock) {
                    metaClass(fun, metaclassInBlock.metaclassDeclaration());
                } else if (localTypeDeclaration instanceof AgoParser.MethodInBlockContext methodInBlock) {
                    parseFunctionDef(methodInBlock.methodDeclaration(), fun);
                } else if (localTypeDeclaration instanceof AgoParser.EnumInBlockContext enumInBlockContext) {
                    parseEnumDef(enumInBlockContext.enumDeclaration(), fun);
                }
            } else if(blockStatement instanceof AgoParser.TypeOfVariableContext typeOfVariableContext){
                if (typeOfVariableContext instanceof AgoParser.AsClassDeclContext asClassDeclContext) {
                    parseClassDef(asClassDeclContext.classDeclaration(), fun);
                }
            } else {
                for (int i = 0; i < blockStatement.getChildCount(); i++) {
                    stack.add(blockStatement.getChild(i));
                }
            }
        }
    }

    private void parseConstructorDef(AgoParser.ConstructorDeclarationContext methodDecl, ClassDef classDef) throws CompilationError {
        var modifiers = Compiler.constructorModifier(this, methodDecl.methodStarter());
        var fun = new ConstructorDef(modifiers, methodDecl);
        fun.setUnit(this);
        fun.setSourceLocation(sourceLocation(methodDecl));
        functionDefs.add(fun);
        classDef.addChild(fun);
        fun.nextCompilingStage(CompilingStage.ParseGenericParams);
        if (fun.isStatic()) {
            throw syntaxError(methodDecl.methodStarter(), "functions(include constructors) cannot be 'static'");
        }
        AgoParser.BlockContext constructorBody = methodDecl.constructorBody;
        if (constructorBody != null) {
            scanTypesInBlock(fun, constructorBody);
        }
    }

    protected ClassDef parseType(ClassDef scopeClass, AgoParser.TypeOfVariableContext typeOfVariable, boolean allowGenericPlaceHolder) throws CompilationError {
        if (typeOfVariable == null) {
            return PrimitiveClassDef.VOID;
        }
        if (typeOfVariable instanceof AgoParser.AsTypeContext asType) {
            var expr = parseType(scopeClass, asType.variableType(), false, false);
            return extractType(expr);
        } else {
            Literal[] args;
            if (typeOfVariable instanceof AgoParser.AsTypeRangeContext asTypeRange) {
                args = parseTypeRange(asTypeRange.typeRange(), scopeClass);
            } else if (typeOfVariable instanceof AgoParser.LikeTypeContext likeType) {
                var type = parseTypeName(scopeClass, likeType.namePath(), false);
                type = tryExtractFunctionInterfaceInstantiation(likeType, type);
                args = new Literal[]{new ClassRefLiteral(type), new ClassRefLiteral(root.getAnyClass())};
            } else if(typeOfVariable instanceof AgoParser.AsClassDeclContext classDeclContext){
                var type = scopeClass.getChild(classDeclContext.classDeclaration().className.getText());
                assert type != null;        // this class should be already recognized
                return type;
            } else {
                throw new RuntimeException("impossible");
            }
            ClassDef classInterval = root.getScopedClassInterval();
            var pc = ((ClassContainer) classInterval.getParent()).getOrCreateScopedClassInterval(classInterval, classInterval.getMetaClassDef().getConstructor(), args, null);
            scopeClass.registerConcreteType(pc);
            return pc;
        }
    }

    protected ClassDef parseType(ClassDef scopeClass, AgoParser.TypeOfFunctionContext typeOfFunction) throws CompilationError {
        if (typeOfFunction == null) {
            return PrimitiveClassDef.VOID;
        }
        if (typeOfFunction instanceof AgoParser.ReturnVariableTypeContext asType) {
            var expr = parseType(scopeClass, asType.variableType(), false, false);
            return extractType(expr);
        } else {
            Literal[] args;
            if (typeOfFunction instanceof AgoParser.ReturnTypeRangeContext asTypeRange) {
                args = parseTypeRange(asTypeRange.typeRange(), scopeClass);
            } else if (typeOfFunction instanceof AgoParser.ReturnLikeContext likeType) {
                var type = parseTypeName(scopeClass, likeType.namePath(), false);
                type = tryExtractFunctionInterfaceInstantiation(likeType, type);
                args = new Literal[]{new ClassRefLiteral(type), new ClassRefLiteral(root.getAnyClass())};
            } else {
                throw new RuntimeException("impossible");
            }
            ClassDef classInterval = root.getScopedClassInterval();
            var pc = ((ClassContainer) classInterval.getParent()).getOrCreateScopedClassInterval(classInterval, classInterval.getMetaClassDef().getConstructor(), args, null);
            scopeClass.registerConcreteType(pc);
            return pc;
        }
    }

    private ClassDef tryExtractFunctionInterfaceInstantiation(ParserRuleContext ruleContext, ClassDef type) throws CompilationError {
        if(type instanceof FunctionDef functionDef){
            ClassDef functionInterfaceInstantiation = functionDef.getFunctionInterfaceInstantiation();
            if(functionInterfaceInstantiation != null){
                type = functionInterfaceInstantiation;
            } else if(functionDef.compilingStage == CompilingStage.ParseFields){
                if(functionDef.parseFields()){
                    type = functionDef.getFunctionInterfaceInstantiation();
                } else {
                    throw this.resolveError(ruleContext, "extract function interface of '%s' failed".formatted(functionDef));
                }
            }
        }
        return type;
    }

    Literal<?>[] parseTypeRange(AgoParser.TypeRangeContext typeRange, ClassDef scopeClass) throws CompilationError {
        var from = parseTypeName(scopeClass, typeRange.from.declarationType().namePath(), false);
        var to = typeRange.to == null ? root.getAnyClass() : parseTypeName(scopeClass, typeRange.to.declarationType().namePath(),false);
        if(from instanceof FunctionDef){
            from = tryExtractFunctionInterfaceInstantiation(typeRange.from, from);
        }
        if(to instanceof FunctionDef){
            to = tryExtractFunctionInterfaceInstantiation(typeRange.to, to);
        }
        var args = new Literal[]{new ClassRefLiteral(from), new ClassRefLiteral(to)};
        return args;
    }

    public static ClassDef extractType(Expression typeExpr) throws CompilationError {
        var r = extractTypeIfPossible(typeExpr);
        if(r == null)
            throw new UnsupportedOperationException("cannot extract type from %s".formatted(typeExpr));
        return r;
    }

    public static ClassDef extractTypeIfPossible(Expression typeExpr) throws CompilationError {
        if (typeExpr instanceof ConstClass constClass) {
            return constClass.getClassDef();
        } else if (typeExpr instanceof ClassOf.ClassOfInstance classOfInstance) {
            return classOfInstance.getClassDef();
        } else if (typeExpr instanceof ClassOf.ClassOfScope classOfScope) {
            return classOfScope.getClassDef();
        } else if (typeExpr instanceof ClassUnder.ClassUnderScope classUnderScope) {
            return classUnderScope.getClassDef();
        } else if (typeExpr instanceof ClassUnder.ClassUnderInstance classUnderInstance) {
            return classUnderInstance.getClassDef();
        } else {
            return null;
        }
    }

    protected Expression parseType(ClassDef scopeClass, AgoParser.VariableTypeContext variableType, boolean acceptTypeExpr, boolean allowGenericPlaceHolder) throws CompilationError {
        if (variableType instanceof AgoParser.VarTypeNormalContext varTypeNormal) {
            return parseType(scopeClass, varTypeNormal.declarationType(), acceptTypeExpr, allowGenericPlaceHolder);
        } else if (variableType instanceof AgoParser.VarTypeArrayContext varTypeArray) {
            var elementType = parseTypeName(scopeClass, varTypeArray.declarationType().namePath(), allowGenericPlaceHolder);
            var dimension = varTypeArray.LBRACK().size();
            ArrayClassDef lastArrayType = null;
            for (var i = 0; i < dimension; i++) {
                elementType = lastArrayType = scopeClass.getOrCreateArrayType(elementType, null);
            }
            return new ConstClass(lastArrayType).setSourceLocation(SourceLocation.UNKNOWN);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected Expression parseType(ClassDef scopeClass, AgoParser.DeclarationTypeContext declarationType, boolean acceptTypeExpr, boolean allowGenericPlaceHolder) throws CompilationError {
        var namePath = declarationType.namePath();
        Expression expr;
        if(acceptTypeExpr){
            expr = resolveNamePath(scopeClass instanceof FunctionDef f? f : null, scopeClass, namePath, NamePathResolver.ResolveMode.ForTypeExpr);
        } else {
            expr = resolveNamePath(null, scopeClass, namePath, NamePathResolver.ResolveMode.ForTypeName);
        }
        if (expr instanceof ConstClass || expr instanceof ClassOf.ClassOfScope
                || expr instanceof ClassUnder.ClassUnderScope ||
                (expr instanceof ClassUnder.ClassUnderInstance u && u.getClassDef().getParent() instanceof MetaClassDef)) {
            //
        } else {
            if (!acceptTypeExpr)
                throw syntaxError(declarationType, "type '%s' not allowed in declaration".formatted(declarationType.getText()));
        }
        if(!allowGenericPlaceHolder){
            if(expr.inferType() instanceof GenericInstantiationPlaceHolder){
                throw syntaxError(declarationType, "empty args generic instantitation not allowed here");
            }
        }
        return expr;
    }

    public ClassDef parseTypeName(ClassDef scopeClass, AgoParser.NamePathContext namePath, boolean allowGenericPlaceHolder) throws CompilationError {
        if (namePath instanceof AgoParser.PrimitiveContext primitive) {
            return PrimitiveClassDef.fromPrimitiveTypeAst(primitive.primitiveType());
        } else if (namePath instanceof AgoParser.FormalNamePathContext formalNamePath) {
            if(formalNamePath.getChildCount() == 1 && formalNamePath.getText().equals("_")){
                return root.getAnyClass();
            }
            var resolver = new NamePathResolver(NamePathResolver.ResolveMode.ForTypeName, this, scopeClass, formalNamePath);
            var expr = resolver.resolve();
            var t = extractType(expr);
            if(!allowGenericPlaceHolder){
                if(t instanceof GenericInstantiationPlaceHolder){
                    throw syntaxError(namePath, "empty args generic instantiation not allowed here");
                }
            }
            return t;
        } else {
            throw new UnsupportedOperationException("unexpected namePath " + namePath.getText());
        }
    }

    protected Expression resolveNamePath(FunctionDef ownerFunction, ClassDef scopeClass, AgoParser.NamePathContext namePath, NamePathResolver.ResolveMode resolveMode) throws CompilationError {
        if (namePath instanceof AgoParser.PrimitiveContext primitive) {
            return new ConstClass(PrimitiveClassDef.fromPrimitiveTypeAst(primitive.primitiveType()));
        } else if (namePath instanceof AgoParser.FormalNamePathContext formalNamePath) {
            var resolver = new NamePathResolver(resolveMode, this, ownerFunction, scopeClass, formalNamePath);
            return resolver.resolve();
        } else {
            throw new UnsupportedOperationException("unexpected namePath " + namePath.getText());
        }
    }

    public List<ClassDef> getTopClasses() {
        return topClasses;
    }

    void parseFormalParameters(FunctionDef fun, AgoParser.FormalParametersContext formalParameters) throws CompilationError {
        //TODO check ReceiverParameter at head if found, and VarArgsParameter at the end if found
        //formalParameters.receiverParameter()
        boolean receiverParamFound = false;
        AgoParser.VarArgsParameterContext varArgsParam = null;
        var paramList = formalParameters.formalParameter();
        if (paramList != null) {
            for (var param : paramList) {
                if (varArgsParam != null) {
                    throw syntaxError(varArgsParam, "a varargs parameter already existed");
                }
                if (param instanceof AgoParser.DefaultParameterContext defaultParameter) {
                    var paramName = defaultParameter.identifier().getText();
                    var paramType = parseType(fun, defaultParameter.typeOfVariable(), false);
                    Parameter parameter = new Parameter(paramName, param);
                    int modifiers = Compiler.variableModifiers(this, defaultParameter.variableModifier(), Compiler.ModifierTarget.Param);
                    // default visibility of this field?
//                    if((modifiers & AgoClass.FIELD_PARAM) == AgoClass.FIELD_PARAM){
//                        if(defaultParameter.fieldGetterSetter() != null) {
//                            modifiers |= AgoClass.PUBLIC;
//                        }
//                    }
                    parameter.setModifiers(modifiers);
                    parameter.setType(paramType);
                    parameter.setSourceLocation(sourceLocation(defaultParameter));
                    fun.addParameter(parameter);
                } else if (param instanceof AgoParser.ReceiverParameterContext receiverParameter) {
                    //TODO
                    if (paramList.indexOf(param) != 0) {
                        throw syntaxError(param, "receiver parameter must put at head");
                    } else if (receiverParamFound) {
                        throw syntaxError(param, "a receiver parameter already found");
                    }
                    receiverParamFound = true;

                } else if (param instanceof AgoParser.VarArgsParameterContext varArgsParameter) {
                    var paramName = varArgsParameter.identifier().getText();
                    var paramType = parseType(fun, varArgsParameter.typeOfVariable(), false);
                    Parameter parameter = new Parameter(paramName, param);
                    int modifiers = Compiler.variableModifiers(this, varArgsParameter.variableModifier(), Compiler.ModifierTarget.Param);
                    parameter.setModifiers(modifiers | AgoClass.VAR_ARGS);
                    ArrayClassDef arrayType = fun.getOrCreateArrayType(paramType, null);
                    parameter.setType(arrayType);
                    parameter.setSourceLocation(sourceLocation(varArgsParameter));
                    fun.registerConcreteType(arrayType);
                    fun.addParameter(parameter);
                    varArgsParam = varArgsParameter;
                } else {
                    throw syntaxError(formalParameters, "illegal parameters");
                }
            }
        }
    }

    public SourceLocation sourceLocation(ParserRuleContext ast) {
        if(ast == null) return SourceLocation.UNKNOWN;
        return new SourceLocation(filename, ast);
    }

    public SourceLocation sourceLocation(ParserRuleContext from, ParserRuleContext toInclude) {
        return new SourceLocation(filename, from, toInclude);
    }

    public SourceLocation sourceLocation(TerminalNode ast) {
        if (ast == null)
            return SourceLocation.UNKNOWN;
        Token token = ast.getSymbol();
        return new SourceLocation(filename, token.getLine(), token.getCharPositionInLine(), token.getText().length(), token.getStartIndex(), token.getStopIndex());
    }

    public ResolveError resolveError(ParserRuleContext ast, String message) {
        return new ResolveError(message, sourceLocation(ast));
    }

    public SyntaxError syntaxError(ParserRuleContext ast, String message) {
        return new SyntaxError(message, sourceLocation(ast));
    }

    public TypeMismatchError typeError(ParserRuleContext ast, String message) {
        return new TypeMismatchError(message, sourceLocation(ast));
    }

}

