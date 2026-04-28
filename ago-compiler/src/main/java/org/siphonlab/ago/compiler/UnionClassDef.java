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

import java.util.List;
import java.util.Set;

public class UnionClassDef extends ClassDef implements ConcreteType{

    protected ClassDef[] classes;

    public UnionClassDef(Root root, String name) {
        super(root, name);
    }

    public UnionClassDef(Root root, String name, AgoParser.ClassDeclarationContext classDeclaration) {
        super(root, name, classDeclaration);
    }

    public ClassDef[] getClasses() {
        return classes;
    }


    @Override
    public List<ClassDef> getConcreteDependencyClasses() {
        return List.of(classes);
    }

    @Override
    public void acceptRegisterConcreteType(ClassDef hostClass) {
        //
    }

    @Override
    public ClassDef instantiateAsReferenceClass(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        return instantiate(arguments, returnExisted);
    }

    @Override
    public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        if(!this.isAffectedByTypeArguments(arguments)) {
            if(returnExisted != null) returnExisted.setTrue();
            return this;
        }
        return cloneForInstantiate(arguments, (ClassContainer) this.parent, returnExisted);
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, ClassContainer parent, MutableBoolean returnExisted) throws CompilationError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments) {
        for (ClassDef aClass : this.classes) {
            if(aClass.isAffectedByTypeArguments(instantiationArguments)) return true;
        }
        return false;
    }

    @Override
    public boolean isGenericTerminated(Set<ClassDef> visited) {
        for (ClassDef aClass : this.classes) {
            if(!aClass.isGenericTerminated(visited)) return false;
        }
        return true;
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited) {
        if(this ==  anotherClass) return this;

        if (visited != null) {
            if (visited.contains(anotherClass)) {
                return null;
            }
            visited.add(anotherClass);
        }

        if(anotherClass instanceof UnionClassDef another){
            for(ClassDef b : another.classes) {
                boolean found = false;
                for (ClassDef a : this.classes) {
                    if(a.isThatOrSuperOfThat(b)){
                        found = true;
                        break;
                    }
                }
                if(!found) return null;
            }
        } else {
            for (ClassDef a : this.classes) {
                if(a.isThatOrSuperOfThat(anotherClass)){
                    return this;
                }
            }
        }

        return null;
    }

    @Override
    public TypeCode getTypeCode() {
        return TypeCode.UNION;
    }
}
