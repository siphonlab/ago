package org.siphonlab.ago.lang;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;

import java.util.concurrent.atomic.AtomicBoolean;

public class Atomic {
    public static void AtomicBoolean_create(NativeFrame nativeFrame, boolean initialValue) {
        NativeInstance instance = (NativeInstance) nativeFrame.getParentScope();
        instance.setNativePayload(new AtomicBoolean(initialValue));
        nativeFrame.finishVoid();
    }

    public static void AtomicBoolean_compareAndSet(NativeFrame nativeFrame, boolean expectedValue, boolean newValue) {
        NativeInstance instance = (NativeInstance) nativeFrame.getParentScope();
        AtomicBoolean atomicBoolean = (AtomicBoolean) instance.getNativePayload();
        nativeFrame.finishBoolean(atomicBoolean.compareAndSet(expectedValue, newValue));
    }

}
