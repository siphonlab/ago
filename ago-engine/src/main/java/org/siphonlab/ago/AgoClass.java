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
package org.siphonlab.ago;


import org.agrona.collections.Int2ObjectHashMap;
import org.siphonlab.ago.classloader.AgoClassLoader;

import java.util.*;

public class AgoClass extends Instance<MetaClass>{

    public static final byte TYPE_METACLASS = 1;
    public static final byte TYPE_CLASS = 2;
    public static final byte TYPE_INTERFACE = 3;
    public static final byte TYPE_ENUM = 4;
    public static final byte TYPE_FUNCTION = 5;
    public static final byte TYPE_TRAIT = 6;

    public static final int VISIBILITY_MASK  = 0x0000_0004;
    public static final int PUBLIC           = 0x0000_0001;
    public static final int PRIVATE          = 0x0000_0002;
    public static final int PROTECTED        = 0x0000_0004;
    public static final int STATIC           = 0x0000_0008;
    public static final int FINAL            = 0x0000_0010;
    public static final int SYNCHRONIZED     = 0x0000_0020;
    public static final int VOLATILE         = 0x0000_0040;
    public static final int TRANSIENT        = 0x0000_0080;
    public static final int NATIVE           = 0x0000_0100;
    public static final int INTERFACE        = 0x0000_0200;
    public static final int ABSTRACT         = 0x0000_0400;
    public static final int STRICT           = 0x0000_0800;
    public static final int PARAMETER        = 0x0000_1000;
    public static final int FIELD_PARAM      = 0x0000_2000;
    public static final int THIS_PARAM       = 0x0000_4000;
    public static final int OVERRIDE         = 0x0000_8000;
    public static final int GETTER           = 0x0001_0000;
    public static final int SETTER           = 0x0002_0000;
    public static final int EMPTY_METHOD     = 0x0004_0000;      // empty method
    public static final int CONSTRUCTOR      = 0x0008_0000;      // this method is constructor
    public static final int VAR_ARGS         = 0x0010_0000;      // the last argument is varargs

    public static final int GENERIC_TEMPLATE       = 0x0020_0000;     // this class/function is generic template
    public static final int GENERIC_TEMPLATE_NEG   = 0b1111_1111_1101_1111_1111_1111_1111_1111;
    public static final int GENERIC_INSTANTIATION  = 0x0040_0000;     // this class/function come from a template class/function

    public static final int NULLABLE = 0x0080_0000;     // infer a type is Nullable, it equals Nullable<Type>()
    protected byte type;

    protected int classId;
    protected final String fullname;
    protected final String name;
    protected SlotsCreator slotsCreator;
    public AgoField[] fields;
    public AgoSlotDef[] slotDefs;

    protected int modifiers;
    private AgoClass superClass;
    private AgoClass[] interfaces;
    private AgoClass[] children;
    private AgoFunction[] methods;
    private AgoClass parent;

    private AgoClass permitClass;

    private AgoClass parameterizedBaseClass;
    private ConcreteTypeInfo concreteTypeInfo;

    // interface id -> interface method id -> method id in me
    private Int2ObjectHashMap<int[]> interfacesMethods;
    private SourceLocation sourceLocation;

    private AgoClassLoader classLoader;

    public void setSuperClass(AgoClass superClass) {
        this.superClass = superClass;
    }

    public AgoClass getSuperClass() {
        return superClass;
    }

    public void setInterfaces(AgoClass[] interfaces) {
        this.interfaces = interfaces;
    }

    public AgoClass[] getInterfaces() {
        return interfaces;
    }

    public void setChildren(AgoClass[] children) {
        this.children = children;
    }

    public AgoClass[] getChildren() {
        return children;
    }

    public void setParent(AgoClass parent) {
        this.parent = parent;
    }

    public AgoClass getParent() {
        return parent;
    }

    public AgoFunction getEmptyArgsConstructor() {
        if(this.children == null) return null;
        for (AgoClass c : this.children) {
            if(c instanceof AgoFunction function && function.isConstructor() &&
                    (function.getParameters() == null || function.getParameters().length == 0)){
                return function;
            }
        }
        return null;
    }

    public void setMethods(AgoFunction[] methods){
        this.methods = methods;
    }

    public AgoFunction getMethod(int methodIndex) {
        return methods[methodIndex];
    }

    public AgoFunction resolveMethodByInterface(int interfaceClassId, int methodIdInInterface){
        return methods[interfacesMethods.get(interfaceClassId)[methodIdInInterface]];
    }

