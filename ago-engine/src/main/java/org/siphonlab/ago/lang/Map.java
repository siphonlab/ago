package org.siphonlab.ago.lang;

import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;

import java.util.HashMap;

public class Map {

    static class MyKeyWrapper {
        private final Instance<?> instance;
        private final CallFrame<?> callFrame;

        public MyKeyWrapper(Instance<?> instance, CallFrame<?> callFrame) {
            this.instance = instance;
            this.callFrame = callFrame;
        }

        @Override
        public int hashCode() {
            return (Integer) instance.invokeMethod(callFrame, instance.getAgoClass().findMethod("hashCode#"));
        }

        @Override
        public boolean equals(Object obj) {
            if(this.instance == obj) return true;
            if(!(obj instanceof MyKeyWrapper)) return false;

            return (Boolean) instance.invokeMethod(callFrame,
                    instance.getAgoClass().findMethod("equals#"), ((MyKeyWrapper) obj).instance);
        }
    }

    public static void Map_create(NativeFrame callFrame) {
        var instance = callFrame.getParentScope();
        ((NativeInstance) instance).setNativePayload(new HashMap<>());
        callFrame.finishVoid();
    }

    public static void Map_put(NativeFrame callFrame, Instance<?> key, Instance<?> value) {
        var instance = callFrame.getParentScope();
        HashMap map = (HashMap) ((NativeInstance) instance).getNativePayload();
        map.put(new MyKeyWrapper(key, callFrame), value);
    }

    public static Instance<?> Map_get(NativeFrame callFrame, Instance<?> key) {
        var instance = callFrame.getParentScope();
        HashMap map = (HashMap) ((NativeInstance) instance).getNativePayload();
        return (Instance<?>) map.get(new MyKeyWrapper(key, callFrame));
    }

}
