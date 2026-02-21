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
import org.siphonlab.ago.GenericParameterTypeInfo;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.TypeInfo;

import java.util.*;

public class TypeDesc {
    protected final TypeCode typeCode;
    protected final String className;

    public TypeDesc(TypeCode typeCode, String className) {
        this.typeCode = typeCode;
        this.className = className;
    }

    public TypeCode getTypeCode() {
        return typeCode;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TypeDesc) obj;
        return Objects.equals(this.typeCode, that.typeCode) &&
                Objects.equals(this.className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeCode, className);
    }

    @Override
    public String toString() {
        return className == null ? "(TypeDesc %s)".formatted(typeCode) : "(TypeDesc %s %s)".formatted(typeCode, className);
    }

    public String getShortName(){
        if(typeCode == TypeCode.OBJECT){
            return className;
        } else {
            return typeCode.toShortString();
        }
    }

    public String getName(){
        if(typeCode == TypeCode.OBJECT){
            return className;
        } else {
            return typeCode.toString();
        }
    }

    TypeDesc applyTemplate(Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments) {
        if(this.typeCode == TypeCode.OBJECT) {
            ClassHeader typeHeader = headers.get(this.className);

            var t = typeHeader.resolveTemplateInstantiation(headers, genericTypeArguments);

            if (t != null){
                return new TypeDesc(TypeCode.OBJECT, t.fullname);
            }
        }
        return this;
    }

    public boolean isAffectedBy(Map<String, ClassHeader> headers, GenericTypeArguments genericTypeArguments){
        return false;
    }

    public boolean existed(Map<String, ClassHeader> headers) {
        if(this.typeCode == TypeCode.OBJECT){
            return headers.containsKey(this.className);
        }
        return true;
    }

    public boolean resolveGenericTypeDescPlaceHolder(Map<String, ClassHeader> headers) {
        var header = headers.get(this.className);
        if(header == null) return false;
        if(header.genericSource != null){
            return header.genericSource.typeArguments().resolvePlaceHolderArguments(headers)
                && header.genericSource.sourceTemplate().resolvePlaceHolderKeyInstantiatedClassed(headers);
        }
        return header.resolvePlaceHolderKeyInstantiatedClassed(headers);
    }

    public TypeInfo toTypeInfo(Map<String, ClassHeader> headers){
        AgoClass cls;
        if (this.typeCode == TypeCode.OBJECT) {
            cls = headers.get(this.className).agoClass;
            assert cls != null;
        } else {
            cls = null;
        }
        return new TypeInfo(this.typeCode, cls);
    }

    public GenericParameterTypeInfo toGenericParameterTypeInfo(String parameterName, AgoClass sharedGenericTypeParameterClass, Map<String, ClassHeader> headers){
        AgoClass cls;
        if (this.typeCode == TypeCode.OBJECT) {
            cls = headers.get(this.className).agoClass;
            assert cls != null;
        } else {
            cls = null;
        }
        return new GenericParameterTypeInfo(parameterName, sharedGenericTypeParameterClass, this.typeCode, cls);
    }

}
