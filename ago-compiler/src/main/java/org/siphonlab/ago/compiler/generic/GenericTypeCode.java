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
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.CompilingStage;
import org.siphonlab.ago.compiler.Root;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.Set;

/**
 * {@link GenericTypeCode} separates the TypeCode(value >= GENERIC_TYPE_START, and combine its GenericTypeParameterClassDef
 * i.e.
 * ```
 * class G<T as [Animal to _], T2 as [Animal to_]>{}
 * class G2<T as [Animal to _]>{}
 * ```
 * there are 3 `[Animal to _]`, and they are exactly same one `GenericTypeParameterClassDef`, with different GenericTypeCode, they are
 * `class G<T typecode is 0x10, T2 type code is 0x11>`, `class G2<T typecode is 0x00>`
 * the typecode are use for mark types for fields and slots, and other cases which depends on `T`
 *
 * and {@link GenericCodeAvatarClassDef} shipped the above typecode as a ClassDef, like PrimitiveClassDef shipped primitive typecode
 * GenericTypeCode is scoped within its template class, therefore we put all scope information within it
 */
public class GenericTypeCode extends TypeCode implements Comparable<GenericTypeCode> {

    private final int genericParamIndex;
    private final SharedGenericTypeParameterClassDef sharedGenericTypeParameterClassDef;
    private final ClassDef templateClass;
    private final AgoParser.GenericTypeParameterContext genericTypeParameterContext;

    private final GenericCodeAvatarClassDef genericCodeAvatarClassDef;
    private final String name;

    protected GenericTypeCode(int genericTypeCode, int genericParamIndex, String name, String description,
                              SharedGenericTypeParameterClassDef sharedGenericTypeParameterClassDef,
                              ClassDef templateClass, AgoParser.GenericTypeParameterContext genericTypeParameterContext) {
        super(genericTypeCode, description);
        this.genericParamIndex = genericParamIndex;
        this.sharedGenericTypeParameterClassDef = sharedGenericTypeParameterClassDef;
        this.genericCodeAvatarClassDef = new GenericCodeAvatarClassDef(this);
        this.templateClass = templateClass;
        this.genericTypeParameterContext = genericTypeParameterContext;
        this.name = name;
    }

    public static GenericTypeCode createGeneric(int genericTypeCode, int genericParamIndex, String name, SharedGenericTypeParameterClassDef sharedGenericTypeParameterClassDef, AgoParser.GenericTypeParameterContext genericTypeParameterContext, ClassDef templateClass){
        return new GenericTypeCode(genericTypeCode, genericParamIndex, name, name + "_" + genericParamIndex + "_" + templateClass.getFullname(),
                sharedGenericTypeParameterClassDef, templateClass, genericTypeParameterContext);
    }

    public SharedGenericTypeParameterClassDef getGenericTypeParameterClassDef() {
        return sharedGenericTypeParameterClassDef;
    }

    public int getGenericParamIndex() {
        return genericParamIndex;
    }

    public String getName() {
        return name;
    }

    public ClassDef getTemplateClass() {
        return templateClass;
    }

    public AgoParser.GenericTypeParameterContext getGenericTypeParameterContext() {
        return genericTypeParameterContext;
    }

    @Override
    public int compareTo(GenericTypeCode o) {
        var r = this.templateClass.getFullname().compareTo(o.templateClass.getFullname());
        if(r != 0) return r;
        return this.value - o.value;
    }

    public static class GenericCodeAvatarClassDef extends ClassDef implements ClassBound{

        private final GenericTypeCode genericTypeCode;

        public GenericCodeAvatarClassDef(GenericTypeCode genericTypeCode) {
            super(genericTypeCode.sharedGenericTypeParameterClassDef.getFullname());    // this class is just for placeholder, has no package
            this.genericTypeCode = genericTypeCode;
            setCompilingStage(CompilingStage.Compiled);
        }

        @Override
        public boolean isThatOrSuperOfThat(ClassDef anotherClass) {
            return genericTypeCode.sharedGenericTypeParameterClassDef.isThatOrSuperOfThat(anotherClass);
        }

        @Override
        public ClassDef asThatOrSuperOfThat(ClassDef anotherClass, Set<ClassDef> visited) {
            return genericTypeCode.sharedGenericTypeParameterClassDef.asThatOrSuperOfThat(anotherClass, visited);
        }

        @Override
        public GenericTypeCode getTypeCode() {
            return genericTypeCode;
        }

        @Override
        public ClassDef getLBoundClass() {
            return genericTypeCode.sharedGenericTypeParameterClassDef.getLBoundClass();
        }

        @Override
        public ClassDef getUBoundClass() {
            return genericTypeCode.sharedGenericTypeParameterClassDef.getUBoundClass();
        }

        @Override
        public String toString() {
            if(this.compilingStage != CompilingStage.Compiled){
                return "(GenericCodeAvatar %s %s %s)".formatted(this.getFullname(), this.genericTypeCode, this.compilingStage);
            }
            return "(GenericCodeAvatar %s %s)".formatted(this.getFullname(), this.genericTypeCode);
        }

        @Override
        public boolean isAffectedByTemplate(InstantiationArguments instantiationArguments) {
            return instantiationArguments.mapType(this.genericTypeCode) != this;
        }

        @Override
        public ClassDef instantiate(InstantiationArguments arguments, MutableBoolean returnExisted) {
            if(returnExisted != null) returnExisted.setTrue();
            return arguments.mapType(this.genericTypeCode);
        }

        @Override
        public ClassDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) {
            return instantiationArguments.mapType(this.genericTypeCode);
        }

        @Override
        public boolean isGenericInstantiateRequiredForNew() {
            return false;
        }

        @Override
        public boolean isPrimitiveFamily() {
            return this.getLBoundClass().isPrimitiveFamily();
        }

        @Override
        public boolean isPrimitiveNumberFamily() {
            return this.getLBoundClass().isPrimitiveNumberFamily();
        }

        @Override
        public Root getRoot() {
            return genericTypeCode.sharedGenericTypeParameterClassDef.getRoot();
        }
    }

    public GenericCodeAvatarClassDef getGenericCodeAvatarClassDef() {
        return genericCodeAvatarClassDef;
    }
}
