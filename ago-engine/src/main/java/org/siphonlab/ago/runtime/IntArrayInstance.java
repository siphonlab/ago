package org.siphonlab.ago.runtime;

import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Slots;

import java.util.List;

public class IntArrayInstance extends AgoArrayInstance{

    public final int[] value;

    public IntArrayInstance(Slots slots, AgoClass agoClass, int length) {
        super(slots, agoClass, length);
        this.value = new int[length];
    }

    public void fillBytes(int count, byte[] blob) {
        IoBuffer buffer = IoBuffer.wrap(blob);
        for (int i = 0; i < count; i++) {
            value[i] = buffer.getInt();
        }
    }

    public Object getArray() {
        return value;
    }

    public void fill(List<Object> list){
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            value[i] = (Integer) o;
        }
    }
}
