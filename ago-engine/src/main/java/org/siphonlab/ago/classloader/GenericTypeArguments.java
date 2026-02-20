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
import org.siphonlab.ago.TypeCode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * according TypeArguments, but for GenericTypeDesc and TypeDesc
 */
public class GenericTypeArguments {

    TreeMap<GenericTypeDesc, TypeDesc> typeMapping = new TreeMap<>();
    // these arguments associated with which template class, it's always the nearest outer template class
    // it must be a template class, never be the inner class of a template
    // the inner class of a template can be found in GenericSource
    protected final ClassHeader sourceTemplate;

    protected final boolean isIntermediate;
    protected final boolean hasIntermediateArgs;

    private ClassHeader intermediateClass;
    protected String flatString;

    private final TypeDesc[] typeArgumentsArray;

    private Int2IntHashMap typeCodeMap;

    private boolean hasUnsolvedGenericTypeDesc;

    private Set<String> otherTemplates;

    public GenericTypeArguments(ClassHeader sourceTemplate, TypeDesc[] typeArguments, Map<String, ClassHeader> headers){
        this.sourceTemplate = sourceTemplate;
        boolean isIntermediate = false;
        boolean hasIntermediateArgs = false;
        for (int i = 0; i < sourceTemplate.genericTypeParamDescs.length; i++) {
            var tp = sourceTemplate.genericTypeParamDescs[i];
            var v = typeArguments[i];
            typeMapping.put(tp, v);
            if(v instanceof GenericTypeDesc g){
                isIntermediate = true;
            } else if (isIntermediateArg(v, headers)) {
                hasIntermediateArgs = true;
            }
        }
        this.isIntermediate = isIntermediate;
        this.hasIntermediateArgs = hasIntermediateArgs;
        this.flatString = stringify();
        this.typeArgumentsArray = typeArguments;
        buildTypeCodeMap();
        if(this.typeMapping.size() != typeArgumentsArray.length || hasIntermediateArgs)
            this.collectOtherTemplates(headers);
    }

    private void buildTypeCodeMap(){
        var hasUnsolvedGenericTypeDesc = false;
        Int2IntHashMap typeCodeMap = new Int2IntHashMap(-1);
        for (Map.Entry<GenericTypeDesc, TypeDesc> entry : typeMapping.entrySet()) {
            TypeDesc value = entry.getValue();
            if(value instanceof GenericTypeDesc g && g.isPlaceHolder){
                hasUnsolvedGenericTypeDesc = true;
            } else {
                typeCodeMap.put(entry.getKey().typeCode.value, value.typeCode.value);
            }
        }
        this.typeCodeMap = typeCodeMap;
        this.hasUnsolvedGenericTypeDesc = hasUnsolvedGenericTypeDesc;
    }

    private void collectOtherTemplates(Map<String, ClassHeader> headers){
        var otherTemplates = new HashSet<String>();
        for (GenericTypeDesc genericTypeDesc : this.typeMapping.keySet()) {
            if(!genericTypeDesc.templateClass.equals(this.sourceTemplate.fullname)){
                otherTemplates.add(genericTypeDesc.templateClass);
            }
        }
        if (hasIntermediateArgs) {
            for (TypeDesc typeDesc : this.typeArgumentsArray) {
                if(isIntermediateArg(typeDesc, headers)){
                    var args = headers.get(typeDesc.className).genericSource.typeArguments();
                    if(args.otherTemplates != null){
                        otherTemplates.addAll(args.otherTemplates);
                    }
                }
            }

        }
        this.otherTemplates = otherTemplates;
    }

    public TypeDesc[] getTypeArgumentsArray() {
        return typeArgumentsArray;
    }

