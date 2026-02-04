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
