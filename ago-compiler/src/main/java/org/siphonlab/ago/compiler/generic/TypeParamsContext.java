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

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TypeParamsContext {
    private final ClassDef templateClass;
    private TypeParamsContext parent;
    // "T" -> GenericTypeParameterClassDef::(Animal, Any, 1)
    // for G<T>, it's GenericParameterizedClassDef base on this template class, GenericParameterizedClassDef has realized argument value, some classref
    // I didn't create GenericTemplateClassDef for FunctionDef can be Template too, it's a classic diamond problem
    private final LinkedHashMap<String, GenericTypeCode> genericTypeParamsMap = new LinkedHashMap<>();
    private final List<GenericTypeCode> genericTypeParams = new ArrayList<>();
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

    public void addGenericTypeParam(String name, SharedGenericTypeParameterClassDef pc,
                                    AgoParser.GenericTypeParameterContext genericTypeParameterContext) {

        templateClass.idOfClass(templateClass);
        templateClass.registerConcreteType(pc);

        int index = genericTypeParams.size();
        GenericTypeCode genericTypeCode = GenericTypeCode.createGeneric(nextGenericTypeCode.getAndIncrement(), index, name, pc, genericTypeParameterContext, templateClass);
        this.genericTypeParamsMap.put(name, genericTypeCode);
        this.genericTypeParams.add(genericTypeCode);
    }

    public InstantiationArguments createDefaultArguments(){
        var args = createDefaultArgumentsArray();

        return new InstantiationArguments(this, args);
    }

    public ClassRefLiteral[] createDefaultArgumentsArray() {
        var args = new ClassRefLiteral[this.genericTypeParams.size()];
        for (int i = 0; i < genericTypeParams.size(); i++) {
            GenericTypeCode genericTypeParam = genericTypeParams.get(i);
            args[i] = new ClassRefLiteral(genericTypeParam.getGenericCodeAvatarClassDef());
        }
        return args;
    }

    /*
        for name path resolver, resolve GenericTypeCode via genericTypeName, i.e. "T"
     */
    public GenericTypeCode get(String genericTypeName) {
        return genericTypeParamsMap.getOrDefault(genericTypeName, this.parent == null ? null : this.parent.get(genericTypeName));   //recursive to parent
    }

    public String getName(int index){
        return genericTypeParamsMap.sequencedKeySet().stream().toList().get(index);
    }

    /*
    when apply arguments, enum generic type code one by one to match argument.  i.e. 0, 1, 2
    see TypeArgumentsApplier
     */
    public GenericTypeCode get(int index){
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
        return genericTypeParamsMap.sequencedValues().stream().map(g -> g.getGenericCodeAvatarClassDef().toString())
                .collect(Collectors.joining(","));
    }

    public ClassDef findByGenericTypeCode(int genericCodeValue) {
        for (GenericTypeCode genericTypeParam : this.genericTypeParams) {
            if(genericTypeParam.value == genericCodeValue){
                return genericTypeParam.getGenericCodeAvatarClassDef();
            }
        }
        return null;
    }
}
