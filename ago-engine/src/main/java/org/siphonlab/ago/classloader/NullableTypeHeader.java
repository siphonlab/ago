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

import static org.siphonlab.ago.classloader.LoadingStage.*;

public class NullableTypeHeader extends UnionTypeHeader {

    private final String nullableBaseClassName;
    protected ClassHeader nullableBaseClass;

    public NullableTypeHeader(String fullname, String baseClass, String metaClass, String constructor, Object[] arguments, AgoClassLoader classLoader) {
        super(fullname, baseClass, metaClass, constructor, arguments, classLoader);
        this.nullableBaseClassName = classNames.getFirst();
    }

    public ClassHeader getNullableBaseClass() {
        if(this.nullableBaseClass == null){
            this.nullableBaseClass = Objects.requireNonNull(classLoader.getClassHeader(this.nullableBaseClassName));
        }
        return this.nullableBaseClass;
    }

    @Override
    public boolean isGenericTerminated() {
        return this.getNullableBaseClass().isGenericTerminated();
    }

    private static Pair<String, String> composeName(ClassHeader baseClassHeader, Map<String, ClassHeader> headers){
        String name = '?' + baseClassHeader.fullname + ';';
        return Pair.of(name, name);
    }

    @Override
    protected ClassHeader instantiate(InstantiationArguments typeArguments, ClassHeader parentInstantiation, String suggestionName, String suggestionFullName) {
        if(!this.isAffectedByTypeArguments(typeArguments)) return this;

        var baseInst = classLoader.instantiateReferenceClass(getNullableBaseClass().fullname, typeArguments);
        String name;
        String fullname;
        if(suggestionName == null) {
            var p = composeName(baseInst, this.classLoader.getHeaders());
            name = p.getLeft();
            fullname = p.getRight();
        } else {
            name = suggestionName;
            fullname = suggestionFullName;
        }
        var existed = classLoader.getClassHeader(fullname);
        if(existed != null) return existed;
        var inst = new NullableTypeHeader(fullname, this.baseClass, this.getMetaClass(), this.constructor, new Object[]{new ClassRefValue(baseInst.fullname)}, classLoader);
        classLoader.registerNewClass(inst);
        inst.setLoadingStage(ResolveHierarchicalClasses);
        return inst;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments typeArguments) {
        return this.getNullableBaseClass().isAffectedByTypeArguments(typeArguments);
    }

    @Override
    public boolean processLoadClassName(MutableObject<ClassHeader> createdClass) {
        if(this.loadingStage != LoadClassNames) return true;
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
        ClassHeader baseType = this.getNullableBaseClass();
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
        var anyHeader = classLoader.getClassHeader("lang.any");
        if(anyHeader.getLoadingStage() == BuildClass){
            anyHeader.buildClass();
        }
        return super.buildClass();
    }

    @Override
    void setConcreteTypeInfo() {
        var nullHeader = classLoader.getClassHeader("null");
        agoClass.setConcreteTypeInfo(new NullableTypeInfo(this.nullableBaseClass.agoClass, (AgoNullClass) nullHeader.agoClass));
    }

    @Override
    public boolean isReady() {
        return isReady(this.nullableBaseClassName);
    }
}
