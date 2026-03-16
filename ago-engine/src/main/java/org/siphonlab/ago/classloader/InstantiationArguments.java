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
    // these arguments associated with which template class, it's always the nearest outer template class
    // it must be a template class, never be the inner class of a template
    // the inner class of a template can be found in GenericSource
    protected ClassHeader sourceTemplate;

    protected String flatString;

    private final ClassHeader[] typeArgumentsArray;

    private boolean isTerminatedResolved = false;
    private boolean isTerminated;

    private ClassHeader parentClassHeader;
    private String suggestionName;
    private String suggestionFullname;

    public InstantiationArguments(ClassHeader sourceTemplate, ClassHeader[] typeArguments, Map<String, ClassHeader> headers){
        this.sourceTemplate = sourceTemplate;
        for (int i = 0; i < sourceTemplate.genericTypeParams.length; i++) {
            var tp = sourceTemplate.genericTypeParams[i];
            var v = typeArguments[i];
            typeMapping.put(tp, v);
        }
        this.flatString = stringify();
        this.typeArgumentsArray = typeArguments;
    }

    private boolean determineTerminated() {
        for (var value : this.typeMapping.values()) {
            if(!value.isGenericTerminated()) return false;
        }
        return true;
    }

    public ClassHeader[] getTypeArgumentsArray() {
        return typeArgumentsArray;
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

    public ClassHeader getSourceTemplate() {
        return sourceTemplate;
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


    // a reduce procedure
    public InstantiationArguments apply(InstantiationArguments next, AgoClassLoader classLoader) {
        var r = new TreeMap<GenericTypeCodeAvatarClassHeader, ClassHeader>();
        for (var map : this.typeMapping.entrySet()) {
            var to = map.getValue();
            var nextTo = to instanceof GenericTypeCodeAvatarClassHeader g ? next.typeMapping.get(g) : null;
            if(nextTo != null) {
                r.put(map.getKey(), nextTo);
            } else if(to.isAffectedByTypeArguments(next)){
                r.put(map.getKey(), classLoader.instantiateDependencyClass(to.fullname(),next));
            } else {
                r.put(map.getKey(), to);
            }
        }
        if(this.sourceTemplate == null){
            return new InstantiationArguments(r);
        } else {
            var arr = Arrays.stream(this.sourceTemplate.genericTypeParams).map(r::get).toArray(ClassHeader[]::new);
            return new InstantiationArguments(r, this.sourceTemplate, arr);
        }
    }

    public InstantiationArguments applyParent(InstantiationArguments parentTypeArguments, AgoClassLoader classLoader) {
        var r = apply(parentTypeArguments, classLoader);
        r.typeMapping.putAll(parentTypeArguments.typeMapping);
        return r;
    }

    public InstantiationArguments withoutTemplate(){
        return new InstantiationArguments(this.typeMapping);
    }

    public InstantiationArguments withoutNames(){
        return new InstantiationArguments(this.typeMapping, this.sourceTemplate, this.typeArgumentsArray);
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

    public InstantiationArguments takeFor(ClassHeader templ) {
        if(this.sourceTemplate == templ){
            return this;
        }
        var r = new TreeMap<GenericTypeCodeAvatarClassHeader, ClassHeader>();
        for (var genericTypeParam : templ.genericTypeParams) {
            r.put(genericTypeParam, this.typeMapping.get(genericTypeParam));
        }
        return new InstantiationArguments(r, templ, this.typeArgumentsArray);
    }

    public InstantiationArguments(TreeMap<GenericTypeCodeAvatarClassHeader, ClassHeader> mapping) {
        this.typeMapping = mapping;
        this.flatString = stringify();
        this.typeArgumentsArray = null;
        this.sourceTemplate = null;
    }

    public InstantiationArguments(TreeMap<GenericTypeCodeAvatarClassHeader, ClassHeader> typeMapping, ClassHeader sourceTemplate, ClassHeader[] typeArguments) {
        this.typeMapping = typeMapping;
        this.sourceTemplate = sourceTemplate;
        this.typeArgumentsArray = typeArguments;
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

    public void setSuggestionName(String suggestionName) {
        this.suggestionName = suggestionName;
    }

    public String getSuggestionName() {
        return suggestionName;
    }

    public void setSuggestionFullname(String suggestionFullname) {
        this.suggestionFullname = suggestionFullname;
    }

    public String getSuggestionFullname() {
        return suggestionFullname;
    }

    public ClassHeader getParentClassHeader() {
        return parentClassHeader;
    }

    public void setParentClassHeader(ClassHeader parentClassHeader) {
        this.parentClassHeader = parentClassHeader;
    }
}
