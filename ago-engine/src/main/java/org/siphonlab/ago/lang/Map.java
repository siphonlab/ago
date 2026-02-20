/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
