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

import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.literal.*;
import org.siphonlab.ago.compiler.generic.SharedGenericTypeParameterClassDef;
import org.siphonlab.ago.compiler.generic.TypeParamsContext;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AgoClassParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgoClassParser.class);
    private final AgoClassLoader classLoader;
    private final Compiler compiler;
    private final Root root;

    Set<AgoClass> concreteTypes = new HashSet<>();

    Map<AgoClass, ClassDef> classes = new HashMap<>();

    public AgoClassParser(AgoClassLoader classLoader, Compiler compiler, Root root) {
        this.classLoader = classLoader;
        this.compiler = compiler;
        this.root = root;
    }

    Collection<ClassDef> load() throws CompilationError {
        // ParseClassName stage

        LinkedList<AgoClass> classesExcludeConcreteTypesAndMeta = new LinkedList<>();
        LinkedList<AgoClass> templateClasses = new LinkedList<>();
        LinkedList<AgoClass> allClasses = new LinkedList<>();
        LinkedList<AgoClass> concreteChildren = new LinkedList<>();
        LinkedList<AgoClass> metas = new LinkedList<>();

        for (AgoClass agoClass : classLoader.getClasses()) {
            if("<Meta>".equals(agoClass.getFullname())) continue;
            allClasses.add(agoClass);
            if (agoClass instanceof MetaClass || belongToMetaClass(agoClass)) {
                metas.add(agoClass);
                continue;      // create metaclass in processClass
            }
            ConcreteTypeInfo concreteTypeInfo = agoClass.getConcreteTypeInfo();
            if (concreteTypeInfo != null) {
                if (concreteTypeInfo instanceof GenericTypeParametersInfo) {
                    templateClasses.add(agoClass);
                } else {
                    concreteTypes.add(agoClass);
                    continue;       // ignore Array, ParameterizedClass, GenericInstantiation, preserve Template class
                }
            }

            if (belongToConcreteTypes(agoClass)) {
                concreteChildren.add(agoClass);
                continue;
            }

            classesExcludeConcreteTypesAndMeta.add(agoClass);
        }
        processStage(CompilingStage.ParseClassName, new LinkedList<>(classesExcludeConcreteTypesAndMeta));
        root.resolveLangClasses();

        processStage(CompilingStage.ParseGenericParams, templateClasses);

        LinkedList<AgoClass> concreteTypesToSolve = new LinkedList<>(concreteTypes.stream().sorted(Comparator.comparing(AgoClass::getFullname)).toList());
        processStage(CompilingStage.ParseClassName, new LinkedList<>(concreteTypesToSolve));

        processStage(CompilingStage.ResolveHierarchicalClasses, new LinkedList<>(concreteTypesToSolve));

        for (AgoClass child : concreteChildren) {
            ClassDef classDef = root.findByFullname(child.getFullname());
            assert classDef != null;
            classes.put(child, classDef);
        }
        // ResolveHierarchicalClasses
        classesExcludeConcreteTypesAndMeta.addAll(concreteChildren);
        processStage(CompilingStage.ResolveHierarchicalClasses, classesExcludeConcreteTypesAndMeta);

        root.resolveLangClasses();
        // ParseFields, no InheritsFields since the fields already inherited in AgoClass
        // and no AllocateSlots
        processStage(CompilingStage.ParseFields, new LinkedList<>(allClasses));

        processStage(CompilingStage.InheritsFields, new LinkedList<>(allClasses));

        processStage(CompilingStage.InheritsInnerClasses, new LinkedList<>(allClasses));

        for (AgoClass child : metas) {
            ClassDef classDef = root.findByFullname(child.getFullname());
            assert classDef != null;
            classes.put(child, classDef);
        }

        processStage(CompilingStage.ValidateMembers, new LinkedList<>(allClasses));

        processStage(CompilingStage.AllocateSlots, new LinkedList<>(allClasses));

        root.getAndCleanNewFoundClasses();

        assert classes.size() == allClasses.size();

        Collection<ClassDef> r = classes.values();
        while(true) {
            ArrayList<Namespace<?>> namespaces = new ArrayList<>(root.getAllDescendants().getUniqueElements());
            for (Namespace<?> n : namespaces) {
                if (n instanceof ClassDef classDef) {
                    if (classDef.getCompilingStage() != CompilingStage.Compiled) {
                        Compiler.processClassTillStage(classDef, CompilingStage.Compiled);      // lang.Function<>, lang.FunctionN<>
                        if (r instanceof List<ClassDef>) {
                            r.add(classDef);
                        } else {
                            var ls = new ArrayList<>(r);
                            ls.add(classDef);
                            r = ls;
                        }
//                    throw new RuntimeException("'%s' not compiled".formatted(classDef));
                    }
                }
            }
            if(namespaces.size() == root.getAllDescendants().getUniqueElements().size()) break;
        }

        return r;
    }

    void processStage(CompilingStage stage, LinkedList<AgoClass> toSolve) throws CompilationError {
        int initialSize = classes.size();
        while(true){
            while(!toSolve.isEmpty()){
                var agoClass = toSolve.poll();
                var classDef = classes.get(agoClass);
                boolean r;
                if(stage == CompilingStage.ParseClassName || classDef == null){
                    r = processLoadClassName(agoClass, classDef);
                    continue;
                } else if(classDef != null && classDef.getCompilingStage().getValue() <= stage.getValue()) {
                    switch (classDef.getCompilingStage()){
                        case ParseClassName:                r = processLoadClassName(agoClass, classDef); break;
                        case ParseGenericParams:            r = parseGenericParams(agoClass, classDef); break;
                        case ResolveHierarchicalClasses:    r = resolveHierarchy(agoClass, classDef); break;
                        case ParseFields:                   r = parseFields(agoClass, classDef); break;     // jump to InheritsInnerClasses
                        case ValidateHierarchy:             classDef.nextCompilingStage(CompilingStage.InheritsFields); r = true; break;
                        case InheritsFields:                r = inheritsFields(classDef); break;      // for GenericInstantiation
                        case ValidateNewFunctions:          r = true; classDef.setCompilingStage(CompilingStage.InheritsInnerClasses); break;
                        case InheritsInnerClasses:          r = inheritsInnerClasses(classDef); break;  // jump to Compiled
                        case ValidateMembers:               r = validateMembers(classDef); break;
                        case AllocateSlots:                 r = allocateSlots(agoClass, classDef); break;      // for GenericInstantiation
                        default: throw new RuntimeException("unexpected stage " + classDef.getCompilingStage());
                    }
                } else {
                    r = true;
                }
                if(!r || (classDef == null || classDef.getCompilingStage().getValue() <= stage.getValue())) {
                    toSolve.add(agoClass);
                }
            }
            break;
        }
    }

    private boolean validateMembers(ClassDef classDef) {
        if(classDef.getCompilingStage() == CompilingStage.ValidateMembers){
            classDef.setCompilingStage(CompilingStage.AllocateSlots);
        }
        return true;
    }

    private boolean belongToConcreteTypes(AgoClass agoClass) {
        for(var p = agoClass.getParent(); p != null; p = p.getParent()){
            ConcreteTypeInfo concreteTypeInfo = p.getConcreteTypeInfo();
            if(concreteTypeInfo != null){
                if(!(concreteTypeInfo instanceof GenericTypeParametersInfo)){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean belongToMetaClass(AgoClass agoClass) {
        for(var p = agoClass.getParent(); p != null; p = p.getParent()){
            if(p instanceof MetaClass) return true;
        }
        return false;
    }

    private boolean resolveHierarchy(AgoClass agoClass, ClassDef classDef) throws CompilationError {
        if (classDef.getCompilingStage() != CompilingStage.ResolveHierarchicalClasses) return true;

        if(classDef.isInGenericInstantiation()){
            ClassDef templateClass = classDef.getTemplateClass();
            if(templateClass.getCompilingStage() == CompilingStage.ResolveHierarchicalClasses){
                GenericArgumentsInfo argumentsInfo = (GenericArgumentsInfo) agoClass.getConcreteTypeInfo();
                if(!resolveHierarchy(argumentsInfo.getTemplateClass(), templateClass)){
                    return false;
                }
            }
            classDef.instantiateHierarchy();
            return true;
        }

        if(agoClass.getSuperClass() != null) {
            ClassDef c = mapClass(agoClass.getSuperClass());
            if (c == null)
                return false;
            classDef.setSuperClass(c);
        }

        if (agoClass.getAgoClass() != null && !agoClass.getAgoClass().getFullname().equals("<Meta>")) {
            MetaClassDef metaClassDef = (MetaClassDef) mapClass(agoClass.getAgoClass());
            if(metaClassDef == null) return false;
            classDef.setMetaClassDef(metaClassDef);
        }
        if(agoClass.getPermitClass() != null) {
            ClassDef permitClass = mapClass(agoClass.getPermitClass());
            if(permitClass == null) return false;
            classDef.setPermitClass(permitClass);
        }
        var interfaces = agoClass.getInterfaces();
        if (interfaces != null) {
            List<ClassDef> ls = new ArrayList<>();
            for (AgoClass cls : interfaces) {
                var c = mapClass(cls);
                if(c == null) return false;
                ls.add(c);
            }
            classDef.setInterfaces(ls);
        } else {
            classDef.setInterfaces(new ArrayList<>());
        }
        classDef.setCompilingStage(CompilingStage.ParseFields);
        return true;
    }

    private ClassDef mapClass(AgoClass agoClass) throws CompilationError {
        if(agoClass == null) return null;
        var existed = classes.get(agoClass);
        if(existed != null) return existed;

        ClassDef r;
        ConcreteTypeInfo concreteTypeInfo = agoClass.getConcreteTypeInfo();
        if(concreteTypeInfo == null || concreteTypeInfo instanceof GenericTypeParametersInfo) {
            r = createClassDefFromAgoClass(agoClass);
            return r;
        } else if(concreteTypeInfo instanceof ArrayInfo arrayInfo){
            var elementType = mapClass(arrayInfo.getElementType());
            if(elementType == null) return null;
            r = root.getOrCreateArrayType(elementType, null);
            classes.put(agoClass,r);
            for (AgoClass child : agoClass.getChildren()) {
                classes.put(child, r.getChild(child.getName()));
            }
        } else if(concreteTypeInfo instanceof NullableTypeInfo nullableTypeInfo){
            var baseClass = mapClass(nullableTypeInfo.getBaseClass());
            if(baseClass == null) return null;
            r = root.getOrCreateNullableType(baseClass, null);
            classes.put(agoClass,r);
        } else if(concreteTypeInfo instanceof ParameterizedClassInfo pInfo){
            var base = mapClass(pInfo.getParameterizedBaseClass());
            if(base == null) return null;
            ConstructorDef constructor = (ConstructorDef) mapClass(pInfo.getParameterizedConstructor());
            if(constructor == null) return null;
            Literal<?>[] args = mapLiteralArguments(pInfo.getArguments(), pInfo.getParameterizedConstructor().getParameters());
            if(base == root.getGenericTypeParameter()){
                r = ((ClassContainer)base.getParent()).getOrCreateGenericTypeParameter(base, constructor,
                            ((ClassRefLiteral)args[0]).getClassDefValue(),
                            ((ClassRefLiteral)args[1]).getClassDefValue(),
                            Variance.of(((ByteLiteral)args[2]).value), null);
            } else if(base == root.getGenericTypeCodeAvatar()){     // create it, but not register into the template
                r = ((ClassContainer)base.getParent()).getOrCreateGenericTypeAvatarClassDef(base, (SharedGenericTypeParameterClassDef) ((ClassRefLiteral)args[0]).getClassDefValue(),
                        ((ClassRefLiteral)args[1]).getClassDefValue(),
                        ((IntLiteral)args[2]).value,
                        ((IntLiteral)args[3]).value,
                        ((StringLiteral)args[4]).getString(), null);
            } else {
                r = ((ClassContainer) base.getParent()).getOrCreateParameterizedClass(base, constructor, args, null);
            }
            classes.put(agoClass,r);
            for (AgoClass child : agoClass.getChildren()) {
                r.addChild(mapClass(child));
            }
        } else if(concreteTypeInfo instanceof GenericArgumentsInfo argumentsInfo) {
            var templateClass = mapClass(argumentsInfo.getTemplateClass());
            if (templateClass == null || templateClass.getTypeParamsContext() == null)      // not ready
                return null;

            ClassRefLiteral[] args = new ClassRefLiteral[argumentsInfo.getArguments().length];
            var arguments = argumentsInfo.getArguments();
            for (int i = 0; i < arguments.length; i++) {
                var arg = arguments[i];
                args[i] = mapClass(arg).toClassRefLiteral();
            }
            ClassContainer parent = agoClass.getParent() == null ? (ClassContainer) templateClass.getParent() : mapClass(agoClass.getParent());
            r = (ClassDef) parent.getOrCreateGenericInstantiationClassDef(templateClass, args, null);
            classes.put(agoClass, r);
        } else {
            throw new RuntimeException("unexpected class " + agoClass);
        }
        return r;
    }

    private Literal<?> [] mapLiteralArguments(Object[] objectArguments, AgoParameter[] parameters) throws CompilationError {
        Literal<?>[] args = new Literal[objectArguments.length];
        for (int i = 0; i < objectArguments.length; i++) {
            Object argument = objectArguments[i];
            args[i] = objectToLiteral(argument, parameters[i].getTypeCode());
        }
        return args;
    }

    public Root getRoot() {
        return root;
    }

    private Literal<?> objectToLiteral(Object argument, TypeCode typeCode) throws CompilationError {
        if(argument instanceof String s){
            return getRoot().createStringLiteral(s);
        } else if(argument instanceof Boolean b){
            return getRoot().createBooleanLiteral( b);
        } else if(argument instanceof Character c){
            return getRoot().createCharLiteral(c);
        } else if(argument instanceof Float f){
            return getRoot().createFloatLiteral(f);
        } else if(argument instanceof Double d){
            return getRoot().createDoubleLiteral(d);
        } else if(argument instanceof Byte b){
            return getRoot().createByteLiteral( b);
        } else if(argument instanceof Short s){
            return getRoot().createShortLiteral(s);
        } else if(argument instanceof Integer i) {
            if (typeCode == TypeCode.INT) {
                return getRoot().createIntLiteral(i);
            } else if (typeCode == TypeCode.CLASS_REF) {
                String className = classLoader.getStrings().get(i);
                return mapClass(classLoader.getClass(className)).toClassRefLiteral();
            }
        } else if(argument instanceof ClassRefValue classRefValue) {
            return mapClass(classLoader.getClass(classRefValue.className())).toClassRefLiteral();
        } else if(argument instanceof AgoClass agoClass){
            return mapClass(agoClass).toClassRefLiteral();
        } else if(argument instanceof Long l){
            return getRoot().createLongLiteral( l);
        } else if(argument == null){
            return root.createNullLiteral();
        }
        throw new RuntimeException("unexpected type " + argument);
    }

    private ClassDef mapClass(ClassDef ownerClass, AgoClass agoClass, TypeCode typeCode) throws CompilationError {
        if(typeCode != TypeCode.OBJECT && typeCode != TypeCode.UNION){
            if(typeCode.isGeneric()){
                ClassDef r = mapClass(agoClass);
                if(r == null) throw new NullPointerException("class not found");
                return r;
            } else {
                return root.fromPrimitiveTypeCode(typeCode);
            }
        } else {
            ClassDef r = mapClass(agoClass);
            if(r == null) throw new NullPointerException("class not found");
            return r;
        }
    }

    private boolean parseFields(AgoClass agoClass, ClassDef classDef) throws CompilationError {
        if(classDef.getCompilingStage() != CompilingStage.ParseFields) return true;

        if(classDef.isInGenericInstantiation()){
            return parseFieldsForGenericInstantiation(classDef);
        }

        // validate all classes are ready
        for (AgoField field : agoClass.getFields()) {
            ClassDef c = mapClass(classDef, field.getAgoClass(), field.getTypeCode());
            if (c == null)
                return false;
        }
        if(agoClass instanceof AgoFunction fun) {
            for (AgoParameter agoParameter : fun.getParameters()) {
                ClassDef c = mapClass(classDef, agoParameter.getAgoClass(), agoParameter.getTypeCode());
                if (c == null)
                    return false;
            }
            if(fun.getVariables() != null) {
                for (AgoVariable variable : fun.getVariables()) {
                    ClassDef c = mapClass(classDef, variable.getAgoClass(), variable.getTypeCode());
                    if(c == null) return false;
                }
            }
        }
        var slots = agoClass.getSlotDefs();
        for (var slot : slots) {
            ClassDef slotClass = mapClass(classDef, slot.getAgoClass(), slot.getTypeCode());
            if (slotClass == null) return false;
        }

        if(agoClass.getFields() != null) {
            for (AgoField field : agoClass.getFields()) {
                Field f = new Field(classDef, field.getName(), null);
                ClassDef c = mapClass(classDef, field.getAgoClass(), field.getTypeCode());
                f.setType(c);
                f.setSourceLocation(field.getSourceLocation());
                registerConcreteType(classDef,c);
                f.setModifiers(field.getModifiers());
                classDef.addField(f);
                f.setSlotIndex(field.getSlotIndex());
                if(field.getConstLiteralValue() != null) {
                    f.setConstLiteralValue(objectToLiteral(field.getConstLiteralValue(), field.getTypeCode()));
                }
            }
        }
        if(agoClass instanceof AgoFunction fun){
            FunctionDef functionDef = (FunctionDef) classDef;
            ClassDef resultType = mapClass(classDef, fun.getResultClass(), fun.getResultTypeCode());
            if(resultType == null) return false;
            functionDef.setResultType(resultType);
            registerConcreteType(classDef, resultType);
            if(fun.getParameters() != null) {
                for (AgoParameter agoParameter : fun.getParameters()) {
                    Parameter p = new Parameter(functionDef, agoParameter.getName(), null);
                    p.setModifiers(agoParameter.getModifiers());
                    ClassDef c = mapClass(classDef, agoParameter.getAgoClass(), agoParameter.getTypeCode());
                    if(c == null) return false;
                    registerConcreteType(classDef,c);
                    p.setType(c);
                    p.setSourceLocation(agoParameter.getSourceLocation());
                    functionDef.addParameter(p);
                    p.setSlotIndex(agoParameter.getSlotIndex());
                }
            }
            if(fun.getVariables() != null) {
                for (AgoVariable variable : fun.getVariables()) {
                    Variable v = new Variable();
                    v.setName(variable.getName());
                    v.setModifiers(variable.getModifiers());
                    ClassDef c = mapClass(classDef, variable.getAgoClass(), variable.getTypeCode());
                    if(c == null) return false;
                    registerConcreteType(classDef,c);
                    v.setType(c);
                    v.setOwnerClass(functionDef);
                    v.setSourceLocation(variable.getSourceLocation());
                    functionDef.addLocalVariable(v);
                    v.setSlotIndex(variable.getSlotIndex());
                }
            }
            functionDef.setBody(fun.getCode());     // for AgoFunction it's the compiled code, just make the body fulfilled
        }
        classDef.setCompilingStage(CompilingStage.InheritsInnerClasses);
        return true;
    }

    private boolean parseFieldsForGenericInstantiation(ClassDef classDef) throws CompilationError {
        var b = classDef.parseFields();
        if(!b) return false;
        for (ClassDef child : classDef.getDirectChildren()) {
            b = parseFieldsForGenericInstantiation(child);
            if(!b) return false;
        }
        return true;
    }

    private void registerConcreteType(ClassDef owner, ClassDef refType) {
        if(refType instanceof ConcreteType concreteType){
            owner.registerConcreteType(concreteType);
        }
    }

    private ClassDef createClassDefFromAgoClass(AgoClass agoClass) throws CompilationError {
        if(agoClass == null) return null;
        var existed = classes.get(agoClass);
        if(existed != null) return classes.get(agoClass);

        var r = root.findByFullname(agoClass.getFullname());
        if(r instanceof ClassDef c) {
            classes.put(agoClass, c);       // include AgoNullClass
            return c;
        }

        if(LOGGER.isDebugEnabled()) LOGGER.debug("create class def " + agoClass.getFullname());

        if(agoClass instanceof AgoPrimitiveClass agoPrimitiveClass){
            var classDef = switch (agoPrimitiveClass.getTypeCode().value) {
                case TypeCode.BOOLEAN_VALUE -> new PrimitiveClassDef(root, TypeCode.BOOLEAN);
                case TypeCode.CHAR_VALUE -> new PrimitiveClassDef(root, TypeCode.CHAR);
                case TypeCode.SHORT_VALUE -> new PrimitiveClassDef(root, TypeCode.SHORT);
                case TypeCode.INT_VALUE -> new PrimitiveClassDef(root, TypeCode.INT);
                case TypeCode.LONG_VALUE -> new PrimitiveClassDef(root, TypeCode.LONG);
                case TypeCode.DOUBLE_VALUE -> new PrimitiveClassDef(root, TypeCode.DOUBLE);
                case TypeCode.DECIMAL_VALUE -> new PrimitiveClassDef(root, TypeCode.DECIMAL);
                case TypeCode.FLOAT_VALUE -> new PrimitiveClassDef(root, TypeCode.FLOAT);
                case TypeCode.STRING_VALUE -> new PrimitiveClassDef(root, TypeCode.STRING);
                case TypeCode.BYTE_VALUE -> new PrimitiveClassDef(root, TypeCode.BYTE);
                case TypeCode.VOID_VALUE -> new PrimitiveClassDef(root, TypeCode.VOID);
                case TypeCode.CLASS_REF_VALUE -> new PrimitiveClassDef(root, TypeCode.CLASS_REF);
                default -> throw new RuntimeException("not supported type " + agoPrimitiveClass);
            };
            classDef.setSourceLocation(agoClass.getSourceLocation());
            Package defaultPackage = root.getDefaultPackage();
            defaultPackage.addChild(classDef);
            classes.put(agoClass, classDef);
            loadChildren(agoClass, classDef);
            return classDef;
        }

        if(agoClass instanceof MetaClass metaClass) {
            AgoClass instanceClass = metaClass.getInstanceClass();
            ClassDef instanceClassDef = mapClass(instanceClass);
            if(instanceClassDef == null) return null;
            var classDef = new MetaClassDef(root, instanceClassDef, instanceClass instanceof MetaClass ? 2 : 1, null);
            classDef.setSourceLocation(metaClass.getSourceLocation());
            instanceClassDef.getPackage().addChild(classDef);
            classes.put(agoClass, classDef);
            loadChildren(agoClass, classDef);
            classDef.setCompilingStage(CompilingStage.ResolveHierarchicalClasses);
            return classDef;
        }

        String upname = extractPackagePrefix(agoClass);
        var n = root.findByFullname(upname);
        ClassDef classDef;
        if(agoClass instanceof AgoFunction agoFunction){
            if(agoFunction.isConstructor()){
                classDef = new ConstructorDef(root, agoFunction.getModifiers(),  agoFunction.getName());
            } else {
                classDef = new FunctionDef(root, agoClass.getName(),  null);     // TODO top function
            }
            if(agoFunction instanceof AgoNativeFunction agoNativeFunction){
                ((FunctionDef)classDef).setNativeEntrance(agoNativeFunction.getNativeEntrance());
            }
        } else {
            classDef = new ClassDef(root, agoClass.getName());
            if(agoClass instanceof AgoInterface){
                classDef.setClassType(AgoClass.TYPE_INTERFACE);
            } else if(agoClass instanceof AgoTrait){
                classDef.setClassType(AgoClass.TYPE_TRAIT);
            } else if(agoClass instanceof AgoEnum agoEnum){
                classDef.setClassType(AgoClass.TYPE_ENUM);
                TypeCode primitiveType = agoEnum.getBasePrimitiveType();
                classDef.setEnumBasePrimitiveType(root.fromPrimitiveTypeCode(primitiveType));
                var values = new LinkedHashMap<String, Literal<?>>();
                for (Map.Entry<String, Object> entry : agoEnum.getEnumValues().entrySet()) {
                    values.put(entry.getKey(), objectToLiteral(entry.getValue(),primitiveType));  // TODO source location
                }
                classDef.setEnumValues(values);
            }
        }
        classDef.setModifiers(agoClass.getModifiers());
        classDef.setSourceLocation(agoClass.getSourceLocation());
        classDef.setCompilingStage(agoClass.isGenericTemplate() ? CompilingStage.ParseGenericParams : CompilingStage.ResolveHierarchicalClasses);

        if(agoClass.getParent() != null){
            //
        } else if(n == null){
            Package pkg = root.createPackage(upname);
            pkg.addChild(classDef);
        } else {
            if(n instanceof Package pkg){
                pkg.addChild(classDef);
            } else {
                throw new RuntimeException("no parent found");
            }
        }
        classes.put(agoClass, classDef);

        loadChildren(agoClass, classDef);

        return classDef;
    }

    private void loadChildren(AgoClass agoClass, ClassDef classDef) throws CompilationError {
        for (AgoClass child : agoClass.getChildren()) {
            ClassDef c = mapClass(child);
            if(c != null) {
                classDef.addChild(c);
            }
        }
    }

    protected String extractPackagePrefix(AgoClass agoClass) {
        if(agoClass.getParent() != null) return extractPackagePrefix( agoClass.getParent());
        var s = agoClass.getFullname();
        if(s.length() == agoClass.getName().length()) return "";
        return s.substring(0, s.length() - agoClass.getName().length() - 1);
    }

    private boolean allocateSlots(AgoClass agoClass, ClassDef classDef) throws CompilationError {
        if(classDef.getCompilingStage() == CompilingStage.AllocateSlots) {
            ClassDef superClass = classDef.getSuperClass();
            if(superClass == classDef){
                classDef.setCompilingStage(CompilingStage.Compiled);
                return true;
            }
            if(superClass != null && superClass.getCompilingStage() == CompilingStage.AllocateSlots){
                if(!allocateSlots(classLoader.getClass(superClass.getFullname()), superClass)) {
                    return false;
                }
            }
            if(classDef.isInGenericInstantiation()){
                ClassDef templateClass = classDef.getTemplateClass();
                if(templateClass.getCompilingStage() == CompilingStage.AllocateSlots){
                    if(!allocateSlots(classLoader.getClass(templateClass.getFullname()),templateClass)){
                        return false;
                    }
                }
            }
            // allocate slots
            SlotsAllocator slotsAllocator = classDef.getSlotsAllocator();
            for (var slot : agoClass.getSlotDefs()) {
                ClassDef slotClass = mapClass(classDef, slot.getAgoClass(), slot.getTypeCode());
                var create = slotsAllocator.allocateSlot(slot.getName(), slotClass.getTypeCode(), slotClass);
                assert create.getIndex() == slot.getIndex();
            }

            // assign to variables
            for (Field field : classDef.getFields().values()) {
                if(field.getSlot() == null) {
                    field.setSlot(classDef.getSlotsAllocator().getSlot(field.getSlotIndex()));
                    field.getSlot().setVariable(field);
                } else {
                    assert field.getSlot().getIndex() == field.getSlotIndex();  // maybe inherited field
                }
            }
            if(classDef instanceof FunctionDef functionDef){
                for (Variable variable : functionDef.getLocalVariables().values()) {
                    variable.setSlot(classDef.getSlotsAllocator().getSlot(variable.getSlotIndex()));
                    variable.getSlot().setVariable(variable);
                }
            }

            classDef.setCompilingStage(CompilingStage.Compiled);
        }
        return true;
    }

    private boolean inheritsFields(ClassDef classDef) throws CompilationError {
        if(classDef.getCompilingStage() == CompilingStage.InheritsFields) {
            classDef.inheritsFields();
            for (ClassDef child : classDef.getDirectChildren()) {
                inheritsFields(child);
            }
        }
        return true;
    }

    private boolean parseGenericParams(AgoClass agoClass, ClassDef templClass) throws CompilationError {
        ConcreteTypeInfo concreteTypeInfo = agoClass.getConcreteTypeInfo();
        if(concreteTypeInfo instanceof GenericTypeParametersInfo genericTypeParametersInfo){
            templClass.shiftToTemplate();
            TypeParamsContext typeParamsContext = templClass.getTypeParamsContext();
            for (var genericTypeCodeAvatar : genericTypeParametersInfo.getGenericParameters()) {
                var avatarInfo = GenericTypeCodeAvatarInfo.extract(genericTypeCodeAvatar);

                var sharedGenericTypeParameterClass = avatarInfo.getGenericParameterInfo();

                var gt = root.getGenericTypeParameter();
                // here we need the constructor of MetaClass of SharedGenericTypeParameterClassDef
                if(gt.getCompilingStage().lte(CompilingStage.ResolveHierarchicalClasses)){
                    resolveHierarchy(avatarInfo.genericParameter().getParameterizedBaseClass(), gt);
                }
                var avatar = root.getGenericTypeCodeAvatar();
                if(avatar.getCompilingStage().lte(CompilingStage.ResolveHierarchicalClasses)){
                    resolveHierarchy(genericTypeCodeAvatar.getParameterizedBaseClass(), avatar);
                }

                SharedGenericTypeParameterClassDef pc = ((ClassContainer) gt.getParent()).getOrCreateGenericTypeParameter(gt,
                        gt.getMetaClassDef().getConstructor(),
                        mapClass(sharedGenericTypeParameterClass.lBound()),
                        mapClass(sharedGenericTypeParameterClass.uBound()),
                        sharedGenericTypeParameterClass.variance(), null);

                templClass.getTypeParamsContext().createGenericTypeParam(avatarInfo.name(), pc, avatarInfo.index());
            }
            templClass.createTemplateDefaultGenericSource();
        }
        templClass.setCompilingStage(CompilingStage.ResolveHierarchicalClasses);
        return true;
    }

    private boolean inheritsInnerClasses(ClassDef classDef) throws CompilationError {
        if(classDef.getCompilingStage() != CompilingStage.InheritsInnerClasses) return true;
        classDef.inheritsChildClasses();
        classDef.setCompilingStage(CompilingStage.AllocateSlots);
        if(classDef.isInGenericInstantiation()){
            for (ClassDef child : classDef.getDirectChildren()) {
                inheritsInnerClasses(child);
            }
        }
        return true;
    }

    private boolean processLoadClassName(AgoClass agoClass, ClassDef classDef) throws CompilationError {
        if(classDef == null){
            if(mapClass(agoClass) == null){
                return false;
            }
        } else if(classDef.getCompilingStage() == CompilingStage.ParseClassName) {
            classDef.setCompilingStage(CompilingStage.ResolveHierarchicalClasses);
        }
        return true;
    }
}
