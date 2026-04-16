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

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.literal.*;
import org.siphonlab.ago.compiler.generic.GenericConcreteType;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.generic.ScopedClassIntervalClassDef;

import java.math.BigDecimal;
import java.util.*;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;
import static org.siphonlab.ago.TypeCode.OBJECT_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;
import static org.siphonlab.ago.TypeCode.VOID_VALUE;

public class Root extends Namespace<Package> {

    private ClassDef ANY_CLASS;
    private ClassDef PRIMITIVE_CLASS;
    private ClassDef OBJECT_CLASS;
    private ClassDef CLASS_CLASS;
    private ClassDef PRIMITIVE_NUMBER_CLASS;
    private ClassDef THROWABLE_CLASS;
    private ClassDef EXCEPTION_CLASS;
    private ClassDef RUNTIME_EXCEPTION_CLASS;

    private PrimitiveClassDef VOID;
    private PrimitiveClassDef BOOLEAN;
    private PrimitiveClassDef CHAR;
    private PrimitiveClassDef FLOAT;
    private PrimitiveClassDef DOUBLE;
    private PrimitiveClassDef DECIMAL;
    private PrimitiveClassDef BYTE;
    private PrimitiveClassDef SHORT;
    private PrimitiveClassDef INT;
    private PrimitiveClassDef LONG;
    private PrimitiveClassDef STRING;
    private PrimitiveClassDef CLASREF;
    private NullClassDef NULL;

    private ClassDef NUMBER_CLASS;
    private ClassDef INTEGER_CLASS;
    private ClassDef BYTE_CLASS;
    private ClassDef DOUBLE_CLASS;
    private ClassDef DECIMAL_CLASS;
    private ClassDef SHORT_CLASS;
    private ClassDef FLOAT_CLASS;
    private ClassDef LONG_CLASS;
    private ClassDef CHAR_CLASS;
    private ClassDef BOOLEAN_CLASS;
    private ClassDef STRING_CLASS;
    private ClassDef ARRAY_CLASS;
    private ClassDef ANY_ARRAY_CLASS;
    private ClassDef CLASS_REF_CLASS;
    private ClassDef CLASS_INTERVAL_CLASS;
    private ClassDef SCOPED_CLASS_INTERVAL_CLASS;
    private ClassDef GENERIC_TYPE_PARAMETER_CLASS;
    private ClassDef GENERIC_TYPE_CODE_AVATAR_CLASS;
    private ClassDef FUNCTION_CLASS;
    private ClassDef FUNCTION_INTERFACE_BASE_OF_ANY;
    private ClassDef NATIVE_FUNCTION_INTERFACE_BASE;
    private ClassDef ITERABLE_INTERFACE;
    private ClassDef ITERATOR_INTERFACE;
    private ClassDef KEY_VALUE_PAIR_CLASS;

    private ClassDef VIA_OBJECT_INTERFACE;
    private ClassDef FORK_CONTEXT_INTERFACE;

    private ClassDef READONLY_LIST_CLASS;
    private ClassDef ANY_READONLY_LIST_CLASS;
    private ClassDef READWRITE_LIST_CLASS;
    private ClassDef ANY_READWRITE_LIST_CLASS;
    private ClassDef LIST_CLASS;
    private ClassDef ANY_LIST_CLASS;
    private ClassDef COLLECTION_CLASS;
    private ClassDef ANY_COLLECTION_CLASS;

    private ClassDef RUN_SPACE_CLASS;

    private ClassDef READONLY_MAP_CLASS;
    private ClassDef ANY_READONLY_MAP_CLASS;
    private ClassDef READWRITE_MAP_CLASS;
    private ClassDef ANY_READWRITE_MAP_CLASS;
    private ClassDef MAP_CLASS;
    private ClassDef ANY_MAP_CLASS;
    private ClassDef HASH_MAP_CLASS;

    private Map<String, ArrayClassDef> knownArrayTypes = new HashMap<>();

