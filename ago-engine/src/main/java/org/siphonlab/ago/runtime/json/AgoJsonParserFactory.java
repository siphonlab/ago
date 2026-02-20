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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.CallFrame;

import java.io.*;

/**
 * mapper.copyWith(new AgoJsonParserFactory(initialClass)).readValue()
 */
public class AgoJsonParserFactory extends AgoJsonFactory {

    private final AgoClass initialClass;
    private final CallFrame<?> callFrame;

    public AgoJsonParserFactory(AgoJsonConfig config, AgoClass initialClass, CallFrame<?> callFrame){
        super(config);
        this.initialClass = initialClass;
        this.callFrame = callFrame;
    }

    public AgoJsonParserFactory(AgoJsonConfig config) {
        super(config);
        this.initialClass = null;
        this.callFrame = null;
    }

    @Override
    protected JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt, boolean recyclable) throws IOException {
        return wrapJsonParser(super._createParser(data, offset, len, ctxt, recyclable));
    }

    private JsonParser wrapJsonParser(JsonParser jsonParser) {
        if(jsonParser == null) return null;
        return new AgoJsonParser(jsonParser,initialClass, callFrame, jsonConfig.isWriteSlots());
    }

    @Override
    protected JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        return wrapJsonParser(super._createParser(data, offset, len, ctxt));
    }

    @Override
    protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return wrapJsonParser(super._createParser(r, ctxt));
    }

    @Override
    protected JsonParser _createParser(DataInput input, IOContext ctxt) throws IOException {
        return wrapJsonParser(super._createParser(input, ctxt));
    }

    @Override
    protected JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        return wrapJsonParser(super._createParser(in, ctxt));
    }

    @Override
    protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        return super._createGenerator(out, ctxt);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        return super._createUTF8Generator(out, ctxt);
    }

}
