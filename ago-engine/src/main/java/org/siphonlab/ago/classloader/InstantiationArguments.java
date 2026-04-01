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

import org.agrona.collections.Int2IntHashMap;

import java.util.*;
import java.util.stream.Collectors;

public class InstantiationArguments {

    TreeMap<GenericTypeCodeAvatarClassHeader, ClassHeader> typeMapping = new TreeMap<>();

    protected String flatString;

    private boolean isTerminatedResolved = false;
    private boolean isTerminated;

    public InstantiationArguments(ClassHeader sourceTemplate, ClassHeader[] typeArguments){
        for (int i = 0; i < sourceTemplate.genericTypeParams.length; i++) {
            var tp = sourceTemplate.genericTypeParams[i];
            var v = typeArguments[i];
            typeMapping.put(tp, v);
        }
        this.flatString = stringify();
    }

    private boolean determineTerminated() {
        for (var value : this.typeMapping.values()) {
            if(!value.isGenericTerminated()) return false;
        }
        return true;
    }

    private String stringify() {
        return typeMapping.sequencedEntrySet().stream().map(
                entry ->{
                    var classHeader = entry.getValue();
                    return entry.getKey().fullname() + "=" + classHeader.fullname();
                }).collect(Collectors.joining(","));
    }

    @Override
    public String toString() {
        return "(TypeArguments %s)".formatted(this.flatString);
    }


    // for parent->child, it must apply via `parent.applyChild(child)`, no `child.applyChild(parent)`
    public ClassHeader mapType(GenericTypeCodeAvatarClassHeader genericTypeCodeAvatarClassHeader){
        return typeMapping.getOrDefault(genericTypeCodeAvatarClassHeader, genericTypeCodeAvatarClassHeader);
    }

    @Override
    public int hashCode() {
        return this.flatString.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InstantiationArguments that = (InstantiationArguments) o;
        return Objects.equals(this.typeMapping, that.typeMapping);
    }


    public int size() {
        return this.typeMapping.size();
    }

    public boolean canApplyOnTemplate(ClassHeader templateClass) {
        if(templateClass.isGenericTemplate()){
            for (var genericTypeParam : templateClass.genericTypeParams) {
                if(this.typeMapping.containsKey(genericTypeParam)) return true;
            }
        }
        return false;
    }

    public InstantiationArguments apply(InstantiationArguments next, AgoClassLoader classLoader) {
        return apply(next, null, classLoader);
    }
    // a reduce procedure
    public InstantiationArguments apply(InstantiationArguments next, InstantiationArguments parent, AgoClassLoader classLoader) {
        var r = new TreeMap<GenericTypeCodeAvatarClassHeader, ClassHeader>();
        for (var map : this.typeMapping.entrySet()) {
            var to = map.getValue();
            var nextTo = to instanceof GenericTypeCodeAvatarClassHeader g ? next.typeMapping.get(g) : null;
            if(nextTo != null) {
                r.put(map.getKey(), nextTo);
            } else if(to.isAffectedByTypeArguments(next)){
                r.put(map.getKey(), classLoader.instantiateReferenceClass(to.fullname(),next));
            } else {
                r.put(map.getKey(), to);
            }
        }
        if(parent != null){
            r.putAll(parent.typeMapping);
        }
        return new InstantiationArguments(r);
    }

    public InstantiationArguments applyParent(InstantiationArguments parentTypeArguments, AgoClassLoader classLoader) {
        return apply(parentTypeArguments, parentTypeArguments,  classLoader);
    }


    public boolean canApply(InstantiationArguments next) {
        for (var map : this.typeMapping.entrySet()) {
            var to = map.getValue();
            var nextTo = to instanceof GenericTypeCodeAvatarClassHeader g ? next.typeMapping.get(g) : null;
            if(nextTo != null) {
                return true;
            } else if(to.isAffectedByTypeArguments(next)){
                return true;
            }
        }
        return false;
    }

    Map<ClassHeader, ClassRefValue[]> takeForCache = new HashMap<>();

    public ClassRefValue[] takeFor(ClassHeader templ) {
        Objects.requireNonNull(templ);
        if(!templ.isGenericTemplate()) return null;
        return takeForCache.computeIfAbsent(templ, _ -> {
            var typeParams = templ.genericTypeParams;
            var typeArgumentsArray = new ClassRefValue[typeParams.length];
            for (int i = 0; i < typeParams.length; i++) {
                var typeParam = typeParams[i];
                var p = typeMapping.get(typeParam);
                if (p == null) return null;      // not my arguments
                typeArgumentsArray[i] = new ClassRefValue(p.fullname);
            }
            return typeArgumentsArray;
        });
    }

    public Collection<ClassHeader> getAllArguments(){
        return this.typeMapping.values();
    }

    public InstantiationArguments(TreeMap<GenericTypeCodeAvatarClassHeader, ClassHeader> mapping) {
        this.typeMapping = mapping;
        this.flatString = stringify();
    }

    public boolean isTerminated() {
        if(isTerminatedResolved) return isTerminated;
        isTerminated = determineTerminated();
        isTerminatedResolved  = true;
        return isTerminated;
    }

    private Int2IntHashMap typeCodeMap;

    public int mapTypeCode(int genericTypeCode) {
        if(typeCodeMap == null){
            typeCodeMap = new Int2IntHashMap(-1);
            for (Map.Entry<GenericTypeCodeAvatarClassHeader, ClassHeader> entry : this.typeMapping.entrySet()) {
                typeCodeMap.put(entry.getKey().getTypeCode().getValue(), entry.getValue().getTypeCode().getValue());
            }
        }
        return typeCodeMap.getOrDefault(genericTypeCode, genericTypeCode);
    }

    public String mapTypeCodeToClassName(int genericCode) {
        return mapType(genericCode).fullname;
    }

    public ClassHeader mapType(int genericCode) {
        for (var entry : this.typeMapping.entrySet()) {
            if (entry.getKey().getTypeCode().value == genericCode) {
                return entry.getValue();
            }
        }
        return null;
    }

}
