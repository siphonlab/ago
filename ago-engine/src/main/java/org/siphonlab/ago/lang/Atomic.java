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
