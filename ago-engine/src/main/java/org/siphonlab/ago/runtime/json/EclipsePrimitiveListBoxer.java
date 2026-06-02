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
package org.siphonlab.ago.runtime.json;

import org.eclipse.collections.api.*;

import java.util.List;

public class EclipsePrimitiveListBoxer {
    public static List<?> box(PrimitiveIterable iterable) {
        if (iterable == null) {
            throw new IllegalArgumentException("Iterable cannot be null");
        }

        // 利用 Java 17+ 模式匹配，将 8 种原始类型一网打尽
        return switch (iterable) {
            case BooleanIterable it -> it.toList().boxed();
            case ByteIterable it    -> it.toList().boxed();
            case CharIterable it    -> it.toList().boxed();
            case ShortIterable it   -> it.toList().boxed();
            case IntIterable it     -> it.toList().boxed();
            case LongIterable it    -> it.toList().boxed();
            case FloatIterable it   -> it.toList().boxed();
            case DoubleIterable it  -> it.toList().boxed();
            default -> throw new IllegalArgumentException("unknown primitive list: " + iterable.getClass().getName());
        };
    }

}