    private CompilingStage compilingStage = CompilingStage.ParseClassName;

    private List<ClassDef> newFoundClasses = new ArrayList<>();

    // all classes and functions, sorted from hierarchy base to descendants
    private LinkedHashSet<ClassDef> sortedClassesAndFunctions = new LinkedHashSet<>();

    public Root() {
        super("");
        addNullClassDef();
    }

    private void addNullClassDef() {
        this.getDefaultPackage().addChild(NULL = new NullClassDef(this));
    }

    public Package createPackage(String packageName) {
        return new Package(packageName, this);
    }

    public synchronized ClassDef getObjectClass(){
        if(OBJECT_CLASS != null) return OBJECT_CLASS;
        return OBJECT_CLASS = findByFullname("lang.Object");
    }

    public synchronized ClassDef getClassClass(){
        if(CLASS_CLASS != null) return CLASS_CLASS;
        return CLASS_CLASS = findByFullname("lang.Class");
    }

    public synchronized ClassDef getThrowableClass(){
        if(THROWABLE_CLASS != null) return THROWABLE_CLASS;
        return THROWABLE_CLASS = findByFullname("lang.Throwable");
    }

    public synchronized ClassDef getRuntimeExceptionClass(){
        if(RUNTIME_EXCEPTION_CLASS != null) return RUNTIME_EXCEPTION_CLASS;
        return RUNTIME_EXCEPTION_CLASS = findByFullname("lang.RuntimeException");
    }

    public synchronized ClassDef getExceptionClass(){
        if(EXCEPTION_CLASS != null) return EXCEPTION_CLASS;
        return EXCEPTION_CLASS = findByFullname("lang.Exception");
    }

    public synchronized ClassDef getNumberClass(){
        if(NUMBER_CLASS != null) return NUMBER_CLASS;
        return NUMBER_CLASS = findByFullname("lang.Number");
    }

    public synchronized ClassDef getIntegerClass(){
        if(INTEGER_CLASS != null) return INTEGER_CLASS;
        return INTEGER_CLASS = findByFullname("lang.Integer");
    }

    public synchronized ClassDef getByteClass(){
        if(BYTE_CLASS != null) return BYTE_CLASS;
        return BYTE_CLASS = findByFullname("lang.Byte");
    }

    public synchronized ClassDef getDoubleClass(){
        if(DOUBLE_CLASS != null) return DOUBLE_CLASS;
        return DOUBLE_CLASS = findByFullname("lang.Double");
    }

    public synchronized ClassDef getShortClass(){
        if(SHORT_CLASS != null) return SHORT_CLASS;
        return SHORT_CLASS = findByFullname("lang.Short");
    }

    public synchronized ClassDef getFloatClass(){
        if(FLOAT_CLASS != null) return FLOAT_CLASS;
        return FLOAT_CLASS = findByFullname("lang.Float");
    }

    public synchronized ClassDef getLongClass(){
        if(LONG_CLASS != null) return LONG_CLASS;
        return LONG_CLASS = findByFullname("lang.Long");
    }

    public synchronized ClassDef getCharClass(){
        if(CHAR_CLASS != null) return CHAR_CLASS;
        return CHAR_CLASS = findByFullname("lang.Char");
    }

    public synchronized ClassDef getStringClass(){
        if(STRING_CLASS != null) return STRING_CLASS;
        return STRING_CLASS = findByFullname("lang.String");
    }

    public synchronized ClassDef getBooleanClass(){
        if(BOOLEAN_CLASS != null) return BOOLEAN_CLASS;
        return BOOLEAN_CLASS = findByFullname("lang.Boolean");
    }

    public synchronized ClassDef getArrayClass(){
        if(ARRAY_CLASS != null) return ARRAY_CLASS;
        return ARRAY_CLASS = findByFullname("lang.Array");
    }

