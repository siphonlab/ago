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
import org.siphonlab.ago.compiler.ParameterizedClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.parser.AgoParser;

/**
 * +T as [Animal to _] got GenericTypeParameter::(Animal, Any, variance=1, G, 0, genericTypeCodeIndex)
 * a box type for classref, like {@link ScopedClassIntervalClassDef}, however it only box classref, accept no scope
 */
public class GenericTypeParameterClassDef extends ClassIntervalClassDef{

    private final Variance variance;
    private final ClassDef templateClass;
    private final int paramIndex;
    private final GenericTypeCode genericTypeCode;

    public GenericTypeParameterClassDef(ClassDef baseClass, ConstructorDef parameterizedConstructor, ClassDef lBound, ClassDef uBound,
                                        Variance variance, ClassDef templateClass, int paramIndex, GenericTypeCode genericTypeCode) {
        super(baseClass, parameterizedConstructor, lBound, uBound);
        this.variance = variance;
        this.templateClass = templateClass;
        this.paramIndex = paramIndex;
        this.genericTypeCode = genericTypeCode;
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
    public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) {
        GenericTypeParameterClassDef c = null;
        try {
            c = this.getParentClass().getOrCreateGenericTypeParameter(baseClass.instantiate(instantiationArguments, returnExisted), constructor, mapArguments(instantiationArguments), returnExisted);
            if(c.getUnit() == null) c.setUnit(this.getUnit());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    @Override
    public ClassDef getTemplateClass() {
        return templateClass;
    }

    public int getParamIndex() {
        return paramIndex;
    }

    public GenericTypeCode getGenericTypeCode() {
        return genericTypeCode;
    }

    public static String composeName(ClassDef baseClass, String typeParamName, int paramIndex, ClassDef templateClass, ClassDef lBound, ClassDef uBound, Variance variance, int genericTypeCodeValue) {
        var root = baseClass.getRoot();
        return "%s_%d_%s|%s".formatted(typeParamName, paramIndex, templateClass.getFullname(),
                ParameterizedClassDef.composeName(baseClass, new Literal[]{
                        lBound.toClassRefLiteral(), uBound.toClassRefLiteral(), root.createByteLiteral(variance.byteValue()),
                            templateClass.toClassRefLiteral(), root.createIntLiteral(paramIndex), root.createIntLiteral(genericTypeCodeValue) }));
    }

}
