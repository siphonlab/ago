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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TryCatchTable {

    private final FunctionDef functionDef;

    private List<Item> items = new ArrayList<>();

    public TryCatchTable(FunctionDef functionDef) {
        this.functionDef = functionDef;
    }

    public void register(int begin, int end, int handler, List<ClassDef> exceptionTypes) {
        this.items.add(new Item(begin, end, handler, exceptionTypes));
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    record Item(int begin, int end, int handler, List<ClassDef> exceptionTypes){

    }

    public IoBuffer createBlob(){
        IoBuffer buffer = IoBuffer.allocate(512);
        items.sort(new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                return o2.begin - o1.begin;     // inner at head
            }
        });

        for (Item item : items) {
            buffer.putInt(item.begin).putInt(item.end).putInt(item.handler);
            buffer.putInt(item.exceptionTypes.size());
            for (ClassDef exceptionType : item.exceptionTypes) {
                buffer.putInt(functionDef.idOfKnownClass(exceptionType));
            }
        }
        return buffer.flip();
    }

}
