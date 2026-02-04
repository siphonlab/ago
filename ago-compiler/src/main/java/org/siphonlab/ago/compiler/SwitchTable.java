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
