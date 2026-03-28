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

import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.*;
import java.util.stream.Collectors;

public class InstantiationArguments {

    protected final String flatString;

    TreeMap<GenericTypeCodeAvatarClassDef, ClassDef> typeMapping = new TreeMap<>();          // may have other template's type parameter

    private final boolean isTerminated;

    // create default InstantiationArguments for template class
    public InstantiationArguments(TypeParamsContext typeParamsContext, ClassRefLiteral[] typeArguments){
        for (int i = 0; i < typeParamsContext.size(); i++) {
            var tp = typeParamsContext.get(i);
            ClassDef v = typeArguments[i].getClassDefValue();
            typeMapping.put(tp, v);
        }
        this.flatString = stringify();
        isTerminated = determineTerminated();
    }

    private boolean determineTerminated() {
        for (ClassDef value : this.typeMapping.values()) {
            if(!value.isGenericTerminated()) return false;
        }
        return true;
    }

    public boolean isTerminated() {
        return isTerminated;
    }

    public InstantiationArguments(TreeMap<GenericTypeCodeAvatarClassDef, ClassDef> mapping) {
        this.typeMapping = mapping;
        this.flatString = stringify();
        isTerminated = determineTerminated();
    }

    public InstantiationArguments(TreeMap<GenericTypeCodeAvatarClassDef, ClassDef> typeMapping, ClassDef sourceTemplate, ClassRefLiteral[] typeArguments) {
        this.typeMapping = typeMapping;
        this.flatString = stringify();
        isTerminated = determineTerminated();
    }

    private String stringify() {
        return typeMapping.sequencedEntrySet().stream().map(
                entry ->{
                    ClassDef classDef = entry.getValue();
                    return entry.getKey().getFullname() + "=" + classDef.getFullname();
                }).collect(Collectors.joining(","));
    }

    @Override
    public String toString() {
        return this.flatString;
    }

    public ClassDef mapType(GenericTypeCodeAvatarClassDef genericTypeCodeAvatarClassDef){
        return typeMapping.getOrDefault(genericTypeCodeAvatarClassDef, genericTypeCodeAvatarClassDef);
    }

    @Override
    public int hashCode() {
        return this.flatString.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InstantiationArguments that = (InstantiationArguments) o;
        return Objects.equals(flatString, that.flatString);
    }


    public int size() {
        return this.typeMapping.size();
    }

    /**
     * determine whether this arguments may apply to given templateClass.
     * @param templateClass
     * @return true - there are some args can apply to type param of the template class
     */
    public boolean canApplyOnTemplate(ClassDef templateClass) {
        if(templateClass.getTypeParamsContext() != null){
            for (GenericTypeCodeAvatarClassDef genericTypeParam : templateClass.getTypeParamsContext().getGenericTypeParams()) {
                if(this.typeMapping.containsKey(genericTypeParam)) return true;
            }
        }
        return false;
    }

    // a reduce procedure
    public InstantiationArguments apply(InstantiationArguments next) throws CompilationError {
        var r = new TreeMap<GenericTypeCodeAvatarClassDef, ClassDef>();
        for (Map.Entry<GenericTypeCodeAvatarClassDef, ClassDef> map : this.typeMapping.entrySet()) {
            var to = map.getValue();
            var nextTo = to instanceof GenericTypeCodeAvatarClassDef g ? next.typeMapping.get(g) : null;
            if(nextTo != null) {
                r.put(map.getKey(), nextTo);
            } else if(to.isAffectedByTypeArguments(next)){
                r.put(map.getKey(), to.instantiate(next, null));
            } else {
                r.put(map.getKey(), to);
            }
        }
        return new InstantiationArguments(r);
    }

    // inner template apply parent type arguments
    // first apply, then mix parent params
    public InstantiationArguments applyParent(InstantiationArguments parentTypeArguments) throws CompilationError{
        var r = apply(parentTypeArguments);
        r.typeMapping.putAll(parentTypeArguments.typeMapping);
        return r;
    }

    public boolean canApply(InstantiationArguments next) {
        for (Map.Entry<GenericTypeCodeAvatarClassDef, ClassDef> map : this.typeMapping.entrySet()) {
            var to = map.getValue();
            var nextTo = to instanceof GenericTypeCodeAvatarClassDef g ? next.typeMapping.get(g) : null;
            if(nextTo != null) {
                return true;
            } else if(to.isAffectedByTypeArguments(next)){
                return true;
            }
        }
        return false;
    }

    Map<ClassDef, ClassRefLiteral[]> takeForCache = new HashMap<>();

    public ClassRefLiteral[] takeFor(ClassDef templ) {
        return takeForCache.computeIfAbsent(templ, _ -> {
            List<GenericTypeCodeAvatarClassDef> typeParams = templ.getTypeParamsContext().getGenericTypeParams();
            var typeArgumentsArray = new ClassRefLiteral[typeParams.size()];
            for (int i = 0; i < typeParams.size(); i++) {
                var typeParam = typeParams.get(i);
                var p = typeMapping.get(typeParam);
                if (p == null) return null;      // not my arguments
                typeArgumentsArray[i] = p.toClassRefLiteral();
            }
            return typeArgumentsArray;
        });
    }

    public Collection<ClassDef> getAllArguments() {
        return this.typeMapping.values();
    }
}
