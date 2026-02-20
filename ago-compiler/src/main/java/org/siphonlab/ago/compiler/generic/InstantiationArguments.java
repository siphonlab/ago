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
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.*;
import java.util.stream.Collectors;

public class InstantiationArguments {

    protected final boolean isIntermediate;
    protected final boolean hasIntermediateArgs;

    protected final String flatString;

    private final ClassRefLiteral[] typeArgumentsArray;

    TreeMap<GenericTypeCode, ClassDef> typeMapping = new TreeMap<>();

    // these arguments associated with which template class, it's always the nearest outer template class
    // it must be a template class, never be the inner class of a template
    // the inner class of a template can be found in GenericSource
    protected final ClassDef sourceTemplate;
    private ClassDef intermediateClass;
    private Set<ClassDef> otherTemplates;

    public InstantiationArguments(TypeParamsContext typeParamsContext, ClassRefLiteral[] typeArguments){
        this.sourceTemplate = typeParamsContext.getTemplateClass();
        boolean isIntermediate = false;
        boolean hasIntermediateArgs = false;
        for (int i = 0; i < typeParamsContext.size(); i++) {
            var tp = typeParamsContext.get(i);
            ClassDef v = typeArguments[i].getClassDefValue();
            typeMapping.put(tp, v);
            if(v instanceof GenericTypeCode.GenericCodeAvatarClassDef){
                isIntermediate = true;
            } else if(isIntermediateArg(v)){
                hasIntermediateArgs = true;
            }
        }
        this.isIntermediate = isIntermediate;
        this.hasIntermediateArgs = hasIntermediateArgs;
        this.flatString = stringify();
        this.typeArgumentsArray = typeArguments;
        if(isIntermediate) resolveIntermediateClass();
        if(this.typeMapping.size() != this.typeArgumentsArray.length || hasIntermediateArgs) collectOtherTemplates();
    }

    public ClassRefLiteral[] getTypeArgumentsArray() {
        return typeArgumentsArray;
    }

    private String stringify() {
        return typeMapping.sequencedEntrySet().stream().map(
                entry ->{
                    ClassDef classDef = entry.getValue();
                    if(classDef.getTypeCode().isGeneric()){
                        return entry.getKey() + "=" + classDef.getTypeCode();
                    } else {
                        return entry.getKey() + "=" + classDef.getFullname();
                    }
                }).collect(Collectors.joining(","));
    }

    @Override
    public String toString() {
        return this.flatString;
    }

    private InstantiationArguments(TreeMap<GenericTypeCode, ClassDef> typeMapping, ClassRefLiteral[] typeArgumentsArray, ClassDef sourceTemplate, boolean isIntermediate, boolean hasIntermediateArgs) {
        this.typeMapping = typeMapping;
        this.sourceTemplate = sourceTemplate;
        this.isIntermediate = isIntermediate;
        this.flatString = stringify();
        this.typeArgumentsArray = typeArgumentsArray;
        this.hasIntermediateArgs = hasIntermediateArgs;
        if(isIntermediate) resolveIntermediateClass();
        if(this.typeMapping.size() != this.typeArgumentsArray.length || hasIntermediateArgs) collectOtherTemplates();
    }

    private void collectOtherTemplates(){
        var otherTemplates = new HashSet<ClassDef>();
        for (var genericType : this.typeMapping.keySet()) {
            if(!genericType.getTemplateClass().equals(this.sourceTemplate)){
                otherTemplates.add(genericType.getTemplateClass());
            }
        }
        if(hasIntermediateArgs){
            for (ClassRefLiteral classRefLiteral : this.typeArgumentsArray) {
                ClassDef classDefValue = classRefLiteral.getClassDefValue();
                if(isIntermediateArg(classDefValue)){
                    InstantiationArguments instantiationArguments = classDefValue.getGenericSource().instantiationArguments();
                    if(instantiationArguments.otherTemplates != null)
                        otherTemplates.addAll(instantiationArguments.otherTemplates);
                }
            }
        }
        this.otherTemplates = otherTemplates;
    }

