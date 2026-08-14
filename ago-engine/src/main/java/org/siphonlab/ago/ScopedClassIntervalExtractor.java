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

public class ScopedClassIntervalExtractor {
    public record Result(int classId, AgoClass agoClass, Instance<?> scope){}

    public static Result extract(Instance<?> scopedClassInterval){
        Slots slots = scopedClassInterval.getSlots();
        return new Result(slots.getClassRef(0), (AgoClass) slots.getObject(1), slots.getObject(2));
    }
}
