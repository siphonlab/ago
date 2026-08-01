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
    // Buffer constructors
    // ========================================================================

    public static void Buffer_create(NativeFrame frame){
        frame.getParentScope().setNativePayload(Buffer.buffer());
        frame.finishVoid();
    }

    public static void Buffer_createSize(NativeFrame frame, int size){
        frame.getParentScope().setNativePayload(Buffer.buffer(size));
        frame.finishVoid();
    }

    public static void Buffer_createString(NativeFrame frame, String s){
        frame.getParentScope().setNativePayload(Buffer.buffer(s));
        frame.finishVoid();
    }

    // ========================================================================
    // Buffer property
    // ========================================================================

    public static void Buffer_length(NativeFrame frame){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        frame.finishInt(buffer.length());
    }

    // ========================================================================
    // Buffer read (getter) operations
    // ========================================================================

    public static void Buffer_getByte(NativeFrame frame, int pos){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        frame.finishByte(buffer.getByte(pos));
    }

    public static void Buffer_getInt(NativeFrame frame, int pos){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        frame.finishInt(buffer.getInt(pos));
    }

    public static void Buffer_getLong(NativeFrame frame, int pos){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        frame.finishLong(buffer.getLong(pos));
    }

    public static void Buffer_getFloat(NativeFrame frame, int pos){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        frame.finishFloat(buffer.getFloat(pos));
    }

    public static void Buffer_getDouble(NativeFrame frame, int pos){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        frame.finishDouble(buffer.getDouble(pos));
    }

    public static void Buffer_getString(NativeFrame frame, int start, int end){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        frame.finishString(buffer.getString(start, end));
    }

    // ========================================================================
    // Buffer write (setter) operations — return self for chaining
    // ========================================================================

    public static void Buffer_setByte(NativeFrame frame, int pos, byte value){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        buffer.setByte(pos, value);
        frame.finishObject(frame.getParentScope());
    }

    public static void Buffer_setInt(NativeFrame frame, int pos, int value){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        buffer.setInt(pos, value);
        frame.finishObject(frame.getParentScope());
    }

    public static void Buffer_setLong(NativeFrame frame, int pos, long value){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        buffer.setLong(pos, value);
        frame.finishObject(frame.getParentScope());
    }

    public static void Buffer_setFloat(NativeFrame frame, int pos, float value){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        buffer.setFloat(pos, value);
        frame.finishObject(frame.getParentScope());
    }

    public static void Buffer_setDouble(NativeFrame frame, int pos, double value){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        buffer.setDouble(pos, value);
        frame.finishObject(frame.getParentScope());
    }

    public static void Buffer_setString(NativeFrame frame, int pos, String s){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        buffer.setString(pos, s);
        frame.finishObject(frame.getParentScope());
    }

    // ========================================================================
    // Buffer append operations — return self for chaining
    // ========================================================================

    public static void Buffer_appendByte(NativeFrame frame, byte value){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        buffer.appendByte(value);
        frame.finishObject(frame.getParentScope());
    }

    public static void Buffer_appendInt(NativeFrame frame, int value){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        buffer.appendInt(value);
        frame.finishObject(frame.getParentScope());
    }

    public static void Buffer_appendLong(NativeFrame frame, long value){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        buffer.appendLong(value);
        frame.finishObject(frame.getParentScope());
    }

    public static void Buffer_appendString(NativeFrame frame, String s){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        buffer.appendString(s);
        frame.finishObject(frame.getParentScope());
    }

    // ========================================================================
    // Buffer copy / slice — return new Buffer instance
    // ========================================================================

    public static void Buffer_copy(NativeFrame frame){
        Buffer src = (Buffer) frame.getParentScope().getNativePayload();
        var inst = frame.getAgoEngine().createNativeInstance(
                null, frame.getAgoEngine().getClass("io.Buffer"), frame.getRunSpace());
        inst.setNativePayload(src.copy());
        frame.finishObject(inst);
    }

    public static void Buffer_slice(NativeFrame frame, int start, int end){
        Buffer src = (Buffer) frame.getParentScope().getNativePayload();
        var inst = frame.getAgoEngine().createNativeInstance(
                null, frame.getAgoEngine().getClass("io.Buffer"), frame.getRunSpace());
        inst.setNativePayload(src.slice(start, end));
        frame.finishObject(inst);
    }

    // ========================================================================
    // Buffer toString
    // ========================================================================

    public static void Buffer_toString(NativeFrame frame){
        Buffer buffer = (Buffer) frame.getParentScope().getNativePayload();
        frame.finishString(buffer.toString());
    }

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
