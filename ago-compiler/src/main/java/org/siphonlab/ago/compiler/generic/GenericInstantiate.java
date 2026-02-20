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


import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class GenericInstantiate {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericInstantiate.class);

    private final ClassDef templateClass;
    private final ClassDef instantiateClass;
    private final InstantiationArguments instantiationArguments;
    private final Map<ClassDef, ClassDef> complexTypeMapping = new HashMap<>();

    public GenericInstantiate(ClassDef instantiateClass, ClassDef templateClass, InstantiationArguments instantiationArguments) {
        this.templateClass = templateClass;
        this.instantiateClass = instantiateClass;
        this.instantiationArguments = instantiationArguments;
    }

    public static void syncCompilingStage(ClassDef instantiateClass, CompilingStage compilingStage){
        try {
            CompilingStage prevStage = compilingStage.prev();
            Compiler.processClassTillStage(instantiateClass, prevStage);
            for (Namespace<?> n : new ArrayList<>(instantiateClass.getAllDescendants().getUniqueElements())) {
                if(n instanceof ClassDef classDef){
                    Compiler.processClassTillStage(classDef, prevStage);
                }
            }
            if(instantiateClass.getMetaClassDef() != null){
                Compiler.processClassTillStage(instantiateClass.getMetaClassDef(), prevStage);
                for (Namespace<?> n : new ArrayList<>(instantiateClass.getAllDescendants().getUniqueElements())) {
                    if(n instanceof ClassDef classDef){
                        Compiler.processClassTillStage(classDef, prevStage);
                    }
                }
            }
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

//    public ClassDef mapType(ClassDef classDef, ClassDef ownerClass){
//        if(classDef == null) return null;
//        if(classDef.getTypeCode() instanceof GenericTypeCode){
//            return typeMapping.getOrDefault(classDef, classDef);
//        } else {
//            var existed = typeMapping.get(classDef);
//            if(existed != null) return existed;
//
//            if (classDef.isAffectedByTemplate(templateClass)) {
//                var isExisted = new MutableBoolean();
//                var cloned = classDef.cloneForTemplate(this, ownerClass, isExisted);
//                if(!isExisted.booleanValue()){
//                    syncCompilingStage(cloned, templateClass.getCompilingStage());
//                }
//                typeMapping.put(classDef, cloned);
//                return cloned;
//            } else {
//                return classDef;
//            }
//        }
//    }

//    public ClassDef mapType(ClassDef classDef, ClassDef ownerClass){
//        if(classDef == null) return null;
//        if(classDef.getTypeCode() instanceof GenericTypeCode genericTypeCode){
//            return instantiationArguments.mapType(genericTypeCode);
//        } else {
//            var existed = complexTypeMapping.get(classDef);
//            if(existed != null) return existed;
//
//            if (classDef.isAffectedByTemplate(instantiationArguments)) {
//                var isExisted = new MutableBoolean();
//                var cloned = classDef.cloneForInstantiate(this, isExisted);
//                if(!isExisted.booleanValue()){
//                    syncCompilingStage(cloned, templateClass.getCompilingStage());
//                }
//                complexTypeMapping.put(classDef, cloned);
//                return cloned;
//            } else {
//                return classDef;
//            }
//        }
//    }

    public Map<ClassDef, ClassDef> getTypeMapping() {
        return complexTypeMapping;
    }

    public ClassDef getTemplateClass() {
        return templateClass;
    }

    public ClassDef getInstantiateClass() {
        return instantiateClass;
    }

    public static String composeName(ClassDef templateClass, ClassRefLiteral[] typeArguments) {
        return templateClass.getName() + "<" + Arrays.stream(typeArguments)
                .map(l -> {
                    var v = l.getClassDefValue();
                    if(v instanceof GenericTypeCode.GenericCodeAvatarClassDef a){
                        var typeCode = a.getTypeCode();
                        return typeCode.toShortString() + '|' + v.getFullname();
                    } else {
                        return v.getFullname();
                    }
                }).collect(Collectors.joining("|")) + ">";
    }

    public ClassRefLiteral[] getTypeArguments() {
        return this.instantiationArguments.getTypeArgumentsArray();
    }

    private boolean isTypeArgumentsMatch(GenericInstantiate anotherClass) {
        ClassRefLiteral[] typeArgumentsArray = this.instantiationArguments.getTypeArgumentsArray();
        if(anotherClass.getTypeArguments().length != typeArgumentsArray.length) return false;

        TypeParamsContext paramsContext = this.templateClass.getTypeParamsContext();
        for (int i = 0; i < typeArgumentsArray.length; i++) {
            var p = paramsContext.get(i).getGenericTypeParameterClassDef();
            var variance = p.getVariance();
            var a1 = typeArgumentsArray[i].getClassDefValue();
            var a2 = anotherClass.getTypeArguments()[i].getClassDefValue();
            switch (variance){
                case Invariance:
                    if(a1 != a2) return false;
                    break;
                case Covariance:
                    if(!a1.isThatOrSuperOfThat(a2)) return false;
                    break;
                case Contravariance:
                    if(!a2.isThatOrSuperOfThat(a1)) return false;
                    break;
            }
        }
        return true;
    }

    public static List<ClassDef> getConcreteDependencyClasses(ClassDef genericInstantiationClassDef) {
        List<ClassDef> r = new ArrayList<>();
        var genericSource = genericInstantiationClassDef.getGenericSource();
        ClassDef template = genericSource.originalTemplate();
        r.add(template);
        TypeParamsContext typeParamsContext = template.getTypeParamsContext();
        for (int i = 0; i < typeParamsContext.size(); i++) {
            r.add(typeParamsContext.get(i).getGenericTypeParameterClassDef());
        }

        if(template instanceof ConcreteType c){
            r.addAll(c.getConcreteDependencyClasses());
        }
        for(var p = genericInstantiationClassDef.getParentClass(); p != null; p = p.getParentClass()){
            if(p instanceof ConcreteType pc){
                r.add((ClassDef) pc);
                r.addAll(pc.getConcreteDependencyClasses());
            }
        }

        for (ClassRefLiteral typeArgument : genericSource.instantiationArguments().getTypeArgumentsArray()) {
            var v = typeArgument.getClassDefValue();
            if(v instanceof GenericTypeCode.GenericCodeAvatarClassDef a){
                r.add(a.getTypeCode().getGenericTypeParameterClassDef());
            } else {
                r.add(typeArgument.getClassDefValue());
            }
            if(typeArgument.getClassDefValue() instanceof ConcreteType a){
                r.addAll(a.getConcreteDependencyClasses());
            }
        }
        r.addAll(genericInstantiationClassDef.getConcreteTypes().values().stream().map(c -> (ClassDef)c).toList());
        return r;
    }

}
