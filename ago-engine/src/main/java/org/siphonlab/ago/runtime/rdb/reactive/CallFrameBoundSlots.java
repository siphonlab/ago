package org.siphonlab.ago.runtime.rdb.reactive;

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Slots;

public interface CallFrameBoundSlots<T extends Slots> extends Slots{


    public CallFrame<?> getCallFrame();

}
