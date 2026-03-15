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

import org.siphonlab.ago.TypeCode;

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
 * and {@link GenericTypeCodeAvatarClassDef} shipped the above typecode as a ClassDef, like PrimitiveClassDef shipped primitive typecode
 * GenericTypeCode is scoped within its template class, therefore we put all scope information within it
 *
 * GenericTypeParameterClassDef shipped class bound and variance, and GenericTypeCode/GenericCodeAvatarClassDef ships `templateClass` and `genericParamIndex`
 * we need ship GenericTypeCode to ClassDef to unify Variable type as ClassDef
 */
public class GenericTypeCode extends TypeCode implements Comparable<GenericTypeCode> {

    private final int genericParamIndex;
    private GenericTypeCodeAvatarClassDef genericTypeCodeAvatarClassDef;

    private final String name;

    public GenericTypeCode(int genericTypeCode, int genericParamIndex, String name, String description) {
        super(genericTypeCode, description);
        this.name = name;
        this.genericParamIndex = genericParamIndex;
    }

    public int getGenericParamIndex() {
        return genericParamIndex;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(GenericTypeCode o) {
        var r = this.genericTypeCodeAvatarClassDef.getTemplateClass().getFullname().compareTo(o.genericTypeCodeAvatarClassDef.getTemplateClass().getFullname());
        if(r != 0) return r;
        return this.value - o.value;
    }

    public void setGenericTypeCodeAvatar(GenericTypeCodeAvatarClassDef genericTypeCodeAvatarClassDef) {
        this.genericTypeCodeAvatarClassDef = genericTypeCodeAvatarClassDef;
    }

    public GenericTypeCodeAvatarClassDef getGenericTypeCodeAvatar() {
        return genericTypeCodeAvatarClassDef;
    }
}
