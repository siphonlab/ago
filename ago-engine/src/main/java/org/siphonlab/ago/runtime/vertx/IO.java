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
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class IO {

    public static void Buffer_create(NativeFrame frame){
        frame.getParentScope().setNativePayload(Buffer.buffer());
        frame.finishVoid();
    }

    public static void Buffer_toString(NativeFrame frame){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        frame.finishString(buffer.toString());
    }

    public static void FileSystem_open(NativeFrame frame, String path){
        frame.beginAsync();
        VertxRunSpaceHost runSpaceHost = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        runSpaceHost.getVertx().fileSystem().open(path, new OpenOptions().setRead(true).setCreateNew(false).setCreate(false))
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.File", "io.IOException"));
    }

    public static void File_read(NativeFrame frame, Instance<?> buffer, int offset, long position, int length){
        frame.beginAsync();
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.read((Buffer) buffer.getNativePayload(), offset, position, length)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.Buffer", "io.IOException"));
    }

}
