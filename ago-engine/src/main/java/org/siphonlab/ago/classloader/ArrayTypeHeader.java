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

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.ArrayInfo;
import org.siphonlab.ago.TypeCode;

import java.util.Map;

import static org.siphonlab.ago.AgoClass.*;
import static org.siphonlab.ago.TypeCode.OBJECT;
import static org.siphonlab.ago.classloader.LoadingStage.BuildClass;
import static org.siphonlab.ago.classloader.LoadingStage.LoadClassNames;

public class ArrayTypeHeader extends ClassHeader {

    protected TypeDesc elementType;

    public ArrayTypeHeader(String fullname, String name, TypeDesc elementType, AgoClassLoader classLoader) {
        super(fullname, TYPE_CLASS, PUBLIC, null, classLoader);
        this.elementType = elementType;
        this.name = name;
    }


    @Override
    public ClassHeader tryInstantiate(ClassHeader newParent, Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments) {
        if (this.parent != null && !this.fullname.startsWith(this.parent.fullname)) {
            return this;
        }
        TypeDesc elementInst = elementType.applyTemplate(headers, genericTypeArguments);
        var p = composeName(newParent, elementInst, headers);
        var existed = headers.get(p.getRight());
        if(existed != null) return existed;

        return instantiate(newParent, headers, genericTypeArguments);
    }

    private static Pair<String, String> composeName(ClassHeader newParent, TypeDesc elementDesc, Map<String, ClassHeader> headers){
        String name;
        String fullname;
        if (newParent == null) {
            if(elementDesc.typeCode.isObject()) {
                var el = headers.get(elementDesc.getClassName());
                name = '[' + el.getName();
                fullname = el.extractPackagePrefix() + name;
            } else if(elementDesc instanceof GenericTypeDesc g){
                if(g.className != null){
                    var el = headers.get(g.className);
                    name = '[' + g.asClassNamePart();
                    fullname = el.extractPackagePrefix() + name;
                } else {
                    name = '[' + g.asClassNamePart();
                    fullname = name;
                }
            } else {
                name = '[' + elementDesc.asClassNamePart();
                fullname = name;
            }
        } else {
            name = '[' + elementDesc.asClassNamePart();
            fullname = newParent.fullname() + '[' + elementDesc.asClassNamePart();
        }
        return Pair.of(name, fullname);
    }

    @Override
    protected ClassHeader instantiate(ClassHeader newParent, Map<String, ClassHeader> headers, GenericTypeArguments typeArguments) {
        boolean b = false;
        if(elementType instanceof GenericTypeDesc g){
            if(g.isPlaceHolder){
                this.elementType = g = g.resolveExactType(headers);
            }
            if(typeArguments.canApplyToTemplate(headers.get(g.templateClass), headers)) {
                b = true;
            }
        } else if(elementType.getTypeCode() == OBJECT && typeArguments.canApplyToTemplate(headers.get(elementType.getClassName()), headers)){
            b = true;
        }
        if(!b) return this;

        TypeDesc elementInst = elementType.applyTemplate(headers, typeArguments);
        var p = composeName(newParent, elementInst, headers);
        String fullname = p.getRight();
        String name = p.getLeft();
        var existed = headers.get(fullname);
        if(existed != null) return existed;
        var inst = new ArrayTypeHeader(fullname, name, elementInst, classLoader);
        inst.setClassId(headers.size());
        classLoader.registerNewClass(inst);
        applyInstantiation(inst, typeArguments, newParent, headers);
        return inst;
    }

    @Override
    public boolean isAffectedBy(Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments) {
        if(elementType.typeCode == TypeCode.OBJECT){
            var h = headers.get(elementType.className);
            return h.isAffectedBy(headers, genericTypeArguments);
        } else if(elementType instanceof GenericTypeDesc g){
            return genericTypeArguments.containsType(g);
        }
        return false;
    }

    @Override
    public ClassHeader resolveTemplateInstantiation(Map<String, ClassHeader> headers,  GenericTypeArguments typeArguments) {
        return this.tryInstantiate(this.parent, headers, typeArguments);
    }

