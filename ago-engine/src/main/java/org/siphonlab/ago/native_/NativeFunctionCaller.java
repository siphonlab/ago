package org.siphonlab.ago.native_;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.Slots;

public abstract class NativeFunctionCaller {

    public abstract void invoke(NativeFrame nativeFrame, Slots slots);

}
