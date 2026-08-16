package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class HttpClientResponse {

    private static io.vertx.core.http.HttpClientResponse get(NativeFrame frame){
        return (io.vertx.core.http.HttpClientResponse) frame.getParentScope().getNativePayload();
    }

    // ========================================================================
    // Status code and reason phrase
    // ========================================================================

    public static void statusCode_get(NativeFrame frame){
        frame.finishInt(get(frame).statusCode());
    }

    public static void statusMessage_get(NativeFrame frame){
        String msg = get(frame).statusMessage();
        if (msg == null) {
            frame.finishObject(null);
        } else {
            frame.finishString(msg);
        }
    }

    // ========================================================================
    // HTTP version
    // ========================================================================

    public static void version_get(NativeFrame frame){
        var version = get(frame).version();
        if (version == null) {
            frame.finishObject(null);
        } else {
            frame.finishString(version.toString());
        }
    }

    // ========================================================================
    // Headers
    // ========================================================================

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

    public static void getHeader(NativeFrame frame, String name){
        String value = get(frame).getHeader(name);
        if (value == null) {
            frame.finishObject(null);
        } else {
            frame.finishString(value);
        }
    }

    // ========================================================================
    // Body as Buffer (async)
    // ========================================================================

    public static void body_get(NativeFrame frame){
        frame.beginAsync();
        var bodyInst = frame.getParentScope().getObjectField("body");
        Future<Buffer> body;
        if(bodyInst != null){
            body = (Future<Buffer>) bodyInst.getNativePayload();
        } else {
            body = get(frame).body();
        }
        body.onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    // ========================================================================
    // Pause / Resume / Fetch (ReadStream<Buffer>)
    // ========================================================================

    public static void pause(NativeFrame frame){
        get(frame).pause();
        frame.finishObject(frame.getParentScope());
    }

    public static void resume(NativeFrame frame){
        get(frame).resume();
        frame.finishObject(frame.getParentScope());
    }

    public static void fetch(NativeFrame frame, long amount){
        get(frame).fetch(amount);
        frame.finishObject(frame.getParentScope());
    }

}
