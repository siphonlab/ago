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
package org.siphonlab.ago.compiler;

public enum CompilingStage {
    /**
     * for CompilingStage is used in ClassDef, and when ClassDef created the stage already turn to ParseGenericParams
     * therefore it will never disappear in real use cases
     */
    ParseClassName(0),

    // parse generic param define
    // the inherited children classes can't be referenced at now
    ParseGenericParams(1),
    /**
     * hierarchical classes: superclass, interfaces, traits, permit class
     * expand the symbols to ClassDef,
     * generic intermediate class expanded to GenericInstantiationClassDef, with AllocateSlots stage
     */
    ResolveHierarchicalClasses(2),

    ParseFields(3),     // we need parse fields extract result type and param types for create Function<R> and Function<R, A1,..>

    ValidateHierarchy(4),

    InheritsFields(5),

    ValidateNewFunctions(6),    // functions by parse,

    InheritsInnerClasses(7),   // include methods

    ValidateMembers(8),

    AllocateSlots(9),

    CompileMethodBody(10),

    Compiled(11),
    ;

    private final int value;

    CompilingStage(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static CompilingStage of(int value) {
        switch (value) {
            case 0:
                return ParseClassName;
            case 1:
                return ParseGenericParams;
            case 2:
                return ResolveHierarchicalClasses;
            case 3:
                return ParseFields;
            case 4:
                return ValidateHierarchy;
            case 5:
                return InheritsFields;
            case 6:
                return ValidateNewFunctions;
            case 7:
                return InheritsInnerClasses;
            case 8:
                return ValidateMembers;
            case 9:
                return AllocateSlots;
            case 10:
                return CompileMethodBody;
            case 11:
                return Compiled;
            default:
                throw new IllegalStateException("illegal state %d".formatted(value));
        }
    }

    public CompilingStage nextStage() {
        // 使用更新后的 Compiled.value
        if (this.value < Compiled.value) {
            // of 方法已经更新，所以这里不需要改动
            return CompilingStage.of(this.value + 1);
        } else {
            return Compiled; // Or throw an exception if you prefer
        }
    }

    public CompilingStage prev() {
        return of(this.value - 1);
    }

    public boolean gt(CompilingStage compilingStage) {
        return this.getValue() > compilingStage.getValue();
    }

    public boolean gte(CompilingStage compilingStage) {
        return this.getValue() >= compilingStage.getValue();
    }

    public boolean lt(CompilingStage compilingStage) {
        return this.getValue() < compilingStage.getValue();
    }

    public boolean lte(CompilingStage compilingStage) {
        return this.getValue() <= compilingStage.getValue();
    }

}