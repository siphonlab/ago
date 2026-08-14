package org.siphonlab.ago.runtime.vertx;

import io.netty.handler.logging.ByteBufFormat;
import org.siphonlab.ago.native_.NativeFrame;

import java.util.concurrent.TimeUnit;

public class HttpServerOptions {

    public static void create(NativeFrame frame){
        frame.getParentScope().setNativePayload(new io.vertx.core.http.HttpServerOptions());
        frame.finishVoid();
    }

    private static io.vertx.core.http.HttpServerOptions get(NativeFrame frame){
        return (io.vertx.core.http.HttpServerOptions) frame.getParentScope().getNativePayload();
    }

    // ========================================================================
    // HttpServerOptions own properties
    // ========================================================================

    public static void port_get(NativeFrame frame){
        frame.finishInt(get(frame).getPort());
    }
    public static void port_set(NativeFrame frame, int value){
        get(frame).setPort(value);
        frame.finishVoid();
    }

    public static void host_get(NativeFrame frame){
        frame.finishString(get(frame).getHost());
    }
    public static void host_set(NativeFrame frame, String value){
        get(frame).setHost(value);
        frame.finishVoid();
    }

    public static void acceptBacklog_get(NativeFrame frame){
        frame.finishInt(get(frame).getAcceptBacklog());
    }
    public static void acceptBacklog_set(NativeFrame frame, int value){
        get(frame).setAcceptBacklog(value);
        frame.finishVoid();
    }

    // -- HTTP-specific properties ---------------------------------------------

    public static void decompressionSupported_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isDecompressionSupported());
    }
    public static void decompressionSupported_set(NativeFrame frame, boolean value){
        get(frame).setDecompressionSupported(value);
        frame.finishVoid();
    }

    public static void handle100ContinueAutomatically_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isHandle100ContinueAutomatically());
    }
    public static void handle100ContinueAutomatically_set(NativeFrame frame, boolean value){
        get(frame).setHandle100ContinueAutomatically(value);
        frame.finishVoid();
    }

    public static void http2_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isHttp2ClearTextEnabled());
    }
    public static void http2_set(NativeFrame frame, boolean value){
        get(frame).setHttp2ClearTextEnabled(value);
        frame.finishVoid();
    }

    public static void http2ConnectionWindowSize_get(NativeFrame frame){
        frame.finishInt(get(frame).getHttp2ConnectionWindowSize());
    }
    public static void http2ConnectionWindowSize_set(NativeFrame frame, int value){
        get(frame).setHttp2ConnectionWindowSize(value);
        frame.finishVoid();
    }

    public static void compressionLevel_get(NativeFrame frame){
        frame.finishInt(get(frame).getCompressionLevel());
    }
    public static void compressionLevel_set(NativeFrame frame, int value){
        get(frame).setCompressionLevel(value);
        frame.finishVoid();
    }


    public static void maxFormFields_get(NativeFrame frame){
        frame.finishInt(get(frame).getMaxFormFields());
    }

    public static void maxFormFields_set(NativeFrame frame, int value){
        get(frame).setMaxFormFields(value);
        frame.finishVoid();
    }


    // -- SSL/TLS (inherited from NetServerOptions / TCPSSLOptions) -------------

    public static void ssl_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isSsl());
    }
    public static void ssl_set(NativeFrame frame, boolean value){
        get(frame).setSsl(value);
        frame.finishVoid();
    }

    public static void useAlpn_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isUseAlpn());
    }
    public static void useAlpn_set(NativeFrame frame, boolean value){
        get(frame).setUseAlpn(value);
        frame.finishVoid();
    }

    // -- Timeout properties ---------------------------------------------------

    public static void idleTimeout_get(NativeFrame frame){
        frame.finishInt(get(frame).getIdleTimeout());
    }
    public static void idleTimeout_set(NativeFrame frame, int value){
        get(frame).setIdleTimeout(value);
        frame.finishVoid();
    }

    public static void idleTimeoutUnit_get(NativeFrame frame){
        frame.finishString(get(frame).getIdleTimeoutUnit().name());
    }
    public static void idleTimeoutUnit_set(NativeFrame frame, String value){
        get(frame).setIdleTimeoutUnit(TimeUnit.valueOf(value));
        frame.finishVoid();
    }

    // -- NetworkOptions properties (inherited) ---------------------------------

    public static void sendBufferSize_get(NativeFrame frame){
        frame.finishInt(get(frame).getSendBufferSize());
    }
    public static void sendBufferSize_set(NativeFrame frame, int value){
        get(frame).setSendBufferSize(value);
        frame.finishVoid();
    }

    public static void receiveBufferSize_get(NativeFrame frame){
        frame.finishInt(get(frame).getReceiveBufferSize());
    }
    public static void receiveBufferSize_set(NativeFrame frame, int value){
        get(frame).setReceiveBufferSize(value);
        frame.finishVoid();
    }

    public static void reuseAddress_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isReuseAddress());
    }
    public static void reuseAddress_set(NativeFrame frame, boolean value){
        get(frame).setReuseAddress(value);
        frame.finishVoid();
    }

    public static void logActivity_get(NativeFrame frame){
        frame.finishBoolean(get(frame).getLogActivity());
    }
    public static void logActivity_set(NativeFrame frame, boolean value){
        get(frame).setLogActivity(value);
        frame.finishVoid();
    }

    // -- Activity log data format ----------------------------------------------

    public static void activityLogDataFormat_get(NativeFrame frame){
        frame.finishString(get(frame).getActivityLogDataFormat().name());
    }
    public static void activityLogDataFormat_set(NativeFrame frame, String value){
        get(frame).setActivityLogDataFormat(ByteBufFormat.valueOf(value));
        frame.finishVoid();
    }

    // -- TCP options (inherited) -----------------------------------------------

    public static void tcpNoDelay_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isTcpNoDelay());
    }
    public static void tcpNoDelay_set(NativeFrame frame, boolean value){
        get(frame).setTcpNoDelay(value);
        frame.finishVoid();
    }

    public static void tcpKeepAlive_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isTcpKeepAlive());
    }
    public static void tcpKeepAlive_set(NativeFrame frame, boolean value){
        get(frame).setTcpKeepAlive(value);
        frame.finishVoid();
    }

}
