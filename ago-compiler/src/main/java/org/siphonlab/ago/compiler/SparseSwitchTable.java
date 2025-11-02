package org.siphonlab.ago.compiler;

import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.compiler.statement.Label;

public class SparseSwitchTable extends SwitchTable{

    private Label defaultEntrance;

    public SparseSwitchTable(FunctionDef functionDef, int id) {
        super(functionDef, id);
    }

    @Override
    public void composeBlob() {
        var buff = IoBuffer.allocate(512).setAutoExpand(true);
        buff.put((byte)2);      // type dense
        buff.putInt(defaultEntrance.getIndex());
        buff.putInt(this.labels.size());
        for (Integer key : this.labels.keySet()) {
            buff.putInt(key);
            buff.putInt(labels.get(key).getResolvedAddress());
        }
        this.composedBlob = buff.flip();
        this.labels.clear();
    }

    public void setDefaultEntrance(Label defaultEntrance) {
        this.defaultEntrance = defaultEntrance;
    }

}
