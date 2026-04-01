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

import org.siphonlab.ago.SourceLocation;

import java.util.Objects;

public class VariableDesc {

    enum VariableKind {Variable, Field, Parameter}

    public VariableDesc(String name, int modifiers, VariableKind variableKind, String type,
                        int slotIndex, Object constLiteralValue, SourceLocation sourceLocation, AgoClassLoader agoClassLoader) {
        this.name = name;
        this.modifiers = modifiers;
        this.variableKind = variableKind;
        this.slotIndex = slotIndex;
        this.constLiteralValue = constLiteralValue;
        this.sourceLocation = sourceLocation;
        this.type = type;
        this.agoClassLoader = agoClassLoader;
    }

    final int modifiers;
    VariableKind variableKind;
    String name;
    String type;
    private final AgoClassLoader agoClassLoader;
    int slotIndex;
    final Object constLiteralValue;

    private SourceLocation sourceLocation;

    public int getSlotIndex() {
        return slotIndex;
    }

    VariableDesc applyTemplate(InstantiationArguments typeArguments) {
        var t = agoClassLoader.instantiateReferenceClass(this.type, typeArguments);
        return new VariableDesc(name, modifiers, variableKind, t.fullname(), slotIndex, constLiteralValue, sourceLocation,  agoClassLoader);
    }

    public ClassHeader getType() {
        return Objects.requireNonNull(this.agoClassLoader.getClassHeader(this.type));
    }

    public void setSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }
}
