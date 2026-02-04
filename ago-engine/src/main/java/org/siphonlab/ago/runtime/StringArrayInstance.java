package org.siphonlab.ago.runtime;

import org.apache.mina.core.buffer.IoBuffer;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Slots;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StringArrayInstance extends AgoArrayInstance{

    public final String[] value;

    public StringArrayInstance(Slots slots, AgoClass agoClass, int length) {
        super(slots, agoClass, length);

        this.value = new String[length];
    }

    public void fillBytes(int count, byte[] blob) {
        IoBuffer buffer = IoBuffer.wrap(blob);
        for (int i = 0; i < count; i++) {
            try {
                value[i] = buffer.getString(count, StandardCharsets.UTF_8.newDecoder());
            } catch (CharacterCodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Object getArray() {
        return value;
    }

    public void fill(List<Object> list) {
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            value[i] = (String) o;
        }
    }
}
