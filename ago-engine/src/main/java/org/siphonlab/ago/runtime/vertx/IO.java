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
import io.vertx.core.streams.ReadStream;
import org.apache.commons.io.input.QueueInputStream;
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
        if(frame.getReenterState() == NativeFrame.REENTER_CREATE_INSTANCE){
            Object nativePayload = frame.getNativePayload();
            frame.finishObjectAsync((Instance<?>) nativePayload);
            return;
        }

        frame.beginAsync();
        VertxRunSpaceHost runSpaceHost = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        runSpaceHost.getVertx().fileSystem().open(path, new OpenOptions().setRead(true).setCreateNew(false).setCreate(false))
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.File"));
    }

    public static void File_create(NativeFrame frame){
        Instance<?> fileInstance = frame.getParentScope();
        ReadStream<?> file = (ReadStream<?>) fileInstance.getNativePayload();

//        ReadStreamWrapper readStreamWrapper = new ReadStreamWrapper(file);
//        file.endHandler(readStreamWrapper::endHandler);
//        file.exceptionHandler(readStreamWrapper::exceptionHandler);
//        file.handler(readStreamWrapper::handle);

//        var wrapperInst = frame.getAgoEngine().createInstance(fileInstance.getAgoClass().getSlotDefs()[0].getAgoClass(), frame.getRunSpace());
//        wrapperInst.setNativePayload(readStreamWrapper);

//        fileInstance.getSlots().setObject(0, wrapperInst);     // readStreamWrapper
        frame.finishVoid();
    }

    public static void File_read(NativeFrame frame, Instance<?> buffer, int offset, long position, int length){
        frame.beginAsync();
        AsyncFile file = (AsyncFile) frame.getParentScope().getNativePayload();
        file.read((Buffer) buffer.getNativePayload(), offset, position, length)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.Buffer"));
    }

    public static void ReadStream_pause(NativeFrame frame){
        frame.beginAsync();
        ReadStream<?> readStream = (ReadStream<?>) frame.getParentScope().getNativePayload();
        readStream.pause();
        frame.finishVoid();
    }

    public static void ReadStream_resume(NativeFrame frame){
        frame.beginAsync();
        ReadStream<?> readStream = (ReadStream<?>) frame.getParentScope().getNativePayload();
        readStream.resume();
        frame.finishVoid();
    }

    public static void ReadStream_fetch(NativeFrame frame, long amount){
        frame.beginAsync();
        ReadStream<?> readStream = (ReadStream<?>) frame.getParentScope().getNativePayload();
        readStream.fetch(amount);
        frame.finishVoid();
    }

    public static void ReadStream_generator(NativeFrame frame){
        ReadStreamGenerator<?> readStreamGenerator = (ReadStreamGenerator<?>) frame.getParentScope().getNativePayload();
        if(frame.getNativePayload() == null){
            ReadStream<?> readStream = (ReadStream<?>) frame.getParentScope().getNativePayload();
            readStreamGenerator = new ReadStreamGenerator<>(readStream, frame);
            frame.setNativePayload(readStreamGenerator);
            readStreamGenerator.init();
        }
        readStreamGenerator.next(frame);
    }

    public static void ReadStream_hasNext(NativeFrame frame){
        frame.beginAsync();
        ReadStream<?> readStream = (ReadStream<?>) frame.getParentScope().getNativePayload();
        frame.finishVoid();
    }


}
