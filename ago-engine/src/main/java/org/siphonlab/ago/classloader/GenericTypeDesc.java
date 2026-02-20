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

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.TypeInfo;

import java.util.Map;
import java.util.Objects;

class GenericTypeDesc extends TypeDesc implements Comparable<GenericTypeDesc> {
    String templateClass;
    int paramIndex;
    String name;
    boolean isPlaceHolder;

    GenericTypeDesc(TypeCode typeCode, String className, String templateClass, int index, String name) {
        super(typeCode, className);
        this.templateClass = templateClass;
        this.paramIndex = index;
        this.name = name;
    }

    GenericTypeDesc(String templateClass, int index) {
        super(TypeCode.OBJECT, "<NA>");
        this.templateClass = templateClass;
        this.paramIndex = index;
        this.isPlaceHolder = true;
    }

    public String asClassNamePart(){
        return "%s_%d_%s|%s".formatted(typeCode, paramIndex, templateClass, className);
    }

    public GenericTypeDesc resolveExactType(Map<String, ClassHeader> headers){
        ClassHeader classHeader = headers.get(this.templateClass);
        if(classHeader == null) return null;
        var t = classHeader.genericTypeParamDescs[paramIndex];
        return t;
    }

    @Override
    public int compareTo(GenericTypeDesc o) {
        int i = this.templateClass.compareTo(o.templateClass);
        if(i != 0) return i;
        return paramIndex - o.paramIndex;
    }

    interface ResolveExactTypeCallback{
        void resolve(GenericTypeDesc t);
    }

    public boolean isAffectedBy(Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments){
        return genericTypeArguments.containsType(this);
    }

    @Override
    TypeDesc applyTemplate(Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments) {
        return genericTypeArguments.mapType(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GenericTypeDesc that = (GenericTypeDesc) o;
        return paramIndex == that.paramIndex && Objects.equals(templateClass, that.templateClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass(), templateClass, paramIndex);
    }

    @Override
    public TypeInfo toTypeInfo(Map<String, ClassHeader> headers) {
        AgoClass cls;
        if (this.typeCode == TypeCode.OBJECT) {
            cls = headers.get(this.className).agoClass;
            assert cls != null;
        } else {
            cls = null;
        }
        return new GenericTypeInfo(this.typeCode, cls, this.templateClass, this.name, this.paramIndex);
    }
}
