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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.siphonlab.ago.AgoClass;

import java.util.*;
import java.util.stream.Collectors;

import static org.siphonlab.ago.AgoClass.GENERIC_TEMPLATE_NEG;
import static org.siphonlab.ago.classloader.LoadingStage.*;

public class GenericInstantiationClassHeader extends ClassHeader {

    final String templateClass;
    private String[] argumentNames;
    private InstantiationArguments arguments;
    protected String parentClassName;

    public GenericInstantiationClassHeader(String fullname, String templateClass, String[] arguments, AgoClassLoader agoClassLoader) {
        super(fullname, (byte) 0, 0, null, agoClassLoader);
        this.fullname = fullname;
        this.templateClass = templateClass;
        this.argumentNames = arguments;
    }

    public GenericInstantiationClassHeader(String fullname, byte type,String templateClass, InstantiationArguments instantiationArguments, AgoClassLoader classLoader) {
        super(fullname, type, 0, null, classLoader);
        ClassRefValue[] typeArguments = instantiationArguments.takeFor(classLoader.getClassHeader(templateClass));
        this.genericSource = new GenericSource(templateClass, instantiationArguments, typeArguments);
        this.templateClass = templateClass;
        this.arguments = instantiationArguments;
        if(typeArguments != null) {
            this.argumentNames = Arrays.stream(typeArguments).map(ClassRefValue::className).toArray(String[]::new);
        }
    }

    private static int composeModifiers(ClassHeader templateClass) {
        return (templateClass.modifiers & GENERIC_TEMPLATE_NEG) | AgoClass.GENERIC_INSTANTIATION;
    }

    public static String composeClassName(String baseTemplateName, ClassRefValue[] instantiationArguments) {
        return baseTemplateName + "<" + Arrays.stream(instantiationArguments).map(ClassRefValue::className).collect(Collectors.joining(",")) + ">";
    }

    public static String[] composeMetaClassName(ClassHeader instanceTemplate, InstantiationArguments instantiationArguments) {
        var instance = instanceTemplate.classLoader.instantiateReferenceClass(instanceTemplate.fullname, instantiationArguments);
        String name = "Meta@<" + instance.name + ">";
        var parent = instanceTemplate.parent;
        if(parent != null) {
            String fullCalssName = instance.name;       // full class name without package, parent.parent...me
            List<ClassHeader> parents = new ArrayList<>();
            for(; parent != null; parent = parent.parent){
                parents.add(parent.instantiate(instantiationArguments, null, null, null));
            }
            for (int i = 0; i < parents.size(); i++) {
                ClassHeader p = parents.get(i);
                if(i == parents.size() - 1){
                    name = "Meta@<%s>".formatted(p.name + "." + fullCalssName);
                    fullCalssName = p.extractPackagePrefix() + name;
                } else {
                    fullCalssName = p.name + "." + fullCalssName;
                }
            }
            return new String[]{name, fullCalssName};
        } else {
            String fullname = instanceTemplate.extractPackagePrefix() + name;
            return new String[]{name, fullname};
        }
    }

    @Override
    public ClassHeader clone(ClassHeader newParent) {
        if (this.parent == null || !this.fullname.startsWith(this.parent.fullname)) {
            return this;
        }
        String fullname = newParent.fullname + '.' + this.name;
        var existed = classLoader.getClassHeader(fullname);
        if (existed != null)
            return existed;

        var inst = new GenericInstantiationClassHeader(fullname, this.type, this.templateClass, this.arguments, this.classLoader);
        inst.setName(this.name);
        copyToClone(inst);
        inst.parent = newParent;
        inst.modifiers = this.modifiers;
        classLoader.registerNewClass(inst);
        return inst;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments typeArguments) {
        return super.isAffectedByTypeArguments(typeArguments);
    }

    public InstantiationArguments genericTypeArguments(){
        if(genericSource != null) {
            return genericSource.instantiationArguments();
        } else {
            InstantiationArguments args;
            ClassHeader sourceTemplate = this.getSourceTemplate();
            if(this.arguments != null) {
                args = this.arguments;
            } else {
                args = new InstantiationArguments(sourceTemplate,
                        Arrays.stream(this.argumentNames).map(a -> Objects.requireNonNull(classLoader.getClassHeader(a))).toArray(ClassHeader[]::new));
            }
            this.genericSource = new GenericSource(sourceTemplate.fullname(), args, args.takeFor(sourceTemplate));
            this.type = sourceTemplate.type;
            return this.arguments = args;
        }
    }

