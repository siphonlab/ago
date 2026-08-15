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

import io.vertx.core.buffer.Buffer;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class RequestBody {

    private static io.vertx.ext.web.RequestBody get(NativeFrame frame){
        return (io.vertx.ext.web.RequestBody) frame.getParentScope().getNativePayload();
    }

    // ========================================================================
    // buffer — returns the body as Buffer (synchronous in Vert.x 5)
    // ========================================================================

    public static void buffer_get(NativeFrame frame){
        Buffer buf = get(frame).buffer();
        Instance<?> inst = ClassMapping.mapObject(buf, frame);
        frame.finishObject(inst);
    }

    // ========================================================================
    // asString — body as String (synchronous)
    // ========================================================================

    public static void asString(NativeFrame frame){
        frame.finishUnion(get(frame).asString());
    }

    // ========================================================================
    // length — content-length in bytes
    // ========================================================================

    public static void length_get(NativeFrame frame){
        frame.finishInt(get(frame).length());
    }

    // ========================================================================
    // isEmpty — whether body is empty
    // ========================================================================

    public static void isEmpty_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isEmpty());
    }

    // ========================================================================
    // available — whether the body has been read and is available
    // ========================================================================

    public static void available_get(NativeFrame frame){
        frame.finishBoolean(get(frame).available());
    }

}
