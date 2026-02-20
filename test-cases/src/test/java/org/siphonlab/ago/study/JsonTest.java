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
package org.siphonlab.ago.study;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

public class JsonTest {
    static void main() throws IOException {
        String json = "{\"name\":\"Alice\",\"age\":25}";
        SimpleModule module = new SimpleModule();

        module.addDeserializer(Map.class, new JsonDeserializer<Map>() {
            @Override
            public Map deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                assert p.getCurrentToken() == JsonToken.START_OBJECT;
                switch (p.getCurrentToken()){
                    case START_OBJECT:
                        p.nextToken();
                        break;
                }
                return null;
            }
        });

        ObjectMapper mapper = new ObjectMapper(new MappingJsonFactory(){

            @Override
            protected JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException {
                return super._createParser(in, ctxt);
            }

            @Override
            protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
                return super._createParser(r, ctxt);
            }

            @Override
            protected JsonParser _createParser(DataInput input, IOContext ctxt) throws IOException {
                return super._createParser(input, ctxt);
            }

            @Override
            protected JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
                return super._createParser(data, offset, len, ctxt);
            }

            @Override
            protected JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt, boolean recyclable) throws IOException {
                return super._createParser(data, offset, len, ctxt, recyclable);
            }

            JsonParser wrapParser(JsonParser parser, IOContext ioContext){
                return new JsonParser() {

                    @Override
                    public ObjectCodec getCodec() {
                        return null;
                    }

                    @Override
                    public void setCodec(ObjectCodec oc) {

                    }

                    @Override
                    public Version version() {
                        return null;
                    }

                    @Override
                    public void close() throws IOException {
                        parser.close();
                    }

                    @Override
                    public boolean isClosed() {
                        return parser.isClosed();
                    }

                    @Override
                    public JsonStreamContext getParsingContext() {
                        return null;
                    }

                    @Override
                    public JsonLocation currentLocation() {
                        return parser.currentLocation();
                    }

                    @Override
                    public JsonLocation currentTokenLocation() {
                        return parser.currentTokenLocation();
                    }

                    @Override
                    public JsonLocation getCurrentLocation() {
                        return null;
                    }

                    @Override
                    public JsonLocation getTokenLocation() {
                        return null;
                    }

                    @Override
                    public JsonToken nextToken() throws IOException {
                        return parser.nextToken();
                    }

                    @Override
                    public JsonToken nextValue() throws IOException {
                        return null;
                    }

                    @Override
                    public JsonParser skipChildren() throws IOException {
                        return null;
                    }

                    @Override
                    public JsonToken getCurrentToken() {
                        return null;
                    }

                    @Override
                    public int getCurrentTokenId() {
                        return 0;
                    }

                    @Override
                    public boolean hasCurrentToken() {
                        return false;
                    }

                    @Override
                    public boolean hasTokenId(int id) {
                        return false;
                    }

                    @Override
                    public boolean hasToken(JsonToken t) {
                        return false;
                    }

                    @Override
                    public void clearCurrentToken() {

                    }

                    @Override
                    public JsonToken getLastClearedToken() {
                        return null;
                    }

                    @Override
                    public void overrideCurrentName(String name) {

                    }

                    @Override
                    public String getCurrentName() throws IOException {
                        return "";
                    }

                    @Override
                    public String getText() throws IOException {
                        return parser.getText();
                    }

                    @Override
                    public char[] getTextCharacters() throws IOException {
                        return parser.getTextCharacters();
                    }

                    @Override
                    public int getTextLength() throws IOException {
                        return parser.getTextLength();
                    }

                    @Override
                    public int getTextOffset() throws IOException {
                        return parser.getTextOffset();
                    }

                    @Override
                    public boolean hasTextCharacters() {
                        return false;
                    }

                    @Override
                    public Number getNumberValue() throws IOException {
                        return null;
                    }

                    @Override
                    public NumberType getNumberType() throws IOException {
                        return null;
                    }

                    @Override
                    public int getIntValue() throws IOException {
                        return 0;
                    }

                    @Override
                    public long getLongValue() throws IOException {
                        return 0;
                    }

                    @Override
                    public BigInteger getBigIntegerValue() throws IOException {
                        return null;
                    }

                    @Override
                    public float getFloatValue() throws IOException {
                        return 0;
                    }

                    @Override
                    public double getDoubleValue() throws IOException {
                        return 0;
                    }

                    @Override
                    public BigDecimal getDecimalValue() throws IOException {
                        return null;
                    }

                    @Override
                    public byte[] getBinaryValue(Base64Variant bv) throws IOException {
                        return new byte[0];
                    }

                    @Override
                    public String getValueAsString(String def) throws IOException {
                        return "";
                    }

                };
            }

        });
        mapper.registerModule(module);

        Map map = mapper.readValue(new StringReader('[' + json + ']'), Map.class);

    }

    static class MyTypeReference<T> extends TypeReference<T>{

    }


}
