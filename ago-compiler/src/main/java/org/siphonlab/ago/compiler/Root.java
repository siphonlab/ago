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
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;
import org.siphonlab.ago.compiler.generic.GenericConcreteType;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.generic.ScopedClassIntervalClassDef;

import java.util.*;

public class Root extends Namespace<Package> {

    private ClassDef OBJECT_CLASS;
    private ClassDef CLASS_CLASS;
    private ClassDef THROWABLE_CLASS;
    private ClassDef EXCEPTION_CLASS;
    private ClassDef RUNTIME_EXCEPTION_CLASS;
    private ClassDef NUMBER_CLASS;
    private ClassDef INTEGER_CLASS;
    private ClassDef BYTE_CLASS;
    private ClassDef DOUBLE_CLASS;
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
    private ClassDef ANY_CLASS;
    private ClassDef FUNCTION_INTERFACE_BASE;
    private ClassDef FUNCTION_INTERFACE_BASE_OF_ANY;
    private ClassDef NATIVE_FUNCTION_INTERFACE_BASE;
    private ClassDef PRIMITIVE_TYPE_INTERFACE;
    private ClassDef PRIMITIVE_NUMBER_TYPE_INTERFACE;
    private ClassDef ITERABLE_INTERFACE;
    private ClassDef ITERATOR_INTERFACE;
    private ClassDef VIA_OBJECT_INTERFACE;
    private ClassDef FORK_CONTEXT_INTERFACE;

    private ClassDef READONLY_LIST_CLASS;
    private ClassDef ANY_READONLY_LIST_CLASS;
    private ClassDef READWRITE_LIST_CLASS;
    private ClassDef ANY_READWRITE_LIST_CLASS;

    private ClassDef READONLY_MAP_CLASS;
    private ClassDef ANY_READONLY_MAP_CLASS;
    private ClassDef READWRITE_MAP_CLASS;
    private ClassDef ANY_READWRITE_MAP_CLASS;

    private ClassDef NULL_CLASS = new ClassDef(TypeCode.NULL.toString()) {
        {
            setCompilingStage(CompilingStage.Compiled);
        }

        @Override
        public TypeCode getTypeCode() {
            return TypeCode.NULL;
        }

        @Override
        public Root getRoot() {
            return Root.this;
        }
    };

    private Map<String, ArrayClassDef> knownArrayTypes = new HashMap<>();

    private CompilingStage compilingStage = CompilingStage.ParseClassName;

    private List<ClassDef> newFoundClasses = new ArrayList<>();

    // all classes and functions, sorted from hierarchy base to descendants
    private LinkedHashSet<ClassDef> sortedClassesAndFunctions = new LinkedHashSet<>();

    public Root() {
        super("");
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
            return ANY_ARRAY_CLASS = getArrayClass().instantiate(new InstantiationArguments(getArrayClass().typeParamsContext, new ClassRefLiteral[]{new ClassRefLiteral(this.getAnyClass())}), null);
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

    public synchronized ClassDef getAnyClass(){
        if(ANY_CLASS != null) return ANY_CLASS;
        return ANY_CLASS = findByFullname("lang.Any");
    }

    public synchronized ClassDef getFunctionInterface(int parameterCount){
        var interfaceName = "lang.Function%d".formatted(parameterCount);
        return findByFullname(interfaceName);
    }

    public synchronized ClassDef getFunctionBaseClass() {
        if(FUNCTION_INTERFACE_BASE != null) return FUNCTION_INTERFACE_BASE;
        return FUNCTION_INTERFACE_BASE = findByFullname("lang.Function");
    }

    public ClassDef getFunctionBaseOfAnyClass() {
        if (FUNCTION_INTERFACE_BASE_OF_ANY != null) return FUNCTION_INTERFACE_BASE_OF_ANY;
        ClassDef functionBaseClass = getFunctionBaseClass();
        try {
            return FUNCTION_INTERFACE_BASE_OF_ANY = functionBaseClass.instantiate(new InstantiationArguments(functionBaseClass.typeParamsContext,
                    new ClassRefLiteral[]{new ClassRefLiteral(this.getAnyClass())}), null);
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

    public ClassDef getPrimitiveTypeInterface() {
        if (PRIMITIVE_TYPE_INTERFACE != null)
            return PRIMITIVE_TYPE_INTERFACE;
        return PRIMITIVE_TYPE_INTERFACE = findByFullname("lang.Primitive");
    }

    public ClassDef getPrimitiveNumberTypeInterface() {
        if (PRIMITIVE_NUMBER_TYPE_INTERFACE != null)
            return PRIMITIVE_NUMBER_TYPE_INTERFACE;
        return PRIMITIVE_NUMBER_TYPE_INTERFACE = findByFullname("lang.PrimitiveNumber");
    }

    public ClassDef nullClass(){
        return this.NULL_CLASS;
    }

    public NullLiteral createNullLiteral(){
        return new NullLiteral(this.nullClass());
    }

    public ClassDef getIterableInterface() {
        if(ITERABLE_INTERFACE != null) return ITERABLE_INTERFACE;
        ClassDef iterableInterface = findByFullname("lang.Iterable");
        try {
            return ITERABLE_INTERFACE = iterableInterface.instantiate(new InstantiationArguments(iterableInterface.typeParamsContext,
                    new ClassRefLiteral[]{new ClassRefLiteral(this.getAnyClass())}), null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }
    public ClassDef getIteratorInterface() {
        if (ITERATOR_INTERFACE != null) return ITERATOR_INTERFACE;
        ClassDef iterator = findByFullname("lang.Iterator");
        try {
            return ITERATOR_INTERFACE = iterator.instantiate(new InstantiationArguments(iterator.typeParamsContext, new ClassRefLiteral[]{new ClassRefLiteral(this.getAnyClass())}), null);
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
        return ((ClassContainer)baseClassDef.getParent()).getOrCreateScopedClassInterval(baseClassDef,constructor, new Literal[]{new ClassRefLiteral(lBound), new ClassRefLiteral(uBound)}, returnExisted);
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
                            new ClassRefLiteral[]{new ClassRefLiteral(this.getAnyClass())}),
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
                            new ClassRefLiteral[]{new ClassRefLiteral(this.getAnyClass())}),
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
                                    new ClassRefLiteral(this.getAnyClass()),   // K
                                    new ClassRefLiteral(this.getAnyClass())    // V
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
                                    new ClassRefLiteral(this.getAnyClass()),   // K
                                    new ClassRefLiteral(this.getAnyClass())    // V
                            }),
                    null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }



}
