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
package org.siphonlab.ago.classloader;

import org.agrona.collections.Int2ObjectHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.collection.IntSortedArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

import static org.siphonlab.ago.AgoClass.*;
import static org.siphonlab.ago.TypeCode.OBJECT;
import static org.siphonlab.ago.classloader.LoadingStage.*;

public class ClassHeader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassHeader.class);

    LoadingStage loadingStage = LoadClassNames;

    protected String fullname;
    protected String sourceFilename;
    final byte type;
    int modifiers;
    private final IoBuffer slice;
    protected final AgoClassLoader classLoader;
    public int functionResultSlot;
    protected ClassHeader parent;

    private String metaClass;
    String superClass;
    protected String[] interfaces;
    List<MethodDesc> methods;

    protected int classId;

    protected String[] strings;
    public int blobOffset = -1;
    protected String name;
    private int buffStart;
    private int buffEnd;

    public AgoClass agoClass;       // built class
    protected SlotDesc[] slotDescs;

    // function name -> id, not full name
    Map<String, Integer> nonPrivateFunctionIndexes = new HashMap<>();
    Map<String, MethodDesc> methodsByName = new HashMap<>();

    GenericTypeDesc[] genericTypeParamDescs;

    protected GenericSource genericSource;

    protected VariableDesc[] fields;
    public TypeDesc functionResultType;
    protected VariableDesc[] functionParams;
    String nativeFunctionEntrance;
    protected VariableDesc[] functionVariables;

    public SwitchTableDesc[] switchTables;
    public TryCatchItemDesc[] tryCatchItems;

    IoBuffer compiledCode;
    SourceMapEntry[] sourceMap;

    protected IntSortedArraySet handledInstructions = new IntSortedArraySet();
    private boolean bodyParsed = false;
    private String permitClass;
    private TypeCode enumBasePrimitiveType;
    private Map<String, Object> enumValues;
    private SourceLocation sourceLocation;
    private ClassHeader sourceHeader;

    public List<ClassHeader> getChildren() {
        return children;
    }

    public void setChildren(List<ClassHeader> children) {
        this.children = children;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(String[] interfaces) {
        this.interfaces = interfaces;
    }

    public void setMethods(List<MethodDesc> methods) {
        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s set methods to %s".formatted(this, methods));
        this.methods = new ArrayList<>(methods);
        this.methodsByName.clear();
        for (MethodDesc methodDesc : methods) {
            this.methodsByName.put(methodDesc.getName(), methodDesc);
        }
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    List<ClassHeader> children = new ArrayList<>();

    public ClassHeader(String fullname, byte type, int modifiers, IoBuffer slice, AgoClassLoader classLoader) {
        this.fullname = fullname;
        this.type = type;
        this.modifiers = modifiers;
        this.slice = slice;
        this.classLoader = classLoader;
    }

    public String fullname() {
        return fullname;
    }

    public byte type() {
        return type;
    }

    public int modifiers() {
        return modifiers;
    }

    public IoBuffer getSlice() {
        return slice;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public void setSlots(SlotDesc[] slotDescs) {
        this.slotDescs = slotDescs;
    }

    public SlotDesc[] getSlots() {
        return slotDescs;
    }

    public String getMetaClass() {
        return metaClass;
    }

    public void setMetaClass(String metaClass) {
        this.metaClass = metaClass;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setRange(int start, int end) {
        this.buffStart = start;
        this.buffEnd = end;
    }

    @Override
    public String toString() {
        return "(Header %s %s %s to %s)".formatted(this.fullname, this.classId, buffStart, buffEnd);
    }

    public void addChild(ClassHeader child){
        if(children.contains(child)) return;
        children.add(child);
        if(child.isFunction() && !this.methodsByName.containsKey(child.name)){
            this.addMethod(new MethodDesc(child.name, child.fullname));
        }
    }

    public int getBlobOffset() {
        return blobOffset == -1 ? this.parent.getBlobOffset() : this.blobOffset;
    }

    public boolean instantiateFunctionFamily(Map<String, ClassHeader> headers) {
        if (this.loadingStage != InstantiateFunctionFamily)
            return true;

        if(this.children != null){
            for (ClassHeader child : this.children) {
                if(child instanceof GenericInstantiationClassHeader && child.isFunction()){
                    child.instantiateFunctionFamily(headers);
                }
            }
        }

        this.nextStage();
        return true;
    }


    public boolean isInGenericTemplate(Map<String, ClassHeader> headers) {
        for (var h = this; h != null; h = h.parent) {
            if(h instanceof ParameterizedClassHeader p){
                h = headers.get(p.baseClass);
            }
            if ((h.modifiers & GENERIC_TEMPLATE) == GENERIC_TEMPLATE) {
                return true;
            } else if(h instanceof MetaClassHeader metaClassHeader && metaClassHeader.instanceClass instanceof ClassHeader i && i.isInGenericTemplate(headers)){
                return true;
            }
        }
        return false;
    }

    public boolean isGenericTemplate(){
        return (this.modifiers & GENERIC_TEMPLATE) == GENERIC_TEMPLATE;
    }

    public boolean isGenericInstantiation(){
        return (this.modifiers & GENERIC_INSTANTIATION) == GENERIC_INSTANTIATION;
    }

    public static boolean isGenericType(TypeDesc typeDesc, Map<String, ClassHeader> headers){
        if(typeDesc instanceof GenericTypeDesc) return true;
        if(typeDesc.getTypeCode() == OBJECT){
            var c = headers.get(typeDesc.getClassName());
            // concrete types
            return isGenericType(c,headers);
        }
        return false;
    }

    public static boolean isGenericType(ClassHeader header, Map<String, ClassHeader> headers){
        // concrete types
        if(header instanceof ArrayTypeHeader arrayTypeHeader){
            return isGenericType(arrayTypeHeader.elementType, headers);
        } else if(header instanceof GenericInstantiationClassHeader instantiationClassHeader){
            return instantiationClassHeader.isInGenericTemplate(headers);
        } else if(header instanceof ParameterizedClassHeader parameterizedClassHeader){
            return isGenericType(headers.get(parameterizedClassHeader.baseClass), headers);
        }
        return false;
    }

    public String[] loadStrings(){
        if(this.strings == null) return this.parent != null ? this.parent.loadStrings() : null;
        return this.strings;
    }

    public ClassHeader clone(ClassHeader newParent, Map<String, ClassHeader> headers){
        if (this.parent == null || !this.fullname.startsWith(this.parent.fullname)) {
            return this;
        }
        String fullname = newParent.fullname + '.' + this.name;
        var existed = headers.get(fullname);
        if(existed != null) return existed;

        var inst = new ClassHeader(fullname, this.type, this.modifiers, this.slice != null ? this.slice.slice() : null, this.classLoader);
        inst.setName(this.name);
        copyToClone(inst, headers);
        inst.parent = newParent;
        classLoader.registerNewClass(inst);
        return inst;
    }
    protected void copyToClone(ClassHeader inst, Map<String, ClassHeader> headers){
        inst.strings = this.strings;
        inst.genericTypeParamDescs = this.genericTypeParamDescs;
        inst.setSuperClass(this.superClass);
        inst.setPermitClass(this.permitClass);
        inst.setInterfaces(this.interfaces);
        inst.setMetaClass(this.metaClass);
        inst.setSlots(this.slotDescs);

        if(this.isFunction()) {
            inst.functionResultType = this.functionResultType;
            inst.functionParams = this.functionParams;
            inst.functionVariables = this.functionVariables;
            inst.nativeFunctionEntrance = this.nativeFunctionEntrance;
        }

        inst.genericTypeParamDescs = this.genericTypeParamDescs;
        inst.fields = this.fields;
        inst.genericSource = this.genericSource;
        inst.setLoadingStage(loadingStage);
        inst.setSourceHeader(this);
        if (this.children != null) {
            inst.setChildren(new ArrayList<>(this.children.stream().map(c -> c.clone(inst, headers)).toList()));
            inst.setMethods(mapMethods(this.methods, this.children, inst.children));
        }
    }

    // I am a child of template, I apply template and got an instantiated class
    public ClassHeader tryInstantiate(ClassHeader newParent, Map<String, ClassHeader> headers, GenericTypeArguments typeArguments) {
        if (this.parent != null && !this.fullname.startsWith(this.parent.fullname)) {  // inherited child
            return this;
        }
        if(!typeArguments.canApplyToTemplate(this, headers)){
            typeArguments.canApplyToTemplate(this, headers);
            return this;
        }
        var existed = this.findInstantiatedClass(typeArguments);
        if(existed != null) return existed;

        return instantiate(newParent, headers, typeArguments);
    }

    protected ClassHeader instantiate(ClassHeader newParent, Map<String, ClassHeader> headers, GenericTypeArguments typeArguments) {
        String name;
        String fullname;
        ClassHeader inst;

        if(this.isGenericTemplate() && typeArguments.sourceTemplate == this) {
            name = GenericInstantiationClassHeader.composeClassName(this.name, typeArguments);
            fullname = newParent == null ? extractPackagePrefix() + name : newParent.fullname + '.' + name;
            inst = new GenericInstantiationClassHeader(fullname, this.type, this, typeArguments, this.classLoader);
        } else if(this instanceof MetaClassHeader metaClassHeader){
            String[] names = GenericInstantiationClassHeader.composeMetaClassName(metaClassHeader.instanceClass, typeArguments, headers);
            name = names[0];
            fullname = names[1];
            inst = metaClassHeader.instantiateMetaClass(newParent, fullname, headers, typeArguments);
            if(LOGGER.isDebugEnabled()) LOGGER.debug("%s apply template and got inst %s".formatted(this.fullname, inst.fullname));
            inst.setName(name);
            return inst;
        } else {
            name = this.name;
            fullname = newParent == null ? extractPackagePrefix() + name : newParent.fullname + '.' + name;
            inst = new ClassHeader(fullname, this.type, this.modifiers, this.slice != null ? this.slice.slice() : null, this.classLoader);
        }
        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s apply template and got inst %s".formatted(this.fullname, inst.fullname));
        inst.setName(name);
        var r = this.applyInstantiation(inst, typeArguments, newParent, headers, classHeader -> {
            if (this.isGenericTemplate() && typeArguments.sourceTemplate == this) {
                String name2 = GenericInstantiationClassHeader.composeClassName(this.name, typeArguments);
                String fullname2 = newParent == null ? extractPackagePrefix() + name2 : newParent.fullname + '.' + name2;
                if(!name2.equals(classHeader.name)) {
                    classHeader.fullname = fullname2;
                    classHeader.name = name2;
                }
            } else if (this instanceof MetaClassHeader metaClassHeader) {
                //
            } else {
                //
            }
        });
        if(r == inst) {
            classLoader.registerNewClass(inst);
        } else {
            inst = r;   // use existed
        }

        if(inst instanceof GenericInstantiationClassHeader && this.isFunction() && newParent != null){
            newParent.registerFunctionInstantiation(inst, headers);
            instantiateFunctionFamily(newParent, inst, 1, headers, typeArguments);
        }

        return inst;
    }

    void registerFunctionInstantiation(ClassHeader inst, Map<String, ClassHeader> headers) {
        MethodDesc methodDesc = new MethodDesc(inst.name, inst.fullname);
        this.addMethod(methodDesc);
        assert this.findMethod(methodDesc, headers) != null;
    }

    private void addMethod(MethodDesc methodDesc) {
        if(this.methodsByName.containsKey(methodDesc.getName())) return;
        if (LOGGER.isDebugEnabled()) LOGGER.debug("%s add method %s".formatted(this, methodDesc));
        if(this.methods == null) this.methods = new ArrayList<>();
        this.methods.add(methodDesc);
        this.methodsByName.put(methodDesc.getName(), methodDesc);
    }

    void instantiateFunctionFamily(ClassHeader parent, ClassHeader instantiationOfThis, int depth, Map<String, ClassHeader> headers, GenericTypeArguments typeArguments) {
        if(StringUtils.isNotEmpty(parent.superClass)){
            ClassHeader superClass = headers.get(parent.superClass);
            if(superClass == parent) return;
            ClassHeader overriddenFunction = superClass.getSameSignatureFunction(this);
            if(overriddenFunction != null){
                overriddenFunction.tryInstantiate(superClass, headers, new GenericTypeArguments(overriddenFunction.getSourceTemplate(), typeArguments.getTypeArgumentsArray(), headers));
            }
            for (String interfaceName : parent.getInterfaces()) {
                var interface_ = headers.get(interfaceName);
                overriddenFunction = interface_.getSameSignatureFunction(this);
                if (overriddenFunction != null) {
                    overriddenFunction.tryInstantiate(superClass, headers, new GenericTypeArguments(overriddenFunction.getSourceTemplate(), typeArguments.getTypeArgumentsArray(), headers));
                }
            }
        }

        broadcastSubInstantiationMethod(parent, instantiationOfThis, depth, headers, typeArguments);

    }

    private void broadcastSubInstantiationMethod(ClassHeader parent, ClassHeader instantiationOfThis, int depth, Map<String, ClassHeader> headers, GenericTypeArguments typeArguments) {
        for (ClassHeader sub : headers.values()) {
            boolean isSub = false;
            if(StringUtils.equals(sub.getSuperClass(), parent.fullname)){
                isSub = true;
            } else {
                if (sub.interfaces != null && (parent.type == TYPE_INTERFACE || parent.type == TYPE_TRAIT)) {
                    for (String anInterface : sub.interfaces) {
                        if(StringUtils.equals(anInterface, parent.fullname)){
                            isSub = true;
                            break;
                        }
                    }
                }
            }
            if(isSub){
                var newFun = sub.getSameSignatureFunction(this);
                if(newFun != null && newFun != this){       // have a new(overridden) version, instantiate with the new version
                    newFun.tryInstantiate(sub, headers, typeArguments);
                } else {
                    newFun = instantiationOfThis;       // won't add child to sub, only save to methods
                    parent.addMethod(new MethodDesc(instantiationOfThis.name, instantiationOfThis.fullname));
                }
                broadcastSubInstantiationMethod(sub,newFun,depth + 1, headers,typeArguments);
            }
        }
    }

    private ClassHeader getSourceTemplate() {
        if(this.genericSource != null) return this.genericSource.sourceTemplate();
        return this;
    }

    private int getParamsLength(){
        if(functionParams == null) return 0;
        return functionParams.length;
    }

    boolean isSameSignatureWith(ClassHeader anotherFunction){
        if(anotherFunction.isFunction() && anotherFunction.getParamsLength() != this.getParamsLength()) return false;

        if(this.getParamsLength() == 0) return true;

        var anotherParameters = anotherFunction.functionParams;
        for (int i = 0; i < anotherParameters.length; i++) {
            var anotherParam = anotherParameters[i];
            var myParam = functionParams[i];
            if (!anotherParam.type.equals(this.functionParams[i].type) || !(myParam.type instanceof GenericTypeDesc && anotherParam.type instanceof GenericTypeDesc)){
                return false;
            }
        }
        return true;
    }

    String getCommonName(){
        if(this.isFunction()) {
            for (int i = 0; i < this.name.length(); i++) {
                char c = this.name.charAt(i);
                if (c == '#'){
                    return this.name.substring(0, i);
                } else if(c == '<'){
                    return this.name;       // even f#<int,double> return f
                }
            }
        }
        return name;
    }

    public ClassHeader getSameSignatureFunction(ClassHeader newFun){
        var it = children.stream().filter(c -> c.isFunction() && c.getCommonName().equals(newFun.getCommonName())).iterator();
        while (it.hasNext()) {
            ClassHeader c = it.next();
            if (c == newFun) continue;
            if (c instanceof ClassHeader existed) {
                if (newFun.isSameSignatureWith(existed))
                    return existed;
            }
        }
        return null;
    }


    public boolean isFunction() {
        return type == TYPE_FUNCTION;
    }


    protected void applyInstantiation(ClassHeader inst, GenericTypeArguments typeArguments, ClassHeader newParent, Map<String, ClassHeader> headers) {
        applyInstantiation(inst,typeArguments,newParent,headers, null);
    }
    // for child of template ClassHeader from org.siphonlab.ago.classloader.ClassHeader.instantiate
    // for template GenericInstantiationClassDef from GenericInstantiationClassHeader.PlaceHolder.resolve
    protected ClassHeader applyInstantiation(ClassHeader inst, GenericTypeArguments typeArguments, ClassHeader newParent, Map<String, ClassHeader> headers, Consumer<ClassHeader> callback) {
        if(this.genericSource != null){
            GenericTypeArguments mixInstantiationArgs = mixInstantiationArgs(typeArguments, headers);
            if(mixInstantiationArgs != null)
                return this.genericSource.sourceTemplate().applyInstantiation(inst, mixInstantiationArgs, newParent, headers, callback);
        }
        if(inst.name == null) inst.name = name;
        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s apply template to %s".formatted(this.fullname, inst.fullname));
        inst.genericSource = new GenericSource(this, typeArguments);
        if(typeArguments.resolvePlaceHolderArguments(headers)){
            var existed = this.findInstantiatedClass(typeArguments);
            if(existed != null){
                if(existed.getLoadingStage().value <= ResolveHierarchicalClasses.value) {
                    assert headers.containsKey(existed.fullname());
                    inst = existed;
                } else{
                    return existed;
                }
            } else {
                // after typeArguments.resolvePlaceHolderArguments() found an existed instance, that means, some '<NA>' become available, use existed instance instead
                if (callback != null) callback.accept(inst);
            }
        }
        assert !typeArguments.toString().contains("NA");
        this.registerGenericInstantiationClass(typeArguments, inst);
        inst.strings = this.strings;
        inst.parent = newParent;
        if (newParent != null) newParent.addChild(inst);
        inst.genericTypeParamDescs = this.genericTypeParamDescs;
        // create slots later
        inst.setSuperClass(this.superClass);
        inst.setInterfaces(this.interfaces);    // apply instantiation for interface at resolveHierarchicalClasses, LN748
        inst.setPermitClass(this.permitClass);
        inst.setSlots(this.slotDescs);
        inst.fields = this.fields;
        if (this.children != null) {
            ClassHeader finalInst = inst;
            inst.setChildren(new ArrayList<>(this.children.stream().map(c -> c.tryInstantiate(finalInst, headers, typeArguments)).toList()));
            inst.setMethods(mapMethods(this.methods, this.children, inst.children));
        } else {
            inst.setMethods(this.methods);
        }
        inst.setLoadingStage(ResolveHierarchicalClasses);
        return inst;
    }

    private List<MethodDesc> mapMethods(List<MethodDesc> methods, List<ClassHeader> children, List<ClassHeader> newChildren) {
        if(children.isEmpty()) return methods;

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < children.size(); i++) {
            ClassHeader child = children.get(i);
            map.put(child.fullname, newChildren.get(i).fullname);
        }
        var ls = new ArrayList<MethodDesc>(methods.size());
        for (MethodDesc method : methods) {
            ls.add(new MethodDesc(method.getName(), map.getOrDefault(method.getFullname(), method.getFullname())));
        }
        return ls;
    }

    private GenericVMCodeTransformer genericVMCodeTransformer = new GenericVMCodeTransformer(this);
    private IoBuffer applyTemplateOnCode(IoBuffer code, GenericTypeArguments genericTypeArguments, ClassHeader instantFunction, Map<String, ClassHeader> headers) {
        if ((this.modifiers & NATIVE) == NATIVE) return code;
        return genericVMCodeTransformer.transform(code, genericTypeArguments, instantFunction, headers);
    }

    public boolean isNativeClass() {
        return this.type == TYPE_CLASS && ((this.modifiers & NATIVE) == NATIVE);
    }

    public ClassHeader findMethod(MethodDesc methodDesc, Map<String, ClassHeader> headers) {
        var r = headers.get(methodDesc.getFullname());
        if(r == null)
            throw new RuntimeException("'%s' not found, its fullname is '%s'".formatted(methodDesc.getName(), methodDesc.getFullname()));
        return r;
    }

    private static List<ClassHeader> getChildrenIncludeSuperInterface(ClassHeader header, Map<String, ClassHeader> headers) {
        if(header.type() == TYPE_INTERFACE){
            String[] superInterfaces = header.getInterfaces();
            if(superInterfaces == null || superInterfaces.length == 0){
                return header.getChildren();
            }
            List<ClassHeader> children = new ArrayList<>(header.getChildren());
            for (String s : superInterfaces) {
                ClassHeader superInterface = headers.get(s);
                children.addAll(getChildrenIncludeSuperInterface(superInterface, headers));
            }
            return children;
        }
        return header.getChildren();
    }

    private boolean isAbstract() {
        return (this.modifiers & AgoClass.ABSTRACT) == AgoClass.ABSTRACT;
    }
    public int getVisibility() {
        return this.modifiers & 0b111;
    }

    public MethodDesc findMethod(String methodName, Map<String, ClassHeader> headers) {
        var r = this.methodsByName.get(methodName);
        if (r != null) return r;
        throw new RuntimeException("'%s' not found in '%s'".formatted(methodName, this));
    }


    protected Map<GenericTypeArguments, ClassHeader> instantiatedClasses;
    private boolean instantiateClassesHasUnsolvedGenericTypeDesc = false;
    public void registerGenericInstantiationClass(GenericTypeArguments typeArguments, ClassHeader instantiation){
        if(this.genericSource != null){
            this.genericSource.sourceTemplate().registerGenericInstantiationClass(typeArguments, instantiation);
            return;
        }
        if(this.instantiatedClasses == null) this.instantiatedClasses = new HashMap<>();
        this.instantiatedClasses.put(typeArguments, instantiation);
        if(typeArguments.hasUnsolvedGenericTypeDesc()){
            this.instantiateClassesHasUnsolvedGenericTypeDesc = true;
        }
    }
    public ClassHeader findInstantiatedClass(GenericTypeArguments genericTypeArguments){
        if(this.genericSource != null){
            return this.genericSource.sourceTemplate().findInstantiatedClass(genericTypeArguments);
        }
        if(instantiatedClasses == null) return null;
        return instantiatedClasses.get(genericTypeArguments);
    }

    public boolean isAffectedBy(Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments){
        return genericTypeArguments.canApplyToTemplate(this, headers);
    }

    /**
     * resolve existed instantiation class, or create a new instantiation
     */
    public ClassHeader resolveTemplateInstantiation(Map<String, ClassHeader> headers,  GenericTypeArguments typeArguments){
        if(this.genericSource == null){
            if(this.isInGenericTemplate(headers)) {
                var args = typeArguments.takeFor(this, headers);
                if (args == null) return this;
                assert args.size() <= typeArguments.size();
                return findOrCreateInstantiation(headers, args);
            } else {
                return this;
            }
        } else {
            GenericTypeArguments args = mixInstantiationArgs(typeArguments, headers);
            if(args == null) return this;
            // assert args.size() >= typeArguments.size();  // Function1<R=Foo, Bar> -> Function<R=Foo>
            return this.findOrCreateInstantiation(headers, args);
        }
    }

    public GenericTypeArguments mixInstantiationArgs(GenericTypeArguments args, Map<String, ClassHeader> headers){
        if(this.genericSource == null) return args;
        GenericTypeArguments existedArgs = this.genericSource.typeArguments();
        ClassHeader mySourceTempl = this.genericSource.sourceTemplate();
        // not like compile time, generic source may be already set to child of intermediate class
        // ~~if(mySourceTempl == existedArgs.sourceTemplate())~~
        var remappedValue = false;
        if(existedArgs.valuesMatch(args, headers)){
            existedArgs = mapTypeValues(existedArgs, args, headers);
            remappedValue = true;
        }
        if(existedArgs.isIntermediate()) {
            existedArgs = existedArgs.applyIntermediate(args, headers);
            if(mySourceTempl.belongsTo(args.sourceTemplate)){
                return args.applyChild(existedArgs, headers);
            } else if(args.sourceTemplate.belongsTo(mySourceTempl)){
                return existedArgs.applyChild(args, headers);
            } else {
                return existedArgs;
            }
        }
        if(!args.canApplyToTemplate(this,headers)) {
            if(remappedValue) return existedArgs;
            return null;
        }
        if(args.sourceTemplate.belongsTo(existedArgs.sourceTemplate)){
            return existedArgs.applyChild(args, headers);
        } else if(existedArgs.sourceTemplate.belongsTo(args.sourceTemplate)){
            return args.applyChild(existedArgs, headers);
        } else {
            if(remappedValue) return existedArgs;
            return existedArgs.size() >= args.size() ? existedArgs : args;
        }
    }

    private GenericTypeArguments mapTypeValues(GenericTypeArguments existedTypeArguments, GenericTypeArguments instantiationArguments, Map<String, ClassHeader> headers) {
        var typeArgumentsArray = existedTypeArguments.getTypeArgumentsArray();
        var newArray = new TypeDesc[typeArgumentsArray.length];
        boolean set = false;
        for (int i = 0; i < typeArgumentsArray.length; i++) {
            var typeDesc = typeArgumentsArray[i];
            if(typeDesc.typeCode != OBJECT) {
                newArray[i] = typeDesc;
                continue;
            }
            var c = headers.get(typeDesc.getClassName());
            var b = false;
            if (c.genericSource != null) {
                var innerArgs = c.genericSource.typeArguments();
                if (instantiationArguments.canApplyToTemplate(c, headers) || innerArgs.valuesMatch(instantiationArguments, headers)) {
                    b = true;
                    newArray[i] = typeDesc.applyTemplate(headers, instantiationArguments);
                }
            }
            if(!b) newArray[i] = typeDesc;
            if(b) set = true;
        }
        if(!set) return existedTypeArguments;
        return new GenericTypeArguments(existedTypeArguments.getSourceTemplate(), newArray, headers);
    }

    private ClassHeader findOrCreateInstantiation(Map<String, ClassHeader> headers, GenericTypeArguments typeArguments){
        var existed = this.findInstantiatedClass(typeArguments);
        if(existed != null) return existed;
        ClassHeader parentInst;
        ClassHeader templ;
        if(this.genericSource != null){
            templ = this.genericSource.sourceTemplate();
        } else {
            templ = this;
        }
        if(this.parent != null && this.parent.isAffectedBy(headers, typeArguments)){  // therefore it's not for any parent
            parentInst = this.parent.resolveTemplateInstantiation(headers, typeArguments.takeFor(this.parent, headers));
        } else {
            parentInst = this.parent;
        }

        return templ.instantiate(parentInst, headers, typeArguments);
    }

    public void nextStage() {
        this.setLoadingStage(loadingStage.nextStage());
    }

    public boolean resolveHierarchicalClasses(Map<String, ClassHeader> headers) {
        if(this.loadingStage != ResolveHierarchicalClasses) return true;

        if(this.genericSource != null) {
            ClassHeader templ = this.genericSource.sourceTemplate();
            GenericTypeArguments typeArguments = this.genericSource.typeArguments();
            if(!typeArguments.resolvePlaceHolderArguments(headers)) return false;
            if (StringUtils.isNotEmpty(templ.metaClass)) {
                MetaClassHeader metaClassHeader = (MetaClassHeader) headers.get(templ.metaClass);
                var appliedMetaClassHeader = (MetaClassHeader) metaClassHeader.tryInstantiate(metaClassHeader.parent, headers, typeArguments);
                appliedMetaClassHeader.setInstanceClass(this);
                this.metaClass = appliedMetaClassHeader.fullname;
            } else {
                assert StringUtils.isEmpty(this.metaClass);
            }
            if(templ.superClass != null) {
                if (!headers.containsKey(templ.superClass)) return false;
                TypeDesc superType = new TypeDesc(OBJECT, this.superClass);
                if (!superType.resolveGenericTypeDescPlaceHolder(headers)) return false;
                this.setSuperClass(superType.applyTemplate(headers, typeArguments).className);
            }
            if(templ.permitClass != null){
                if(!headers.containsKey(templ.permitClass)) return false;
                TypeDesc permitClass = new TypeDesc(OBJECT, templ.permitClass);
                if (!permitClass.resolveGenericTypeDescPlaceHolder(headers)) return false;
                this.setPermitClass(permitClass.applyTemplate(headers, typeArguments).className);
            }

            String[] interfaces = templ.interfaces;
            if(interfaces != null){
                for (String interface_ : interfaces) {
                    if (!headers.containsKey(interface_)) return false;
                }
                var applied = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    String interface_ = interfaces[i];
                    var interfaceHeader = headers.get(interface_);
                    if(this.isFunction() && interfaceHeader.isPermitForFunction(headers) && interfaceHeader.isGenericTemplate()){
                        applied[i] = interface_; // skip, till parse fields, for params not loaded
                    } else {
                        applied[i] = interfaceHeader.resolveTemplateInstantiation(headers, typeArguments).fullname;
                    }
                }
                this.setInterfaces(applied);
            }
            if(this.methods != null) {
                List<MethodDesc> methodDescs = this.methods;
                boolean changed = false;
                for (int i = 0; i < methodDescs.size(); i++) {
                    MethodDesc methodDesc = methodDescs.get(i);
                    var m = headers.get(methodDesc.getFullname());
                    if (m.parent != this) {       // inherited
                        var mInst = m.resolveTemplateInstantiation(headers, typeArguments);
                        if(mInst != m) {
                            changed = true;
                            methodDescs.set(i, new MethodDesc(mInst.getName(), mInst.fullname()));
                        }
                    }
                }
                if(changed){
                    this.setMethods(new ArrayList<>(this.methods)); // update methods again
                }
            }

        } else {
            if (StringUtils.isNotEmpty(this.getMetaClass())) {
                MetaClassHeader metaClassHeader = (MetaClassHeader) headers.get(this.getMetaClass());
                metaClassHeader.setInstanceClass(this);
            }
        }
        this.setLoadingStage(ParseFields);
        return true;
    }

    private boolean isPermitForFunction(Map<String, ClassHeader> headers){
        if(this.permitClass == null) return false;
        var permitClass = headers.get(this.permitClass);
        if(permitClass.getSourceTemplate() != null && permitClass.getSourceTemplate().fullname.equals("lang.Function")){
            return true;
        }
        return false;
    }

    private boolean isImplemented(String interfaceName) {
        if(this.interfaces == null) return false;
        for (String anInterface : this.interfaces) {
            if(anInterface.equals(interfaceName)) return true;
        }
        return false;
    }

    private ClassHeader applyFunctionInterface(ClassHeader interfaceHeader, Map<String, ClassHeader> headers) {
        if(interfaceHeader.genericSource != null){
            interfaceHeader = interfaceHeader.genericSource.sourceTemplate();
        }
        TypeDesc[] arr = new TypeDesc[this.functionParams == null ? 1 : this.functionParams.length + 1];
        arr[0] = this.functionResultType;
        if(this.functionParams != null) {
            for (int i = 0; i < functionParams.length; i++) {
                arr[i + 1] = functionParams[i].type;
            }
        }
        var a = new GenericTypeArguments(interfaceHeader, arr, headers);
        return interfaceHeader.tryInstantiate(interfaceHeader.parent, headers, a);
    }

    boolean resolvePlaceHolderKeyInstantiatedClassed(Map<String, ClassHeader> headers){
        if(this.instantiatedClasses != null && instantiateClassesHasUnsolvedGenericTypeDesc){
            var h = new HashMap<GenericTypeArguments, ClassHeader>();
            for (Map.Entry<GenericTypeArguments, ClassHeader> entry : this.instantiatedClasses.entrySet()) {
                var arg = entry.getKey();
                if(!arg.resolvePlaceHolderArguments(headers)) return false;
                h.put(arg, entry.getValue());
            }
            this.instantiatedClasses = h;
        }
        return true;
    }

    public boolean parseFields(Map<String, ClassHeader> headers) {
        if(this.loadingStage != ParseFields) return true;

        if(this.genericSource != null) {
            ClassHeader templ = this.genericSource.sourceTemplate();
            if(!templ.resolvePlaceHolderKeyInstantiatedClassed(headers)) return false;

            GenericTypeArguments typeArguments = this.genericSource.typeArguments();
            if(!typeArguments.resolvePlaceHolderArguments(headers)) return false;
            if(templ.loadingStage == ParseFields){
                if(!templ.parseFields(headers)) return false;
            }

            this.fields = Arrays.stream(templ.fields).map(f -> f.applyTemplate(headers, typeArguments)).toArray(VariableDesc[]::new);
            var slots = Arrays.stream(templ.slotDescs).map(slotDesc -> slotDesc.applyTemplate(headers, typeArguments)).toArray(SlotDesc[]::new);
            this.setSlots(slots);

            if (this.isFunction()) {
                if (templ.functionResultType != null) {
                    this.functionResultType = templ.functionResultType.applyTemplate(headers, typeArguments);
                }
                if (templ.functionParams != null) {
                    this.functionParams = Arrays.stream(templ.functionParams).map(f -> f.applyTemplate(headers, typeArguments)).toArray(VariableDesc[]::new);
                }
                if (templ.functionVariables != null) {
                    this.functionVariables = Arrays.stream(templ.functionVariables).map(f -> f.applyTemplate(headers, typeArguments)).toArray(VariableDesc[]::new);
                }
                this.nativeFunctionEntrance = templ.nativeFunctionEntrance;
                this.functionResultSlot = templ.functionResultSlot;

                var applied = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    String interface_ = interfaces[i];
                    var interfaceHeader = headers.get(interface_);
                    if (this.isFunction() && interfaceHeader.isPermitForFunction(headers) && interfaceHeader.isGenericTemplate()) {
                        applied[i] = applyFunctionInterface(interfaceHeader,headers).fullname;
                    } else {
                        applied[i] = interface_;
                    }
                }
                this.setInterfaces(applied);
            }
        } else {
            if(!this.resolvePlaceHolderKeyInstantiatedClassed(headers)) return false;

            classLoader.parseBody(this);

            for (VariableDesc field : this.fields) {
                if(!field.resolveGenericTypeDescPlaceHolder(headers)) return false;
            }
            for (SlotDesc slotDesc : this.slotDescs) {
                if(!slotDesc.resolveGenericTypeDescPlaceHolder(headers)) {
                    slotDesc.resolveGenericTypeDescPlaceHolder(headers);
                    return false;
                }
            }
            if(this.genericTypeParamDescs != null){
                GenericTypeDesc[] typeParamDescs = this.genericTypeParamDescs;
                for (int i = 0; i < typeParamDescs.length; i++) {
                    GenericTypeDesc g = typeParamDescs[i];
                    if (g.isPlaceHolder) {
                        GenericTypeDesc resolved = g.resolveExactType(headers);
                        if (resolved == null) return false;
                        genericTypeParamDescs[i] = resolved;
                    }
                }
            }
            if(this.isFunction()){
                if(this.functionResultType != null){
                    if(this.functionResultType instanceof GenericTypeDesc g && g.isPlaceHolder){
                        GenericTypeDesc resolved = g.resolveExactType(headers);
                        if(resolved == null) return false;
                        this.functionResultType = resolved;
                    }
                }
                if(this.functionParams != null){
                    for (VariableDesc functionParam : functionParams) {
                        if(!functionParam.resolveGenericTypeDescPlaceHolder(headers)) return false;
                    }
                }
                if(this.functionVariables != null){
                    for (VariableDesc functionVariable : functionVariables) {
                        if(!functionVariable.resolveGenericTypeDescPlaceHolder(headers)) return false;
                    }
                }
            }
        }
        this.setLoadingStage(InstantiateFunctionFamily);
        return true;
    }

    public boolean parseCode(Map<String, ClassHeader> headers) {
        if(this.loadingStage != ParseCode) return false;
        if(this.genericSource != null){
            if(this.isFunction()) {
                ClassHeader template = genericSource.sourceTemplate();
                if (this.isInGenericTemplate(headers)) {
                    this.compiledCode = template.compiledCode;
                } else {
                    var functionCode = template.compiledCode.slice();
                    IoBuffer applied = template.applyTemplateOnCode(functionCode, genericSource.typeArguments(), this, headers);
                    this.compiledCode = applied.rewind();
                }
                this.sourceMap = template.sourceMap;
            }
        }
        this.setLoadingStage(BuildClass);
        return true;
    }

    public AgoClass buildClass(Map<String, ClassHeader> headers){
        if(this.loadingStage != BuildClass) return this.agoClass;

        if(this.genericSource != null){
            var templ = this.genericSource.sourceTemplate();
            if(templ.loadingStage == BuildClass){
                templ.buildClass(headers);
            }
        }

        MetaClass metaClass;
        if(StringUtils.isNotEmpty(this.getMetaClass())){
            var metaHeader = headers.get(this.getMetaClass());
            if(metaHeader.loadingStage == BuildClass){
                metaClass = (MetaClass) metaHeader.buildClass(headers);
                if(metaClass == null) return null;
            } else {
                metaClass = (MetaClass) metaHeader.agoClass;
            }
        } else {
            metaClass = classLoader.getTheMeta();
        }

        AgoClass agoClass;
        switch (this.type) {
            case TYPE_METACLASS:
                agoClass = new MetaClass(classLoader, metaClass, this.fullname);
                break;
            case TYPE_CLASS:
                agoClass = new AgoClass(classLoader, metaClass, this.fullname, this.name);
                break;
            case TYPE_ENUM:
                var enumClass = new AgoEnum(classLoader, metaClass, this.fullname, this.name);
                enumClass.setBasePrimitiveType(this.enumBasePrimitiveType);
                enumClass.setEnumValues(this.enumValues);
                agoClass = enumClass;
                break;
            case TYPE_INTERFACE:
                agoClass = new AgoInterface(classLoader, metaClass, this.fullname, this.name);
                break;
            case TYPE_TRAIT:
                agoClass = new AgoTrait(classLoader, metaClass, this.fullname, this.name);
                break;
            case TYPE_FUNCTION:
                if ((this.modifiers & NATIVE) == NATIVE) {
                    AgoNativeFunction n = new AgoNativeFunction(classLoader, metaClass, this.fullname, this.name);
                    n.setNativeEntrance(this.nativeFunctionEntrance);
                    agoClass = n;
                } else {
                    agoClass = new AgoFunction(classLoader, metaClass, this.fullname, this.name);
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }
        agoClass.setModifiers(this.modifiers);
        agoClass.setClassId(this.getClassId());
        agoClass.setSourceLocation(this.sourceLocation);
        this.agoClass = agoClass;
        classLoader.getClassByName().put(this.fullname, agoClass);

        if(StringUtils.isNotEmpty(this.getMetaClass())){
            if(!(this instanceof ParameterizedClassHeader)) {
                metaClass.setInstanceClass(agoClass);
            }
        }

        this.setLoadingStage(ResolveFunctionIndex);
        return agoClass;
    }


    public void setLoadingStage(LoadingStage loadingStage) {
        this.loadingStage = loadingStage;
    }

    public LoadingStage getLoadingStage() {
        return loadingStage;
    }

    public boolean belongsTo(ClassHeader parent) {
        ClassHeader prev = this;
        for(var p = this.parent; ; prev = p, p = p.parent){
            if(p == null && prev instanceof MetaClassHeader m){
                p = m.instanceClass;
            }
            if(p == null) break;
            if(p == parent) return true;
            if(parent instanceof MetaClassHeader m){
                if(p == m.instanceClass) return true;
            }
        }
        return false;
    }

    public boolean processLoadClassName(Map<String, ClassHeader> headers, MutableObject<ClassHeader> createdClass) {
        if(this.loadingStage == LoadClassNames){
            this.nextStage();
        }
        return true;
    }

    public void setBodyParsed(boolean bodyParsed) {
        this.bodyParsed = bodyParsed;
    }

    public boolean isBodyParsed() {
        return bodyParsed;
    }

    protected String extractPackagePrefix() {
        if(this.parent != null) return this.parent.extractPackagePrefix();
        var s = this.fullname;
        if(s.length() == this.name.length()) return "";
        return s.substring(0, s.length() - this.name.length()); // ends with .
    }

    void collectMethods(Map<String, ClassHeader> headers){
        if(this.loadingStage != CollectMethods) return;

        var methods = new ArrayList<AgoFunction>();
        if(this.methods != null) {
            for (var method : this.methods) {
                var f = this.findMethod(method, headers);
                method.setFunctionClassHeader(f);
                int functionIndex = method.getMethodIndex();
                if (functionIndex <= methods.size() - 1) {
                    if (methods.get(functionIndex) != null) {
                        this.findMethod(method, headers);
                        throw new RuntimeException("funId collision");
                    }
                    methods.set(functionIndex, (AgoFunction) f.agoClass);
                } else {
                    while (functionIndex > methods.size()) {
                        methods.add(null);
                    }
                    methods.add((AgoFunction) f.agoClass);
                }
            }
        }
        this.agoClass.setMethods(methods.toArray(new AgoFunction[0]));

        this.setLoadingStage(EnqueueParameterizingClassTask);
    }

    void setConcreteTypeInfo(Map<String, ClassHeader> headers){
        if(this.genericSource != null && genericSource.sourceTemplate() == genericSource.typeArguments().sourceTemplate){
            GenericTypeArguments genericTypeArguments = genericSource.typeArguments();
            var typeInfos = Arrays.stream(genericTypeArguments.getTypeArgumentsArray()).map(t ->
                    t.toTypeInfo(headers)
            ).toArray(TypeInfo[]::new);
            agoClass.setConcreteTypeInfo(new GenericArgumentsInfo(genericSource.sourceTemplate().agoClass, typeInfos));
        }

        if(this.isGenericTemplate() && this.genericSource == null){
            var parameters = Arrays.stream(this.genericTypeParamDescs).map(p ->
                    p.toGenericParameterTypeInfo(p.name, headers.get(p.getClassName()).agoClass, headers)
            ).toArray(GenericParameterTypeInfo[]::new);
            agoClass.setConcreteTypeInfo(new GenericTypeParametersInfo(parameters));
        }
    }

    void buildInterfaceMethodMap(Map<String, ClassHeader> headers){
        if(this.getInterfaces() != null && this.interfaces.length != 0){
            Int2ObjectHashMap<int[]> interfaceMethods = new Int2ObjectHashMap<>();
            String[] interfaces = this.getInterfaces();
            int maxClassId = 0;
            for (int k = 0; k < interfaces.length; k++) {
                String interface_ = interfaces[k];
                var interfaceHeader = headers.get(interface_);
                if(interfaceHeader.genericSource != null){
                    interfaceHeader = interfaceHeader.genericSource.sourceTemplate();
                }
                int[] map = new int[interfaceHeader.methods.size()];

                List<MethodDesc> methodDescs = interfaceHeader.methods;
                for (int i = 0; i < methodDescs.size(); i++) {
                    MethodDesc interfaceMethod = methodDescs.get(i);
                    var index = this.nonPrivateFunctionIndexes.get(interfaceMethod.getName());
                    if (index == null) {
                        if (!this.isAbstract()) throw new NullPointerException("'%s' not found in '%s'".formatted(interfaceMethod.getName()));
                        map[interfaceMethod.getMethodIndex()] = -1;
                    } else {
                        map[interfaceMethod.getMethodIndex()] = index;
                    }
                }

                int classId = interfaceHeader.classId;
                if(maxClassId < classId) maxClassId = classId;
                interfaceMethods.put(classId, map);
            }
//                int[][] matrix = new int[maxClassId + 1][];
//                for (Map.Entry<Integer, int[]> entry : interfaceMethods.entrySet()) {
//                    matrix[entry.getKey()] = entry.getValue();
//                }
            this.agoClass.setInterfacesMethods(interfaceMethods);
        }
    }

    public void setPermitClass(String permitClass) {
        this.permitClass = permitClass;
    }

    public String getPermitClass() {
        return permitClass;
    }

    public void setEnumBasePrimitiveType(TypeCode enumBasePrimitiveType) {
        this.enumBasePrimitiveType = enumBasePrimitiveType;
    }

    public TypeCode getEnumBasePrimitiveType() {
        return enumBasePrimitiveType;
    }

    public void setEnumValues(Map<String, Object> enumValues) {
        this.enumValues = enumValues;
    }

    public Map<String, Object> getEnumValues() {
        return enumValues;
    }

    public void setSourceLocation(SourceLocation sourceLocation) {
        this.sourceFilename = sourceLocation.getFilename();
        this.sourceLocation = sourceLocation;
    }

    public void setSourceHeader(ClassHeader sourceHeader) {
        this.sourceHeader = sourceHeader;
    }

    public ClassHeader getSourceHeader() {
        return sourceHeader;
    }

}
