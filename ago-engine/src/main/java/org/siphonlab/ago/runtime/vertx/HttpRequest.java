package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.siphonlab.ago.AgoEnum;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class HttpRequest {

    private static HttpServerRequest get(NativeFrame frame){
        return (HttpServerRequest) frame.getParentScope().getNativePayload();
    }

    // ========================================================================
    // Basic request properties
    // ========================================================================

    public static void method_get(NativeFrame frame){
        String methodName = get(frame).method().name();
        String capitalized = methodName.substring(0, 1) + methodName.substring(1).toLowerCase();
        AgoEnum httpMethodEnum = (AgoEnum) frame.getAgoEngine().getClass("io.HttpMethod");
        Instance<?> enumInstance = httpMethodEnum.findMember(capitalized);
        frame.finishObject(enumInstance);
    }

    public static void path_get(NativeFrame frame){
        frame.finishString(get(frame).path());
    }

    public static void uri_get(NativeFrame frame){
        frame.finishString(get(frame).uri());
    }

    public static void version_get(NativeFrame frame){
        frame.finishString(get(frame).version().toString());
    }

    // ========================================================================
    // Headers
    // ========================================================================

    public static void getHeader(NativeFrame frame, String name){
        String value = get(frame).getHeader(name);
        if (value == null) {
            frame.finishObject(null);
        } else {
            frame.finishString(value);
        }
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
    // Query parameters
    // ========================================================================

    public static void query_get(NativeFrame frame){
        frame.finishString(get(frame).query());
    }

    public static void getQueryParam(NativeFrame frame, String name){
        String value = get(frame).params().get(name);
        if (value == null) {
            frame.finishObject(null);
        } else {
            frame.finishString(value);
        }
    }

    // ========================================================================
    // Body — Future<Buffer>, async
    // ========================================================================

    public static void body_get(NativeFrame frame){
        frame.beginAsync();
        get(frame).body()
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    // ========================================================================
    // Response
    // ========================================================================

    public static void response_get(NativeFrame frame){
        HttpServerResponse response = get(frame).response();
        Instance<?> inst = ClassMapping.mapObject(response, frame);
        frame.finishObject(inst);
    }

    // ========================================================================
    // Remote/Local address info
    // ========================================================================

    public static void remoteAddress_get(NativeFrame frame){
        var addr = get(frame).remoteAddress();
        if (addr == null) {
            frame.finishObject(null);
        } else {
            frame.finishString(addr.host());
        }
    }

    public static void remotePort_get(NativeFrame frame){
        var addr = get(frame).remoteAddress();
        if (addr == null) {
            frame.finishInt(0);
        } else {
            frame.finishInt(addr.port());
        }
    }

    // ========================================================================
    // ReadStream<Buffer> operations
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
