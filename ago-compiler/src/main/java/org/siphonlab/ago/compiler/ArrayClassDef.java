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
package org.siphonlab.ago.compiler;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;

import java.util.List;
import java.util.Set;

public class ArrayClassDef extends ClassDef implements ConcreteType{

    public ArrayClassDef(Root root, ClassDef elementType) throws CompilationError {
        super(composeArrayTypeName(elementType));
        this.root = root;
        this.elementType = elementType;
        this.compilingStage = CompilingStage.ResolveHierarchicalClasses;
    }

    public static String composeArrayTypeName(ClassDef componentType) {
        return "[" + componentType.getName();
    }

    private final Root root;
    private ClassDef elementType;

    public ClassDef getElementType() {
        return elementType;
    }

    public void setElementType(ClassDef elementType) {
        this.elementType = elementType;
    }

    @Override
    public int hashCode() {
        return this.getFullname().hashCode();
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;

        ClassDef arrayClass = root.getArrayClass();
        this.setSourceLocation(arrayClass.getSourceLocation());
        ClassDef arrayInstantiationType = arrayClass.instantiate(new InstantiationArguments(arrayClass.getTypeParamsContext(), new ClassRefLiteral[]{new ClassRefLiteral(elementType)}), null);
        this.setSuperClass(arrayInstantiationType);
        this.registerConcreteType((ConcreteType) arrayInstantiationType);
        this.setCompilingStage(CompilingStage.InheritsFields);
        try {
            Compiler.processClassTillStage(this,elementType.getCompilingStage());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Root getRoot() {
        return root;
    }

    @Override
    public void inheritsChildClasses() throws CompilationError {
        super.inheritsChildClasses();
        this.setCompilingStage(CompilingStage.Compiled);
    }

    @Override
    public List<ClassDef> getConcreteDependencyClasses() {
        if(this.elementType instanceof ConcreteType c){
            return ListUtils.union(List.of(this.elementType), c.getConcreteDependencyClasses());
        }
        return List.of(this.elementType);
    }

    @Override
    public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        return cloneForInstantiate(arguments,returnExisted);
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) throws CompilationError {
        // when T[] works on T=int, apply template got a new type `int[]`
        var newEleType = elementType.instantiate(instantiationArguments, returnExisted);
        if(newEleType == elementType) {
            if(returnExisted != null) returnExisted.setTrue();
            return this;
        }

        var arrayType = root.getOrCreateArrayType(newEleType, returnExisted);
//        ownerClass.idOfClass(arrayType);
//        if(!elementType.isPrimary()) ownerClass.idOfClass(elementType);
        return arrayType;
    }

    @Override
    public boolean isAffectedByTemplate(InstantiationArguments instantiationArguments) {
        return this.elementType.isAffectedByTemplate(instantiationArguments);
    }

    @Override
    public boolean isGenericInstantiateRequiredForNew() {
        return this.getElementType().isGenericInstantiateRequiredForNew();
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited) {
        if(anotherClass instanceof ArrayClassDef another){
            if(another.getElementType() == this.elementType){
                return anotherClass;
            }
        }
        return null;
    }

    @Override
    public void acceptRegisterConcreteType(ClassDef hostClass) {
        //
    }
}
