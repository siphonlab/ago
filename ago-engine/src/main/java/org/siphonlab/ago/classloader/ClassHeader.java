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

import static org.siphonlab.ago.AgoClass.*;
import static org.siphonlab.ago.TypeCode.OBJECT;
import static org.siphonlab.ago.classloader.LoadingStage.*;

public class ClassHeader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassHeader.class);

    LoadingStage loadingStage = LoadClassNames;

    protected String fullname;
    protected String sourceFilename;
    byte type;

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

    GenericTypeCodeAvatarClassHeader[]  genericTypeParams;

    protected GenericSource genericSource;

    protected VariableDesc[] fields;
    protected String functionResultType;
    protected VariableDesc[] functionParams;
    String nativeFunctionEntrance;
    protected VariableDesc[] functionVariables;

    public SwitchTableDesc[] switchTables;
    public TryCatchItemDesc[] tryCatchItems;

    IoBuffer compiledCode;
    SourceMapEntry[] sourceMap;

    protected IntSortedArraySet handledInstructions = new IntSortedArraySet();
    private boolean bodyParsed = false;
    protected String permitClass;
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

    public TypeCode getTypeCode(){
        return OBJECT;
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

    public boolean instantiateFunctionFamily() {
        if (this.loadingStage != InstantiateFunctionFamily)
            return true;

        if(this.children != null){
            for (ClassHeader child : this.children) {
                if(child instanceof GenericInstantiationClassHeader && child.isFunction()){
                    child.instantiateFunctionFamily();
                }
            }
        }

        this.nextStage();
        return true;
    }


    public boolean isInGenericTemplate() {
        for (var h = this; h != null; h = h.parent) {
            if(h instanceof ParameterizedClassHeader p){
                h = this.classLoader.getClassHeader(p.baseClass);
            }
            if ((h.modifiers & GENERIC_TEMPLATE) == GENERIC_TEMPLATE) {
                return true;
            } else if(h.genericSource != null && !h.genericSource.instantiationArguments().isTerminated()) {
                return true;
            } else if(h instanceof MetaClassHeader metaClassHeader && metaClassHeader.instanceClass instanceof ClassHeader i && i.isInGenericTemplate()){
                return true;
            }
        }
        return false;
    }

    public boolean isGenericTemplate(){
        return (this.modifiers & GENERIC_TEMPLATE) == GENERIC_TEMPLATE;
    }

    public boolean isGenericInstantiation(){
        return this.genericSource != null && !this.isGenericTemplate();
    }

    public void createTemplateDefaultGenericSource() {
        InstantiationArguments instantiationArguments = new InstantiationArguments(this, genericTypeParams);
        for(var p = this.parent; p != null; p = p.parent){
            if(p.isGenericTemplate() && p.genericSource != null){
                instantiationArguments = instantiationArguments.applyParent(p.genericSource.instantiationArguments(), classLoader);
                break;
            }
        }
        this.genericSource = new GenericSource(this.fullname, instantiationArguments, Arrays.stream(genericTypeParams).map(t -> new ClassRefValue(t.fullname)).toArray(ClassRefValue[]::new));
        this.putInstantiatedClassToCache(instantiationArguments, this);
    }

    public String[] loadStrings(){
        if(this.strings == null) return this.parent != null ? this.parent.loadStrings() : null;
        return this.strings;
    }

    public ClassHeader clone(ClassHeader newParent){
        if (this.parent == null || !this.fullname.startsWith(this.parent.fullname)) {
            return this;
        }
        String fullname = newParent.fullname + '.' + this.name;
        var existed = classLoader.getClassHeader(fullname);
        if(existed != null) return existed;

        var inst = new ClassHeader(fullname, this.type, this.modifiers, this.slice != null ? this.slice.slice() : null, this.classLoader);
        inst.setName(this.name);
        copyToClone(inst);
        inst.parent = newParent;
        classLoader.registerNewClass(inst);
        return inst;
    }
    protected void copyToClone(ClassHeader inst){
        inst.strings = this.strings;
        inst.genericTypeParams = this.genericTypeParams;
        inst.setSuperClass(this.superClass);
        inst.setPermitClass(this.permitClass);
        inst.setInterfaces(this.interfaces);
        inst.setMetaClass(this.metaClass);
        inst.setSlots(this.slotDescs);

        if(this.isFunction()) {
            inst.setFunctionResultType(this.functionResultType);
            inst.functionParams = this.functionParams;
            inst.functionVariables = this.functionVariables;
            inst.nativeFunctionEntrance = this.nativeFunctionEntrance;
        }

        inst.fields = this.fields;
        inst.genericSource = this.genericSource;
        inst.setLoadingStage(loadingStage);
        inst.setSourceHeader(this);
        if (this.children != null) {
            inst.setChildren(new ArrayList<>(this.children.stream().map(c -> c.clone(inst)).toList()));
            inst.setMethods(mapMethods(this.methods, this.children, inst.children));
        }
    }

    private Set<InstantiationArguments> instantiatingChildren = new HashSet<>();

    protected ClassHeader instantiate(InstantiationArguments typeArguments, ClassHeader newParent, String suggestionName, String suggestionFullName) {
        if(newParent == null && !this.isAffectedByTypeArguments(typeArguments)) return this;

        ClassHeader inst;

        ClassHeader templ;
        InstantiationArguments args;
        GenericSource genericSource = this.genericSource;
        if(genericSource != null){
            templ = Objects.requireNonNull(classLoader.getClassHeader(genericSource.sourceTemplate()));
            var myArgs = genericSource.instantiationArguments();
            args = myArgs.apply(typeArguments, classLoader);
            if(myArgs.equals(args)){
                return this;
            }
        } else {
            templ = this;
            args = typeArguments;
        }

        var existed = templ.getCachedInstantiatedClass(args);
        String name = suggestionName;
        String fullname = suggestionFullName;
        if(existed != null) {
            //TODO the children should already instantiated if arguments has no instantiate-child-first problem
            if (!args.equals(existed.genericSource.instantiationArguments()) && !this.instantiatingChildren.contains(args)) {    // arguments changed, try children
                this.instantiateChildren(existed, args);
            }
            return existed;
        } else {
            if(templ instanceof MetaClassHeader metaClassHeader) {
                if (name == null) {
                    String[] names = GenericInstantiationClassHeader.composeMetaClassName(metaClassHeader.instanceClass, args);
                    name = names[0];
                    fullname = names[1];
                }
                assert newParent == null;
                inst = metaClassHeader.instantiateMetaClass(newParent, name, fullname, args);
                if (LOGGER.isDebugEnabled()) LOGGER.debug("%s apply template and got inst %s".formatted(templ.fullname, inst.fullname));
                inst.setName(name);
                return inst;
            } else if(templ.isGenericTemplate()) {
                var argsForTempl =  args.takeFor(templ);
                if(argsForTempl != null && !Arrays.equals(templ.genericSource.typeArguments(), argsForTempl)) {
                    if (name == null) {
                        assert newParent == null;
                        name = GenericInstantiationClassHeader.composeClassName(templ.name, argsForTempl);
                        fullname = newParent == null ? extractPackagePrefix() + name : newParent.fullname + '.' + name;
                    }
                    inst = new GenericInstantiationClassHeader(fullname, templ.type, templ.fullname, args, this.classLoader);
                    inst.setName(name);
                    templ.putInstantiatedClassToCache(args, inst);

                    templ.classLoader.registerNewClass(inst);
                    templ.applyInstantiation(inst, args, newParent);

                    if(this.isFunction() && newParent != null){
                        newParent.registerFunctionInstantiation(inst);
                        instantiateFunctionFamily(newParent, inst, 1, typeArguments);
                    }

                    return inst;
                }
            }
        }

        assert fullname != null;
        existed = this.classLoader.getClassHeader(fullname);        // some arguments for parent may never match me, but I won't know I already created
        if(existed != null) {
            templ.putInstantiatedClassToCache(args, existed);
            return existed;
        }
//                name = this.name;
//                fullname = newParent == null ? extractPackagePrefix() + name : newParent.fullname + '.' + name;
        inst = new ClassHeader(fullname, templ.type, templ.modifiers, templ.slice != null ? templ.slice.slice() : null, templ.classLoader);
        inst.name = name;
        templ.putInstantiatedClassToCache(args, inst);
        this.classLoader.registerNewClass(inst);
        this.applyInstantiation(inst, args, newParent);

        return inst;
    }

    protected void instantiateChildren(ClassHeader instantiatedClass, InstantiationArguments arguments) {
        this.instantiatingChildren.add(arguments);
        for (var child : this.children) {
            if(child.parent == this){
                instantiateChild(instantiatedClass, arguments, child);
            }
        }
        this.instantiatingChildren.remove(arguments);
    }

    public ClassHeader instantiateChild(ClassHeader instantiatedClass, InstantiationArguments arguments, ClassHeader child) {
        InstantiationArguments childArgs;
        if(child.genericSource != null){
            childArgs = child.genericSource.instantiationArguments().applyParent(arguments, classLoader);
        } else {
            childArgs = arguments;
        }

        var names = composeGenericInstanceNames(instantiatedClass.fullname, child, childArgs);
        var suggestionName = names[0];
        var suggestionFullanme = names[1];
        var instantiated = child.instantiate(childArgs, instantiatedClass, suggestionName, suggestionFullanme);
        if(!instantiatedClass.children.contains(instantiated)) {
            instantiatedClass.addChild(instantiated);
        }
        return instantiated;
    }

    private static String[] composeGenericInstanceNames(String parentName, ClassHeader child, InstantiationArguments childArgs) {
        if(child.isGenericTemplate() && childArgs.takeFor(child) != null) {
            String name = GenericInstantiationClassHeader.composeClassName(child.name, childArgs.takeFor(child));
            String fullname = parentName + '.' + name;
            return new String[]{name, fullname};
        } else if(child instanceof MetaClassHeader metaClassHeader){
            return GenericInstantiationClassHeader.composeMetaClassName(metaClassHeader.instanceClass, childArgs);
        } else {
            return new String[]{child.name, parentName + '.' + child.name};
        }
    }


    void registerFunctionInstantiation(ClassHeader inst) {
        MethodDesc methodDesc = new MethodDesc(inst.name, inst.fullname);
        this.addMethod(methodDesc);
        assert this.findMethod(methodDesc) != null;
    }

    private void addMethod(MethodDesc methodDesc) {
        if(this.methodsByName.containsKey(methodDesc.getName())) return;
        if (LOGGER.isDebugEnabled()) LOGGER.debug("%s add method %s".formatted(this, methodDesc));
        if(this.methods == null) this.methods = new ArrayList<>();
        this.methods.add(methodDesc);
        this.methodsByName.put(methodDesc.getName(), methodDesc);
    }

    void instantiateFunctionFamily(ClassHeader parent, ClassHeader instantiationOfThis, int depth, InstantiationArguments instantiationArguments) {
        if(StringUtils.isNotEmpty(parent.superClass)){
            ClassHeader superClass = classLoader.getClassHeader(parent.superClass);
            if(superClass == parent) return;
            ClassHeader overriddenFunction = superClass.getSameSignatureFunction(this);
            if(overriddenFunction != null){
                overriddenFunction.instantiate(instantiationArguments, superClass, null, null);
            }
            for (String interfaceName : parent.getInterfaces()) {
                var interface_ = classLoader.getClassHeader(interfaceName);
                overriddenFunction = interface_.getSameSignatureFunction(this);
                if (overriddenFunction != null) {
                    overriddenFunction.instantiate(instantiationArguments, superClass, null, null);
                }
            }
        }

        broadcastSubInstantiationMethod(parent, instantiationOfThis, depth, instantiationArguments);

    }

    private void broadcastSubInstantiationMethod(ClassHeader parent, ClassHeader instantiationOfThis, int depth, InstantiationArguments instantiationArguments) {
        for (ClassHeader sub : classLoader.getHeaders().values()) {
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
                    newFun.instantiate(instantiationArguments, sub, null, null);
                } else {
                    newFun = instantiationOfThis;       // won't add child to sub, only save to methods
                    parent.addMethod(new MethodDesc(instantiationOfThis.name, instantiationOfThis.fullname));
                }
                broadcastSubInstantiationMethod(sub,newFun,depth + 1, instantiationArguments);
            }
        }
    }

    public ClassHeader getSourceTemplate() {
        if(this.isGenericTemplate()) return this;
        if(this.genericSource != null) return Objects.requireNonNull(classLoader.getClassHeader(this.genericSource.sourceTemplate()));
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
            if (!anotherParam.type.equals(this.functionParams[i].type) || !(myParam.getType() instanceof GenericTypeCodeAvatarClassHeader && anotherParam.getType() instanceof GenericTypeCodeAvatarClassHeader)){
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


    // for child of template ClassHeader from org.siphonlab.ago.classloader.ClassHeader.instantiate
    // for template GenericInstantiationClassDef from GenericInstantiationClassHeader.PlaceHolder.resolve
    protected ClassHeader applyInstantiation(ClassHeader inst, InstantiationArguments typeArguments, ClassHeader newParent) {
        if(inst.name == null) inst.name = name;
        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s apply template to %s".formatted(this.fullname, inst.fullname));
        inst.genericSource = new GenericSource(this.fullname, typeArguments, typeArguments.takeFor(this));
        this.putInstantiatedClassToCache(typeArguments, inst);
        inst.strings = this.strings;
        inst.parent = newParent;
        if (newParent != null) newParent.addChild(inst);
        // create slots later
        inst.setSuperClass(this.superClass);
        inst.type = this.type;
        if(inst instanceof GenericInstantiationClassHeader){
            inst.modifiers = (this.modifiers & GENERIC_TEMPLATE_NEG) | AgoClass.GENERIC_INSTANTIATION;
        } else {
            inst.modifiers = this.modifiers;
        }
        inst.setSourceLocation(this.sourceLocation);
        inst.setInterfaces(this.interfaces);    // apply instantiation for interface at resolveHierarchicalClasses, LN748
        inst.setPermitClass(this.permitClass);
        inst.setSlots(this.slotDescs);
        inst.fields = this.fields;

        instantiateChildren(inst, typeArguments);
        inst.setMethods(this.methods);

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
    private IoBuffer applyTemplateOnCode(IoBuffer code, InstantiationArguments instantiationArguments, ClassHeader instantFunction) {
        if ((this.modifiers & NATIVE) == NATIVE) return code;
        return genericVMCodeTransformer.transform(code, instantiationArguments, instantFunction);
    }

    public boolean isNativeClass() {
        return this.type == TYPE_CLASS && ((this.modifiers & NATIVE) == NATIVE);
    }

    public ClassHeader findMethod(MethodDesc methodDesc) {
        var r = classLoader.getClassHeader(methodDesc.getFullname());
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


    protected Map<InstantiationArguments, ClassHeader> instantiatedClasses;
    private boolean instantiateClassesHasUnsolvedGenericTypeDesc = false;
    public void putInstantiatedClassToCache(InstantiationArguments typeArguments, ClassHeader instantiation){
        if(this.isGenericInstantiation()){
            this.getSourceTemplate().putInstantiatedClassToCache(typeArguments, instantiation);
            return;
        }
        if(this.instantiatedClasses == null) this.instantiatedClasses = new HashMap<>();
        this.instantiatedClasses.put(typeArguments, instantiation);
    }
    public ClassHeader getCachedInstantiatedClass(InstantiationArguments instantiationArguments){
        if(this.isGenericInstantiation()){
            return this.getSourceTemplate().getCachedInstantiatedClass(instantiationArguments);
        }
        if(instantiatedClasses == null) return null;
        return instantiatedClasses.get(instantiationArguments);
    }

    public boolean isAffectedByTypeArguments(InstantiationArguments typeArguments){
        if(this.type == TYPE_PRIMITIVE_CLASS) return false;

        for(var p = this; p != null; p = p.parent){
            if(p.isGenericTemplate()){
                var r = typeArguments.canApplyOnTemplate(p);
                if(r) return true;
            }
        }
        if(this.superClass != null && !this.superClass.equals(this.fullname)){
            var superClass = classLoader.getClassHeader(this.superClass);
            if(superClass != this) {
                if (superClass.isAffectedByTypeArguments(typeArguments))return true;
            }
        }
        if(this.getInterfaces() != null){
            for (var anInterface : this.getInterfaces()) {
                var i =  classLoader.getClassHeader(anInterface);
                if(i.isAffectedByTypeArguments(typeArguments)) return true;
            }
        }
        if(this.genericSource != null){
            if(this.genericSource.instantiationArguments().canApply(typeArguments)){
                return true;
            }
        }
        if(this.permitClass != null){
            var p =  classLoader.getClassHeader(this.permitClass);
            if(p.isAffectedByTypeArguments(typeArguments)) return true;
        }
        return false;

    }

    public void nextStage() {
        this.setLoadingStage(loadingStage.nextStage());
    }

    ClassHeader getSuperClassHeader(){
        if(this.superClass == null) return null;
        return Objects.requireNonNull(classLoader.getClassHeader(this.superClass));
    }

    public boolean resolveHierarchicalClasses() {
        if(this.loadingStage != ResolveHierarchicalClasses) return true;

        var headers = classLoader.getHeaders();
        if(this.isGenericInstantiation()) {
            ClassHeader templ = this.getSourceTemplate();

            InstantiationArguments typeArguments = this.genericSource.instantiationArguments();
            if (StringUtils.isNotEmpty(templ.metaClass)) {
                MetaClassHeader metaClassHeader = (MetaClassHeader) classLoader.getClassHeader(templ.metaClass);
                if(metaClassHeader.instanceClass == null) metaClassHeader.setInstanceClass(templ);
                var names = GenericInstantiationClassHeader.composeMetaClassName(metaClassHeader.instanceClass, typeArguments);
                String fullname = names[1];
                String name = names[0];
                var appliedMetaClassHeader = (MetaClassHeader) metaClassHeader.instantiateMetaClass(null, name, fullname, typeArguments);
                appliedMetaClassHeader.setInstanceClass(this);
                appliedMetaClassHeader.setName(names[0]);
                this.metaClass = appliedMetaClassHeader.fullname;
            } else {
                assert StringUtils.isEmpty(this.metaClass);
            }
            if(templ.superClass != null) {
                if (!headers.containsKey(templ.superClass)) return false;
                this.setSuperClass(classLoader.instantiateDependencyClass(templ.superClass, typeArguments).fullname);
            }
            if(templ.permitClass != null){
                if(!headers.containsKey(templ.permitClass)) return false;
                this.setPermitClass(classLoader.instantiateDependencyClass(templ.permitClass, typeArguments).fullname);
            }

            String[] interfaces = templ.interfaces;
            if(interfaces != null){
                for (String interface_ : interfaces) {
                    if (!headers.containsKey(interface_)) return false;
                }
                var applied = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    String interface_ = interfaces[i];
                    var interfaceHeader = classLoader.getClassHeader(interface_);
                    if(this.isFunction() && interfaceHeader.isPermitForFunction() && interfaceHeader.isGenericTemplate()){
                        applied[i] = interface_; // skip, till parse fields, for params not loaded
                    } else {
                        applied[i] = classLoader.instantiateDependencyClass(interface_, typeArguments).fullname;
                    }
                }
                this.setInterfaces(applied);
            }
            if(this.methods != null) {
                List<MethodDesc> methodDescs = this.methods;
                boolean changed = false;
                for (int i = 0; i < methodDescs.size(); i++) {
                    MethodDesc methodDesc = methodDescs.get(i);
                    var m = classLoader.getClassHeader(methodDesc.getFullname());
                    if (m.parent != this) {       // inherited
                        var mInst = classLoader.instantiateDependencyClass(methodDesc.getFullname(), typeArguments);
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
                MetaClassHeader metaClassHeader = (MetaClassHeader) classLoader.getClassHeader(this.getMetaClass());
                metaClassHeader.setInstanceClass(this);
            }
        }
        this.setLoadingStage(ParseFields);
        return true;
    }

    private boolean isPermitForFunction(){
        if(this.permitClass == null) return false;
        var permitClass = classLoader.getClassHeader(this.permitClass);
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

    private ClassHeader applyFunctionInterface(ClassHeader interfaceHeader) {
        if(interfaceHeader.isGenericInstantiation()){
            interfaceHeader = interfaceHeader.getSourceTemplate();
        }
        ClassHeader[] arr = new ClassHeader[this.functionParams == null ? 1 : this.functionParams.length + 1];
        arr[0] = this.getFunctionResultType();
        if(this.functionParams != null) {
            for (int i = 0; i < functionParams.length; i++) {
                arr[i + 1] = functionParams[i].getType();
            }
        }
        var a = new InstantiationArguments(interfaceHeader, arr);
        return interfaceHeader.instantiate(a, null, null, null);
    }

    public boolean parseFields() {
        if(this.loadingStage != ParseFields) return true;

        if(this.isGenericInstantiation()) {
            ClassHeader templ = this.getSourceTemplate();

            InstantiationArguments typeArguments = this.genericSource.instantiationArguments();
            if(templ.loadingStage == ParseFields){
                if(!templ.parseFields()) return false;
            }

            this.fields = Arrays.stream(templ.fields).map(f -> f.applyTemplate(typeArguments)).toArray(VariableDesc[]::new);
            var slots = Arrays.stream(templ.slotDescs).map(slotDesc -> slotDesc.applyTemplate(typeArguments)).toArray(SlotDesc[]::new);
            this.setSlots(slots);

            if (this.isFunction()) {
                if (templ.getFunctionResultType() != null) {
                    this.setFunctionResultType(classLoader.instantiateDependencyClass(templ.functionResultType, typeArguments).fullname);
                }
                if (templ.functionParams != null) {
                    this.functionParams = Arrays.stream(templ.functionParams).map(f -> f.applyTemplate(typeArguments)).toArray(VariableDesc[]::new);
                }
                if (templ.functionVariables != null) {
                    this.functionVariables = Arrays.stream(templ.functionVariables).map(f -> f.applyTemplate(typeArguments)).toArray(VariableDesc[]::new);
                }
                this.nativeFunctionEntrance = templ.nativeFunctionEntrance;
                this.functionResultSlot = templ.functionResultSlot;

                var applied = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    String interface_ = interfaces[i];
                    var interfaceHeader = classLoader.getClassHeader(interface_);
                    if (this.isFunction() && interfaceHeader.isPermitForFunction() && interfaceHeader.isGenericTemplate()) {
                        applied[i] = applyFunctionInterface(interfaceHeader).fullname;
                    } else {
                        applied[i] = interface_;
                    }
                }
                this.setInterfaces(applied);
            }
        } else {
            classLoader.parseBody(this);
        }
        this.setLoadingStage(InstantiateFunctionFamily);
        return true;
    }

    public boolean parseCode() {
        if(this.loadingStage != ParseCode) return false;
        if(this.isGenericInstantiation()){
            if(this.isFunction()) {
                ClassHeader template = getSourceTemplate();
                if (this.isInGenericTemplate()) {
                    this.compiledCode = template.compiledCode;
                } else {
                    var functionCode = template.compiledCode.slice();
                    IoBuffer applied = template.applyTemplateOnCode(functionCode, genericSource.instantiationArguments(), this);
                    this.compiledCode = applied.rewind();
                }
                this.sourceMap = template.sourceMap;
            }
        }
        this.setLoadingStage(BuildClass);
        return true;
    }

    public AgoClass buildClass(){
        if(this.loadingStage != BuildClass) return this.agoClass;

        if(this.isGenericInstantiation()){
            var templ = this.getSourceTemplate();
            if(templ.loadingStage == BuildClass){
                templ.buildClass();
            }
        }

        MetaClass metaClass;
        if(StringUtils.isNotEmpty(this.getMetaClass())){
            var metaHeader = classLoader.getClassHeader(this.getMetaClass());
            if(metaHeader.loadingStage == BuildClass){
                metaClass = (MetaClass) metaHeader.buildClass();
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
            case TYPE_PRIMITIVE_CLASS:
                agoClass = new AgoPrimitiveClass(classLoader, this.name, this.getTypeCode().value);
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

    public boolean processLoadClassName(MutableObject<ClassHeader> createdClass) {
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
                var f = this.findMethod(method);
                method.setFunctionClassHeader(f);
                int functionIndex = method.getMethodIndex();
                if (functionIndex <= methods.size() - 1) {
                    if (methods.get(functionIndex) != null) {
                        this.findMethod(method);
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

    void setConcreteTypeInfo(){
        if(this.isGenericInstantiation()) {
            InstantiationArguments instantiationArguments = genericSource.instantiationArguments();
            ClassHeader sourceTemplate = this.getSourceTemplate();
            var args = instantiationArguments.takeFor(sourceTemplate);
            if (args != null) {
                var typeInfos = Arrays.stream(args).map(t ->
                        classLoader.getClass(t.className())
                ).toArray(AgoClass[]::new);
                agoClass.setConcreteTypeInfo(new GenericArgumentsInfo(sourceTemplate.agoClass, typeInfos));
            }
        }

        if(this.isGenericTemplate()){
            var arr = Arrays.stream(this.genericTypeParams).map(p -> p.agoClass).toArray(AgoClass[]::new);
            agoClass.setConcreteTypeInfo(new GenericTypeParametersInfo(arr));
        }
    }

    void buildInterfaceMethodMap(Map<String, ClassHeader> headers){
        if(this.getInterfaces() != null && this.interfaces.length != 0){
            Int2ObjectHashMap<int[]> interfaceMethods = new Int2ObjectHashMap<>();
            String[] interfaces = this.getInterfaces();
            int maxClassId = 0;
            for (int k = 0; k < interfaces.length; k++) {
                String interface_ = interfaces[k];
                var interfaceHeader = classLoader.getClassHeader(interface_);
                if(interfaceHeader.isGenericInstantiation()){
                    interfaceHeader = interfaceHeader.getSourceTemplate();
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
        if(sourceLocation != null) {
            this.sourceFilename = sourceLocation.getFilename();
        }
        this.sourceLocation = sourceLocation;
    }

    public void setSourceHeader(ClassHeader sourceHeader) {
        this.sourceHeader = sourceHeader;
    }

    public ClassHeader getSourceHeader() {
        return sourceHeader;
    }

    public boolean isGenericTerminated() {
        return false;
    }

    public boolean isReady(String className){
        var classHeader = this.classLoader.getClassHeader(className);
        if(classHeader == null) return false;
//        if(!classHeader.isReady()) return false;      // not sure
        return true;
    }

    public boolean isReady(){
        switch (this.loadingStage){
            case LoadClassNames:
                return true;
            case ResolveHierarchicalClasses:
                if(this.superClass != null && !this.superClass.equals(this.fullname)) {
                    if(!isReady(this.superClass)) return false;
                }
                if(this.interfaces != null){
                    for (String anInterface : this.interfaces) {
                        if(!isReady(anInterface)) return false;
                    }

                }
                if(this.permitClass != null) {
                    if(!isReady(this.permitClass)) return false;
                }
                return true;
            default:
                if(this.isFunction()){
                    if(this.functionResultType != null) {
                        if(!isReady(functionResultType)) return false;
                    }
                }
                if(this.slotDescs != null) {
                    for (SlotDesc slotDesc : this.slotDescs) {
                        if (!isReady(slotDesc.type)) return false;
                    }
                }
                return true;
        }
    }

    public ClassHeader getFunctionResultType() {
        return classLoader.getClassHeader(functionResultType);
    }

    public void setFunctionResultType(String functionResultType) {
        this.functionResultType = functionResultType;
    }
}
