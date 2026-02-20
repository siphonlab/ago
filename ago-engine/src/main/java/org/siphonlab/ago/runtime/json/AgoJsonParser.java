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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.CallFrame;

public class AgoJsonParser extends JsonParserDelegate {
    protected final JsonParser parser;
    protected final AgoClass initialClass;
    private final CallFrame<?> callFrame;
    private final boolean serializeSlots;

    public AgoJsonParser(JsonParser parser, AgoClass initialClass, CallFrame<?> callFrame, boolean serializeSlots) {
        super(parser);
        this.parser = parser;
        this.initialClass = initialClass;
        this.callFrame = callFrame;
        this.serializeSlots = serializeSlots;
    }

    public boolean isSerializeSlots() {
        return serializeSlots;
    }

    public JsonParser getParser() {
        return parser;
    }

    public AgoClass getInitialClass() {
        return initialClass;
    }

    public CallFrame<?> getCallFrame() {
        return callFrame;
    }
}
