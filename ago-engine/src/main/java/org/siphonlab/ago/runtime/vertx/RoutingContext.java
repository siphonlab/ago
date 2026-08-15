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
import org.siphonlab.ago.native_.NativeInstance;

import java.util.List;

public class RoutingContext {

    private static io.vertx.ext.web.RoutingContext get(NativeFrame frame){
        return (io.vertx.ext.web.RoutingContext) frame.getParentScope().getNativePayload();
    }

    public static void end(NativeFrame frame, String s){
        get(frame).response().end(s);
        frame.finishVoid();
    }

    public static void end(NativeFrame frame, Instance<?> buffer){
        Buffer buf = (Buffer) ((NativeInstance)buffer).getNativePayload();
        get(frame).response().end(buf);
        frame.finishVoid();
    }

    public static void request_get(NativeFrame frame){
        var inst = ClassMapping.mapObject(get(frame).request(), frame);
        frame.finishObject(inst);
    }

    public static void response_get(NativeFrame frame){
        var inst = ClassMapping.mapObject(get(frame).response(), frame);
        frame.finishObject(inst);
    }

    public static void body_get(NativeFrame frame){
        var agoClass = frame.getAgoEngine().getClass("io.RequestBody");
        Instance<?> inst = frame.getAgoEngine().createNativeInstance(null, agoClass, frame.getRunSpace());
        inst.setNativePayload(get(frame).body());
        frame.finishObject(inst);
    }

    public static void pathParam(NativeFrame frame, String name){
        var val = get(frame).pathParam(name);
        frame.finishUnion(val);
    }

    public static void queryParams_get(NativeFrame frame){
        var inst = ClassMapping.mapObject(get(frame).queryParams(), frame);
        frame.finishObject(inst);
    }

    public static void queryParam(NativeFrame frame, String name){
        List<String> val = get(frame).queryParam(name);
        if(val == null){
            frame.finishUnion(null);
        } else {
            throw new UnsupportedOperationException("TODO");
//            frame.finishUnion(ClassMapping.mapObject(val, frame));
        }
    }

    public static void next(NativeFrame frame){
        get(frame).next();
        frame.finishVoid();
    }

    public static void fail(NativeFrame frame, int statusCode){
        get(frame).fail(statusCode);
        frame.finishVoid();
    }

    public static void putData(NativeFrame frame, String key, Object value){
        get(frame).put(key, value);
        frame.finishVoid();
    }

    public static void data_get(NativeFrame frame, String key){
        var val = get(frame).get(key);
        frame.finishUnion(val);
    }

    public static void mountPoint_get(NativeFrame frame){
        var val = get(frame).mountPoint();
        frame.finishUnion(val);
    }

    public static void normalizedPath_get(NativeFrame frame){
        var val = get(frame).normalizedPath();
        frame.finishUnion(val);
    }

    public static void isFailed_get(NativeFrame frame){
        frame.finishBoolean(get(frame).failed());
    }

    public static void statusCode_get(NativeFrame frame){
        frame.finishInt(get(frame).response().getStatusCode());
    }

    public static void session_get(NativeFrame frame){
        var session = get(frame).session();
        if(session != null){
            var inst = ClassMapping.mapObject(session, frame);
            frame.finishUnion(inst);
        } else {
            frame.finishUnion(null);
        }
    }

    public static void isSessionAccessed_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isSessionAccessed());
    }

    public static void fileUploads_get(NativeFrame frame){
        frame.finishUnion(null);
    }

    public static void redirect(NativeFrame frame, String location){
        get(frame).redirect(location);
        frame.finishVoid();
    }

//    public static void json(NativeFrame frame, Object obj){
//        get(frame).response().putHeader("Content-Type", "application/json").end(String.valueOf(obj));
//        frame.finishVoid();
//    }

    public static void preferredLanguage_get(NativeFrame frame){
        frame.finishUnion(get(frame).preferredLanguage());
    }
}
