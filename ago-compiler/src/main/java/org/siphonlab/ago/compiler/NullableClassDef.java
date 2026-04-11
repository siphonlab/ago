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

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.Set;

public class NullableClassDef extends UnionClassDef {

    private final ClassDef baseClass;

    public NullableClassDef(Root root, ClassDef classDef) {
        super(root, composeName(classDef.getName()));
        this.baseClass = classDef;
        this.classes = new ClassDef[]{baseClass, root.NULL()};
        this.compilingStage = CompilingStage.ResolveHierarchicalClasses;
    }

    public static String composeName(String name) {
        return name + "?";
    }

    public ClassDef getBaseClass() {
        return baseClass;
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;

        if(this.baseClass.compilingStage.lte(CompilingStage.ResolveHierarchicalClasses)){
            this.baseClass.resolveHierarchicalClasses();
        }

        this.setSuperClass(root.getAnyClass());

        this.setCompilingStage(CompilingStage.InheritsFields);
        try {
            Compiler.processClassTillStage(this, baseClass.getCompilingStage());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, ClassContainer parent, MutableBoolean returnExisted) throws CompilationError {
        var newBaseType = baseClass.instantiate(instantiationArguments, returnExisted);
        if(newBaseType == baseClass) {
            if(returnExisted != null) returnExisted.setTrue();
            return this;
        }

        var nullableType = root.getOrCreateNullableType(newBaseType, returnExisted);
//        ownerClass.idOfClass(arrayType);
//        if(!elementType.isPrimary()) ownerClass.idOfClass(elementType);
        return nullableType;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments) {
        return this.baseClass.isAffectedByTypeArguments(instantiationArguments);
    }

    @Override
    public boolean isGenericTerminated(Set<ClassDef> visited) {
        return this.baseClass.isGenericTerminated(visited);
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited) {
        if(this == anotherClass) return this;

        if (visited != null) {
            if (visited.contains(anotherClass)) {
                return null;
            }
            visited.add(anotherClass);
        }

        if(anotherClass instanceof NullableClassDef another){
            if(baseClass.isThatOrSuperOfThat(another.baseClass, visited)){
                return this;
            }
        }

        if(baseClass.isThatOrSuperOfThat(anotherClass, visited) || anotherClass instanceof NullClassDef)
            return this;

        return null;
    }


}
