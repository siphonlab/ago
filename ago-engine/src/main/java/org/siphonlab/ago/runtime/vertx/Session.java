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

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class Session {

    private static io.vertx.ext.web.Session get(NativeFrame frame){
        return (io.vertx.ext.web.Session) frame.getParentScope().getNativePayload();
    }

    // ========================================================================
    // id
    // ========================================================================

    public static void id_get(NativeFrame frame){
        frame.finishString(get(frame).id());
    }

    // ========================================================================
    // put / get / remove
    // ========================================================================

    @SuppressWarnings("unchecked")
    public static void put(NativeFrame frame, String key, Object value){
        get(frame).put(key, value);
        frame.finishVoid();
    }

    @SuppressWarnings("unchecked")
    public static void get(NativeFrame frame, String key){
        Object obj = get(frame).get(key);
        if (obj == null) {
            frame.finishUnion(null);
        } else if (obj instanceof Instance<?>) {
            frame.finishUnion((Instance<?>) obj);
        } else {
            var agoClass = frame.getAgoEngine().getClass("lang.Object");
            Instance<?> inst = frame.getAgoEngine().createNativeInstance(null, agoClass, frame.getRunSpace());
            inst.setNativePayload(obj);
            frame.finishUnion(inst);
        }
    }

    @SuppressWarnings("unchecked")
    public static void remove(NativeFrame frame, String key){
        Object obj = get(frame).remove(key);
        if (obj == null) {
            frame.finishUnion(null);
        } else if (obj instanceof Instance<?>) {
            frame.finishUnion((Instance<?>) obj);
        } else {
            var agoClass = frame.getAgoEngine().getClass("lang.Object");
            Instance<?> inst = frame.getAgoEngine().createNativeInstance(null, agoClass, frame.getRunSpace());
            inst.setNativePayload(obj);
            frame.finishUnion(inst);
        }
    }

    // ========================================================================
    // data — all session attributes as Map
    // ========================================================================

    public static void data_get(NativeFrame frame){
        var map = get(frame).data();
        Instance<?> inst = frame.getAgoEngine().createNativeInstance(
                null,
                frame.getAgoEngine().getClass("lang.Map"),
                frame.getRunSpace()
        );
        inst.setNativePayload(map);
        frame.finishObject(inst);
    }

    // ========================================================================
    // isEmpty
    // ========================================================================

    public static void isEmpty_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isEmpty());
    }

    // ========================================================================
    // lastAccessed
    // ========================================================================

    public static void lastAccessed_get(NativeFrame frame){
        frame.finishLong(get(frame).lastAccessed());
    }

    // ========================================================================
    // timeout
    // ========================================================================

    public static void timeout_get(NativeFrame frame){
        frame.finishLong(get(frame).timeout());
    }

    // ========================================================================
    // destroy
    // ========================================================================

    public static void destroy(NativeFrame frame){
        get(frame).destroy();
        frame.finishVoid();
    }

    // ========================================================================
    // isDestroyed
    // ========================================================================

    public static void isDestroyed_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isDestroyed());
    }

    // ========================================================================
    // regenerateId
    // ========================================================================

    public static void regenerateId(NativeFrame frame){
        io.vertx.ext.web.Session sess = get(frame).regenerateId();
        Instance<?> inst = ClassMapping.mapObject(sess, frame);
        frame.finishObject(inst);
    }

    // ========================================================================
    // oldId
    // ========================================================================

    public static void oldId_get(NativeFrame frame){
        frame.finishUnion(get(frame).oldId());
    }

    // ========================================================================
    // isRegenerated
    // ========================================================================

    public static void isRegenerated_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isRegenerated());
    }

}
