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

import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;

import java.util.Objects;

/// `originalTemplate + arguments = instantiation class`.
public final class GenericSource {
    private final ClassDef originalTemplate;
    private final InstantiationArguments instantiationArguments;
    private final ClassRefLiteral[] typeArguments;
    private final boolean isTemplatePlaceHolder;

    /**
     *
     */
    public GenericSource(ClassDef originalTemplate, InstantiationArguments instantiationArguments,
                         ClassRefLiteral[] typeArguments, boolean isTemplatePlaceHolder) {
        this.originalTemplate = originalTemplate;
        this.instantiationArguments = instantiationArguments;
        this.typeArguments = typeArguments;
        this.isTemplatePlaceHolder = isTemplatePlaceHolder;
    }

    public GenericSource(ClassDef originalTemplate, InstantiationArguments instantiationArguments,
                         ClassRefLiteral[] typeArguments) {
        this.originalTemplate = originalTemplate;
        this.instantiationArguments = instantiationArguments;
        this.typeArguments = typeArguments;
        this.isTemplatePlaceHolder = false;
    }

    public boolean isPlaceHolderOfTemplate() {
        return this.isTemplatePlaceHolder;
    }

    public ClassDef originalTemplate() {return originalTemplate;}

    public InstantiationArguments instantiationArguments() {return instantiationArguments;}

    public ClassRefLiteral[] typeArguments() {return typeArguments;}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GenericSource) obj;
        return Objects.equals(this.originalTemplate, that.originalTemplate) &&
                Objects.equals(this.instantiationArguments, that.instantiationArguments) &&
                Objects.equals(this.typeArguments, that.typeArguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalTemplate, instantiationArguments, typeArguments);
    }

    @Override
    public String toString() {
        return "GenericSource[" +
                "originalTemplate=" + originalTemplate + ", " +
                "instantiationArguments=" + instantiationArguments + ", " +
                "typeArguments=" + typeArguments + ']';
    }

}
