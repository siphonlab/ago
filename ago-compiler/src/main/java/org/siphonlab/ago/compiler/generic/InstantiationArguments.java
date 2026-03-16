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

    private final ClassRefLiteral[] typeArgumentsArray;     // for direct applied on template class, it has value, for intermediate class, it's empty

    TreeMap<GenericTypeCodeAvatarClassDef, ClassDef> typeMapping = new TreeMap<>();          // may have other template's type parameter

    // these arguments associated with which template class, it's always the nearest outer template class
    // it must be a template class, never be the inner class of a template
    // the inner class of a template can be found in GenericSource
    protected final ClassDef sourceTemplate;
//    private ClassDef intermediateClass;
//    private Set<ClassDef> otherTemplates;

    private final boolean isTerminated;

    public InstantiationArguments(TypeParamsContext typeParamsContext, ClassRefLiteral[] typeArguments){
        this.sourceTemplate = typeParamsContext.getTemplateClass();
        for (int i = 0; i < typeParamsContext.size(); i++) {
            var tp = typeParamsContext.get(i);
            ClassDef v = typeArguments[i].getClassDefValue();
            typeMapping.put(tp, v);
        }
        this.flatString = stringify();
        this.typeArgumentsArray = typeArguments;
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
        this.typeArgumentsArray = null;
        this.sourceTemplate = null;
        isTerminated = determineTerminated();
    }

    public InstantiationArguments(TreeMap<GenericTypeCodeAvatarClassDef, ClassDef> typeMapping, ClassDef sourceTemplate, ClassRefLiteral[] typeArguments) {
        this.typeMapping = typeMapping;
        this.sourceTemplate = sourceTemplate;
        this.typeArgumentsArray = typeArguments;
        this.flatString = stringify();
        isTerminated = determineTerminated();
    }

    public ClassRefLiteral[] getTypeArgumentsArray() {
        return typeArgumentsArray;
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

//    private InstantiationArguments(TreeMap<GenericTypeCode, ClassDef> typeMapping, ClassRefLiteral[] typeArgumentsArray, ClassDef sourceTemplate, boolean isIntermediate, boolean hasIntermediateArgs) {
//        this.typeMapping = typeMapping;
//        this.sourceTemplate = sourceTemplate;
//        this.isIntermediate = isIntermediate;
//        this.flatString = stringify();
//        this.typeArgumentsArray = typeArgumentsArray;
//        this.hasIntermediateArgs = hasIntermediateArgs;
//        if(isIntermediate) resolveIntermediateClass();
//        if(this.typeMapping.size() != this.typeArgumentsArray.length || hasIntermediateArgs) collectOtherTemplates();
//    }
//
//    private void collectOtherTemplates(){
//        var otherTemplates = new HashSet<ClassDef>();
//        for (var genericType : this.typeMapping.keySet()) {
//            if(!genericType.getTemplateClass().equals(this.sourceTemplate)){
//                otherTemplates.add(genericType.getTemplateClass());
//            }
//        }
//        if(hasIntermediateArgs){
//            for (ClassRefLiteral classRefLiteral : this.typeArgumentsArray) {
//                ClassDef classDefValue = classRefLiteral.getClassDefValue();
//                if(isIntermediateArg(classDefValue)){
//                    InstantiationArguments instantiationArguments = classDefValue.getGenericSource().instantiationArguments();
//                    if(instantiationArguments.otherTemplates != null)
//                        otherTemplates.addAll(instantiationArguments.otherTemplates);
//                }
//            }
//        }
//        this.otherTemplates = otherTemplates;
//    }
//
//    // for parent->child, it must apply via `parent.applyChild(child)`, no `child.applyChild(parent)`
//    public InstantiationArguments applyChild(InstantiationArguments childArguments){
//        boolean stillIntermediate = this.isIntermediate || childArguments.isIntermediate;
//        boolean stillHasIntermediateArgs = this.hasIntermediateArgs || childArguments.hasIntermediateArgs;
//        TreeMap<GenericTypeCode, ClassDef> newTypeMapping = new TreeMap<>();
//        newTypeMapping.putAll(this.typeMapping);
//        newTypeMapping.putAll(childArguments.typeMapping);
//        return new InstantiationArguments(newTypeMapping, childArguments.typeArgumentsArray, childArguments.sourceTemplate, stillIntermediate, stillHasIntermediateArgs);
//    }
//
//
//    /// continue to apply instantiation on an intermediate template class. i.e. `A<TA>{ something as G<TA>}; new A<Dog>`, now call `G<TA>.apply(A<Dog>.instantiationArgs)`
//    /// for intermediate class, call `intermediate.apply(new_args)`
//    /// @param concreteArguments
//    /// @return
//    public InstantiationArguments applyIntermediate(InstantiationArguments concreteArguments){
//        if(!this.isIntermediate) return this;
//        if(!this.intermediateClass.isAffectedByTemplate(concreteArguments)){
//            return this;
//        }
//
//        boolean stillIntermediate = false;
//        boolean hasIntermediateArgs = false;
//        TreeMap<GenericTypeCode, ClassDef> newTypeMapping = new TreeMap<>();
//        for (Map.Entry<GenericTypeCode, ClassDef> entry : this.typeMapping.entrySet()) {
//            ClassDef classDef = entry.getValue();
//            GenericTypeCode typeCode = entry.getKey();
//            if (isIntermediate && classDef instanceof GenericTypeCode.GenericCodeAvatarClassDef avatarClassDef) {
//                ClassDef mapped = concreteArguments.mapType(avatarClassDef.getTypeCode());
//                newTypeMapping.put(typeCode, mapped);
//                if(!hasIntermediateArgs) hasIntermediateArgs = isIntermediateArg(mapped);
//                if (mapped instanceof GenericTypeCode.GenericCodeAvatarClassDef) {
//                    stillIntermediate = true;
//                }
//            } else {
//                newTypeMapping.put(typeCode, classDef);
//                if(!hasIntermediateArgs) {
//                    hasIntermediateArgs = isIntermediateArg(classDef);
//                }
//            }
//        }
//        var arr = newTypeMapping.sequencedValues().stream().map(ClassDef::toClassRefLiteral).toArray(ClassRefLiteral[]::new);
//        return new InstantiationArguments(newTypeMapping, arr, sourceTemplate, stillIntermediate, hasIntermediateArgs);
//    }
//
//    private boolean isIntermediateArg(ClassDef classDef) {
//        if(classDef.isGenericTemplateOrIntermediate() && classDef.getGenericSource() != null){
//            InstantiationArguments instantiationArguments = classDef.getGenericSource().instantiationArguments();
//            return instantiationArguments.isIntermediate || instantiationArguments.hasIntermediateArgs;
//        }
//        return false;
//    }
//
//    void resolveIntermediateClass(){
//        ClassDef intermediateClass = null;
//        for (var ref : this.typeArgumentsArray) {
//            var v = ref.getClassDefValue();
//            if(v instanceof GenericTypeCode.GenericCodeAvatarClassDef a){
//                var c = a.getTypeCode().getTemplateClass();
//                if(intermediateClass == null || c.belongsTo(intermediateClass)){        // find most inner class
//                    intermediateClass = c;
//                }
//            }
//        }
//        this.intermediateClass = intermediateClass;
//    }
//
//    public ClassDef getSourceTemplate() {
//        return sourceTemplate;
//    }
//
//    /// take arguments for some template class, include parent arguments
//    public InstantiationArguments takeFor(ClassDef sourceTemplate){
//        if(sourceTemplate == this.sourceTemplate || sourceTemplate.belongsTo(this.sourceTemplate)){
//            return this;
//        }
//        boolean isIntermediate = false;
//        boolean hasIntermediateArgs = false;
//        ClassDef nearestTempl = null;
//        TreeMap<GenericTypeCode, ClassDef> newTypeMapping = new TreeMap<>();
//        for (Map.Entry<GenericTypeCode, ClassDef> entry : this.typeMapping.sequencedEntrySet()) {
//            ClassDef t = entry.getKey().getTemplateClass();
//            if(t == sourceTemplate || sourceTemplate.belongsTo(t)){
//                ClassDef classDef = entry.getValue();
//                newTypeMapping.put(entry.getKey(), classDef);
//                if(this.isIntermediate && classDef instanceof GenericTypeCode.GenericCodeAvatarClassDef){
//                    isIntermediate = true;
//                }
//                if(this.hasIntermediateArgs && isIntermediateArg(classDef)){
//                    hasIntermediateArgs = true;
//                }
//                nearestTempl = t;
//            }
//        }
//        if(newTypeMapping.isEmpty()){
//            return null;
//        }
//
//        var context = nearestTempl.getTypeParamsContext();
//        var arr = new ClassRefLiteral[context.size()];
//        for (int i = 0; i < context.size(); i++) {
//            arr[i] = newTypeMapping.get(context.get(i)).toClassRefLiteral();
//        }
//        return new InstantiationArguments(newTypeMapping, arr, nearestTempl, isIntermediate, hasIntermediateArgs);
//    }

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
        if(this.sourceTemplate == null){
            return new InstantiationArguments(r);
        } else {
            var arr = this.sourceTemplate.getTypeParamsContext().getGenericTypeParams().stream().map(t -> r.get(t).toClassRefLiteral()).toArray(ClassRefLiteral[]::new);
            return new InstantiationArguments(r, this.sourceTemplate, arr);
        }
    }

    // inner template apply parent type arguments
    // first apply, then mix parent params
    public InstantiationArguments applyParent(InstantiationArguments parentTypeArguments) throws CompilationError{
        var r = apply(parentTypeArguments);
        r.typeMapping.putAll(parentTypeArguments.typeMapping);
        return r;
    }

    public InstantiationArguments withoutTemplate(){
        if(this.typeArgumentsArray == null){
            return this;
        } else {
            return new InstantiationArguments(this.typeMapping);
        }
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

    public InstantiationArguments takeFor(ClassDef templ) {
        if(this.sourceTemplate == templ){
            return this;
        }
        var r = new TreeMap<GenericTypeCodeAvatarClassDef, ClassDef>();
        for (GenericTypeCodeAvatarClassDef genericTypeParam : templ.getTypeParamsContext().getGenericTypeParams()) {
            r.put(genericTypeParam, this.typeMapping.get(genericTypeParam));
        }
        return new InstantiationArguments(r, templ, this.typeArgumentsArray);
    }

    public ClassDef getSourceTemplate() {
        return sourceTemplate;
    }

    public Collection<ClassDef> getAllArguments() {
        return this.typeMapping.values();
    }
}