    public void setParameterizedBaseClass(AgoClass parameterizedBaseClass) {
        this.parameterizedBaseClass = parameterizedBaseClass;
    }

    public AgoClass getParameterizedBaseClass() {
        return parameterizedBaseClass;
    }

    public Int2ObjectHashMap<int[]> getInterfacesMethods() {
        return interfacesMethods;
    }

    public void setInterfacesMethods(Int2ObjectHashMap<int[]> interfacesMethods) {
        this.interfacesMethods = interfacesMethods;
    }

    public boolean isThatOrDerivedFrom(AgoClass anotherClass) {
        if(anotherClass == null) return false;
        return anotherClass.isThatOrSuperOfThat(this);
    }

    public boolean isDeriveFrom(AgoClass maybeSuperClass){
        if(maybeSuperClass == null)
            return false;

        return !maybeSuperClass.equals(this) && maybeSuperClass.isThatOrSuperOfThat(this);
    }

    public boolean isThatOrSuperOfThat(AgoClass anotherClass){
        return asThatOrSuperOfThat(anotherClass) != null;
    }

    private boolean isThatOrSuperOfThat(AgoClass anotherClass, Set<AgoClass> visited){
        return asThatOrSuperOfThat(anotherClass, visited) != null;
    }

    public AgoClass asThatOrSuperOfThat(AgoClass anotherClass) {
        return asThatOrSuperOfThat(anotherClass, null);
    }

    // org.siphonlab.ago.compile.ClassDef.asThatOrSuperOfThat
    public AgoClass asThatOrSuperOfThat(AgoClass anotherClass, Set<AgoClass> visited){
        if(this.equals(anotherClass)) return anotherClass;

//        ClassDef anyClass = getRoot().getAnyClass();      // any class only works in ClassBound
        if(this.getSuperClass().equals(this)) return anotherClass;       // lang.Object

        if (visited != null) {
            if (visited.contains(anotherClass)) {
                return null;
            }
            visited.add(anotherClass);
        }

        if(anotherClass.isGenericInstantiation() && this.isGenericInstantiation()){        // Template -> GenericSource
            if(anotherClass.getConcreteTypeInfoAsGenericArguments().getTemplateClass() == this.getConcreteTypeInfoAsGenericArguments().getTemplateClass() && isTypeArgumentsMatch(anotherClass)){
                return anotherClass;
            }
        }

        // the value of ClassBound is classref, not an object
//        if(anotherClass instanceof ClassBound classBound){  // for class interval, we can only determine it by lBound
//            ClassDef lBound = classBound.getLBoundClass();
//            if(lBound == anyClass){
//                return anotherClass;
//            } else {
//                return this.asAssignableFrom(lBound);
//            }
//        }

        if(anotherClass.concreteTypeInfo instanceof ParameterizedClassInfo p){
            return this.asThatOrSuperOfThat(p.getParameterizedBaseClass(), visited);
        }

        // org.siphonlab.ago.compile.ArrayClassDef.asThatOrSuperOfThat
        if(this.concreteTypeInfo instanceof ArrayInfo arrayInfo){
            if(anotherClass.concreteTypeInfo instanceof ArrayInfo another){
                if(another.getElementType() == arrayInfo.getElementType()){
                    return anotherClass;
                }
            }
            return null;
        }

        if(this.concreteTypeInfo instanceof ParameterizedClassInfo parameterizedClassInfo){
            // ClassInterval, ScopedClassInterval, GenericTypeParameter
            if(ClassBound.isClassBound( this)){     // see org.siphonlab.ago.compile.generic.ClassIntervalClassDef.asThatOrSuperOfThat
                var lBound = ClassBound.getLBound(this);
                var uBound = ClassBound.getUBound(this);
                var any = classLoader.getLangClasses().getAnyClass();
                if(lBound.equals(any) || uBound.equals(any)) return anotherClass;

                if(ClassBound.isClassBound(anotherClass)){
                    var l2 = ClassBound.getLBound(anotherClass);
                    var u2 = ClassBound.getUBound(anotherClass);
                    return  (uBound == any || u2.isThatOrSuperOfThat(uBound, visited)) ? (lBound == any ? anotherClass : lBound.asThatOrSuperOfThat(l2, visited)) : null;
                }
                return (uBound == any || anotherClass.isThatOrSuperOfThat(uBound, visited)) ? (lBound == any ? anotherClass : lBound.asThatOrSuperOfThat(anotherClass, visited)) : null;
            }
            // org.siphonlab.ago.compile.ParameterizedClassDef.asThatOrSuperOfThat
            if (anotherClass.concreteTypeInfo instanceof ParameterizedClassInfo another) {
                if(another.getParameterizedBaseClass() == parameterizedClassInfo.getParameterizedBaseClass()){
                    return null;    // the arguments must be same, if same it matched (anotherClass == this)
                }
            }
        }

        if(anotherClass.superClass != null && anotherClass.superClass != anotherClass){    // solve derived class in recursive
            var sp = this.asThatOrSuperOfThat(anotherClass.superClass);
            if(sp != null) return sp;
        }
        if(anotherClass.isInterfaceOrTrait()){
            var permitClass = anotherClass.getPermitClass();
            var t = this.asThatOrSuperOfThat(permitClass, visited == null ? new LinkedHashSet<>() : visited);
            if(t != null) return t;
        }
        if(this.isInterfaceOrTrait()) {
            for (var implementedInterface : anotherClass.interfaces) {
                var i = this.asThatOrSuperOfThat(implementedInterface, visited == null ? new LinkedHashSet<>() : visited);
                if (i != null) {
                    return i;
                }
            }
        }
        return null;
    }

