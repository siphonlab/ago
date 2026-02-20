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

import java.util.TreeMap;

public abstract class SwitchTable {
    private final FunctionDef functionDef;
    protected final int id;
    protected TreeMap<Integer, Label> labels = new TreeMap<>();

    IoBuffer composedBlob;

    public SwitchTable(FunctionDef functionDef, int id) {
        this.functionDef = functionDef;
        this.id = id;
    }

    public void addLabel(int key, Label label){
        labels.put(key,label);
    }

    public int getId() {
        return id;
    }

    public abstract void composeBlob();

    public IoBuffer getComposedBlob() {
        return composedBlob;
    }
}
