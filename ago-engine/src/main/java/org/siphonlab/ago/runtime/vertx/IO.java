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

import io.vertx.core.Future;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;

public class IO {

    public static void ReadStream_pause(NativeFrame frame){
        ReadStream<?> readStream = (ReadStream<?>) frame.getParentScope().getNativePayload();
        readStream.pause();
        frame.finishObject(frame.getParentScope());
    }

    public static void ReadStream_resume(NativeFrame frame){
        ReadStream<?> readStream = (ReadStream<?>) frame.getParentScope().getNativePayload();
        readStream.resume();
        frame.finishObject(frame.getParentScope());
    }

    public static void ReadStream_fetch(NativeFrame frame, long amount){
        frame.beginAsync();
        ReadStream<?> readStream = (ReadStream<?>) frame.getParentScope().getNativePayload();
        readStream.fetch(amount);
        frame.finishObject(frame.getParentScope());
    }

    public static void ReadStream_generator(NativeFrame frame){
        ReadStreamGenerator<?> readStreamGenerator = (ReadStreamGenerator<?>) frame.getNativePayload();
        if(frame.getNativePayload() == null){
            ReadStream<?> readStream = (ReadStream<?>) frame.getParentScope().getNativePayload();
            readStreamGenerator = new ReadStreamGenerator<>(readStream, frame);
            frame.setNativePayload(readStreamGenerator);
            readStreamGenerator.init();
        }
        readStreamGenerator.next(frame);
    }

    public static void ReadStream_byeRead(NativeFrame frame){
        ReadStream<?> readStream = (ReadStream<?>) frame.getParentScope().getNativePayload();
        readStream.handler(null);
        readStream.endHandler(null);
        readStream.exceptionHandler(null);
        try {
            readStream.pause();
        } catch (Exception _) {}        // nothing to do
        frame.finishVoid();
    }

    public static void WriteStream_byeWrite(NativeFrame frame){
        WriteStream<?> writeStream = (WriteStream<?>) frame.getParentScope().getNativePayload();
        writeStream.drainHandler(null);
        writeStream.exceptionHandler(null);
        try {
            writeStream.end();
        } catch (Exception _) {}        // nothing to do
        frame.finishVoid();
    }

    public static void WriteStream_write(NativeFrame frame, Object data){
//        frame.beginAsync();
        WriteStream<Object> writeStream = (WriteStream<Object>) frame.getParentScope().getNativePayload();
        Future<Void> written;
        if(data instanceof NativeInstance nativeInstance) {
            written = writeStream.write(nativeInstance.getNativePayload());
        } else {
            written = writeStream.write(data);
        }
        frame.finishVoid();     // the WriteStream may be async and give a Future.succeededFuture
//        written.onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void WriteStream_writeQueueFull(NativeFrame frame){
        WriteStream<?> writeStream = (WriteStream<?>) frame.getParentScope().getNativePayload();
        frame.finishBoolean(writeStream.writeQueueFull());
    }

    public static void WriteStream_setWriteQueueMaxSize(NativeFrame frame, int maxSize){
        WriteStream<?> writeStream = (WriteStream<?>) frame.getParentScope().getNativePayload();
        writeStream.setWriteQueueMaxSize(maxSize);
        frame.finishVoid();
    }

    public static void WriteStream_watchDrain(NativeFrame frame){
        WriteStream<?> writeStream = (WriteStream<?>) frame.getParentScope().getNativePayload();
        var writeStreamClass = frame.getAgoEngine().getClass("io.WriteStream").asThatOrSuperOfThat(frame.getParentScope().getAgoClass());
        var drainHandler = writeStreamClass.getAgoClass().findChild("DrainHandler");
        var inst = frame.getAgoEngine().createNativeInstance(null, drainHandler, frame.getRunSpace());
        inst.setNativePayload(new DrainHandler(writeStream));
        frame.finishObject(inst);
    }

    public static void DrainHandler_wait(NativeFrame frame){
        var drainHandler = (DrainHandler) frame.getParentScope().getNativePayload();
        drainHandler.connect(frame);
    }

}
