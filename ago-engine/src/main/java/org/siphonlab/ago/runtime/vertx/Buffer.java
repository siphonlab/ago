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

public class Buffer {

    // ========================================================================
    // constructors
    // ========================================================================

    public static void create(NativeFrame frame){
        frame.getParentScope().setNativePayload(io.vertx.core.buffer.Buffer.buffer());
        frame.finishVoid();
    }

    public static void createSize(NativeFrame frame, int size){
        frame.getParentScope().setNativePayload(io.vertx.core.buffer.Buffer.buffer(size));
        frame.finishVoid();
    }

    public static void createString(NativeFrame frame, String s){
        frame.getParentScope().setNativePayload(io.vertx.core.buffer.Buffer.buffer(s));
        frame.finishVoid();
    }

    public static void createStringEncoding(NativeFrame frame, String s, String enc){
        frame.getParentScope().setNativePayload(io.vertx.core.buffer.Buffer.buffer(s, enc));
        frame.finishVoid();
    }

    // ========================================================================
    // property
    // ========================================================================

    public static void length(NativeFrame frame){
        io.vertx.core.buffer.Buffer b = get(frame);
        frame.finishInt(b.length());
    }

    // ========================================================================
    // read (getter) operations
    // ========================================================================

    public static void getByte(NativeFrame frame, int pos){
        frame.finishByte(get(frame).getByte(pos));
    }

    public static void getShort(NativeFrame frame, int pos){
        frame.finishShort(get(frame).getShort(pos));
    }

    public static void getShortLE(NativeFrame frame, int pos){
        frame.finishShort(get(frame).getShortLE(pos));
    }

    public static void getInt(NativeFrame frame, int pos){
        frame.finishInt(get(frame).getInt(pos));
    }

    public static void getIntLE(NativeFrame frame, int pos){
        frame.finishInt(get(frame).getIntLE(pos));
    }

    public static void getLong(NativeFrame frame, int pos){
        frame.finishLong(get(frame).getLong(pos));
    }

    public static void getLongLE(NativeFrame frame, int pos){
        frame.finishLong(get(frame).getLongLE(pos));
    }

    public static void getFloat(NativeFrame frame, int pos){
        frame.finishFloat(get(frame).getFloat(pos));
    }

    public static void getFloatLE(NativeFrame frame, int pos){
        frame.finishFloat(get(frame).getFloatLE(pos));
    }

    public static void getDouble(NativeFrame frame, int pos){
        frame.finishDouble(get(frame).getDouble(pos));
    }

    public static void getDoubleLE(NativeFrame frame, int pos){
        frame.finishDouble(get(frame).getDoubleLE(pos));
    }

    public static void getMedium(NativeFrame frame, int pos){
        frame.finishInt(get(frame).getMedium(pos));
    }

    public static void getMediumLE(NativeFrame frame, int pos){
        frame.finishInt(get(frame).getMediumLE(pos));
    }

    public static void getString(NativeFrame frame, int start, int end){
        frame.finishString(get(frame).getString(start, end));
    }

    public static void getStringEnc(NativeFrame frame, int start, int end, String enc){
        frame.finishString(get(frame).getString(start, end, enc));
    }

    // ========================================================================
    // write (setter) operations — return self for chaining
    // ========================================================================

    public static void setByte(NativeFrame frame, int pos, byte value){
        get(frame).setByte(pos, value);
        finishSelf(frame);
    }

    public static void setShort(NativeFrame frame, int pos, short value){
        get(frame).setShort(pos, value);
        finishSelf(frame);
    }

    public static void setShortLE(NativeFrame frame, int pos, short value){
        get(frame).setShortLE(pos, value);
        finishSelf(frame);
    }

    public static void setInt(NativeFrame frame, int pos, int value){
        get(frame).setInt(pos, value);
        finishSelf(frame);
    }

    public static void setIntLE(NativeFrame frame, int pos, int value){
        get(frame).setIntLE(pos, value);
        finishSelf(frame);
    }

    public static void setLong(NativeFrame frame, int pos, long value){
        get(frame).setLong(pos, value);
        finishSelf(frame);
    }

    public static void setLongLE(NativeFrame frame, int pos, long value){
        get(frame).setLongLE(pos, value);
        finishSelf(frame);
    }

    public static void setFloat(NativeFrame frame, int pos, float value){
        get(frame).setFloat(pos, value);
        finishSelf(frame);
    }

    public static void setFloatLE(NativeFrame frame, int pos, float value){
        get(frame).setFloatLE(pos, value);
        finishSelf(frame);
    }

    public static void setDouble(NativeFrame frame, int pos, double value){
        get(frame).setDouble(pos, value);
        finishSelf(frame);
    }

    public static void setDoubleLE(NativeFrame frame, int pos, double value){
        get(frame).setDoubleLE(pos, value);
        finishSelf(frame);
    }

    public static void setMedium(NativeFrame frame, int pos, int value){
        get(frame).setMedium(pos, value);
        finishSelf(frame);
    }

    public static void setMediumLE(NativeFrame frame, int pos, int value){
        get(frame).setMediumLE(pos, value);
        finishSelf(frame);
    }

    public static void setString(NativeFrame frame, int pos, String s){
        get(frame).setString(pos, s);
        finishSelf(frame);
    }

