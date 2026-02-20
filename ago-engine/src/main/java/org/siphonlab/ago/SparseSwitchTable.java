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

import org.agrona.collections.Int2IntHashMap;

public class SparseSwitchTable extends SwitchTable{
    final Int2IntHashMap map = new Int2IntHashMap(-1);
    final int defaultAddress;
    public SparseSwitchTable(int[] data) {
        defaultAddress = data[0];
        for (int i = 1; i < data.length; ) {
            map.put(data[i++], data[i++]);
        }
    }
    public int resolve(int key){
        return map.getOrDefault(key,defaultAddress);
    }

    public Int2IntHashMap getMap() {
        return map;
    }
}