    // org.siphonlab.ago.compile.ClassDef.isTypeArgumentsMatch
    private boolean isTypeArgumentsMatch(AgoClass anotherClass) {
        var typeArgumentsArray = ((GenericArgumentsInfo)concreteTypeInfo).getArguments();
        var anotherArguments = ((GenericArgumentsInfo) anotherClass.concreteTypeInfo).getArguments();
        if(anotherArguments.length != typeArgumentsArray.length) return false;

        GenericTypeParametersInfo paramsContext = (GenericTypeParametersInfo) (((GenericArgumentsInfo) concreteTypeInfo).getTemplateClass().concreteTypeInfo);
        var params = paramsContext.getGenericParameters();
        var any = classLoader.getLangClasses().getAnyClass();
        for (int i = 0; i < typeArgumentsArray.length; i++) {
            GenericParameterTypeInfo p = params[i];
            Variance variance = ClassBound.getVariance(p.getSharedGenericTypeParameterClass());        // SharedGenericTypeParameterClassDef
            var a1 = typeArgumentsArray[i].getAgoClass();
            if (any.equals(a1)) return true;
            var a2 = anotherArguments[i].getAgoClass();
            switch (variance){
                case Invariance:
                    if(!Objects.equals(a1, a2)) return false;
                    break;
                case Covariance:
                    if(!a1.isThatOrSuperOfThat(a2)) return false;
                    break;
                case Contravariance:
                    if(!a2.isThatOrSuperOfThat(a1)) return false;
                    break;
            }
        }
        return true;
    }

    public AgoClassLoader getClassLoader() {
        return classLoader;
    }

    private boolean isInterfaceOrTrait() {
        return this.type == AgoClass.TYPE_INTERFACE || this.type == AgoClass.TYPE_TRAIT;
    }


    public void setPermitClass(AgoClass permitClass) {
        this.permitClass = permitClass;
    }

