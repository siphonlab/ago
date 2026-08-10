package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.net.NetSocket;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

import java.util.concurrent.ConcurrentHashMap;

public class NetServer {

    // ========================================================================
    // NetServer operations
    // ========================================================================

    public static void create(NativeFrame frame){
        VertxRunSpaceHost host = (VertxRunSpaceHost) frame.getRunSpace().getRunSpaceHost();
        io.vertx.core.net.NetServer server = host.getVertx().createNetServer();
        Instance<?> inst = frame.getParentScope();
        inst.setNativePayload(server);

        var connectionHandlerClz = inst.getAgoClass().getAgoClass().findChild("ConnectionHandler");
        var connHandlerInst = frame.getAgoEngine().createNativeInstance(null, connectionHandlerClz, frame.getRunSpace());
        ConnectionHandler connectionHandler = new ConnectionHandler(server);
        connHandlerInst.setNativePayload(connectionHandler);
        inst.setObjectField("connectionHandler", connHandlerInst);
        connectionHandler.init();

        frame.finishVoid();
    }

    public static void listen(NativeFrame frame, int port){
        frame.beginAsync();
        io.vertx.core.net.NetServer server = (io.vertx.core.net.NetServer) frame.getParentScope().getNativePayload();
        server.listen(port)
                .onComplete(new VertXNativeFrameHandler<>(frame, "io.IOException"));
    }

    public static void accept(NativeFrame frame){
        var handlerInst = frame.getParentScope().getSlots().getObject(0);       // ConnectionHandler instance
        var handler = (ConnectionHandler)handlerInst.getNativePayload();
        handler.accept(frame);
    }

    // ========================================================================
    // Socket operations
    // ========================================================================

    public static void Socket_pause(NativeFrame frame){
        NetSocket socket = (NetSocket) frame.getParentScope().getNativePayload();
        socket.pause();
        frame.finishObject(frame.getParentScope());
    }

    public static void Socket_resume(NativeFrame frame){
        NetSocket socket = (NetSocket) frame.getParentScope().getNativePayload();
        socket.resume();
        frame.finishObject(frame.getParentScope());
    }

    public static void Socket_fetch(NativeFrame frame, long amount){
        NetSocket socket = (NetSocket) frame.getParentScope().getNativePayload();
        socket.fetch(amount);
        frame.finishObject(frame.getParentScope());
    }

    public static void Socket_close(NativeFrame frame){
        frame.beginAsync();
        NetSocket socket = (NetSocket) frame.getParentScope().getNativePayload();
        socket.close();
        frame.finishVoid();
    }
}
