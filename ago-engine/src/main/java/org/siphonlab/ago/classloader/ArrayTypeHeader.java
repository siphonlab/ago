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

    public ArrayTypeHeader(String fullname, TypeDesc elementType, AgoClassLoader classLoader) {
        super(fullname, TYPE_CLASS, PUBLIC, null, classLoader);
        this.elementType = elementType;
        this.name = fullname;
    }


    @Override
    public ClassHeader tryInstantiate(ClassHeader newParent, Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments) {
        if (this.parent != null && !this.fullname.startsWith(this.parent.fullname)) {
            return this;
        }
        TypeDesc elementInst = elementType.applyTemplate(headers, genericTypeArguments);
        String n = newParent == null ? '[' + elementInst.getName() : newParent.fullname() + '[' + elementInst.getName();
        var existed = headers.get(n);
        if(existed != null) return existed;

        return instantiate(newParent, headers, genericTypeArguments);
    }

    @Override
    protected ClassHeader instantiate(ClassHeader newParent, Map<String, ClassHeader> headers, GenericTypeArguments typeArguments) {
        TypeDesc elementInst = elementType.applyTemplate(headers, typeArguments);
        String n = newParent == null ? '[' + elementInst.getName() : newParent.fullname() + '[' + elementInst.getName();
        var inst = new ArrayTypeHeader(n, elementInst, classLoader);
        inst.setClassId(headers.size());
        headers.put(inst.fullname, inst);
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
        TypeDesc appliedElement = elementType.applyTemplate(headers, typeArguments);
        String n = this.parent == null ? '[' + appliedElement.getName() : this.parent.fullname() + '[' + appliedElement.getName();
        return headers.get(n);
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
        this.setMetaClass(arrayBase.getMetaClass());
        this.setInterfaces(arrayBase.interfaces);
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