    private String stringify() {
        return typeMapping.sequencedEntrySet().stream()
                .map(entry -> entry.getKey().typeCode + "=" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    @Override
    public String toString() {
        return "(TypeArguments %s)".formatted(this.flatString);
    }

    private GenericTypeArguments(TreeMap<GenericTypeDesc, TypeDesc> typeMapping, TypeDesc[] typeArgumentsArray, ClassHeader sourceTemplate,
                                 boolean isIntermediate, boolean hasIntermediateArgs, Map<String, ClassHeader> headers) {
        this.typeMapping = typeMapping;
        this.sourceTemplate = sourceTemplate;
        this.isIntermediate = isIntermediate;
        this.flatString = stringify();
        this.typeArgumentsArray = typeArgumentsArray;
        this.hasIntermediateArgs = hasIntermediateArgs;
        buildTypeCodeMap();
        if(this.typeMapping.size() != typeArgumentsArray.length || hasIntermediateArgs)
            this.collectOtherTemplates(headers);
    }

    // for parent->child, it must apply via `parent.applyChild(child)`, no `child.applyChild(parent)`
    public GenericTypeArguments applyChild(GenericTypeArguments childArguments, Map<String, ClassHeader> headers){
        boolean stillIntermediate = this.isIntermediate || childArguments.isIntermediate;
        boolean stillHasIntermediateArgs = this.hasIntermediateArgs || childArguments.hasIntermediateArgs;
        TreeMap<GenericTypeDesc, TypeDesc> newTypeMapping = new TreeMap<>();
        newTypeMapping.putAll(this.typeMapping);
        newTypeMapping.putAll(childArguments.typeMapping);

        return new GenericTypeArguments(newTypeMapping, childArguments.typeArgumentsArray, childArguments.sourceTemplate, stillIntermediate, stillHasIntermediateArgs, headers);
    }


    /// continue to apply instantiation on an intermediate template class. i.e. `A<TA>{ something as G<TA>}; new A<Dog>`, now call `G<TA>.apply(A<Dog>.instantiationArgs)`
    /// for intermediate class, call `intermediate.apply(new_args)`
    ///
    /// @param concreteArguments
    /// @param headers
    /// @return
    public GenericTypeArguments applyIntermediate(GenericTypeArguments concreteArguments, Map<String, ClassHeader> headers){
        if(this.isIntermediate){
            if(this.intermediateClass == null) this.resolveIntermediateClass(headers);
            assert this.intermediateClass != null;
            if(!this.intermediateClass.isAffectedBy(headers, concreteArguments)){
                return this;
            }
        }
        boolean stillIntermediate = false;
        boolean hasIntermediateArgs = false;
        TreeMap<GenericTypeDesc, TypeDesc> newTypeMapping = new TreeMap<>();
        for (Map.Entry<GenericTypeDesc, TypeDesc> entry : this.typeMapping.entrySet()) {
            TypeDesc value = entry.getValue();
            GenericTypeDesc typeCode = entry.getKey();
            if (isIntermediate && value instanceof GenericTypeDesc gv) {
                var mapped = concreteArguments.mapType(gv);
                newTypeMapping.put(typeCode, mapped);
                if (!hasIntermediateArgs) hasIntermediateArgs = isIntermediateArg(mapped, headers);
                if(mapped instanceof GenericTypeDesc){
                    stillIntermediate = true;
                }
            } else {
                newTypeMapping.put(typeCode, value);
                if (!hasIntermediateArgs) {
                    hasIntermediateArgs = isIntermediateArg(value, headers);
                }
            }
        }
        var arr = newTypeMapping.sequencedValues().stream().toArray(TypeDesc[]::new);
        return new GenericTypeArguments(newTypeMapping, arr, sourceTemplate, stillIntermediate, hasIntermediateArgs, headers);
    }

    private boolean isIntermediateArg(TypeDesc typeDesc, Map<String, ClassHeader> headers){
        if(typeDesc.getTypeCode() != TypeCode.OBJECT) return false;
        var header = headers.get(typeDesc.className);
        if(!(typeDesc instanceof GenericTypeDesc) && header != null && header.genericSource != null){
            var instantiationArguments = header.genericSource.typeArguments();
            return instantiationArguments.isIntermediate || instantiationArguments.hasIntermediateArgs;
        }
        return false;

    }

    public boolean resolveIntermediateClass(Map<String, ClassHeader> headers){
        ClassHeader intermediateClass = null;
        for (TypeDesc typeDesc : this.typeArgumentsArray) {
            if(typeDesc instanceof GenericTypeDesc g){
                var c = headers.get(g.templateClass);
                if(c == null) return false;
                if(intermediateClass == null || c.belongsTo(intermediateClass)){        // find most inner class
                    intermediateClass = c;
                }
            }
        }
        this.intermediateClass = intermediateClass;
        return true;
    }

    public boolean resolvePlaceHolderArguments(Map<String, ClassHeader> headers){
        if(!this.hasUnsolvedGenericTypeDesc) return true;

        TypeDesc[] arguments = this.typeArgumentsArray;
        for (int i = 0; i < arguments.length; i++) {
            TypeDesc typeArgument = arguments[i];
            if (typeArgument instanceof GenericTypeDesc g && g.isPlaceHolder) {
                GenericTypeDesc exactType = g.resolveExactType(headers);
                if(exactType == null) return false;
                arguments[i] = exactType;
            }
        }
        for (GenericTypeDesc key : this.typeMapping.keySet()) {
            var v = this.typeMapping.get(key);
            if(v instanceof GenericTypeDesc g && g.isPlaceHolder){
                GenericTypeDesc exactType = g.resolveExactType(headers);
                if(exactType == null) return false;
                this.typeMapping.put(key, exactType);
            }
        }
        this.flatString = this.stringify();
        this.buildTypeCodeMap();
        return true;
    }

    public ClassHeader getSourceTemplate() {
        return sourceTemplate;
    }

    /// take arguments for some template class, include parent arguments
    public GenericTypeArguments takeFor(ClassHeader sourceTemplate, Map<String, ClassHeader> headers){
        if(sourceTemplate.genericSource != null)
            return takeFor(sourceTemplate.genericSource.sourceTemplate(), headers);

        if(sourceTemplate instanceof MetaClassHeader meta)
            return takeFor(meta.instanceClass, headers);

        if(sourceTemplate == this.sourceTemplate || sourceTemplate.belongsTo(this.sourceTemplate))
            return this;

        boolean isIntermediate = false;
        boolean hasIntermediateArgs = false;
        ClassHeader nearestTempl = null;
        TreeMap<GenericTypeDesc, TypeDesc> newTypeMapping = new TreeMap<>();
        for (Map.Entry<GenericTypeDesc, TypeDesc> entry : this.typeMapping.sequencedEntrySet()) {
            ClassHeader t = headers.get(entry.getKey().templateClass);
            if(t == sourceTemplate || sourceTemplate.belongsTo(t)){
                TypeDesc typeDesc = entry.getValue();
                newTypeMapping.put(entry.getKey(), typeDesc);
                if(this.isIntermediate && typeDesc instanceof GenericTypeDesc){
                    isIntermediate = true;
                }
                if (this.hasIntermediateArgs && isIntermediateArg(typeDesc, headers)) {
                    hasIntermediateArgs = true;
                }
                nearestTempl = t;
            }
        }
        if(newTypeMapping.isEmpty()){
            return null;
        }

        var genericTypeParamDescs = nearestTempl.genericTypeParamDescs;
        var arr = new TypeDesc[genericTypeParamDescs.length];
        for (int i = 0; i < genericTypeParamDescs.length; i++) {
            arr[i] = newTypeMapping.get(genericTypeParamDescs[i]);
        }
        return new GenericTypeArguments(newTypeMapping, arr, nearestTempl, isIntermediate, hasIntermediateArgs, headers);
    }

    public TypeDesc mapType(GenericTypeDesc genericTypeDesc){
        return typeMapping.getOrDefault(genericTypeDesc, genericTypeDesc);
    }

    public boolean containsType(GenericTypeDesc genericTypeDesc){
        return typeMapping.containsKey(genericTypeDesc);
    }

    @Override
    public int hashCode() {
        return this.typeMapping.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GenericTypeArguments that = (GenericTypeArguments) o;
        return Objects.equals(this.typeMapping, that.typeMapping);
    }


    public int size() {
        return this.typeMapping.size();
    }

    public boolean canApplyToTemplate(ClassHeader templateClass, Map<String, ClassHeader> headers) {
        if(templateClass.genericSource != null){
            var genericSource = templateClass.genericSource;
            if(canApplyToTemplate(genericSource.sourceTemplate(), headers)) return true;
            var a = genericSource.typeArguments();
            if(a.isIntermediate) {
                if (a.intermediateClass == null) a.resolveIntermediateClass(headers);
            }
            if(a.intermediateClass != null && canApplyToTemplate(a.intermediateClass, headers)) return true;
        }
        if(templateClass == this.sourceTemplate || templateClass.belongsTo(this.sourceTemplate)){
            return true;
        }
        if(this.otherTemplates != null){
            for (String s : this.otherTemplates) {
                var template = headers.get(s);
                assert template != null;
                if(templateClass == template || templateClass.belongsTo(template)){
                    return true;
                }
            }

        }
        return false;
    }

    public boolean isIntermediate() {
        return isIntermediate || hasIntermediateArgs;
    }

    public boolean hasUnsolvedGenericTypeDesc() {
        return hasUnsolvedGenericTypeDesc;
    }

    public int mapTypeCode(int genericTypeCode) {
        return typeCodeMap.getOrDefault(genericTypeCode, genericTypeCode);
    }

    public String mapTypeCodeToClassName(int genericCode) {
        return mapType(genericCode).className;
    }

    public TypeDesc mapType(int genericCode) {
        for (Map.Entry<GenericTypeDesc, TypeDesc> entry : this.typeMapping.entrySet()) {
            if (entry.getKey().typeCode.value == genericCode) {
                return entry.getValue();
            }
        }
        return null;
    }

    public boolean valuesMatch(GenericTypeArguments args, Map<String, ClassHeader> headers) {
        for (var typeDesc : this.getTypeArgumentsArray()) {
            if(typeDesc.getTypeCode() == TypeCode.OBJECT) {
                var c = headers.get(typeDesc.getClassName());
                if (c.genericSource != null) {
                    var innerArgs = c.genericSource.typeArguments();
                    if (args.canApplyToTemplate(c, headers)) return true;
                    if (innerArgs.valuesMatch(args, headers)) return true;
                }
            }
        }
        return false;
    }

}
