package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

public class HttpServer {

    // ========================================================================
    // HttpServer operations
    // ========================================================================

    public static void create(NativeFrame frame){
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        io.vertx.core.http.HttpServer server = host.getVertx().createHttpServer();
        Instance<?> inst = frame.getParentScope();
        inst.setNativePayload(server);

        var connectionHandlerClz = inst.getAgoClass().getAgoClass().findChild("ConnectionHandler");
        var connHandlerInst = frame.getAgoEngine().createNativeInstance(null, connectionHandlerClz, frame.getRunSpace());
        HttpConnectionHandler connectionHandler = new HttpConnectionHandler(server);
        connHandlerInst.setNativePayload(connectionHandler);
        inst.setObjectField("connectionHandler", connHandlerInst);
        connectionHandler.init();

        frame.finishVoid();
    }

    public static void createWithOptions(NativeFrame frame, Instance<?> options){
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        HttpServerOptions opts = (HttpServerOptions) options.getNativePayload();
        io.vertx.core.http.HttpServer server = host.getVertx().createHttpServer(opts);
        Instance<?> inst = frame.getParentScope();
        inst.setNativePayload(server);

        var connectionHandlerClz = inst.getAgoClass().getAgoClass().findChild("ConnectionHandler");
        var connHandlerInst = frame.getAgoEngine().createNativeInstance(null, connectionHandlerClz, frame.getRunSpace());
        HttpConnectionHandler connectionHandler = new HttpConnectionHandler(server);
        connHandlerInst.setNativePayload(connectionHandler);
        inst.setObjectField("connectionHandler", connHandlerInst);
        connectionHandler.init();

        frame.finishVoid();
    }

    public static void listen(NativeFrame frame, int port){
        frame.beginAsync();
        io.vertx.core.http.HttpServer server = (io.vertx.core.http.HttpServer) frame.getParentScope().getNativePayload();
        server.listen(port)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void acceptRequest(NativeFrame frame){
        var handlerInst = frame.getParentScope().getSlots().getObject(0);
        var handler = (HttpConnectionHandler)handlerInst.getNativePayload();
        handler.accept(frame);
    }

    // ========================================================================
    // Close
    // ========================================================================

    public static void close(NativeFrame frame){
        frame.beginAsync();
        io.vertx.core.http.HttpServer server = (io.vertx.core.http.HttpServer) frame.getParentScope().getNativePayload();
        server.close()
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

}
