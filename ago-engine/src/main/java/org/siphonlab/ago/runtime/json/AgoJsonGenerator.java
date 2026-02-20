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
package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class AgoJsonGenerator extends JsonGeneratorDelegate {

    private final AgoJsonConfig config;

    private boolean writeType;          // the 1st tier is always true, applied on the inner members

    private Deque<Boolean> writeTypeStack = new ArrayDeque<>();
    private int depth = 0;

    public AgoJsonGenerator(JsonGenerator delegate, AgoJsonConfig config) {
        super(delegate, false);
        this.config = config;
        if(config.getWriteType() == AgoJsonConfig.WriteTypeMode.Always){
            this.writeType = true;
        }
    }

    public void setWriteType(boolean writeType) {
        this.writeType = writeType;
    }

    public boolean isWriteType() {
        return writeType;
    }

    public AgoJsonConfig getConfig() {
        return config;
    }

    public boolean currWriteObjectAsReference(){
        return switch (config.getWriteObjectAsReference()) {
            case Always -> true;
            case Inner -> depth > 0;
            case null, default -> false;
        };
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public void writeStartObject() throws IOException {
        writeTypeStack.push(writeType);
        depth ++;
        super.writeStartObject();
    }

    @Override
    public void writeEndObject() throws IOException {
        super.writeEndObject();
        writeType = writeTypeStack.pop();
        depth --;
    }

    @Override
    public void writeStartArray() throws IOException {
        super.writeStartArray();
        depth++;
    }

    @Override
    public void writeEndArray() throws IOException {
        super.writeEndArray();
        depth--;
    }

}
