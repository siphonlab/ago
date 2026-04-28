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
import org.siphonlab.ago.*;

import java.util.Map;
import java.util.Objects;

import static org.siphonlab.ago.AgoClass.*;
import static org.siphonlab.ago.classloader.LoadingStage.*;

public class NullableTypeHeader extends ClassHeader {

    private final String baseTypeName;
    protected ClassHeader baseType;

    public NullableTypeHeader(String fullname, String name, String baseTypeName, AgoClassLoader classLoader) {
        super(fullname, TYPE_CLASS, PUBLIC, null, classLoader);
        this.baseTypeName = baseTypeName;
        this.name = name;
    }

    public ClassHeader getBaseType() {
        if(this.baseType == null){
            this.baseType = Objects.requireNonNull(classLoader.getClassHeader(this.baseTypeName));
        }
        return this.baseType;
    }

    @Override
    public boolean isGenericTerminated() {
        return this.getBaseType().isGenericTerminated();
    }

    private static Pair<String, String> composeName(ClassHeader baseClassHeader, Map<String, ClassHeader> headers){
        String name;
        String fullname;
        if(baseClassHeader.getTypeCode().isObject()) {
            var el = headers.get(baseClassHeader.fullname());
            name = el.getName() + '?';
            fullname = el.extractPackagePrefix() + name;
        } else if(baseClassHeader instanceof GenericTypeCodeAvatarClassHeader g){
            if(g.fullname != null){
                var el = headers.get(g.fullname);
                name = g.getName() + '?';
                fullname = el.extractPackagePrefix() + name;
            } else {
                name = g.getName() + '?';
                fullname = name;
            }
        } else {
            name = baseClassHeader.getName() + '?';
            fullname = name;
        }
        return Pair.of(name, fullname);
    }

    @Override
    public TypeCode getTypeCode() {
        return TypeCode.UNION;
    }

    @Override
    protected ClassHeader instantiate(InstantiationArguments typeArguments, ClassHeader parentInstantiation, String suggestionName, String suggestionFullName) {
        if(!this.isAffectedByTypeArguments(typeArguments)) return this;

        var elementInst = classLoader.instantiateReferenceClass(getBaseType().fullname, typeArguments);
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
        var inst = new NullableTypeHeader(fullname, name, elementInst.fullname, classLoader);
        classLoader.registerNewClass(inst);
        inst.setLoadingStage(ResolveHierarchicalClasses);
        return inst;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments typeArguments) {
        return this.getBaseType().isAffectedByTypeArguments(typeArguments);
    }

    @Override
    public boolean processLoadClassName(MutableObject<ClassHeader> createdClass) {
        if(this.loadingStage != LoadClassNames) return true;
        this.nextStage();
        return true;
    }

    @Override
    public boolean resolveHierarchicalClasses() {
        if(this.loadingStage != ResolveHierarchicalClasses) return true;

        this.setSuperClass("lang.Any");
        this.nextStage();
        return true;
    }

    public boolean parseFields() {
        if(this.loadingStage != LoadingStage.ParseFields) return true;

        this.setLoadingStage(LoadingStage.BuildClass);
        return true;
    }

    @Override
    public AgoClass buildClass() {
        if(this.loadingStage != BuildClass) return this.agoClass;
        ClassHeader baseType = this.getBaseType();
        if(baseType.getTypeCode() == TypeCode.OBJECT) {
            var eleClass = classLoader.getClassHeader(baseType.fullname);
            if (eleClass.loadingStage == BuildClass){
                var r = eleClass.buildClass();
                if (r == null) return null;
            }
        }
        var nullHeader = classLoader.getClassHeader("null");
        if(nullHeader.getLoadingStage() == BuildClass){
            nullHeader.buildClass();
        }
        var anyHeader = classLoader.getClassHeader("lang.Any");
        if(anyHeader.getLoadingStage() == BuildClass){
            anyHeader.buildClass();
        }
        var agoClass = super.buildClass();
        agoClass.setConcreteTypeInfo(new NullableTypeInfo(this.baseType.agoClass, (AgoNullClass) nullHeader.agoClass));
        agoClass.setSuperClass(anyHeader.agoClass);
        return agoClass;
    }

    @Override
    public boolean isReady() {
        return isReady(this.baseTypeName);
    }
}