    public static void setStringEnc(NativeFrame frame, int pos, String s, String enc){
        get(frame).setString(pos, s, enc);
        finishSelf(frame);
    }

    public static void setBuffer(NativeFrame frame, int pos, Instance<?> other){
        get(frame).setBuffer(pos, (io.vertx.core.buffer.Buffer) other.getNativePayload());
        finishSelf(frame);
    }

    public static void setBufferRange(NativeFrame frame, int pos, Instance<?> other, int offset, int len){
        get(frame).setBuffer(pos, (io.vertx.core.buffer.Buffer) other.getNativePayload(), offset, len);
        finishSelf(frame);
    }

    // ========================================================================
    // append operations — return self for chaining
    // ========================================================================

    public static void appendByte(NativeFrame frame, byte value){
        get(frame).appendByte(value);
        finishSelf(frame);
    }

    public static void appendShort(NativeFrame frame, short value){
        get(frame).appendShort(value);
        finishSelf(frame);
    }

    public static void appendShortLE(NativeFrame frame, short value){
        get(frame).appendShortLE(value);
        finishSelf(frame);
    }

    public static void appendInt(NativeFrame frame, int value){
        get(frame).appendInt(value);
        finishSelf(frame);
    }

    public static void appendIntLE(NativeFrame frame, int value){
        get(frame).appendIntLE(value);
        finishSelf(frame);
    }

    public static void appendLong(NativeFrame frame, long value){
        get(frame).appendLong(value);
        finishSelf(frame);
    }

    public static void appendLongLE(NativeFrame frame, long value){
        get(frame).appendLongLE(value);
        finishSelf(frame);
    }

    public static void appendFloat(NativeFrame frame, float value){
        get(frame).appendFloat(value);
        finishSelf(frame);
    }

    public static void appendFloatLE(NativeFrame frame, float value){
        get(frame).appendFloatLE(value);
        finishSelf(frame);
    }

    public static void appendDouble(NativeFrame frame, double value){
        get(frame).appendDouble(value);
        finishSelf(frame);
    }

    public static void appendDoubleLE(NativeFrame frame, double value){
        get(frame).appendDoubleLE(value);
        finishSelf(frame);
    }

    public static void appendMedium(NativeFrame frame, int value){
        get(frame).appendMedium(value);
        finishSelf(frame);
    }

    public static void appendMediumLE(NativeFrame frame, int value){
        get(frame).appendMediumLE(value);
        finishSelf(frame);
    }

    public static void appendString(NativeFrame frame, String s){
        get(frame).appendString(s);
        finishSelf(frame);
    }

    public static void appendStringEnc(NativeFrame frame, String s, String enc){
        get(frame).appendString(s, enc);
        finishSelf(frame);
    }

    public static void appendBuffer(NativeFrame frame, Instance<?> other){
        get(frame).appendBuffer((io.vertx.core.buffer.Buffer) other.getNativePayload());
        finishSelf(frame);
    }

    public static void appendBufferRange(NativeFrame frame, Instance<?> other, int offset, int len){
        get(frame).appendBuffer((io.vertx.core.buffer.Buffer) other.getNativePayload(), offset, len);
        finishSelf(frame);
    }

    // ========================================================================
    // copy / slice — return new Buffer instance
    // ========================================================================

    public static void copy(NativeFrame frame){
        io.vertx.core.buffer.Buffer src = get(frame);
        finishNewBuffer(frame, src.copy());
    }

    public static void slice(NativeFrame frame){
        io.vertx.core.buffer.Buffer src = get(frame);
        finishNewBuffer(frame, src.slice());
    }

    public static void sliceRange(NativeFrame frame, int start, int end){
        io.vertx.core.buffer.Buffer src = get(frame);
        finishNewBuffer(frame, src.slice(start, end));
    }

    // ========================================================================
    // static factory (metaclass methods)
    // ========================================================================

    public static void staticBuffer(NativeFrame frame){
        finishNewBuffer(frame, io.vertx.core.buffer.Buffer.buffer());
    }

    public static void staticBufferSize(NativeFrame frame, int size){
        finishNewBuffer(frame, io.vertx.core.buffer.Buffer.buffer(size));
    }

    public static void staticBufferString(NativeFrame frame, String s){
        finishNewBuffer(frame, io.vertx.core.buffer.Buffer.buffer(s));
    }

    // ========================================================================
    // toString
    // ========================================================================

    public static void toString(NativeFrame frame){
        frame.finishString(get(frame).toString());
    }

    // ========================================================================
    // helpers
    // ========================================================================

    private static io.vertx.core.buffer.Buffer get(NativeFrame frame){
        return (io.vertx.core.buffer.Buffer) frame.getParentScope().getNativePayload();
    }

    private static void finishSelf(NativeFrame frame){
        frame.finishObject(frame.getParentScope());
    }

    private static void finishNewBuffer(NativeFrame frame, io.vertx.core.buffer.Buffer b){
        var inst = frame.getAgoEngine().createNativeInstance(
                null, frame.getAgoEngine().getClass("io.Buffer"), frame.getRunSpace());
        inst.setNativePayload(b);
        frame.finishObject(inst);
    }

}
