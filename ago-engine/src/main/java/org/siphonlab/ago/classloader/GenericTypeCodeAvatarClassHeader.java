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

import java.util.Set;

public class GenericTypeCodeAvatarClassHeader extends ParameterizedClassHeader implements Comparable<GenericTypeCodeAvatarClassHeader>{

    public final String sharedGenericTypeParamClassName;
    public final String templateClassName;
    public final int paramIndex;
    private final GenericTypeCode typeCode;

    public GenericTypeCodeAvatarClassHeader(String fullname, String baseClass, String metaClass, String constructor, Object[] arguments, AgoClassLoader classLoader) {
        super(fullname, baseClass, metaClass, constructor, arguments, classLoader);
        this.sharedGenericTypeParamClassName = ((ClassRefValue)arguments[0]).className();
        this.templateClassName = ((ClassRefValue)arguments[1]).className();
        this.paramIndex = (Integer)arguments[2];
        int typeCode = (Integer)arguments[3];
        String paramName = (String) arguments[4];
        this.typeCode = new GenericTypeCode(typeCode, paramIndex, paramName, paramName + "_" + paramIndex + "_" + templateClassName);
        this.typeCode.genericTypeCodeAvatarClass = fullname;
    }

    @Override
    public GenericTypeCode getTypeCode() {
        return typeCode;
    }

    public ClassHeader getSharedGenericTypeParamClassHeader() {
        return this.classLoader.getClassHeader(sharedGenericTypeParamClassName);
    }

    @Override
    public int compareTo(GenericTypeCodeAvatarClassHeader o) {
        var i = this.templateClassName.compareTo(o.templateClassName);
        if(i == 0){
            return this.paramIndex - o.paramIndex;
        }
        return i;
    }

    @Override
    public ClassHeader clone(ClassHeader newParent) {
        throw new UnsupportedOperationException("TOOD");
    }

    public ClassHeader getTemplateClassHeader() {
        return classLoader.getClassHeader(this.templateClassName);
    }

    public static String composeName(String sharedGenericTypeParameterClassName, String templateClass, String typeParamName, int paramIndex, int genericTypeCodeValue) {
        return "%s_%d_%s_%d|%s".formatted(typeParamName, paramIndex, composeNameOfClassInClassInterval(templateClass), genericTypeCodeValue,
                        composeNameOfClassInClassInterval(sharedGenericTypeParameterClassName));
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments, Set<String> visited) {
        if(instantiationArguments.typeMapping.containsKey(this)){
            return true;
        }
        return classLoader.getClassHeader(this.sharedGenericTypeParamClassName).isAffectedByTypeArguments(instantiationArguments, visited);
    }

    @Override
    public boolean isGenericTerminated(Set<String> visited) {
        return false;
    }

    @Override
    protected ClassHeader instantiate(InstantiationArguments typeArguments, ClassHeader parentInstantiation, String suggestionName, String suggestionFullName) {
        return typeArguments.mapType(this);
    }
}
