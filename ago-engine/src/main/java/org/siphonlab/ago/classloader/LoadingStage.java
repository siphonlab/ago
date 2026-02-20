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

public enum LoadingStage {
    LoadClassNames(0),  // load class names, include children(and inherited children), and load concrete type defines, and expand their children

    ResolveHierarchicalClasses(1),  // hierarchical classes: superclass, interfaces, traits, permit class

    ParseFields(2),     // no InheritsInnerClasses, for the inherited child classes already provided in classfile

    InstantiateFunctionFamily(3),

    ParseCode(4),

    BuildClass(5),

    ResolveFunctionIndex(6),

    CollectMethods(7),

    EnqueueParameterizingClassTask(8),

    BuildVariablesAndFunctionBody(9),

    Done(10);

    final int value;

    LoadingStage(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LoadingStage of(int value) {
        return switch (value) {
            case 0 -> LoadClassNames;
            case 1 -> ResolveHierarchicalClasses;
            case 2 -> ParseFields;
            case 3 -> InstantiateFunctionFamily;
            case 4 -> ParseCode;
            case 5 -> BuildClass;
            case 6 -> ResolveFunctionIndex;
            case 7 -> CollectMethods;
            case 8 -> EnqueueParameterizingClassTask;
            case 9 -> BuildVariablesAndFunctionBody;
            case 10 -> Done;
            default -> throw new IllegalStateException("illegal state %d".formatted(value));
        };
    }

    public LoadingStage nextStage() {
        // 使用更新后的 Compiled.value
        if (this.value < Done.value) {
            // of 方法已经更新，所以这里不需要改动
            return LoadingStage.of(this.value + 1);
        } else {
            return Done; // Or throw an exception if you prefer
        }
    }

    public LoadingStage prev() {
        return of(this.value - 1);
    }
}
