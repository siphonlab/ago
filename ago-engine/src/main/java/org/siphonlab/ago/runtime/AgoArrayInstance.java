package org.siphonlab.ago.runtime;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;

import java.util.List;

public abstract class AgoArrayInstance extends Instance<AgoClass> {

    protected final int length;

    public AgoArrayInstance(Slots slots, AgoClass agoClass, int length) {
        super(slots, agoClass);
        this.length = length;
        slots.setInt(0, length);        // length field
    }

    public abstract Object getArray();

    public abstract void fill(List<Object> list);
}
