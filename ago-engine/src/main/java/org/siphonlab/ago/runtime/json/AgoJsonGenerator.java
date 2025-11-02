package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class AgoJsonGenerator extends JsonGeneratorDelegate {

    private boolean writeType;          // the 1st tier is always true, applied on the inner members
    private boolean writeId;

    private boolean serializeObjectAsReference;         // the 1st tier is always false, and then, inner members use this configuration

    private Deque<Boolean> writeTypeStack = new ArrayDeque<>();
    private int depth = 0;

    public AgoJsonGenerator(JsonGenerator delegate, boolean writeType, boolean writeId, boolean serializeObjectAsReference) {
        super(delegate, false);
        this.writeType = writeType;
        this.writeId = writeId;
        this.serializeObjectAsReference = serializeObjectAsReference;
    }

    public void setWriteType(boolean writeType) {
        this.writeType = writeType;
    }

    public void setWriteId(boolean writeId) {
        this.writeId = writeId;
    }

    public boolean isWriteType() {
        return writeType;
    }

    public boolean isWriteId() {
        return writeId;
    }

    public boolean isSerializeObjectAsReference() {
        return serializeObjectAsReference;
    }

    public void setSerializeObjectAsReference(boolean serializeObjectAsReference) {
        this.serializeObjectAsReference = serializeObjectAsReference;
    }

    public boolean currSerializeObjectAsReference(){
        return serializeObjectAsReference && depth > 0;
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
