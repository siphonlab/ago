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
import org.apache.commons.lang3.mutable.MutableInt;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;

import java.util.Set;

public class NullableClassDef extends UnionClassDef {

    private final ClassDef nullableBaseClass;

    public NullableClassDef(Root root, ClassDef classDef) {
        super(root.getNullableClass(), root.getNullableClass().getMetaClassDef().getConstructor(), new Literal[]{classDef.toClassRefLiteral(), root.NULL().toClassRefLiteral()});
        this.nullableBaseClass = classDef;
        this.compilingStage = CompilingStage.ResolveHierarchicalClasses;
        this.name = composeName(nullableBaseClass.getFullname());
    }

    // constructor changed, from 2 params to 1
    @Override
    public Literal<?>[] getArguments() {
        return new Literal[]{this.arguments[0]};
    }

    public static String composeName(String fullname) {
        return "?" + fullname + ";";
    }

    public ClassDef getNullableBaseClass() {
        return nullableBaseClass;
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;

        if(this.nullableBaseClass.compilingStage.lte(CompilingStage.ResolveHierarchicalClasses)){
            this.nullableBaseClass.resolveHierarchicalClasses();
        }

        this.setSuperClass(root.getAnyClass());

        this.setCompilingStage(CompilingStage.InheritsFields);
        try {
            Compiler.processClassTillStage(this, nullableBaseClass.getCompilingStage());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, ClassContainer parent, MutableBoolean returnExisted) throws CompilationError {
        var newBaseType = nullableBaseClass.instantiate(instantiationArguments, returnExisted);
        if(newBaseType == nullableBaseClass) {
            if(returnExisted != null) returnExisted.setTrue();
            return this;
        }

        var nullableType = root.getOrCreateNullableType(newBaseType, returnExisted);
//        ownerClass.idOfClass(arrayType);
//        if(!elementType.isPrimary()) ownerClass.idOfClass(elementType);
        return nullableType;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments, Set<ClassDef> visited) {
        return this.nullableBaseClass.isAffectedByTypeArguments(instantiationArguments, visited);
    }

    @Override
    public boolean isGenericTerminated(Set<ClassDef> visited) {
        return this.nullableBaseClass.isGenericTerminated(visited);
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited, MutableInt depth) {
        if(this == anotherClass) return this;

        if (visited != null) {
            if (visited.contains(anotherClass)) {
                return null;
            }
            visited.add(anotherClass);
        }
        if(depth != null) depth.increment();

        if(anotherClass instanceof NullableClassDef another){
            if(nullableBaseClass.isThatOrSuperOfThat(another.nullableBaseClass, visited)){
                return this;
            }
        }

        if(nullableBaseClass.isThatOrSuperOfThat(anotherClass, visited) || anotherClass instanceof NullClassDef)
            return this;

        if(depth != null) depth.decrement();
        return null;
    }


}