    // for parent->child, it must apply via `parent.applyChild(child)`, no `child.applyChild(parent)`
    public InstantiationArguments applyChild(InstantiationArguments childArguments){
        boolean stillIntermediate = this.isIntermediate || childArguments.isIntermediate;
        boolean stillHasIntermediateArgs = this.hasIntermediateArgs || childArguments.hasIntermediateArgs;
        TreeMap<GenericTypeCode, ClassDef> newTypeMapping = new TreeMap<>();
        newTypeMapping.putAll(this.typeMapping);
        newTypeMapping.putAll(childArguments.typeMapping);
        return new InstantiationArguments(newTypeMapping, childArguments.typeArgumentsArray, childArguments.sourceTemplate, stillIntermediate, stillHasIntermediateArgs);
    }


    /// continue to apply instantiation on an intermediate template class. i.e. `A<TA>{ something as G<TA>}; new A<Dog>`, now call `G<TA>.apply(A<Dog>.instantiationArgs)`
    /// for intermediate class, call `intermediate.apply(new_args)`
    /// @param concreteArguments
    /// @return
    public InstantiationArguments applyIntermediate(InstantiationArguments concreteArguments){
        if(!this.isIntermediate) return this;
        if(!this.intermediateClass.isAffectedByTemplate(concreteArguments)){
            return this;
        }

        boolean stillIntermediate = false;
        boolean hasIntermediateArgs = false;
        TreeMap<GenericTypeCode, ClassDef> newTypeMapping = new TreeMap<>();
        for (Map.Entry<GenericTypeCode, ClassDef> entry : this.typeMapping.entrySet()) {
            ClassDef classDef = entry.getValue();
            GenericTypeCode typeCode = entry.getKey();
            if (isIntermediate && classDef instanceof GenericTypeCode.GenericCodeAvatarClassDef avatarClassDef) {
                ClassDef mapped = concreteArguments.mapType(avatarClassDef.getTypeCode());
                newTypeMapping.put(typeCode, mapped);
                if(!hasIntermediateArgs) hasIntermediateArgs = isIntermediateArg(mapped);
                if (mapped instanceof GenericTypeCode.GenericCodeAvatarClassDef) {
                    stillIntermediate = true;
                }
            } else {
                newTypeMapping.put(typeCode, classDef);
                if(!hasIntermediateArgs) {
                    hasIntermediateArgs = isIntermediateArg(classDef);
                }
            }
        }
        var arr = newTypeMapping.sequencedValues().stream().map(ClassRefLiteral::new).toArray(ClassRefLiteral[]::new);
        return new InstantiationArguments(newTypeMapping, arr, sourceTemplate, stillIntermediate, hasIntermediateArgs);
    }

    private boolean isIntermediateArg(ClassDef classDef) {
        if(classDef.isGenericTemplateOrIntermediate() && classDef.getGenericSource() != null){
            InstantiationArguments instantiationArguments = classDef.getGenericSource().instantiationArguments();
            return instantiationArguments.isIntermediate || instantiationArguments.hasIntermediateArgs;
        }
        return false;
    }

    void resolveIntermediateClass(){
        ClassDef intermediateClass = null;
        for (var ref : this.typeArgumentsArray) {
            var v = ref.getClassDefValue();
            if(v instanceof GenericTypeCode.GenericCodeAvatarClassDef a){
                var c = a.getTypeCode().getTemplateClass();
                if(intermediateClass == null || c.belongsTo(intermediateClass)){        // find most inner class
                    intermediateClass = c;
                }
            }
        }
        this.intermediateClass = intermediateClass;
    }

    public ClassDef getSourceTemplate() {
        return sourceTemplate;
    }

