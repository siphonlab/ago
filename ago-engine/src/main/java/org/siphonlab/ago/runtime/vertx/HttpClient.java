package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.http.HttpClientOptions;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class HttpClient {

    private static io.vertx.core.http.HttpClient get(NativeFrame frame){
        return (io.vertx.core.http.HttpClient) frame.getParentScope().getNativePayload();
    }

    // ========================================================================
    // Create
    // ========================================================================

    public static void create(NativeFrame frame){
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        io.vertx.core.http.HttpClient client = host.getVertx().createHttpClient();
        Instance<?> inst = frame.getParentScope();
        inst.setNativePayload(client);
        frame.finishVoid();
    }

    public static void createWithOptions(NativeFrame frame, Instance<?> options){
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        HttpClientOptions opts = (HttpClientOptions) options.getNativePayload();
        io.vertx.core.http.HttpClient client = host.getVertx().createHttpClient(opts);
        Instance<?> inst = frame.getParentScope();
        inst.setNativePayload(client);
        frame.finishVoid();
    }

    // ========================================================================
    // Request methods (return HttpClientRequest)
    // ========================================================================

    public static void request(NativeFrame frame, String method, int port, String host, String path){
        frame.beginAsync();
        io.vertx.core.http.HttpClient client = get(frame);
        io.vertx.core.http.HttpMethod httpMethod = new io.vertx.core.http.HttpMethod(method.toUpperCase());
        client.request(httpMethod, port, host, path)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    // ========================================================================
    // Close
    // ========================================================================

    public static void close(NativeFrame frame){
        frame.beginAsync();
        get(frame).close()
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

}
