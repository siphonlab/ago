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
import org.apache.commons.lang3.mutable.MutableInt;
import org.siphonlab.ago.compiler.ClassContainer;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.ConcreteType;
import org.siphonlab.ago.compiler.ParameterizedClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;

import java.util.Set;

import static org.siphonlab.ago.compiler.generic.ClassIntervalClassDef.composeNameOfClassInClassInterval;

public class GenericTypeCodeAvatarClassDef extends ParameterizedClassDef  implements Comparable<GenericTypeCodeAvatarClassDef>, ClassBound {

    private final SharedGenericTypeParameterClassDef sharedGenericTypeParameterClassDef;
    private final ClassDef templateClass;
    private final int paramIndex;
    private final GenericTypeCode typeCode;

    public GenericTypeCodeAvatarClassDef(ClassDef langGenericTypeAvatar, Literal<?>[] arguments) {
        super(langGenericTypeAvatar, langGenericTypeAvatar.getMetaClassDef().getConstructor(), arguments);
        sharedGenericTypeParameterClassDef = (SharedGenericTypeParameterClassDef) ((ClassRefLiteral)arguments[0]).getClassDefValue();
        templateClass = ((ClassRefLiteral)arguments[1]).getClassDefValue();
        paramIndex = ((IntLiteral)arguments[2]).value;
        String paramName = ((StringLiteral)arguments[4]).getString();
        typeCode = new GenericTypeCode(((IntLiteral)arguments[3]).value, paramIndex, paramName,  paramName + "_" + paramIndex + "_" + templateClass.getFullname());
        this.typeCode.setGenericTypeCodeAvatar(this);
        this.name = composeName(sharedGenericTypeParameterClassDef, templateClass, paramName, paramIndex, typeCode.value);
        this.registerConcreteType((ConcreteType) sharedGenericTypeParameterClassDef);
    }

    public GenericTypeCodeAvatarClassDef(ClassDef langGenericTypeAvatar,
                                         SharedGenericTypeParameterClassDef sharedGenericTypeParameterClassDef,
                                         ClassDef templateClass,
                                         int paramIndex,
                                         int typeCode,
                                         String paramName
                                     ) {
        super(langGenericTypeAvatar, langGenericTypeAvatar.getMetaClassDef().getConstructor(),
                    new Literal[]{sharedGenericTypeParameterClassDef.toClassRefLiteral(),
                                templateClass.toClassRefLiteral(),
                                langGenericTypeAvatar.getRoot().createIntLiteral(paramIndex),
                                langGenericTypeAvatar.getRoot().createIntLiteral(typeCode),
                                langGenericTypeAvatar.getRoot().createStringLiteral(paramName)
                    });
        this.sharedGenericTypeParameterClassDef = sharedGenericTypeParameterClassDef;
        this.templateClass = templateClass;
        this.paramIndex = paramIndex;
        this.typeCode = new GenericTypeCode(typeCode, paramIndex, paramName,  paramName + "_" + paramIndex + "_" + templateClass.getFullname());
        this.typeCode.setGenericTypeCodeAvatar(this);
        this.name = composeName(sharedGenericTypeParameterClassDef, templateClass, paramName, paramIndex, typeCode);
        this.registerConcreteType((ConcreteType) sharedGenericTypeParameterClassDef);
    }

    @Override
    public GenericTypeCode getTypeCode() {
        return this.typeCode;
    }

    @Override
    public boolean isAffectedByTypeArguments(InstantiationArguments instantiationArguments, Set<ClassDef> visited) {
        if(instantiationArguments.typeMapping.containsKey(this)){
            return true;
        }
        return this.sharedGenericTypeParameterClassDef.isAffectedByTypeArguments(instantiationArguments, visited);
    }

    @Override
    public ClassDef instantiateAsReferenceClass(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        return this.instantiate(arguments, returnExisted);
    }

    @Override
    public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) throws CompilationError {
        if(!this.isAffectedByTypeArguments(arguments)) {
            if(returnExisted != null) returnExisted.setTrue();
            return this;
        }
        if(arguments.typeMapping.containsKey(this)){
            return arguments.typeMapping.get(this);
        }
        var r = (SharedGenericTypeParameterClassDef) this.sharedGenericTypeParameterClassDef.instantiate(arguments, returnExisted);
        if(r == this.sharedGenericTypeParameterClassDef) return this;
        return ((ClassContainer)this.baseClass.getParent()).getOrCreateGenericTypeAvatarClassDef(this.baseClass,
                r, this.templateClass, this.paramIndex, this.typeCode.value, this.typeCode.getName(), returnExisted);
    }

    public static String composeName(SharedGenericTypeParameterClassDef sharedGenericTypeParameterClassDef, ClassDef templateClass, String typeParamName, int paramIndex, int genericTypeCodeValue) {
        return "%s_%d_%s_%d|%s".formatted(typeParamName, paramIndex, composeNameOfClassInClassInterval(templateClass), genericTypeCodeValue, composeNameOfClassInClassInterval(sharedGenericTypeParameterClassDef));
    }

    public SharedGenericTypeParameterClassDef getSharedGenericTypeParameterClassDef() {
        return sharedGenericTypeParameterClassDef;
    }

    @Override
    public ClassDef getTemplateClass() {
        return templateClass;
    }

    public int getParamIndex() {
        return paramIndex;
    }

    @Override
    public int compareTo(GenericTypeCodeAvatarClassDef o) {
        var i = this.getTemplateClass().getFullname().compareTo(o.getTemplateClass().getFullname());
        if(i == 0){
            return this.paramIndex - o.paramIndex;
        }
        return i;
    }

    public ClassDef getLBoundClass() {
        return sharedGenericTypeParameterClassDef.getLBoundClass();
    }

    public ClassDef getUBoundClass() {
        return sharedGenericTypeParameterClassDef.getUBoundClass();
    }

    @Override
    public boolean isGenericTerminated(Set<ClassDef> visited) {
        return false;
    }

    @Override
    public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited, MutableInt depth) {
        return this.getSharedGenericTypeParameterClassDef().asThatOrSuperOfThat(anotherClass, visited, depth);
    }

    @Override
    public boolean isPrimitiveFamily() {
        //TODO it's strange, should determine with asThatOrSuperOfThat directly
        return this.getLBoundClass().isPrimitiveFamily();
    }

    @Override
    public boolean isPrimitiveNumberFamily() {
        return this.getLBoundClass().isPrimitiveNumberFamily();
    }

}
