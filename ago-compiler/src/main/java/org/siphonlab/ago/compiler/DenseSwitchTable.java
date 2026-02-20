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

import org.apache.mina.core.buffer.IoBuffer;

public class DenseSwitchTable extends SwitchTable{
    int firstKey;

    public DenseSwitchTable(FunctionDef functionDef, int id){
        super(functionDef, id);
    }

    public void setFirstKey(int firstKey) {
        this.firstKey = firstKey;
    }
    @Override
    public void composeBlob() {
        var buff = IoBuffer.allocate(512).setAutoExpand(true);
        buff.put((byte)1);      // type dense
        buff.putInt(firstKey);
        buff.putInt(this.labels.size());

        int pos = buff.position();
        int index = 0;
        for (Integer key : this.labels.keySet()) {
            int i = key - firstKey;
            while(index < i){       // fill with -1 for gap
                buff.putInt(-1);
                index++;
            }
            buff.putInt(labels.get(key).getResolvedAddress());
            index++;
        }
        assert buff.position() - pos == this.labels.size() * 4;
        this.composedBlob = buff.flip();
        this.labels.clear();
    }
}
