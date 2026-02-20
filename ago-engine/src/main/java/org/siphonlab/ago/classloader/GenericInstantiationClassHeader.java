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
import org.siphonlab.ago.TypeCode;

import java.util.*;
import java.util.stream.Collectors;

import static org.siphonlab.ago.AgoClass.GENERIC_TEMPLATE_NEG;
import static org.siphonlab.ago.classloader.LoadingStage.*;

public class GenericInstantiationClassHeader extends ClassHeader {

    final ClassHeader templateClass;
    private String parentClassName;

    public GenericInstantiationClassHeader(String fullname, byte type, ClassHeader templateClass, GenericTypeArguments genericTypeArguments, AgoClassLoader classLoader) {
        super(fullname, type, 0, null, classLoader);
        this.genericSource = new GenericSource(templateClass, genericTypeArguments);
        this.templateClass = templateClass;
    }

    private static int composeModifiers(ClassHeader templateClass, GenericTypeArguments arguments) {
        var modifiers = (templateClass.modifiers & GENERIC_TEMPLATE_NEG) | AgoClass.GENERIC_INSTANTIATION;
        if(arguments.isIntermediate()){
            return modifiers | AgoClass.GENERIC_TEMPLATE;
        }
        return modifiers;
    }

    public static String composeClassName(String baseTemplateName, GenericTypeArguments genericTypeArguments) {
        return baseTemplateName + "<" + Arrays.stream(genericTypeArguments.getTypeArgumentsArray())
                .map(l -> {
                    if(l.typeCode == TypeCode.OBJECT) {
                        return l.getClassName();
                    } else if(l instanceof GenericTypeDesc g) {
                        assert !g.isPlaceHolder;
                        return g.asClassNamePart();
                    } else {
                        return l.typeCode.toString();
                    }
                }).collect(Collectors.joining("|")) + ">";
    }

    public static String[] composeMetaClassName(ClassHeader instanceTemplate, GenericTypeArguments genericTypeArguments, Map<String, ClassHeader> headers) {
        var instance = instanceTemplate.resolveTemplateInstantiation(headers, genericTypeArguments);
        String name = "Meta@<" + instance.name + ">";
        var parent = instanceTemplate.parent;
        if(parent != null) {
            String fullCalssName = instance.name;       // full class name without package, parent.parent...me
            List<ClassHeader> parents = new ArrayList<>();
            for(; parent != null; parent = parent.parent){
                parents.add(parent.resolveTemplateInstantiation(headers, genericTypeArguments));
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
    public ClassHeader clone(ClassHeader newParent, Map<String, ClassHeader> headers) {
        if (this.parent == null || !this.fullname.startsWith(this.parent.fullname)) {
            return this;
        }
        String fullname = newParent.fullname + '.' + this.name;
        var existed = headers.get(fullname);
        if (existed != null)
            return existed;

        var inst = new GenericInstantiationClassHeader(fullname, this.type, this.templateClass, this.genericTypeArguments(), this.classLoader);
        inst.setName(this.name);
        copyToClone(inst, headers);
        inst.parent = newParent;
        inst.modifiers = this.modifiers;
        classLoader.registerNewClass(inst);
        return inst;
    }

    @Override
    public boolean isAffectedBy(Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments) {
        return super.isAffectedBy(headers, genericTypeArguments);
    }

    public GenericTypeArguments genericTypeArguments(){
        return genericSource.typeArguments();
    }

    @Override
    public String toString() {
        return "(GenericInstantiationHeader %s %s [%s])".formatted(this.fullname, this.classId, this.genericTypeArguments());
    }

    @Override
    public boolean processLoadClassName(Map<String, ClassHeader> headers, MutableObject<ClassHeader> createdClass) {
        if(this.loadingStage == LoadClassNames){
            this.nextStage();
        }
        if(StringUtils.isNotEmpty(this.parentClassName)){
            ClassHeader parent = headers.get(this.parentClassName);
            if(parent == null) return false;
            this.parent = parent;
            parent.addChild(this);
        }
        var args = this.genericTypeArguments();
        if(this.parent != null && this.parent.genericSource != null){
            args = this.parent.genericSource.typeArguments().applyChild(args, headers);
        }
        this.templateClass.applyInstantiation(this, args, this.parent, headers);
        return true;
    }

    @Override
    public boolean parseFields(Map<String, ClassHeader> headers) {
        if(this.loadingStage != ParseFields) return true;

        if (this.templateClass != null && this.templateClass.loadingStage == ParseFields) {
            if (!this.templateClass.parseFields(headers)) return false;
        }

        GenericTypeArguments typeArguments = this.genericSource.typeArguments();
        if(!typeArguments.resolvePlaceHolderArguments(headers)) return false;

        this.modifiers = composeModifiers(templateClass, typeArguments);

        return super.parseFields(headers);
    }

    public boolean instantiateFunctionFamily(Map<String, ClassHeader> headers){
        if (this.loadingStage != InstantiateFunctionFamily)
            return true;

        if (this.templateClass.isFunction() && StringUtils.isNotEmpty(this.parentClassName)) {
            ClassHeader parent = headers.get(this.parentClassName);
            if(parent != null) {
                parent.registerFunctionInstantiation(this, headers);
                this.templateClass.instantiateFunctionFamily(parent, this, 0, headers, this.genericTypeArguments());
            }
        }
        this.nextStage();
        return true;
    }

    public static class PlaceHolder extends ClassHeader{

        final String templateClassName;
        final TypeDesc[] arguments;
        public String parentClassName;

        /**
         * sometimes template class is still not loaded in headers, put this placeholder in headers, after all classed loaded, call `resolve` to replace with GenericInstantiationClassHeader
         */
        public PlaceHolder(String fullname, String templateClassName, TypeDesc[] arguments, AgoClassLoader classLoader) {
            super(fullname, (byte)0, 0, null, classLoader);
            this.templateClassName = templateClassName;
            this.arguments = arguments;
        }

        private GenericInstantiationClassHeader resolve(Map<String, ClassHeader> headers){
            var template = headers.get(templateClassName);
            GenericInstantiationClassHeader g = new GenericInstantiationClassHeader(fullname, template.type, template, new GenericTypeArguments(template, arguments, headers), this.classLoader);
            g.setClassId(this.classId);
            g.setName(this.name);
            g.setMetaClass(this.getMetaClass());
            g.setSuperClass(this.superClass);
            g.setInterfaces(this.interfaces);
            g.setPermitClass(this.getPermitClass());
            g.parentClassName = parentClassName;

            return g;
        }

        @Override
        public boolean processLoadClassName(Map<String, ClassHeader> headers, MutableObject<ClassHeader> createdClass) {
            if(this.loadingStage != LoadClassNames) return true;
            ClassHeader templ = headers.get(templateClassName);
            if(templ == null) return false;

            var inst = resolve(headers);
            headers.put(inst.fullname, inst);
            // now the arguments is not available, don't register to cache
//            inst.genericTypeArguments().resolvePlaceHolderArguments(headers);
//            templ.registerGenericInstantiationClass(inst.genericTypeArguments(), inst);
            this.nextStage();
            createdClass.setValue(inst);

            return true;
        }
    }
}
