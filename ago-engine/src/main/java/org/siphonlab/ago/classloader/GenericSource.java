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
package org.siphonlab.ago.classloader;

import java.util.Objects;

public final class GenericSource {
    private final String sourceTemplate;
    private final InstantiationArguments instantiationArguments;
    private final ClassRefValue[] typeArguments;

    public GenericSource(String sourceTemplate, InstantiationArguments instantiationArguments, ClassRefValue[] typeArguments) {
        this.sourceTemplate = sourceTemplate;
        this.instantiationArguments = instantiationArguments;
        this.typeArguments = typeArguments;
    }

    public String sourceTemplate() {return sourceTemplate;}

    public InstantiationArguments instantiationArguments() {return instantiationArguments;}

    public ClassRefValue[] typeArguments() {return typeArguments;}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GenericSource) obj;
        return Objects.equals(this.sourceTemplate, that.sourceTemplate) &&
                Objects.equals(this.instantiationArguments, that.instantiationArguments) &&
                Objects.equals(this.typeArguments, that.typeArguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceTemplate, instantiationArguments, typeArguments);
    }

    @Override
    public String toString() {
        return "GenericSource[" +
                "sourceTemplate=" + sourceTemplate + ", " +
                "instantiationArguments=" + instantiationArguments + ", " +
                "typeArguments=" + typeArguments + ']';
    }

}
