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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.IOContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public class AgoJsonFactory extends JsonFactory {

    protected final AgoJsonConfig jsonConfig;

    public AgoJsonFactory(AgoJsonConfig jsonConfig){
        this.jsonConfig = jsonConfig;
    }

    @Override
    protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        return wrapGenerator(super._createGenerator(out, ctxt));
    }

    private JsonGenerator wrapGenerator(JsonGenerator jsonGenerator) {
        return new AgoJsonGenerator(jsonGenerator, jsonConfig);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        return wrapGenerator(super._createUTF8Generator(out, ctxt));
    }

}