    public ClassDef getAnyArrayClass() {
        if (ANY_ARRAY_CLASS != null) return ANY_ARRAY_CLASS;
        try {
            return ANY_ARRAY_CLASS = getArrayClass().instantiate(new InstantiationArguments(getArrayClass().typeParamsContext, new ClassRefLiteral[]{this.getAnyClass().toClassRefLiteral()}), null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ClassDef getClassRefClass(){
        if(CLASS_REF_CLASS != null) return CLASS_REF_CLASS;
        return CLASS_REF_CLASS = findByFullname("lang.ClassRef");
    }

    public synchronized ClassDef getClassInterval(){
        if(CLASS_INTERVAL_CLASS != null) return CLASS_INTERVAL_CLASS;
        return CLASS_INTERVAL_CLASS = findByFullname("lang.ClassInterval");
    }

    public synchronized ClassDef getScopedClassInterval(){
        if(SCOPED_CLASS_INTERVAL_CLASS != null) return SCOPED_CLASS_INTERVAL_CLASS;
        return SCOPED_CLASS_INTERVAL_CLASS = findByFullname("lang.ScopedClassInterval");
    }

    public synchronized ClassDef getGenericTypeParameter(){
        if(GENERIC_TYPE_PARAMETER_CLASS != null) return GENERIC_TYPE_PARAMETER_CLASS;
        return GENERIC_TYPE_PARAMETER_CLASS = findByFullname("lang.GenericTypeParameter");
    }
    public synchronized ClassDef getGenericTypeCodeAvatar(){
        if(GENERIC_TYPE_CODE_AVATAR_CLASS != null) return GENERIC_TYPE_CODE_AVATAR_CLASS;
        return GENERIC_TYPE_CODE_AVATAR_CLASS = findByFullname("lang.GenericTypeCodeAvatar");
    }

    public synchronized ClassDef getAnyClass(){
        if(ANY_CLASS != null) return ANY_CLASS;
        return ANY_CLASS = findByFullname("lang.Any");
    }

    public synchronized ClassDef getFunctionInterface(int parameterCount){
        var interfaceName = "lang.Function%d".formatted(parameterCount);
        return findByFullname(interfaceName);
    }

    public synchronized ClassDef getFunctionBaseClass() {
        if(FUNCTION_CLASS != null) return FUNCTION_CLASS;
        return FUNCTION_CLASS = findByFullname("lang.Function");
    }

    public ClassDef getFunctionBaseOfAnyClass() {
        if (FUNCTION_INTERFACE_BASE_OF_ANY != null) return FUNCTION_INTERFACE_BASE_OF_ANY;
        ClassDef functionBaseClass = getFunctionBaseClass();
        try {
            return FUNCTION_INTERFACE_BASE_OF_ANY = functionBaseClass.instantiate(new InstantiationArguments(functionBaseClass.typeParamsContext,
                    new ClassRefLiteral[]{this.getAnyClass().toClassRefLiteral()}), null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }


    public synchronized ClassDef getNativeFunctionInterfaceBase() {
        if (NATIVE_FUNCTION_INTERFACE_BASE != null)
            return NATIVE_FUNCTION_INTERFACE_BASE;
        return NATIVE_FUNCTION_INTERFACE_BASE = findByFullname("lang.NativeFunction");
    }

    public CompilingStage getCompilingStage() {
        return compilingStage;
    }

    public void setCompilingStage(CompilingStage compilingStage) {
        if(!newFoundClasses.isEmpty()){
            throw new RuntimeException("classes %s need process yet".formatted(newFoundClasses));
        }
        this.compilingStage = compilingStage;
    }

    @Override
    public boolean appendDescendant(Namespace<?> child) {
        boolean b = super.appendDescendant(child);
        if(b && child instanceof ClassDef classDef){
            newFoundClasses.add(classDef);
        }
        return b;
    }

    public List<ClassDef> getAndCleanNewFoundClasses() {
        if(newFoundClasses.isEmpty()) return newFoundClasses;
        var old = newFoundClasses;
        newFoundClasses = new ArrayList<>();
        return old;
    }

    public ArrayClassDef getOrCreateArrayType(ClassDef elementType, MutableBoolean returnExisted) throws CompilationError {
        var name = ArrayClassDef.composeArrayTypeName(elementType);
        var existed = knownArrayTypes.get(name);
        if(existed != null){
            if(returnExisted != null) returnExisted.setTrue();
            return existed;
        }
        ArrayClassDef arrayClassDef = new ArrayClassDef(this, elementType);
        knownArrayTypes.put(name, arrayClassDef);
        if(this.getArrayClass() != null){
            this.ARRAY_CLASS.getParent().addChild(arrayClassDef);
        }
        if (this.getCompilingStage().getValue() > arrayClassDef.getCompilingStage().getValue()) {
            Compiler.processClassTillStage(arrayClassDef, this.getCompilingStage());
            Compiler.processClassTillStage(arrayClassDef.getMetaClassDef(), this.getCompilingStage());
        }
        return arrayClassDef;
    }

    public void sortClasses(){
        // create a sorted set which include all classes and functions
        List<ClassDef> sortedClassesAndFunctions = new ArrayList<>();
        // append enum at first
        for (Package pkg : this.getUniqueChildren()) {
            for (var classDef : pkg.getAllDescendants().getUniqueElements()) {
                if(classDef instanceof EnumDef) {
                    int pos = sortedClassesAndFunctions.size();
                    appendClassToSort(pos, (ClassDef) classDef, sortedClassesAndFunctions);
                }
            }
        }
        // others
        for (Package pkg : this.getUniqueChildren()) {
            for (var classDef : pkg.getAllDescendants().getUniqueElements()) {
                if (!(classDef instanceof EnumDef)) {
                    int pos = sortedClassesAndFunctions.size();
                    appendClassToSort(pos, (ClassDef) classDef, sortedClassesAndFunctions);
                }
            }
        }
        this.sortedClassesAndFunctions = new LinkedHashSet<>(sortedClassesAndFunctions);
    }

    private void appendClassToSort(int pos, ClassDef classDef, List<ClassDef> output){
        if(classDef instanceof ConcreteType concreteType){
            appendClassToSort(pos, concreteType, output);
            return;
        }
        for(var sp = classDef.getSuperClass(); sp != null; sp = sp.getSuperClass()){
            if(!output.contains(sp)){
                output.add(pos, sp);
            }
            if(sp.getSuperClass() == sp) break;
        }
        for (var interfaceDef : classDef.getInterfaces()) {
            appendClassToSort(pos, interfaceDef, output);
        }

        output.add(classDef);
    }

    private void appendClassToSort(int pos, ConcreteType concreteType, List<ClassDef> output) {
        if(concreteType instanceof GenericConcreteType genericConcreteType){
            appendClassToSort(pos, ((ClassDef) genericConcreteType).getTemplateClass(), output);
        } else if(concreteType instanceof ParameterizedClassDef parameterizedClassDef){
            appendClassToSort(pos, parameterizedClassDef.getBaseClass(), output);
        }
        output.add((ClassDef) concreteType);
    }

    public LinkedHashSet<ClassDef> getSortedClassesAndFunctions() {
        return sortedClassesAndFunctions;
    }

    public ClassDef getPrimitiveType() {
        if (PRIMITIVE_CLASS != null)
            return PRIMITIVE_CLASS;
        return PRIMITIVE_CLASS = findByFullname("lang.Primitive");
    }

    public ClassDef getPrimitiveNumberType() {
        if (PRIMITIVE_NUMBER_CLASS != null)
            return PRIMITIVE_NUMBER_CLASS;
        return PRIMITIVE_NUMBER_CLASS = findByFullname("lang.PrimitiveNumber");
    }

    public ClassDef getAnyIterableInterface() {
        if(ITERABLE_INTERFACE != null) return ITERABLE_INTERFACE;
        ClassDef iterableInterface = findByFullname("lang.Iterable");
        try {
            return ITERABLE_INTERFACE = iterableInterface.instantiate(new InstantiationArguments(iterableInterface.typeParamsContext,
                    new ClassRefLiteral[]{this.getAnyClass().toClassRefLiteral()}), null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }
    public ClassDef getAnyIteratorInterface() {
        if (ITERATOR_INTERFACE != null) return ITERATOR_INTERFACE;
        ClassDef iterator = findByFullname("lang.Iterator");
        try {
            return ITERATOR_INTERFACE = iterator.instantiate(new InstantiationArguments(iterator.typeParamsContext, new ClassRefLiteral[]{this.getAnyClass().toClassRefLiteral()}), null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public ClassDef getAnyKeyValuePairClass() {
        if(KEY_VALUE_PAIR_CLASS != null) return  KEY_VALUE_PAIR_CLASS;
        ClassDef keyValuePair = findByFullname("lang.KeyValuePair");
        try {
            return KEY_VALUE_PAIR_CLASS = keyValuePair.instantiate(new InstantiationArguments(
                    keyValuePair.typeParamsContext, new ClassRefLiteral[]{this.getAnyClass().toClassRefLiteral(), this.getAnyClass().toClassRefLiteral()}), null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public ClassDef getViaObjectInterface() {
        if (VIA_OBJECT_INTERFACE != null)
            return VIA_OBJECT_INTERFACE;
        return VIA_OBJECT_INTERFACE = findByFullname("lang.ViaObject");
    }

    public ClassDef getForkContextInterface() {
        if (FORK_CONTEXT_INTERFACE != null)
            return FORK_CONTEXT_INTERFACE;
        return FORK_CONTEXT_INTERFACE = findByFullname("lang.ForkContext");
    }

    public ScopedClassIntervalClassDef getOrCreateScopedClassInterval(ClassDef lBound, ClassDef uBound, MutableBoolean returnExisted) throws CompilationError {
        ClassDef baseClassDef = getScopedClassInterval();
        ConstructorDef constructor = baseClassDef.getMetaClassDef().getConstructor();
        return ((ClassContainer)baseClassDef.getParent()).getOrCreateScopedClassInterval(baseClassDef,constructor, lBound, uBound, returnExisted);
    }

    private List<ParameterizedClassDef.PlaceHolder> parameterizedClassDefPlaceHolders = new ArrayList<>();
    public void addParameterizedClassDefPlaceHolder(ParameterizedClassDef.PlaceHolder placeHolder) {
        parameterizedClassDefPlaceHolders.add(placeHolder);
    }

    public List<ParameterizedClassDef.PlaceHolder> getParameterizedClassDefPlaceHolders() {
        return parameterizedClassDefPlaceHolders;
    }

    public synchronized ClassDef getReadonlyListClass(){
        if(READONLY_LIST_CLASS != null) return READONLY_LIST_CLASS;
        return READONLY_LIST_CLASS = findByFullname("lang.ReadOnlyList");
    }

    public ClassDef getAnyReadonlyList() {
        if (ANY_READONLY_LIST_CLASS != null) return ANY_READONLY_LIST_CLASS;
        try {
            return ANY_READONLY_LIST_CLASS = getReadonlyListClass().instantiate(
                    new InstantiationArguments(READONLY_LIST_CLASS.typeParamsContext,
                            new ClassRefLiteral[]{this.getAnyClass().toClassRefLiteral()}),
                    null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ClassDef getReadwriteListClass(){
        if(READWRITE_LIST_CLASS != null) return READWRITE_LIST_CLASS;
        return READWRITE_LIST_CLASS = findByFullname("lang.ReadWriteList");
    }

    public ClassDef getAnyReadwriteList() {
        if (ANY_READWRITE_LIST_CLASS != null) return ANY_READWRITE_LIST_CLASS;
        try {
            return ANY_READWRITE_LIST_CLASS = getReadwriteListClass().instantiate(
                    new InstantiationArguments(READWRITE_LIST_CLASS.typeParamsContext,
                            new ClassRefLiteral[]{this.getAnyClass().toClassRefLiteral()}),
                    null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ClassDef getReadonlyMapClass() {
        if (READONLY_MAP_CLASS != null) return READONLY_MAP_CLASS;
        return READONLY_MAP_CLASS = findByFullname("lang.ReadOnlyMap");
    }

    public ClassDef getAnyReadonlyMap() {
        if (ANY_READONLY_MAP_CLASS != null) return ANY_READONLY_MAP_CLASS;
        try {
            return ANY_READONLY_MAP_CLASS = getReadonlyMapClass().instantiate(
                    new InstantiationArguments(READONLY_MAP_CLASS.typeParamsContext,
                            new ClassRefLiteral[]{
                                    this.getAnyClass().toClassRefLiteral(),   // K
                                    this.getAnyClass().toClassRefLiteral()    // V
                            }),
                    null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ClassDef getReadwriteMapClass() {
        if (READWRITE_MAP_CLASS != null) return READWRITE_MAP_CLASS;
        return READWRITE_MAP_CLASS = findByFullname("lang.ReadWriteMap");
    }

    public ClassDef getAnyReadwriteMap() {
        if (ANY_READWRITE_MAP_CLASS != null) return ANY_READWRITE_MAP_CLASS;
        try {
            return ANY_READWRITE_MAP_CLASS = getReadwriteMapClass().instantiate(
                    new InstantiationArguments(READWRITE_MAP_CLASS.typeParamsContext,
                            new ClassRefLiteral[]{
                                    this.getAnyClass().toClassRefLiteral(),   // K
                                    this.getAnyClass().toClassRefLiteral()    // V
                            }),
                    null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ClassDef getListClass(){
        if(LIST_CLASS != null) return LIST_CLASS;
        return LIST_CLASS = findByFullname("lang.List");
    }

    public ClassDef getAnyListClass() {
        if (ANY_LIST_CLASS != null) return ANY_LIST_CLASS;
        try {
            return ANY_LIST_CLASS = getListClass().instantiate(
                    new InstantiationArguments(LIST_CLASS.typeParamsContext,
                            new ClassRefLiteral[]{this.getAnyClass().toClassRefLiteral()}),
                    null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ClassDef getCollectionClass(){
        if(COLLECTION_CLASS != null) return COLLECTION_CLASS;
        return COLLECTION_CLASS = findByFullname("lang.Collection");
    }

    public ClassDef getAnyCollectionClass() {
        if (ANY_COLLECTION_CLASS != null) return ANY_COLLECTION_CLASS;
        try {
            return ANY_COLLECTION_CLASS = getCollectionClass().instantiate(
                    new InstantiationArguments(COLLECTION_CLASS.typeParamsContext,
                            new ClassRefLiteral[]{this.getAnyClass().toClassRefLiteral()}),
                    null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }


    public synchronized ClassDef getMapClass() {
        if (MAP_CLASS != null) return MAP_CLASS;
        return MAP_CLASS = findByFullname("lang.Map");
    }

    public ClassDef getAnyMapClass() {
        if (ANY_MAP_CLASS != null) return ANY_MAP_CLASS;
        try {
            return ANY_MAP_CLASS = getMapClass().instantiate(
                    new InstantiationArguments(MAP_CLASS.typeParamsContext,
                            new ClassRefLiteral[]{
                                    this.getAnyClass().toClassRefLiteral(),   // K
                                    this.getAnyClass().toClassRefLiteral()    // V
                            }),
                    null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ClassDef getHashMapClass() {
        if (HASH_MAP_CLASS != null) return HASH_MAP_CLASS;
        return HASH_MAP_CLASS = findByFullname("lang.HashMap");
    }

    public void resolveLangClasses() {
        if(this.ANY_CLASS == null) this.ANY_CLASS = findByFullname("lang.Any");
        if(this.OBJECT_CLASS == null) this.OBJECT_CLASS = findByFullname("lang.Object");

        if(this.PRIMITIVE_CLASS == null) this.PRIMITIVE_CLASS = findByFullname("lang.Primitive");
        if(this.PRIMITIVE_NUMBER_CLASS == null) this.PRIMITIVE_NUMBER_CLASS = findByFullname("lang.PrimitiveNumber");
        if(this.VOID == null) this.VOID = findByFullname("void");
        if(this.CHAR == null) this.CHAR = findByFullname("char");
        if(this.INT == null) this.INT = findByFullname("int");
        if(this.LONG == null) this.LONG = findByFullname("long");
        if(this.BYTE == null) this.BYTE = findByFullname("byte");
        if(this.SHORT == null) this.SHORT = findByFullname("short");
        if(this.STRING == null) this.STRING = findByFullname("string");
        if(this.FLOAT == null) this.FLOAT = findByFullname("float");
        if(this.BOOLEAN == null) this.BOOLEAN = findByFullname("boolean");
        if(this.DOUBLE == null) this.DOUBLE = findByFullname("double");
        if(this.DECIMAL == null) this.DECIMAL = findByFullname("decimal");
        if(this.CLASREF == null) this.CLASREF = findByFullname("classref");

        if(this.CLASS_CLASS == null) this.CLASS_CLASS = findByFullname("lang.Class");
        if(this.CLASS_REF_CLASS == null) this.CLASS_REF_CLASS = findByFullname("lang.ClassRef");
        if(this.CLASS_INTERVAL_CLASS == null) this.CLASS_INTERVAL_CLASS = findByFullname("lang.ClassInterval");
        if(this.SCOPED_CLASS_INTERVAL_CLASS == null) this.SCOPED_CLASS_INTERVAL_CLASS = findByFullname("lang.ScopedClassInterval");
        if(this.GENERIC_TYPE_PARAMETER_CLASS == null) this.GENERIC_TYPE_PARAMETER_CLASS = findByFullname("lang.GenericTypeParameter");
        if(this.THROWABLE_CLASS == null) this.THROWABLE_CLASS = findByFullname("lang.Throwable");
        if(this.FUNCTION_CLASS == null) this.FUNCTION_CLASS = findByFullname("lang.Function");
        if(this.RUN_SPACE_CLASS == null) this.RUN_SPACE_CLASS = findByFullname("lang.RunSpace");

        if(this.INTEGER_CLASS == null) this.INTEGER_CLASS = findByFullname("lang.Integer");
        if(this.LONG_CLASS == null) this.LONG_CLASS = findByFullname("lang.Long");
        if(this.BYTE_CLASS == null) this.BYTE_CLASS = findByFullname("lang.Byte");
        if(this.CHAR_CLASS == null) this.CHAR_CLASS = findByFullname("lang.Char");
        if(this.SHORT_CLASS == null) this.SHORT_CLASS = findByFullname("lang.Short");
        if(this.STRING_CLASS == null) this.STRING_CLASS = findByFullname("lang.String");
        if(this.BOOLEAN_CLASS == null) this.BOOLEAN_CLASS = findByFullname("lang.Boolean");
        if(this.FLOAT_CLASS == null) this.FLOAT_CLASS = findByFullname("lang.Float");
        if(this.DOUBLE_CLASS == null) this.DOUBLE_CLASS = findByFullname("lang.Double");

        if(this.ARRAY_CLASS == null) this.ARRAY_CLASS = findByFullname("lang.Array");

    }

    public PrimitiveClassDef VOID() {
        return VOID;
    }

    public PrimitiveClassDef INT() {
        return INT;
    }

    public PrimitiveClassDef CHAR() {
        return CHAR;
    }
    public PrimitiveClassDef LONG() {return  LONG;}
    public PrimitiveClassDef SHORT() {return SHORT;}
    public PrimitiveClassDef BYTE() {return BYTE;}
    public PrimitiveClassDef FLOAT() {return FLOAT;}
    public PrimitiveClassDef DOUBLE() {return DOUBLE;}
    public PrimitiveClassDef DECIMAL() {return DECIMAL;}
    public PrimitiveClassDef BOOLEAN() {return BOOLEAN;}
    public PrimitiveClassDef STRING() {return STRING;}
    public PrimitiveClassDef CLASSREF() {return CLASREF;}

    public PrimitiveClassDef fromPrimitiveTypeCode(TypeCode typeCode){
        if(typeCode == null)
            throw new RuntimeException("typeCode is null");
        return switch (typeCode.value) {
            case BYTE_VALUE -> BYTE;
            case SHORT_VALUE -> SHORT;
            case INT_VALUE -> INT;
            case LONG_VALUE -> LONG;
            case FLOAT_VALUE -> FLOAT;
            case DOUBLE_VALUE -> DOUBLE;
            case DECIMAL_VALUE -> DECIMAL;
            case CHAR_VALUE -> CHAR;
            case VOID_VALUE -> VOID;
            case BOOLEAN_VALUE -> BOOLEAN;
            case OBJECT_VALUE, UNION_VALUE -> throw new IllegalArgumentException("this class only handle primary type");
            case STRING_VALUE -> STRING;
            case CLASS_REF_VALUE -> this.CLASREF;

            default -> throw new IllegalStateException("Unexpected value: " + typeCode);
        };
    }

    public IntLiteral createIntLiteral(Integer value) {
        return new IntLiteral(this.INT, value);
    }

    public LongLiteral createLongLiteral(Long value) {
        return new LongLiteral(this.LONG, value);
    }

    public VoidLiteral createVoidLiteral() {
        return new VoidLiteral(this.VOID);
    }

    public BooleanLiteral createBooleanLiteral(Boolean value) {
        return new BooleanLiteral(this.BOOLEAN, value);
    }

    public CharLiteral createCharLiteral(Character value) {
        return new CharLiteral(this.CHAR, value);
    }

    public FloatLiteral createFloatLiteral(Float value) {
        return new FloatLiteral(this.FLOAT, value);
    }

    public DoubleLiteral createDoubleLiteral(Double value) {
        return new DoubleLiteral(this.DOUBLE, value);
    }

    public DecimalLiteral createDecimalLiteral(BigDecimal value) {
        return new DecimalLiteral(this.DECIMAL, value);
    }

    public ByteLiteral createByteLiteral(Byte value) {
        return new ByteLiteral(this.BYTE, value);
    }

    public ShortLiteral  createShortLiteral(Short value) {
        return new ShortLiteral(this.SHORT, value);
    }

    public StringLiteral createStringLiteral(String value) {
        return new StringLiteral(this.STRING, value);
    }

    public ClassRefLiteral createClassRefLiteral(ClassDef classRef){
        return new ClassRefLiteral(this.CLASREF, classRef);
    }

    public NullLiteral nullLiteral(){
        return new NullLiteral(this.NULL);
    }

    public Package getDefaultPackage() {
        Package defaultPackage = getChild("");
        if(defaultPackage == null) {
            return this.createPackage("");
        }
        return defaultPackage;
    }

    public NullClassDef NULL() {
        return NULL;
    }

    public NullableClassDef getOrCreateNullableType(ClassDef classDef, MutableBoolean returnExisted) throws CompilationError {
        var name = NullableClassDef.composeName(classDef.getFullname());

        var existed = this.findByFullname(name);
        if(existed != null){
            if(returnExisted != null) returnExisted.setTrue();
            return (NullableClassDef) existed;
        }
        var n = new NullableClassDef(this, classDef);
        classDef.getParent().addChild(n);
        if (this.getCompilingStage().getValue() > n.getCompilingStage().getValue()) {
            Compiler.processClassTillStage(n, this.getCompilingStage());
            Compiler.processClassTillStage(n.getMetaClassDef(), this.getCompilingStage());
        }
        return n;

    }
}