    public AgoClass getPermitClass() {
        return permitClass;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public boolean isInGenericTemplate() {
        if(this.parent != null) {
            return parent.isGenericTemplate() || parent.isInGenericTemplate();
        }
        return false;
    }

    public static class DefaultSlots implements Slots{

    }

    public static class TraceOwnerSlots implements Slots{
        private final AgoClass agoClass;

        public TraceOwnerSlots(AgoClass agoClass){
            this.agoClass = agoClass;
        }
    }
    private static final Slots DEFAULT_SLOTS = new DefaultSlots();


    protected AgoClass(AgoClassLoader classLoader, String fullname, String name) {
        super( null);
        this.fullname = fullname;
        this.name = name;
        this.type = AgoClass.TYPE_CLASS;
        this.classLoader = classLoader;
    }

    public AgoClass(AgoClassLoader classLoader, MetaClass metaClass, String fullname, String name) {
        super(metaClass);
        this.fullname = fullname;
        this.name = name;
        this.type = AgoClass.TYPE_CLASS;
        this.classLoader = classLoader;
    }

    public ConcreteTypeInfo getConcreteTypeInfo() {
        return concreteTypeInfo;
    }

    public GenericArgumentsInfo getConcreteTypeInfoAsGenericArguments() {
        if(concreteTypeInfo == null) return null;
        return (GenericArgumentsInfo)concreteTypeInfo;
    }

    public void setConcreteTypeInfo(ConcreteTypeInfo concreteTypeInfo) {
        this.concreteTypeInfo = concreteTypeInfo;
    }

    public void initSlots(){
        if(agoClass != null && this.slots == null) this.slots = agoClass.createSlots();
    }

    public void setSlotsCreator(SlotsCreator slotsCreator) {
        this.slotsCreator = slotsCreator;
    }

    public SlotsCreator getSlotsCreator() {
        return slotsCreator;
    }

    public void setFields(AgoField[] fields) {
        this.fields = fields;
    }

    public AgoField[] getFields() {
        return fields;
    }

    public AgoFunction[] getMethods() {
        return methods;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    public int getModifiers() {
        return modifiers;
    }

    public byte getType() {
        return type;
    }

    public boolean isNative() {
        return (this.modifiers & AgoClass.NATIVE) !=0;
    }
    public boolean isStatic() {
        return (this.modifiers & AgoClass.STATIC) !=0;
    }

    public boolean isAbstract() {
        return (this.modifiers & AgoClass.ABSTRACT) !=0;
    }
    public boolean isFinal() {
        return (this.modifiers & AgoClass.FINAL) !=0;
    }
    public int getVisibility() {
        return this.modifiers & 0b111;
    }
    public boolean isPublic(){
        return (this.modifiers & AgoClass.PUBLIC) != 0;
    }
    public boolean isPrivate(){
        return (this.modifiers & AgoClass.PRIVATE) != 0;
    }
    public boolean isProtected(){
        return (this.modifiers & AgoClass.PROTECTED) != 0;
    }

    public boolean isGenericTemplate() {
        return (this.modifiers & AgoClass.GENERIC_TEMPLATE) !=0;
    }

    public boolean isGenericInstantiation() {
        return (this.modifiers & AgoClass.GENERIC_INSTANTIATION) !=0;
    }


    public String getFullname() {
        return fullname;
    }

    public String getName() {
        return name;
    }

    public Slots createSlots(){
        if(this.slotsCreator == null){
//            return DEFAULT_SLOTS;
            return new TraceOwnerSlots(this);
        } else {
            return this.slotsCreator.create();
        }
    }

    public AgoClass cloneWithScope(Instance<?> parentScope) {
        if(parentScope == this.parentScope) return this;
        var cls = new AgoClass(this.classLoader, this.agoClass, this.fullname, this.name);
        cls.setParentScope(parentScope);
        copyTo(cls);
        return cls;
    }

    protected void copyTo(AgoClass cls) {
        cls.slots = this.agoClass.createSlots();        // a new copy, must initial values with meta constructor
        cls.setSlotsCreator(this.getSlotsCreator());
        cls.setModifiers(this.modifiers);
        cls.setFields(this.fields);
        cls.setSlotDefs(this.slotDefs);
        cls.setInterfaces(this.interfaces);
        cls.setChildren(this.children);
        cls.setMethods(this.methods);
        cls.setParent(this.parent);
        cls.setPermitClass(this.permitClass);
        cls.setParameterizedBaseClass(this.parameterizedBaseClass);
        cls.setConcreteTypeInfo(this.concreteTypeInfo);
        cls.setInterfacesMethods(interfacesMethods);
        cls.setSourceLocation(sourceLocation);
        cls.setSuperClass(this.superClass);
        cls.setClassId(this.classId);
        cls.type = this.type;
    }

    @Override
    public String toString() {
        return this.getFullname();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AgoClass c && c.fullname.equals(this.fullname);
    }

    Map<String, AgoClass> childrenByName;
    Map<String, AgoField> fieldsByName;
    public AgoClass findChild(String name){
        if(childrenByName == null){
            childrenByName = new HashMap<>(children.length);
            for (AgoClass child : children) {
                childrenByName.put(child.name,child);
            }
        }
        return childrenByName.get(name);
    }

    public AgoField findField(String name) {
        if (fieldsByName == null) {
            fieldsByName = new HashMap<>(fields.length);
            for (AgoField field : this.fields) {
                fieldsByName.put(field.getName(),field);
            }
        }
        return fieldsByName.get(name);
    }

    Map<String, AgoFunction> methodsByName;
    public AgoFunction findMethod(String functionName) {
        if (methodsByName == null) {
            methodsByName = new HashMap<>(methods.length);
            for (AgoFunction function : methods) {
                if(function != null)
                    methodsByName.put(function.name, function);
            }
        }
        return methodsByName.get(functionName);
    }


    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public void setSlotDefs(AgoSlotDef[] agoSlotDefs) {
        this.slotDefs = agoSlotDefs;
    }

    public AgoSlotDef[] getSlotDefs() {
        return slotDefs;
    }
}
