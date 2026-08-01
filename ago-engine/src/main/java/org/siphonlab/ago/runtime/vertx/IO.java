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
import io.vertx.core.file.OpenOptions;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class IO {

    // ========================================================================
    // FileSystem operations
    // ========================================================================

    public static void FileSystem_open(NativeFrame frame, String path){
        frame.beginAsync();
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        host.getVertx().fileSystem()
                .open(path, new OpenOptions().setRead(true).setCreateNew(false).setCreate(false))
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.File", "io.IOException"));
    }

    public static void FileSystem_readFile(NativeFrame frame, String path){
        frame.beginAsync();
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        host.getVertx().fileSystem()
                .readFile(path)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.Buffer", "io.IOException"));
    }

    public static void FileSystem_writeFile(NativeFrame frame, String path, Instance<?> data){
        frame.beginAsync();
        Buffer buffer = (Buffer) data.getNativePayload();
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        host.getVertx().fileSystem()
                .writeFile(path, buffer)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void FileSystem_exists(NativeFrame frame, String path){
        frame.beginAsync();
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        host.getVertx().fileSystem()
                .exists(path)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void FileSystem_size(NativeFrame frame, String path){
        frame.beginAsync();
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        host.getVertx().fileSystem()
                .props(path)
                .map(fp -> fp.size())
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void FileSystem_mkdir(NativeFrame frame, String path){
        frame.beginAsync();
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        host.getVertx().fileSystem()
                .mkdirs(path)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void FileSystem_rename(NativeFrame frame, String oldPath, String newPath){
        frame.beginAsync();
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        host.getVertx().fileSystem()
                .move(oldPath, newPath)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void FileSystem_delete(NativeFrame frame, String path){
        frame.beginAsync();
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        host.getVertx().fileSystem()
                .delete(path)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    // ========================================================================
    // File operations (wraps AsyncFile)
    // ========================================================================

    public static void File_read(NativeFrame frame, Instance<?> buffer, int offset, long position, int length){
        frame.beginAsync();
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.read((Buffer) buffer.getNativePayload(), offset, position, length)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.Buffer", "io.IOException"));
    }

    public static void File_write(NativeFrame frame, Instance<?> buffer, long position){
        frame.beginAsync();
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.write((Buffer) buffer.getNativePayload(), position)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void File_pause(NativeFrame frame){
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.pause();
        frame.finishObject(frame.getParentScope());
    }

    public static void File_resume(NativeFrame frame){
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.resume();
        frame.finishObject(frame.getParentScope());
    }

    public static void File_fetch(NativeFrame frame, long amount){
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.fetch(amount);
        frame.finishObject(frame.getParentScope());
    }

    public static void File_setReadPos(NativeFrame frame, long pos){
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.setReadPos(pos);
        frame.finishVoid();
    }

    public static void File_setWritePos(NativeFrame frame, long pos){
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.setWritePos(pos);
        frame.finishVoid();
    }

    public static void File_getWritePos(NativeFrame frame){
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        frame.finishLong(file.getWritePos());
    }

    public static void File_setReadLength(NativeFrame frame, long length){
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.setReadLength(length);
        frame.finishVoid();
    }

    public static void File_getReadLength(NativeFrame frame){
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        frame.finishLong(file.getReadLength());
    }

    public static void File_size(NativeFrame frame){
        frame.beginAsync();
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.size()
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void File_flush(NativeFrame frame){
        frame.beginAsync();
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.flush()
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void File_close(NativeFrame frame){
        frame.beginAsync();
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.close()
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

}
