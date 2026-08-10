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

import org.apache.commons.lang3.ClassUtils;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.CallFrame;
import org.siphonlab.ago.Instance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassMapping {
    private static final Map<String, String> NAME_MAPPING = Map.of(
        "io.vertx.core.buffer.Buffer", "io.Buffer",
        "io.vertx.core.file.FileSystem", "io.FileSystem",
        "io.vertx.core.file.AsyncFile", "io.File",
        "io.vertx.core.net.NetServer", "io.NetServer",
        "io.vertx.core.net.NetSocket", "io.Socket"
    );

    //TODO when ago engine release, should release the memory
    private static final Map<AgoEngine, Map<Class<?>, AgoClass>> CLASS_MAPPING = new ConcurrentHashMap<>();

    public static AgoClass map(Class<?> clazz, AgoEngine engine){
        var map = CLASS_MAPPING.computeIfAbsent(engine, k -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(clazz, r -> {
            var n = findByName(r);
            if(n == null) return null;
            return engine.getClass(n);
        });
    }

    public static String findByName(Class<?> clazz){
        for(var c = clazz; c != Object.class;  c = clazz.getSuperclass()){
            var n = NAME_MAPPING.get(c.getName());
            if(n != null) return n;
            for (Class<?> anInterface : ClassUtils.getAllInterfaces(clazz)) {
                n = NAME_MAPPING.get(anInterface.getName());
                if(n != null) return n;
            }
        }
        return null;
    }

    public static Instance<?> mapObject(Object object, CallFrame<?> callFrame){
        AgoEngine engine = callFrame.getAgoEngine();
        var agoClass = map(object.getClass(), engine);
        var instance = engine.createNativeInstance(null, agoClass, callFrame.getRunSpace());
        instance.setNativePayload(object);
        return instance;
    }
}
