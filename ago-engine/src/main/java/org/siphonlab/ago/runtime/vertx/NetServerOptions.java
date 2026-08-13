package org.siphonlab.ago.runtime.vertx;

import io.netty.handler.logging.ByteBufFormat;
import io.netty.handler.ssl.ClientAuth;
import io.vertx.core.buffer.Buffer;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NetServerOptions {

    // ========================================================================
    // Constructor
    // ========================================================================

    public static void create(NativeFrame frame){
        frame.getParentScope().setNativePayload(new io.vertx.core.net.NetServerOptions());
        frame.finishVoid();
    }

    // helper -----------------------------------------------------------------

    private static io.vertx.core.net.NetServerOptions get(NativeFrame frame){
        return (io.vertx.core.net.NetServerOptions) frame.getParentScope().getNativePayload();
    }

    // ========================================================================
    // NetServerOptions own properties
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

    public static void useProxyProtocol_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isUseProxyProtocol());
    }
    public static void useProxyProtocol_set(NativeFrame frame, boolean value){
        get(frame).setUseProxyProtocol(value);
        frame.finishVoid();
    }

    public static void proxyProtocolTimeout_get(NativeFrame frame){
        frame.finishLong(get(frame).getProxyProtocolTimeout());
    }
    public static void proxyProtocolTimeout_set(NativeFrame frame, long value){
        get(frame).setProxyProtocolTimeout(value);
        frame.finishVoid();
    }

    public static void proxyProtocolTimeoutUnit_get(NativeFrame frame){
        frame.finishString(get(frame).getProxyProtocolTimeoutUnit().name());
    }
    public static void proxyProtocolTimeoutUnit_set(NativeFrame frame, String value){
        get(frame).setProxyProtocolTimeoutUnit(TimeUnit.valueOf(value));
        frame.finishVoid();
    }

    public static void registerWriteHandler_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isRegisterWriteHandler());
    }
    public static void registerWriteHandler_set(NativeFrame frame, boolean value){
        get(frame).setRegisterWriteHandler(value);
        frame.finishVoid();
    }

    public static void fileRegionEnabled_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isFileRegionEnabled());
    }

    // ========================================================================
    // SSL/TLS properties (from TCPSSLOptions)
    // ========================================================================

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

    public static void sslHandshakeTimeout_get(NativeFrame frame){
        frame.finishLong(get(frame).getSslHandshakeTimeout());
    }
    public static void sslHandshakeTimeout_set(NativeFrame frame, long value){
        get(frame).setSslHandshakeTimeout(value);
        frame.finishVoid();
    }

    public static void sslHandshakeTimeoutUnit_get(NativeFrame frame){
        frame.finishString(get(frame).getSslHandshakeTimeoutUnit().name());
    }
    public static void sslHandshakeTimeoutUnit_set(NativeFrame frame, String value){
        get(frame).setSslHandshakeTimeoutUnit(TimeUnit.valueOf(value));
        frame.finishVoid();
    }

    // ========================================================================
    // Client auth / SNI (from ServerSSLOptions)
    // ========================================================================
