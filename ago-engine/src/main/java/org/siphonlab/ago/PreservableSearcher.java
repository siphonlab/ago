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
package org.siphonlab.ago;

import java.util.*;
import java.util.function.Predicate;

public class PreservableSearcher<E> {

    private List<E> list;
    private int pos = 0;

    public PreservableSearcher(List<E> list) {
        this.list = list;
    }

    public E search(Predicate<? super E> predicate) {
        int size = this.list.size();
        if (size == 0) {
            return null;
        }

        int startPos = pos;

        for (int i = pos; i < size; i++) {
            E e = this.list.get(i);
            if (predicate.test(e)) {
                pos = i;
                return e;
            }
        }

        // from 0 → startPos-1
        if (startPos > 0) {
            for (int i = 0; i < startPos; i++) {
                E e = this.list.get(i);
                if (predicate.test(e)) {
                    pos = i;
                    return e;
                }
            }
        }

        return null;
    }

}
