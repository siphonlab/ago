package org.siphonlab.ago.runtime.vertx;

import org.siphonlab.ago.native_.NativeFrame;

public class HttpClientOptions {

    public static void create(NativeFrame frame){
        frame.getParentScope().setNativePayload(new io.vertx.core.http.HttpClientOptions());
        frame.finishVoid();
    }

    private static io.vertx.core.http.HttpClientOptions get(NativeFrame frame){
        return (io.vertx.core.http.HttpClientOptions) frame.getParentScope().getNativePayload();
    }

    // ========================================================================
    // Default port / host
    // ========================================================================

    public static void defaultPort_get(NativeFrame frame){
        frame.finishInt(get(frame).getDefaultPort());
    }
    public static void defaultPort_set(NativeFrame frame, int value){
        get(frame).setDefaultPort(value);
        frame.finishVoid();
    }

    public static void defaultHost_get(NativeFrame frame){
        String host = get(frame).getDefaultHost();
        if (host == null) {
            frame.finishObject(null);
        } else {
            frame.finishString(host);
        }
    }
    public static void defaultHost_set(NativeFrame frame, String value){
        get(frame).setDefaultHost(value);
        frame.finishVoid();
    }

    // ========================================================================
    // Keep-alive and connection pool
    // ========================================================================

    public static void keepAlive_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isKeepAlive());
    }
    public static void keepAlive_set(NativeFrame frame, boolean value){
        get(frame).setKeepAlive(value);
        frame.finishVoid();
    }

    public static void keepAliveTimeout_get(NativeFrame frame){
        frame.finishInt(get(frame).getKeepAliveTimeout());
    }
    public static void keepAliveTimeout_set(NativeFrame frame, int value){
        get(frame).setKeepAliveTimeout(value);
        frame.finishVoid();
    }

    // ========================================================================
    // Timeout properties
    // ========================================================================

    public static void idleTimeout_get(NativeFrame frame){
        frame.finishInt(get(frame).getIdleTimeout());
    }
    public static void idleTimeout_set(NativeFrame frame, int value){
        get(frame).setIdleTimeout(value);
        frame.finishVoid();
    }

    public static void connectTimeout_get(NativeFrame frame){
        frame.finishInt(get(frame).getConnectTimeout());
    }
    public static void connectTimeout_set(NativeFrame frame, int value){
        get(frame).setConnectTimeout(value);
        frame.finishVoid();
    }

    // ========================================================================
    // SSL / TLS
    // ========================================================================

    public static void ssl_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isSsl());
    }
    public static void ssl_set(NativeFrame frame, boolean value){
        get(frame).setSsl(value);
        frame.finishVoid();
    }

    public static void trustAll_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isTrustAll());
    }
    public static void trustAll_set(NativeFrame frame, boolean value){
        get(frame).setTrustAll(value);
        frame.finishVoid();
    }

    public static void verifyHost_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isVerifyHost());
    }
    public static void verifyHost_set(NativeFrame frame, boolean value){
        get(frame).setVerifyHost(value);
        frame.finishVoid();
    }

    // ========================================================================
    // Decompression
    // ========================================================================

    public static void decompression_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isDecompressionSupported());
    }
    public static void decompression_set(NativeFrame frame, boolean value){
        get(frame).setDecompressionSupported(value);
        frame.finishVoid();
    }

    // ========================================================================
    // Pipelining
    // ========================================================================

    public static void pipelining_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isPipelining());
    }
    public static void pipelining_set(NativeFrame frame, boolean value){
        get(frame).setPipelining(value);
        frame.finishVoid();
    }

    public static void pipeliningLimit_get(NativeFrame frame){
        frame.finishInt(get(frame).getPipeliningLimit());
    }
    public static void pipeliningLimit_set(NativeFrame frame, int value){
        get(frame).setPipeliningLimit(value);
        frame.finishVoid();
    }

}
