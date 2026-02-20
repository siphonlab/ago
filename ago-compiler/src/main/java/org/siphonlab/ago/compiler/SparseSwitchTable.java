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
import org.siphonlab.ago.compiler.statement.Label;

public class SparseSwitchTable extends SwitchTable{

    private Label defaultEntrance;

    public SparseSwitchTable(FunctionDef functionDef, int id) {
        super(functionDef, id);
    }

    @Override
    public void composeBlob() {
        var buff = IoBuffer.allocate(512).setAutoExpand(true);
        buff.put((byte)2);      // type dense
        buff.putInt(defaultEntrance.getIndex());
        buff.putInt(this.labels.size());
        for (Integer key : this.labels.keySet()) {
            buff.putInt(key);
            buff.putInt(labels.get(key).getResolvedAddress());
        }
        this.composedBlob = buff.flip();
        this.labels.clear();
    }

    public void setDefaultEntrance(Label defaultEntrance) {
        this.defaultEntrance = defaultEntrance;
    }

}
