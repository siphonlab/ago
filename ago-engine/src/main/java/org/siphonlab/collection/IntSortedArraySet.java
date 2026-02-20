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
package org.siphonlab.collection;

import java.util.Arrays;

public class IntSortedArraySet {
    private int[] elements;
    private int size;

    public IntSortedArraySet() {
        elements = new int[16];
        size = 0;
    }

    public boolean add(int value) {
        int index = Arrays.binarySearch(elements, 0, size, value);
        if (index >= 0) {
            return false;
        }
        index = -index - 1;
        if (size == elements.length) {
            elements = Arrays.copyOf(elements, elements.length * 2);
        }
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = value;
        size++;
        return true;
    }

    // 检查元素是否存在
    public boolean contains(int value) {
        return Arrays.binarySearch(elements, 0, size, value) >= 0;
    }

    public int size() {
        return size;
    }
}