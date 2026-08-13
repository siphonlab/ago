/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class InboundMessageQueue<T> {

    final ReadStream<T> upstream;
    final ReentrantLock lock = new ReentrantLock();
    final Condition dataAvailable = this.lock.newCondition();
    final Condition bufferDrained = this.lock.newCondition();

    volatile T[] buffer;
    int head;
    int tail;
    int count;

    final int highWaterMark;
    final int lowWaterMark;

    boolean paused;
    boolean ended;
    boolean closed;

    Handler<Throwable> exceptionHandler;
    Handler<Void> endHandler;

    @SuppressWarnings("unchecked")
    public InboundMessageQueue(ReadStream<T> upstream, int highWaterMark, int lowWaterMark) {
        this.upstream = upstream;
        this.highWaterMark = highWaterMark;
        this.lowWaterMark = lowWaterMark;
        this.buffer = (T[]) new Object[highWaterMark * 2];
        this.head = 0;
        this.tail = 0;
        this.count = 0;
        this.paused = false;
        this.ended = false;
        this.closed = false;

        upstream.exceptionHandler(this::handleException);
        upstream.endHandler(v -> handleEnd());
        upstream.handler(this::handleData);
    }

    void handleData(T msg) {
        lock.lock();
        try {
            if (closed) return;

            while (count == buffer.length) {
                T[] newBuffer = (T[]) new Object[buffer.length * 2];
                System.arraycopy(buffer, head, newBuffer, 0, count);
                buffer = newBuffer;
                head = 0;
                tail = count;
            }

            buffer[tail] = msg;
            tail = (tail + 1) % buffer.length;
            count++;

            dataAvailable.signal();

            if (count >= highWaterMark && !paused) {
                upstream.pause();
                paused = true;
            }
        } finally {
            lock.unlock();
        }
    }

    void handleException(Throwable t) {
        if (exceptionHandler != null) {
            exceptionHandler.handle(t);
        } else {
                throw new RuntimeException(t);
            }
    }

    void handleEnd() {
        lock.lock();
        try {
            ended = true;
            dataAvailable.signalAll();
            bufferDrained.signalAll();
        } finally {
            lock.unlock();
        }
        if (endHandler != null) {
            endHandler.handle(null);
        }
    }

    public T read() {
        lock.lock();
        try {
            while (count == 0 && !ended && !closed) {
                dataAvailable.awaitUninterruptibly();
            }
            if (count == 0) return null;

            T msg = buffer[head];
            buffer[head] = null;
            head = (head + 1) % buffer.length;
            count--;

            if (count <= lowWaterMark && paused) {
                upstream.resume();
                paused = false;
            }

            return msg;
        } finally {
            lock.unlock();
        }
    }

    public T readNonBlocking() {
        lock.lock();
        try {
            if (count == 0) return null;

            T msg = buffer[head];
            buffer[head] = null;
            head = (head + 1) % buffer.length;
            count--;

            if (count <= lowWaterMark && paused) {
                upstream.resume();
                paused = false;
            }

            return msg;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return count == 0;
        } finally {
            lock.unlock();
        }
    }

    public boolean isEnded() {
        lock.lock();
        try {
            return ended;
        } finally {
            lock.unlock();
        }
    }

    public InboundMessageQueue<T> exceptionHandler(Handler<Throwable> handler) {
        this.exceptionHandler = handler;
        return this;
    }

    public InboundMessageQueue<T> endHandler(Handler<Void> handler) {
        this.endHandler = handler;
        return this;
    }

    public void close() {
        lock.lock();
        try {
            if (closed) return;
            closed = true;
            dataAvailable.signalAll();
            bufferDrained.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void drainUntilEmpty() {
        lock.lock();
        try {
            while (count > 0 && !closed) {
                bufferDrained.awaitUninterruptibly();
            }
        } finally {
            lock.unlock();
        }
    }

}
