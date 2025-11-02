package org.siphonlab.ago.native_;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;

public class NativeInstance extends Instance<AgoClass> {

    protected Object nativePayload;

    public NativeInstance(Slots slots, AgoClass agoClass) {
        super(slots, agoClass);
    }

    public NativeInstance(AgoClass agoClass) {
        this(agoClass.createSlots(), agoClass);
    }

    public Object getNativePayload() {
        return nativePayload;
    }

    public void setNativePayload(Object nativePayload) {
        this.nativePayload = nativePayload;
    }
}
