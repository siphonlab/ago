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
package org.siphonlab.ago.runtime.vertx;

import java.nio.charset.StandardCharsets;

import org.siphonlab.ago.native_.NativeFrame;

public class NioCharset {

    public static void create(NativeFrame frame, String name){
        frame.getParentScope().setNativePayload(java.nio.charset.Charset.forName(name));
        frame.finishVoid();
    }

    public static void toString(NativeFrame frame){
        java.nio.charset.Charset cs = (java.nio.charset.Charset) frame.getParentScope().getNativePayload();
        frame.finishString(cs.name());
    }

    public static void UTF_8(NativeFrame frame){
        finishCharset(frame, StandardCharsets.UTF_8);
    }

    public static void UTF_16(NativeFrame frame){
        finishCharset(frame, StandardCharsets.UTF_16);
    }

    public static void ISO_8859_1(NativeFrame frame){
        finishCharset(frame, StandardCharsets.ISO_8859_1);
    }

    private static void finishCharset(NativeFrame frame, java.nio.charset.Charset cs){
        var inst = frame.getAgoEngine().createNativeInstance(
                null, frame.getAgoEngine().getClass("io.Charset"), frame.getRunSpace());
        inst.setNativePayload(cs);
        frame.finishObject(inst);
    }

}
