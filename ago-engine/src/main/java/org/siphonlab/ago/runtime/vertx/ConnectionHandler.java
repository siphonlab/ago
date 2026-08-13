package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionHandler implements Handler<NetSocket> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionHandler.class);

    private final io.vertx.core.net.NetServer server;

    private final ReentrantLock lock = new ReentrantLock();

    private NetSocket overflow;
    private final Queue<NetSocket> queue = new ArrayDeque<>();

    private NativeFrame receiverFrame;

    private boolean initialized = false;

    public ConnectionHandler(io.vertx.core.net.NetServer server) {
        this.server = server;
    }

    synchronized void init() {
        if (!initialized) {
            server.connectHandler(this);
            initialized = true;
        }
    }

    // Called when a new connection arrives from Vert.x event loop
    @Override
    public void handle(NetSocket socket) {
        lock.lock();
        try {
            if (receiverFrame != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("new connection, acceptor is waiting, return immediately");
                }
                var f = receiverFrame;
                receiverFrame = null;
                lock.unlock();
                Instance<?> inst = ClassMapping.mapObject(socket, f);
                f.finishObjectAsync(inst);
            } else {
                if (overflow == null) {
                    overflow = socket;
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("new connection, no acceptor waiting, stored in overflow");
                    }
                } else {
                    queue.add(socket);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("new connection, overflow occupied, enqueued, size={}", queue.size());
                    }
                }
                lock.unlock();
            }
        } catch (Exception e) {
            lock.unlock();
            throw e;
        }
    }

    // Called when accept() is invoked from ago code
    void accept(NativeFrame frame) {
        lock.lock();
        try {
            NetSocket socket;
            if (overflow != null) {
                socket = overflow;
                overflow = null;
            } else if (!queue.isEmpty()) {
                socket = queue.poll();
            } else {
                socket = null;
            }

            if (socket != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("acceptor got a connection from buffer");
                }
                lock.unlock();
                Instance<?> inst = ClassMapping.mapObject(socket, frame);
                frame.finishObject(inst);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("no connection available, acceptor waits asynchronously");
                }
                receiverFrame = frame;
                frame.beginAsync();
                lock.unlock();
            }
        } catch (Exception e) {
            lock.unlock();
            throw e;
        }
    }


}
