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
import org.siphonlab.ago.Variance;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.ConstructorDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.literal.ByteLiteral;
import org.siphonlab.ago.compiler.parser.AgoParser;

/**
 * +T as [Animal to _] got GenericTypeParameter::(Animal, Any, variance=1)
 * a box type for classref, like {@link ScopedClassIntervalClassDef}, however it only box classref, accept no scope
 */
public class SharedGenericTypeParameterClassDef extends ClassIntervalClassDef{

    public SharedGenericTypeParameterClassDef(ClassDef baseClass, ConstructorDef parameterizedConstructor, Literal<?>[] arguments) {
        super(baseClass, parameterizedConstructor, arguments);
    }

    public Variance getVariance(){
        return Variance.of(((ByteLiteral)this.arguments[2]).value);
    }

    @Override
    public AgoParser.ClassBodyContext getClassBody() {
        if(this.classDeclaration == null) return null;
        return super.getClassBody();
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) {
        SharedGenericTypeParameterClassDef c = null;
        try {
            c = this.getParentClass().getOrCreateGenericTypeParameter(baseClass.instantiate(instantiationArguments, returnExisted), constructor, mapArguments(instantiationArguments), returnExisted);
            if(c.getUnit() == null) c.setUnit(this.getUnit());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        return c;
    }

}
