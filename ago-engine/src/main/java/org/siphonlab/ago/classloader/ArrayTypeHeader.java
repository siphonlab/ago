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
import java.util.Objects;

import static org.siphonlab.ago.AgoClass.*;
import static org.siphonlab.ago.classloader.LoadingStage.*;

public class ArrayTypeHeader extends ClassHeader {

    private final String elementTypeName;
    protected ClassHeader elementType;

    public ArrayTypeHeader(String fullname, String name, String elementTypeName, AgoClassLoader classLoader) {
        super(fullname, TYPE_CLASS, PUBLIC, null, classLoader);
        this.elementTypeName = elementTypeName;
        this.name = name;
    }

    public ClassHeader getElementType() {
        if(this.elementType == null){
            this.elementType = Objects.requireNonNull(classLoader.getClassHeader(this.elementTypeName));
        }
        return this.elementType;
    }

    @Override
    public boolean isGenericTerminated() {
        return this.getElementType().isGenericTerminated();
    }

    private static Pair<String, String> composeName(ClassHeader elementClassHeader, Map<String, ClassHeader> headers){
        String name;
        String fullname;
        if(elementClassHeader.getTypeCode().isObject()) {
            var el = headers.get(elementClassHeader.fullname());
            name = '[' + el.getName();
            fullname = el.extractPackagePrefix() + name;
        } else if(elementClassHeader instanceof GenericTypeCodeAvatarClassHeader g){
            if(g.fullname != null){
                var el = headers.get(g.fullname);
                name = '[' + g.getName();
                fullname = el.extractPackagePrefix() + name;
            } else {
                name = '[' + g.getName();
                fullname = name;
            }
        } else {
            name = '[' + elementClassHeader.getName();
            fullname = name;
        }
        return Pair.of(name, fullname);
    }


    @Override
    protected ClassHeader instantiate(InstantiationArguments typeArguments, ClassHeader parentInstantiation, String suggestionName, String suggestionFullName) {
        if(!this.isAffectedByTypeArguments(typeArguments)) return this;

        var elementInst = classLoader.instantiateReferenceClass(getElementType().fullname, typeArguments);
        String name;
        String fullname;
        if(suggestionName == null) {
            var p = composeName(elementInst, this.classLoader.getHeaders());
            name = p.getLeft();
            fullname = p.getRight();
        } else {
            name = suggestionName;
            fullname = suggestionFullName;
        }
        var existed = classLoader.getClassHeader(fullname);
        if(existed != null) return existed;
        var inst = new ArrayTypeHeader(fullname, name, elementInst.fullname, classLoader);
        inst.setClassId(classLoader.getHeaders().size());
        classLoader.registerNewClass(inst);
//        classLoader.getClassHeader("lang.Array").applyInstantiation(inst, typeArguments, parent);     // inst should not register to template lang.Array
        inst.setLoadingStage(ResolveHierarchicalClasses);
        return inst;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments typeArguments) {
        return this.getElementType().isAffectedByTypeArguments(typeArguments);
    }

    @Override
    public boolean processLoadClassName(MutableObject<ClassHeader> createdClass) {
        if(this.loadingStage != LoadClassNames) return true;
        this.nextStage();
        return true;
    }

    @Override
    public boolean resolveHierarchicalClasses() {
        if(this.loadingStage != LoadingStage.ResolveHierarchicalClasses) return true;

        var arrayBase = classLoader.getClassHeader("lang.Array");
        if(arrayBase.loadingStage == LoadingStage.ResolveHierarchicalClasses){
            arrayBase.resolveHierarchicalClasses();
        }
        var instantiation = arrayBase.instantiate(new InstantiationArguments(arrayBase, new ClassHeader[]{this.getElementType()}), null, null, null);
        this.setSuperClass(instantiation.fullname);
        this.setChildren(instantiation.getChildren());
        this.setMethods(instantiation.methods.stream().map(
                m -> new MethodDesc(m.getName(),m.getFullname())
                ).toList());
        this.strings = arrayBase.strings;
        var instantiationMetaClass = classLoader.getClassHeader(instantiation.getMetaClass());
        String metaFullname = this.extractPackagePrefix() + "Meta@<" + this.name + ">";
        var existed = classLoader.getClassHeader(metaFullname);
        if(existed == null){
            var metaHeader = new MetaClassHeader(metaFullname, TYPE_METACLASS, instantiationMetaClass.modifiers, instantiationMetaClass.getSlice().slice(), instantiationMetaClass.classLoader);
            metaHeader.setSuperClass(instantiationMetaClass.fullname);
            metaHeader.resolveHierarchicalClasses();
            classLoader.registerNewClass(metaHeader);
            this.setMetaClass(metaHeader.fullname);
            metaHeader.setInstanceClass(this);
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

    public boolean parseFields() {
        if(this.loadingStage != LoadingStage.ParseFields) return true;

        var arrayBase = classLoader.getClassHeader(this.superClass);
        if(arrayBase.loadingStage == LoadingStage.ParseFields){
            if(!arrayBase.parseFields()) return false;
        }

        this.fields = arrayBase.fields;
        this.slotDescs = arrayBase.slotDescs;

        this.setLoadingStage(LoadingStage.BuildClass);
        return true;
    }

    @Override
    public AgoClass buildClass() {
        if(this.loadingStage != BuildClass) return this.agoClass;
        if(this.elementType.getTypeCode() == TypeCode.OBJECT) {
            var eleClass = classLoader.getClassHeader(elementType.fullname);
            if (eleClass.loadingStage == BuildClass){
                var r = eleClass.buildClass();
                if (r == null) return null;
            }
        }
        var agoClass = super.buildClass();
        agoClass.setConcreteTypeInfo(new ArrayInfo(this.elementType.agoClass));
        return agoClass;
    }

    @Override
    public boolean isReady() {
        return isReady(this.elementTypeName);
    }
}
