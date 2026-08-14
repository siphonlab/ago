package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class HttpResponse {

    private static HttpServerResponse get(NativeFrame frame){
        return (HttpServerResponse) frame.getParentScope().getNativePayload();
    }

    // ========================================================================
    // Status code
    // ========================================================================

    public static void setStatusCode(NativeFrame frame, int statusCode){
        get(frame).setStatusCode(statusCode);
        frame.finishVoid();
    }

    public static void statusCode_get(NativeFrame frame){
        frame.finishInt(get(frame).getStatusCode());
    }

    // ========================================================================
    // Headers
    // ========================================================================

    public static void putHeader(NativeFrame frame, String name, String value){
        get(frame).putHeader(name, value);
        frame.finishVoid();
    }

    public static void removeHeader(NativeFrame frame, String name){
        get(frame).headers().remove(name);
        frame.finishVoid();
    }

    public static void headers_get(NativeFrame frame){
        MultiMap headers = get(frame).headers();
        Instance<?> inst = frame.getAgoEngine().createNativeInstance(
                null,
                frame.getAgoEngine().getClass("io.HttpHeaders"),
                frame.getRunSpace()
        );
        inst.setNativePayload(headers);
        frame.finishObject(inst);
    }

    // ========================================================================
    // Writing response body
    // ========================================================================

    public static void end(NativeFrame frame){
        frame.beginAsync();
        get(frame).end();
        frame.finishVoidAsync();
    }

    public static void end_string(NativeFrame frame, String data){
        frame.beginAsync();
        get(frame).end(data);
        frame.finishVoidAsync();
    }

    public static void write_string(NativeFrame frame, String data){
        frame.beginAsync();
        get(frame).write(data);
        frame.finishVoidAsync();
    }

    public static void write_buffer(NativeFrame frame, Instance<?> buffer){
        frame.beginAsync();
        io.vertx.core.buffer.Buffer vertxBuffer = (io.vertx.core.buffer.Buffer) buffer.getNativePayload();
        get(frame).write(vertxBuffer);
        frame.finishVoidAsync();
    }

    // ========================================================================
    // Chunked encoding
    // ========================================================================

    public static void setChunked(NativeFrame frame, boolean chunked){
        get(frame).setChunked(chunked);
        frame.finishVoid();
    }

}
