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
package org.siphonlab.ago.runtime;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;

import java.util.List;

public abstract class AgoArrayInstance extends Instance<AgoClass> {

    protected final int length;

    public AgoArrayInstance(Slots slots, AgoClass agoClass, int length) {
        super(slots, agoClass);
        this.length = length;
        slots.setInt(0, length);        // length field
    }

    public abstract Object getArray();

    public abstract void fill(List<Object> list);
}
