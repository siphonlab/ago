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


import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.Set;

public class ClassIntervalClassDef extends ParameterizedClassDef implements ClassBound {

    public ClassIntervalClassDef(ClassDef baseClass, ConstructorDef parameterizedConstructor, Literal<?>[] arguments) {
        super(baseClass, parameterizedConstructor, arguments);
    }

    @Override
    public ClassDef getLBoundClass() {
        Root root = this.getRoot();
        ClassDef classDefValue = ((ClassRefLiteral) this.arguments[0]).getClassDefValue();
        //return classDefValue == root.getAnyClass() ? root.getObjectClass() : classDefValue;
        return classDefValue;
    }

    @Override
    public ClassDef getUBoundClass() {
        return ((ClassRefLiteral) this.arguments[1]).getClassDefValue();
    }

    @Override
    public boolean isThatOrSuperOfThat(ClassDef anotherClass) {
        return asThatOrSuperOfThat(anotherClass) != null;
    }

    @Override
    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;
        this.nextCompilingStage(CompilingStage.ValidateHierarchy);
        return true;
    }

    // only accept another ClassInterval as value
    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited) {
        if(this == anotherClass) return anotherClass;

        ClassDef lBound = this.getLBoundClass();
        ClassDef uBound = this.getUBoundClass();
        ClassDef any = getRoot().getAnyClass();
        if(lBound == any && uBound == any) return anotherClass;

        if(anotherClass instanceof ClassBound another){
            var l2 = another.getLBoundClass();
            var u2 = another.getUBoundClass();
            return  (uBound == any || u2.isThatOrSuperOfThat(uBound, visited)) ? (lBound == any ? anotherClass : lBound.asThatOrSuperOfThat(l2,visited)) : null;
        }

        return (uBound == any || anotherClass.isThatOrSuperOfThat(uBound, visited)) ? (lBound == any ? anotherClass : lBound.asThatOrSuperOfThat(anotherClass, visited)) : null;

    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) {
        ClassIntervalClassDef c = null;
        try {
            c = this.getParentClass().getOrCreateClassInterval(baseClass.instantiate(instantiationArguments, returnExisted), constructor, mapArguments(instantiationArguments), returnExisted);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        return c;
    }

//    @Override
//    public boolean isPrimitiveFamily() {
//        return this.getLBoundClass().isPrimitiveFamily();
//    }
//
//    @Override
//    public boolean isPrimitiveNumberFamily() {
//        return this.getLBoundClass().isPrimitiveNumberFamily();
//    }
}
