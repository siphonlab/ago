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
import org.siphonlab.ago.ParameterizedClassInfo;

import java.util.List;
import java.util.Map;

import static org.siphonlab.ago.AgoClass.*;
import static org.siphonlab.ago.classloader.LoadingStage.*;

public class ParameterizedClassHeader extends ClassHeader {
    final String baseClass;
    final String constructor;
    final Object[] arguments;
    private ClassHeader baseClassHeader;

    public ParameterizedClassHeader(String fullname, String baseClass, String metaClass, String constructor, Object[] arguments, AgoClassLoader classLoader) {
        super(fullname, TYPE_CLASS, PUBLIC, null, classLoader);
        this.baseClass = baseClass;
        this.setMetaClass(metaClass);
        this.constructor = constructor;
        this.arguments = arguments;
    }

    @Override
    public boolean processLoadClassName(Map<String, ClassHeader> headers, MutableObject<ClassHeader> createdClass) {
        if(this.loadingStage != LoadClassNames) return true;
        var base = headers.get(baseClass);
        if(base == null) return false;
        if(base.loadingStage == LoadClassNames) {
            if(!base.processLoadClassName(headers, createdClass)) return false;
        }
        base.copyToClone(this, headers);        // copyToClone will overwrite stage
        this.loadingStage = LoadClassNames;
        this.nextStage();
        return true;
    }

    @Override
    public boolean resolveHierarchicalClasses(Map<String, ClassHeader> headers) {
        if(this.loadingStage != LoadingStage.ResolveHierarchicalClasses) return true;

        var base = this.baseClassHeader == null ? this.baseClassHeader = headers.get(baseClass) : this.baseClassHeader;
        if(base.loadingStage == LoadingStage.ResolveHierarchicalClasses){
            if(!base.resolveHierarchicalClasses(headers)) return false;
        }
        this.baseClassHeader = base;
        this.setMetaClass(base.getMetaClass());
        this.setSuperClass(base.getSuperClass());
        this.setPermitClass(base.getPermitClass());
        this.setInterfaces(base.getInterfaces());
        this.setLoadingStage(LoadingStage.ParseFields);
        return true;
    }

    public boolean parseFields(Map<String, ClassHeader> headers) {
        if(this.loadingStage != LoadingStage.ParseFields) return true;

        var base = this.baseClassHeader == null ? this.baseClassHeader = headers.get(baseClass) : this.baseClassHeader;
        if(base.loadingStage == LoadingStage.ParseFields){
            if(!base.parseFields(headers)) return false;
        }

        this.fields = base.fields;
        this.slotDescs = base.slotDescs;
        if(this.isFunction()) {
            this.functionParams = base.functionParams;
            this.functionResultType = base.functionResultType;
            this.functionVariables = base.functionVariables;
            this.nativeFunctionEntrance = base.nativeFunctionEntrance;
            this.functionResultSlot = base.functionResultSlot;
        }

        this.genericSource = base.genericSource;
//        if (this.children != null) {
////            this.setChildren(new ArrayList<>(base.children.stream().map(c -> c.clone(this, headers)).toList()));
//        }
        this.setMethods(base.methods);

        this.setLoadingStage(InstantiateFunctionFamily);
        return true;
    }

    @Override
    public boolean instantiateFunctionFamily(Map<String, ClassHeader> headers) {
        if (this.loadingStage != InstantiateFunctionFamily) return true;

        var base = this.baseClassHeader;
        if (base.loadingStage == LoadingStage.InstantiateFunctionFamily) {
            if (!base.instantiateFunctionFamily(headers))
                return false;
        }

        // the children ref to baseClassHeader.children, not store at here, but store methods
//        if(base.children.size() != this.children.size()) {
//            this.setChildren(new ArrayList<>(base.children.stream().map(c -> c.clone(this, headers)).toList()));
//        }
        if(base.methods.size() != this.methods.size()) {
            this.setMethods(base.methods);
        }

        this.nextStage();
        return true;

    }

    public List<ClassHeader> getChildren() {
        return this.baseClassHeader.getChildren();
    }

    @Override
    public boolean parseCode(Map<String, ClassHeader> headers) {
        if (this.loadingStage != LoadingStage.ParseCode) return false;
        var base = this.baseClassHeader;
        if (base.loadingStage == LoadingStage.ParseCode) {
            if (!base.parseCode(headers)) return false;
        }
        if (this.isFunction()){
            this.compiledCode = base.compiledCode.slice();
            this.sourceMap = base.sourceMap;
        }
        this.setLoadingStage(BuildClass);
        return true;
    }

    @Override
    public AgoClass buildClass(Map<String, ClassHeader> headers) {
        if(this.loadingStage != BuildClass) return this.agoClass;
        var baseClass = this.baseClassHeader;
        if(baseClass.loadingStage == BuildClass){
            var r = baseClass.buildClass(headers);
            if(r == null) return null;
        }
        if(headers.get(baseClass.getMetaClass()).loadingStage.value <= BuildClass.value){
            return null;
        }

        var r = super.buildClass(headers);
        r.setParameterizedBaseClass(baseClass.agoClass);
        this.setLoadingStage(LoadingStage.ResolveFunctionIndex);
        return r;
    }

    @Override
    void collectMethods(Map<String, ClassHeader> headers) {
        ClassHeader baseClass = this.baseClassHeader;
        if(baseClass.getLoadingStage() == CollectMethods){
            baseClass.collectMethods(headers);
        }
        var metaOfBase = headers.get(baseClass.getMetaClass());
        if(metaOfBase.getLoadingStage() == CollectMethods){
            metaOfBase.collectMethods(headers);
        }

        super.collectMethods(headers);
    }

    @Override
    void setConcreteTypeInfo(Map<String, ClassHeader> headers) {
        super.setConcreteTypeInfo(headers);

        ClassHeader baseClass = this.baseClassHeader;
        var metaOfBase = headers.get(baseClass.getMetaClass());
        var constructor = metaOfBase.agoClass.findMethod(this.constructor);
        agoClass.setConcreteTypeInfo(new ParameterizedClassInfo(baseClass.agoClass, constructor,arguments));
    }

    @Override
    public boolean isAffectedBy(Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments) {
        ClassHeader baseClass = this.baseClassHeader;
        if(baseClass.isAffectedBy(headers, genericTypeArguments)) return true;
//        for (var arg : this.arguments) {
//            if(arg instanceof ClassHeader header) {    //TODO classref as parameter
//                if (typeArgument.getClassDefValue().isAffectedByTemplate(instantiationArguments)) {
//                    return true;
//                }
//            }
//        }
        return false;
    }
}
