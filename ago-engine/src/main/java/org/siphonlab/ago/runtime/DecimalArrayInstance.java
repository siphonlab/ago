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

import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.AgoFrame;
import org.siphonlab.ago.Slots;

import java.math.BigDecimal;
import java.util.List;

public class DecimalArrayInstance extends AgoArrayInstance{

    public final BigDecimal[] value;

    public DecimalArrayInstance(Slots slots, AgoClass agoClass, int length) {
        super(slots, agoClass, length);
        this.value = new BigDecimal[length];
    }

    public void fillBytes(int count, byte[] blob, AgoEngine engine) {
        var buffer = IoBuffer.wrap(blob);
        for (int i = 0; i < count; i++) {
            int blobIndex = buffer.getInt();
            value[i] = engine.toDecimal(blobIndex);
        }
    }

    public Object getArray() {
        return value;
    }

    public void fill(List<Object> list) {
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            value[i] = (BigDecimal) o;
        }
    }
}
