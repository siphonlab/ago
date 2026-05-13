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

import static org.siphonlab.ago.AgoClass.TYPE_ANY_CLASS;

public class AnyClassDef extends ClassDef{

    public AnyClassDef(Root root, String name) {
        super(root, name);
        this.classType = TYPE_ANY_CLASS;
        this.compilingStage = CompilingStage.ResolveHierarchicalClasses;
    }

    public AnyClassDef(Root root, String name, AgoParser.ClassDeclarationContext classDeclaration) {
        super(root, name, classDeclaration);
        this.classType = TYPE_ANY_CLASS;
        this.compilingStage = CompilingStage.ResolveHierarchicalClasses;
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        super.resolveHierarchicalClasses();
        this.setCompilingStage(CompilingStage.Compiled);
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited) {
        if (visited != null) {
            if (visited.contains(anotherClass)) {
                return null;
            }
            visited.add(anotherClass);
        }
        return this;
    }

    @Override
    public TypeCode getTypeCode() {
        return TypeCode.UNION;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments) {
        return false;
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, ClassContainer parent, MutableBoolean returnExisted) {
        return this;
    }

    @Override
    public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) {
        if(returnExisted!=null) returnExisted.setTrue();
        return this;
    }

    @Override
    public ClassDef instantiateAsReferenceClass(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        return instantiate(arguments, returnExisted);
    }

    @Override
    public boolean isGenericTerminated(Set<ClassDef> visited) {
        return true;
    }

    @Override
    public Root getRoot() {
        return root;
    }

}
