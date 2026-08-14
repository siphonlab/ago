package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class HttpConnectionHandler extends QueuedHandler<HttpServerRequest> {

    private final io.vertx.core.http.HttpServer server;

    public HttpConnectionHandler(io.vertx.core.http.HttpServer server) {
        this.server = server;
    }

    public synchronized void init() {
        if (!initialized) {
            server.requestHandler(this);
            initialized = true;
        }
    }

}
