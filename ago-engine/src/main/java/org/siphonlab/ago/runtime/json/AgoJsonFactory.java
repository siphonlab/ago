package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.IOContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public class AgoJsonFactory extends JsonFactory {

    private final boolean writeType;
    private final boolean writeId;
    private final boolean serializeObjectAsReference;

    public AgoJsonFactory(boolean writeType, boolean writeId, boolean serializeObjectAsReference){
        this.writeType = writeType;
        this.writeId = writeId;
        this.serializeObjectAsReference = serializeObjectAsReference;
    }

    @Override
    protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        return wrapGenerator(super._createGenerator(out, ctxt));
    }

    private JsonGenerator wrapGenerator(JsonGenerator jsonGenerator) {
        return new AgoJsonGenerator(jsonGenerator,writeType,writeId, serializeObjectAsReference);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        return wrapGenerator(super._createUTF8Generator(out, ctxt));
    }

}