// TODO
//    public static void clientAuth_get(NativeFrame frame){
//        frame.finishString(get(frame).getClientAuth().name());
//    }
//    public static void clientAuth_set(NativeFrame frame, String value){
//        get(frame).setClientAuth(ClientAuth.valueOf(value));
//        frame.finishVoid();
//    }

    public static void sni_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isSni());
    }
    public static void sni_set(NativeFrame frame, boolean value){
        get(frame).setSni(value);
        frame.finishVoid();
    }

    // ========================================================================
    // Timeout properties (from TCPSSLOptions)
    // ========================================================================

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

    public static void readIdleTimeout_get(NativeFrame frame){
        frame.finishInt(get(frame).getReadIdleTimeout());
    }
    public static void readIdleTimeout_set(NativeFrame frame, int value){
        get(frame).setReadIdleTimeout(value);
        frame.finishVoid();
    }

    public static void writeIdleTimeout_get(NativeFrame frame){
        frame.finishInt(get(frame).getWriteIdleTimeout());
    }
    public static void writeIdleTimeout_set(NativeFrame frame, int value){
        get(frame).setWriteIdleTimeout(value);
        frame.finishVoid();
    }

    // ========================================================================
    // TCP socket options (from TCPSSLOptions)
    // ========================================================================

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

    public static void soLinger_get(NativeFrame frame){
        frame.finishInt(get(frame).getSoLinger());
    }
    public static void soLinger_set(NativeFrame frame, int value){
        get(frame).setSoLinger(value);
        frame.finishVoid();
    }

    public static void tcpFastOpen_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isTcpFastOpen());
    }
    public static void tcpFastOpen_set(NativeFrame frame, boolean value){
        get(frame).setTcpFastOpen(value);
        frame.finishVoid();
    }

    public static void tcpCork_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isTcpCork());
    }
    public static void tcpCork_set(NativeFrame frame, boolean value){
        get(frame).setTcpCork(value);
        frame.finishVoid();
    }

    public static void tcpQuickAck_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isTcpQuickAck());
    }
    public static void tcpQuickAck_set(NativeFrame frame, boolean value){
        get(frame).setTcpQuickAck(value);
        frame.finishVoid();
    }

    public static void tcpUserTimeout_get(NativeFrame frame){
        frame.finishInt(get(frame).getTcpUserTimeout());
    }
    public static void tcpUserTimeout_set(NativeFrame frame, int value){
        get(frame).setTcpUserTimeout(value);
        frame.finishVoid();
    }

    public static void tcpKeepAliveIdleSeconds_get(NativeFrame frame){
        frame.finishInt(get(frame).getTcpKeepAliveIdleSeconds());
    }
    public static void tcpKeepAliveIdleSeconds_set(NativeFrame frame, int value){
        get(frame).setTcpKeepAliveIdleSeconds(value);
        frame.finishVoid();
    }

    public static void tcpKeepAliveCount_get(NativeFrame frame){
        frame.finishInt(get(frame).getTcpKeepAliveCount());
    }
    public static void tcpKeepAliveCount_set(NativeFrame frame, int value){
        get(frame).setTcpKeepAliveCount(value);
        frame.finishVoid();
    }

    public static void tcpKeepAliveIntervalSeconds_get(NativeFrame frame){
        frame.finishInt(get(frame).getTcpKeepAliveIntervalSeconds());
    }
    public static void tcpKeepAliveIntervalSeconds_set(NativeFrame frame, int value){
        get(frame).setTcpKeepAliveIntervalSeconds(value);
        frame.finishVoid();
    }

    // ========================================================================
    // Cipher suites (from TCPSSLOptions)
    // ========================================================================

    public static void enabledCipherSuites_get(NativeFrame frame){
        Set<String> set = get(frame).getEnabledCipherSuites();
        if(set == null || set.isEmpty()){
            frame.finishObject(null);
        } else {
            var listClz = frame.getAgoEngine().getClass("lang.collection.ArrayList");
            Instance<?> list = frame.getAgoEngine().createNativeInstance(null, listClz, frame.getRunSpace());
            frame.finishObject(list);
        }
    }

    public static void addEnabledCipherSuite(NativeFrame frame, String value){
        get(frame).addEnabledCipherSuite(value);
        frame.finishVoid();
    }

    public static void removeEnabledCipherSuite(NativeFrame frame, String value){
        get(frame).removeEnabledCipherSuite(value);
        frame.finishVoid();
    }

    // ========================================================================
    // Enabled secure transport protocols (from TCPSSLOptions)
    // ========================================================================

    public static void enabledSecureTransportProtocols_get(NativeFrame frame){
        Set<String> set = get(frame).getEnabledSecureTransportProtocols();
        if(set == null || set.isEmpty()){
            frame.finishObject(null);
        } else {
            var listClz = frame.getAgoEngine().getClass("lang.collection.ArrayList");
            Instance<?> list = frame.getAgoEngine().createNativeInstance(null, listClz, frame.getRunSpace());
            frame.finishObject(list);
        }
    }

    public static void addEnabledSecureTransportProtocol(NativeFrame frame, String value){
        get(frame).addEnabledSecureTransportProtocol(value);
        frame.finishVoid();
    }

    public static void removeEnabledSecureTransportProtocol(NativeFrame frame, String value){
        get(frame).removeEnabledSecureTransportProtocol(value);
        frame.finishVoid();
    }

    // ========================================================================
    // CRL (from TCPSSLOptions)
    // ========================================================================

    public static void addCrlPath(NativeFrame frame, String value){
        get(frame).addCrlPath(value);
        frame.finishVoid();
    }

    public static void addCrlValue(NativeFrame frame, Instance<?> value){
        Buffer buffer = (Buffer) value.getNativePayload();
        get(frame).addCrlValue(buffer);
        frame.finishVoid();
    }

    // ========================================================================
    // NetworkOptions properties (top of hierarchy)
    // ========================================================================

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

    public static void reusePort_get(NativeFrame frame){
        frame.finishBoolean(get(frame).isReusePort());
    }
    public static void reusePort_set(NativeFrame frame, boolean value){
        get(frame).setReusePort(value);
        frame.finishVoid();
    }

    public static void trafficClass_get(NativeFrame frame){
        frame.finishInt(get(frame).getTrafficClass());
    }
    public static void trafficClass_set(NativeFrame frame, int value){
        get(frame).setTrafficClass(value);
        frame.finishVoid();
    }

    public static void logActivity_get(NativeFrame frame){
        frame.finishBoolean(get(frame).getLogActivity());
    }
    public static void logActivity_set(NativeFrame frame, boolean value){
        get(frame).setLogActivity(value);
        frame.finishVoid();
    }

    // ========================================================================
    // Activity log data format (from NetworkOptions) — ByteBufFormat enum
    // ========================================================================

    public static void activityLogDataFormat_get(NativeFrame frame){
        frame.finishString(get(frame).getActivityLogDataFormat().name());
    }
    public static void activityLogDataFormat_set(NativeFrame frame, String value){
        get(frame).setActivityLogDataFormat(ByteBufFormat.valueOf(value));
        frame.finishVoid();
    }

}
