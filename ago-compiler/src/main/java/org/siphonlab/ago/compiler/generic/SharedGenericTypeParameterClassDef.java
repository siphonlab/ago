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
import org.siphonlab.ago.compiler.ClassContainer;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.ConstructorDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.literal.ByteLiteral;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.Set;

/**
 * +T as [Animal to _] got GenericTypeParameter::(Animal, Any, variance=1, G, 0, genericTypeCodeIndex)
 * a box type for classref, like {@link ScopedClassIntervalClassDef}, however it only box classref, accept no scope
 */
public class SharedGenericTypeParameterClassDef extends ClassIntervalClassDef{

    private final Variance variance;

    public SharedGenericTypeParameterClassDef(ClassDef baseClass, ConstructorDef parameterizedConstructor, Literal<?>[] arguments) {
        super(baseClass, parameterizedConstructor, arguments);
        variance = Variance.of(((ByteLiteral)this.arguments[2]).value);
        this.name = composeName(this.getLBoundClass(), this.getUBoundClass(), this.variance);
    }

    public SharedGenericTypeParameterClassDef(ClassDef langGenericTypeParam, ConstructorDef parameterizedConstructor, ClassDef lBound, ClassDef uBound,
                                              Variance variance) {
        super(langGenericTypeParam, parameterizedConstructor, new Literal[]{lBound.toClassRefLiteral(), uBound.toClassRefLiteral(), langGenericTypeParam.getRoot().createByteLiteral(variance.byteValue())});
        this.variance = variance;
        this.name = composeName(this.getLBoundClass(), this.getUBoundClass(), this.variance);
    }

    public Variance getVariance(){
        return variance;
    }


    @Override
    public AgoParser.ClassBodyContext getClassBody() {
        if(this.classDeclaration == null) return null;
        return super.getClassBody();
    }

    @Override
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, ClassContainer parent, MutableBoolean returnExisted) {
        SharedGenericTypeParameterClassDef c = null;
        try {
            c = this.getParentClass().getOrCreateGenericTypeParameter(baseClass, constructor,
                    this.getLBoundClass().instantiate(instantiationArguments, null),
                    this.getUBoundClass().instantiate(instantiationArguments, null),
                    this.variance,
                    returnExisted);
            if(c.getUnit() == null) c.setUnit(this.getUnit());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    public static String composeName(ClassDef lBound, ClassDef uBound, Variance variance){
        StringBuilder sb = switch (variance){
            case Invariance ->  new StringBuilder();
            case Covariance ->   new StringBuilder("+");
            case Contravariance ->   new StringBuilder("-");
        };
        sb.append("[").append(composeNameOfClassInClassInterval(lBound)).append('~').append(composeNameOfClassInClassInterval(uBound)).append(']');
        return sb.toString();
    }

    @Override
    public boolean isGenericTerminated(Set<ClassDef> visited) {
        return false;
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited) {
        return super.asThatOrSuperOfThat(anotherClass, visited);
    }
}
