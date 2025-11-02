package org.siphonlab.ago.compiler;

import org.apache.mina.core.buffer.IoBuffer;

public class DenseSwitchTable extends SwitchTable{
    int firstKey;

    public DenseSwitchTable(FunctionDef functionDef, int id){
        super(functionDef, id);
    }

    public void setFirstKey(int firstKey) {
        this.firstKey = firstKey;
    }
    @Override
    public void composeBlob() {
        var buff = IoBuffer.allocate(512).setAutoExpand(true);
        buff.put((byte)1);      // type dense
        buff.putInt(firstKey);
        buff.putInt(this.labels.size());

        int pos = buff.position();
        int index = 0;
        for (Integer key : this.labels.keySet()) {
            int i = key - firstKey;
            while(index < i){       // fill with -1 for gap
                buff.putInt(-1);
                index++;
            }
            buff.putInt(labels.get(key).getResolvedAddress());
            index++;
        }
        assert buff.position() - pos == this.labels.size() * 4;
        this.composedBlob = buff.flip();
        this.labels.clear();
    }
}