    @Override
    public String toString() {
        return "(GenericInstantiationHeader %s %s [%s])".formatted(this.fullname, this.classId, StringUtils.join(this.argumentNames, ','));
    }

    @Override
    public boolean processLoadClassName(MutableObject<ClassHeader> createdClass) {
        if(this.loadingStage == LoadClassNames){
            this.nextStage();
        }
        if(StringUtils.isNotEmpty(this.parentClassName)){
            ClassHeader parent = classLoader.getClassHeader(this.parentClassName);
            if(parent == null) return false;
            this.parent = parent;
            parent.addChild(this);
        }
        var args = this.genericTypeArguments();
        ClassHeader sourceTemplate = this.getSourceTemplate();
        if(this.parent != null && this.parent.isGenericInstantiation()){
            args = args.applyParent(this.parent.genericSource.instantiationArguments(), classLoader);
            this.genericSource = new GenericSource(this.genericSource.sourceTemplate(), args, this.genericSource.typeArguments());
            sourceTemplate.putInstantiatedClassToCache(args, this);
        }
        sourceTemplate.applyInstantiation(this, args, this.parent);
        return true;
    }

    @Override
    public ClassHeader getSourceTemplate() {
        return Objects.requireNonNull(classLoader.getClassHeader(templateClass));
    }

    @Override
    public boolean parseFields() {
        if(this.loadingStage != ParseFields) return true;

        var templateClass = getSourceTemplate();
        if (templateClass != null && templateClass.loadingStage == ParseFields) {
            if (!templateClass.parseFields()) return false;
        }

        this.modifiers = composeModifiers(templateClass);

        return super.parseFields();
    }

    public boolean instantiateFunctionFamily(){
        if (this.loadingStage != InstantiateFunctionFamily)
            return true;

        var templateClass = getSourceTemplate();
        if (templateClass.isFunction() && StringUtils.isNotEmpty(this.parentClassName)) {
            ClassHeader parent = classLoader.getClassHeader(this.parentClassName);
            if(parent != null) {
                parent.registerFunctionInstantiation(this);
                templateClass.instantiateFunctionFamily(parent, this, 0, this.genericTypeArguments());
            }
        }
        this.nextStage();
        return true;
    }

//    public static class PlaceHolder extends ClassHeader{
//
//        final String templateClassName;
//        final TypeDesc[] arguments;
//        public String parentClassName;
//
//        /**
//         * sometimes template class is still not loaded in headers, put this placeholder in headers, after all classed loaded, call `resolve` to replace with GenericInstantiationClassHeader
//         */
//        public PlaceHolder(String fullname, String templateClassName, TypeDesc[] arguments, AgoClassLoader classLoader) {
//            super(fullname, (byte)0, 0, null, classLoader);
//            this.templateClassName = templateClassName;
//            this.arguments = arguments;
//        }
//
//        private GenericInstantiationClassHeader resolve(Map<String, ClassHeader> headers){
//            var template = headers.get(templateClassName);
//            GenericInstantiationClassHeader g = new GenericInstantiationClassHeader(fullname, template.type, template, new InstantiationArguments(template, arguments, headers), this.classLoader);
//            g.setClassId(this.classId);
//            g.setName(this.name);
//            g.setMetaClass(this.getMetaClass());
//            g.setSuperClass(this.superClass);
//            g.setInterfaces(this.interfaces);
//            g.setPermitClass(this.getPermitClass());
//            g.parentClassName = parentClassName;
//
//            return g;
//        }
//
//        @Override
//        public boolean processLoadClassName(Map<String, ClassHeader> headers, MutableObject<ClassHeader> createdClass) {
//            if(this.loadingStage != LoadClassNames) return true;
//            ClassHeader templ = headers.get(templateClassName);
//            if(templ == null) return false;
//
//            var inst = resolve(headers);
//            headers.put(inst.fullname, inst);
//            // now the arguments is not available, don't register to cache
////            inst.genericTypeArguments().resolvePlaceHolderArguments(headers);
////            templ.registerGenericInstantiationClass(inst.genericTypeArguments(), inst);
//            this.nextStage();
//            createdClass.setValue(inst);
//
//            return true;
//        }
//    }

    @Override
    public boolean isReady() {
        switch (this.loadingStage){
            case LoadClassNames:
                if(!isReady(this.templateClass)) return false;
                for (String argumentName : this.argumentNames) {
                    if(!isReady(argumentName)) return false;
                }
                if(StringUtils.isNotEmpty(this.parentClassName)) {
                    if (!isReady(this.parentClassName)) return false;
                }
                return true;
            default:
                return super.isReady();
        }
    }
}
