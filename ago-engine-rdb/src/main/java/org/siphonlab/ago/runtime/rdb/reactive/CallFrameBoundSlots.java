package org.siphonlab.ago.runtime.rdb.reactive;

import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Slots;

/**
 * defer that the Slots need bound callframe
 * @param <T>
 */
public interface CallFrameBoundSlots<T extends Slots> extends Slots{


    public CallFrame<?> getCallFrame();

}