    @Override
    public boolean processLoadClassName(Map<String, ClassHeader> headers, MutableObject<ClassHeader> createdClass) {
        if(this.loadingStage != LoadClassNames) return true;
        this.nextStage();
        return true;
    }

    @Override
    public boolean resolveHierarchicalClasses(Map<String, ClassHeader> headers) {
        if(this.loadingStage != LoadingStage.ResolveHierarchicalClasses) return true;

        var arrayBase = headers.get("lang.Array");
        if(arrayBase.loadingStage == LoadingStage.ResolveHierarchicalClasses){
            arrayBase.resolveHierarchicalClasses(headers);
        }
        var instantiation = arrayBase.resolveTemplateInstantiation(headers, new GenericTypeArguments(arrayBase, new TypeDesc[]{this.elementType}, headers));
        this.setSuperClass(instantiation.fullname);
        this.setChildren(instantiation.getChildren());
        this.setMethods(instantiation.methods.stream().map(
                m -> new MethodDesc(m.getName(),m.getFullname())
                ).toList());
        this.strings = arrayBase.strings;
        var instantiationMetaClass = headers.get(instantiation.getMetaClass());
        String metaFullname = this.extractPackagePrefix() + "Meta@<" + this.name + ">";
        var existed = headers.get(metaFullname);
        if(existed == null){
            var metaHeader = new MetaClassHeader(metaFullname, TYPE_METACLASS, instantiationMetaClass.modifiers, instantiationMetaClass.getSlice().slice(), instantiationMetaClass.classLoader);
            metaHeader.setSuperClass(instantiationMetaClass.fullname);
            metaHeader.resolveHierarchicalClasses(headers);
            classLoader.registerNewClass(metaHeader);
            this.setMetaClass(metaHeader.fullname);
        } else {
            this.setMetaClass(metaFullname);
        }
        this.setInterfaces(instantiation.interfaces);
        this.setLoadingStage(LoadingStage.ParseFields);
        return true;
    }


    /*
        protected void inherits(ClassHeader superClass, Map<String, ClassHeader> headers){
        this.setMethods(Arrays.stream(superClass.methods).map(m ->
                        new MethodDesc(m.distanceToSuperClass() + 1,m.name())
                ).toArray(MethodDesc[]::new));
        this.setFunctionIndex(superClass.functionIndex);
        this.strings = superClass.strings;
        this.genericTypeParamDescs = superClass.genericTypeParamDescs;
        this.setSlots(superClass.slotDescs);
        this.fields = superClass.fields;
        this.setLoadingStage(ParseCode);
    }
     */

    public boolean parseFields(Map<String, ClassHeader> headers) {
        if(this.loadingStage != LoadingStage.ParseFields) return true;

        if(this.elementType instanceof GenericTypeDesc g && g.isPlaceHolder){
            GenericTypeDesc exactType = g.resolveExactType(headers);
            if(exactType == null) return false;
            this.elementType = exactType;
        }

        var arrayBase = headers.get(this.superClass);
        if(arrayBase.loadingStage == LoadingStage.ParseFields){
            if(!arrayBase.parseFields(headers)) return false;
        }

        this.fields = arrayBase.fields;
        this.slotDescs = arrayBase.slotDescs;

        this.setLoadingStage(LoadingStage.BuildClass);
        return true;
    }

    @Override
    public AgoClass buildClass(Map<String, ClassHeader> headers) {
        if(this.loadingStage != BuildClass) return this.agoClass;
        if(this.elementType.typeCode == OBJECT) {
            var eleClass = headers.get(elementType.className);
            if (eleClass.loadingStage == BuildClass){
                var r = eleClass.buildClass(headers);
                if (r == null) return null;
            }
        }
        var agoClass = super.buildClass(headers);
        agoClass.setConcreteTypeInfo(new ArrayInfo(this.elementType.toTypeInfo(headers)));
        return agoClass;
    }
}