    /// take arguments for some template class, include parent arguments
    public InstantiationArguments takeFor(ClassDef sourceTemplate){
        if(sourceTemplate == this.sourceTemplate || sourceTemplate.belongsTo(this.sourceTemplate)){
            return this;
        }
        boolean isIntermediate = false;
        boolean hasIntermediateArgs = false;
        ClassDef nearestTempl = null;
        TreeMap<GenericTypeCode, ClassDef> newTypeMapping = new TreeMap<>();
        for (Map.Entry<GenericTypeCode, ClassDef> entry : this.typeMapping.sequencedEntrySet()) {
            ClassDef t = entry.getKey().getTemplateClass();
            if(t == sourceTemplate || sourceTemplate.belongsTo(t)){
                ClassDef classDef = entry.getValue();
                newTypeMapping.put(entry.getKey(), classDef);
                if(this.isIntermediate && classDef instanceof GenericTypeCode.GenericCodeAvatarClassDef){
                    isIntermediate = true;
                }
                if(this.hasIntermediateArgs && isIntermediateArg(classDef)){
                    hasIntermediateArgs = true;
                }
                nearestTempl = t;
            }
        }
        if(newTypeMapping.isEmpty()){
            return null;
        }

        var context = nearestTempl.getTypeParamsContext();
        var arr = new ClassRefLiteral[context.size()];
        for (int i = 0; i < context.size(); i++) {
            arr[i] = new ClassRefLiteral(newTypeMapping.get(context.get(i)));
        }
        return new InstantiationArguments(newTypeMapping, arr, nearestTempl, isIntermediate, hasIntermediateArgs);
    }

    public ClassDef mapType(GenericTypeCode genericTypeCode){
        return typeMapping.getOrDefault(genericTypeCode, genericTypeCode.getGenericCodeAvatarClassDef());
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
    public boolean canApplyToTemplate(ClassDef templateClass) {
        if(templateClass.getGenericSource() != null){
            GenericSource genericSource = templateClass.getGenericSource();
            if(canApplyToTemplate(genericSource.originalTemplate())) return true;
            InstantiationArguments a = genericSource.instantiationArguments();
            if(a.intermediateClass != null && canApplyToTemplate(a.intermediateClass)) return true;
        }
        if(templateClass == this.sourceTemplate || templateClass.belongsTo(this.getSourceTemplate())){
            return true;
        }
        if(this.otherTemplates != null){
            for (var template : this.otherTemplates) {
                if(templateClass == template || templateClass.belongsTo(template)){
                    return true;
                }
            }
        }
//        for (ClassRefLiteral classRefLiteral : this.getTypeArgumentsArray()) {
//            GenericSource genericSource = classRefLiteral.getClassDefValue().getGenericSource();
//            if(genericSource != null && genericSource.instantiationArguments().isAffectedToTemplate(templateClass)){
//                return true;
//            }
//        }

        return false;
    }

    public boolean isIntermediate() {
        return isIntermediate || hasIntermediateArgs;
    }

    public boolean hasIntermediateArgs() {
        return hasIntermediateArgs;
    }

    public GenericTypeCode findGenericTypeCode(String typeCodeName) {
        for (ClassRefLiteral classRefLiteral : this.getTypeArgumentsArray()) {
            if(classRefLiteral.getClassDefValue() instanceof GenericTypeCode.GenericCodeAvatarClassDef a){
                if(a.getTypeCode().getName().equals(typeCodeName)){
                    return a.getTypeCode();
                }
            }
        }
        return null;
    }

    public boolean valuesMatch(InstantiationArguments args) {
        for (ClassRefLiteral classRefLiteral : this.getTypeArgumentsArray()) {
            var c = classRefLiteral.getClassDefValue();
            if(c.getGenericSource() != null) {
                InstantiationArguments innerArgs = c.getGenericSource().instantiationArguments();
                if(args.canApplyToTemplate(c)) return true;
                if(innerArgs.valuesMatch(args)) return true;
            }
        }
        return false;
    }
}
