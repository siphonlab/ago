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

public abstract class QueuedHandler<E> implements Handler<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedHandler.class);

    private final ReentrantLock lock = new ReentrantLock();

    private E overflow;
    private final Queue<E> queue = new ArrayDeque<>();

    private NativeFrame receiverFrame;

    protected boolean initialized = false;

    @Override
    public void handle(E event) {
        lock.lock();
        try {
            if (receiverFrame != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("new connection, acceptor is waiting, return immediately");
                }
                var f = receiverFrame;
                receiverFrame = null;
                lock.unlock();
                Instance<?> inst = ClassMapping.mapObject(event, f);
                f.finishObjectAsync(inst);
            } else {
                if (overflow == null) {
                    overflow = event;
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("new connection, no acceptor waiting, stored in overflow");
                    }
                } else {
                    queue.add(event);
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

    public abstract void init();


    // Called when accept() is invoked from ago code
    void accept(NativeFrame frame) {
        lock.lock();
        try {
            E socket;
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
