package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class HttpClientRequest {

    private static io.vertx.core.http.HttpClientRequest get(NativeFrame frame){
        return (io.vertx.core.http.HttpClientRequest) frame.getParentScope().getNativePayload();
    }

    // ========================================================================
    // Headers
    // ========================================================================

    public static void putHeader(NativeFrame frame, String name, String value){
        get(frame).putHeader(name, value);
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
    // Send request and get response (async)
    // ========================================================================

    public static void send_getResponse(NativeFrame frame){
        frame.beginAsync();
        var handleBody = frame.getParentScope().getBooleanField("handleBody");
        get(frame).send()
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"){
                    @Override
                    public void onReceiveInstance(HttpClientResponse result, Instance<?> instance) {
                        if(handleBody){
                            var bodyInst = frame.getAgoEngine().createNativeInstance(null, frame.getAgoEngine().getLangClasses().getNativeObjectClass(), frame.getRunSpace());
                            bodyInst.setNativePayload(result.body());
                            instance.setObjectField("body", bodyInst); ;
                        }
                    }
                });
    }

    public static void send_buffer(NativeFrame frame, Instance<?> buffer){
        frame.beginAsync();
        Buffer vertxBuffer = (Buffer) buffer.getNativePayload();
        get(frame).send(vertxBuffer)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    // ========================================================================
    // End request (no body)
    // ========================================================================

    public static void end(NativeFrame frame){
        frame.beginAsync();
        get(frame).end()
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    // ========================================================================
    // Write chunks (for streaming request body)
    // ========================================================================

    public static void write_string(NativeFrame frame, String data){
        get(frame).write(data);
        frame.finishVoid();
    }

    // ========================================================================
    // Chunked encoding
    // ========================================================================

    public static void setChunked(NativeFrame frame, boolean chunked){
        get(frame).setChunked(chunked);
        frame.finishVoid();
    }


}
