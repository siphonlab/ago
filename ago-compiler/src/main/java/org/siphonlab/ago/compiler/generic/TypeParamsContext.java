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
package org.siphonlab.ago.compiler.generic;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.ClassContainer;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.Root;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// only for template class
public class TypeParamsContext {
    private final ClassDef templateClass;
    private TypeParamsContext parent;

    private final LinkedHashMap<String, GenericTypeCodeAvatarClassDef> genericTypeParamsMap = new LinkedHashMap<>();
    private final List<GenericTypeCodeAvatarClassDef> genericTypeParams = new ArrayList<>();
    private final AtomicInteger nextGenericTypeCode;

    public TypeParamsContext(ClassDef templateClass){
        this.templateClass = templateClass;
        nextGenericTypeCode = new AtomicInteger(TypeCode.GENERIC_TYPE_START);
    }

    public TypeParamsContext(ClassDef templateClass, TypeParamsContext parent){
        this.templateClass = templateClass;
        this.parent = parent;
        this.nextGenericTypeCode = parent.nextGenericTypeCode;
    }

    public GenericTypeCodeAvatarClassDef createGenericTypeParam(String paramName, SharedGenericTypeParameterClassDef genericTypeParameter, int paramIndex) throws CompilationError {

        Root root = templateClass.getRoot();

        var exists = new MutableBoolean();
        var r = ((ClassContainer) root.getGenericTypeCodeAvatar().getParent()).getOrCreateGenericTypeAvatarClassDef(root.getGenericTypeCodeAvatar(), genericTypeParameter, templateClass, paramIndex, nextGenericTypeCode.getAndIncrement(), paramName, exists);

        templateClass.idOfClass(templateClass);
        templateClass.registerConcreteType(genericTypeParameter);

        this.genericTypeParamsMap.put(paramName, r);
        this.genericTypeParams.add(r);
        return r;
    }

    public InstantiationArguments createDefaultArguments(){
        var args = createDefaultArgumentsArray();

        return new InstantiationArguments(this, args);
    }

    public ClassRefLiteral[] createDefaultArgumentsArray() {
        var args = new ClassRefLiteral[this.genericTypeParams.size()];
        for (int i = 0; i < genericTypeParams.size(); i++) {
            var genericTypeParam = genericTypeParams.get(i);
            args[i] = genericTypeParam.toClassRefLiteral();
        }
        return args;
    }

    /*
        for name path resolver, resolve GenericTypeCode via genericTypeName, i.e. "T"
     */
    public GenericTypeCodeAvatarClassDef get(String genericTypeName) {
        return genericTypeParamsMap.get(genericTypeName);   //recursive to parent
    }

    public String getName(int index){
        return genericTypeParamsMap.sequencedKeySet().stream().toList().get(index);
    }

    /*
    when apply arguments, enum generic type code one by one to match argument.  i.e. 0, 1, 2
    see TypeArgumentsApplier
     */
    public GenericTypeCodeAvatarClassDef get(int index){
        return genericTypeParams.get(index);    // don't recursive to parent
    }

    public int size() {
        return genericTypeParams.size();
    }

    public ClassDef getTemplateClass() {
        return templateClass;
    }


    @Override
    public String toString() {
        return genericTypeParamsMap.sequencedValues().stream().map(ClassDef::getFullname)
                .collect(Collectors.joining(","));
    }

    public List<GenericTypeCodeAvatarClassDef> getGenericTypeParams() {
        return genericTypeParams;
    }
}
