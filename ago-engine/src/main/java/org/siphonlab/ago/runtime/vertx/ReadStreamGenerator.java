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
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import static io.vertx.core.streams.impl.MessagePassingQueue.DEFAULT_HIGH_WATER_MARK;
import static io.vertx.core.streams.impl.MessagePassingQueue.DEFAULT_LOW_WATER_MARK;

public class ReadStreamGenerator<E> implements Handler<E> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ReadStreamGenerator.class);

    private final ReadStream<E> stream;

    private final ReentrantLock lock;

    private NativeFrame frame;

    private E overflow;
    private final Queue<E> queue;

    private long counter = 0;
    private boolean ended;

    protected final long highWaterMark;
    protected final long lowWaterMark;

    private NativeFrame receiverFrame;      // invoking next();

    public ReadStreamGenerator(ReadStream<E> stream, NativeFrame frame, long highWaterMark, long lowWaterMark) {
        this.stream = stream;
        this.frame = frame;
        this.highWaterMark = highWaterMark;
        this.lowWaterMark = lowWaterMark;
        this.queue = new ArrayDeque<>();
        this.lock = new ReentrantLock();
    }

    public ReadStreamGenerator(ReadStream<E> stream, NativeFrame frame, long highWaterMark) {
        this(stream, frame, highWaterMark, highWaterMark/2);
    }

    public ReadStreamGenerator(ReadStream<E> stream, NativeFrame frame) {
        this(stream, frame, DEFAULT_HIGH_WATER_MARK, DEFAULT_LOW_WATER_MARK);
    }

    // when invoke `hasNext()` `next()`, set frame
    void setFrame(NativeFrame frame) {
        this.frame = frame;
    }

    void init() {
        stream.handler(this);
        stream.exceptionHandler(ex -> {
            handleEnd();
            if(frame != null) {
                frame.raiseJavaException(frame.self(), ex, true);
            }
        });
        stream.endHandler(v -> handleEnd());

        stream.fetch(highWaterMark);
    }

    // native frame is a generator
    public void next(NativeFrame frame){
        lock.lock();
        this.setFrame(frame);
        E t;
        if(overflow != null) {
            t = overflow;
            overflow = null;
            counter --;
        } else if(!queue.isEmpty()){
            t = queue.poll();
            counter --;
        } else {
            t = null;
        }

        if(t != null) {
            if(LOGGER.isDebugEnabled()) LOGGER.debug("found %s, now count:%d".formatted(t, counter));
            if(counter <= lowWaterMark) {
                if(LOGGER.isDebugEnabled()) LOGGER.debug("found %s, now count:%d, drain".formatted(t, counter));

                stream.fetch(highWaterMark - counter);
            }
            lock.unlock();
            frame.yieldObject((Instance<?>) t);
        } else {
            if(this.ended) {
                if(LOGGER.isDebugEnabled()) LOGGER.debug("stream ended, and no more data in buffer, now count:%d, stop generator".formatted(counter));
                frame.getSlots().setBoolean(0, true);       // done
                frame.finishVoid();
            } else {
                if(LOGGER.isDebugEnabled()) LOGGER.debug("no more data in buffer, but stream not ended, now count:%d, wait new data".formatted(counter));
                receiverFrame = frame;
                frame.beginAsync();
            }
            lock.unlock();
        }

    }

    public void handle(E element) {
        if (element == null) {
            throw new NullPointerException();
        }
        lock.lock();
        if(receiverFrame != null){
            if(LOGGER.isDebugEnabled()) LOGGER.debug("new data come in, receiver is waiting, now count:%d, send data '%s'".formatted(counter, element));
            var f = receiverFrame;
            receiverFrame = null;
            lock.unlock();
            f.yieldObject((Instance<?>) element);
        } else {
            if (counter == 0) {
                counter = 1;
                overflow = element;
                if(LOGGER.isDebugEnabled()) LOGGER.debug("new data come in, overflow set to %s, count become %d".formatted(element, counter));
                if (highWaterMark == 1) {
                    stream.pause();
                }
            } else {
                queue.add(element);
                if ((++counter) == highWaterMark) {
                    stream.pause();
                }
                if(LOGGER.isDebugEnabled()) LOGGER.debug("new data %s come in, add to queue, count become %d".formatted(element, counter));
            }
            lock.unlock();
        }
    }

    private void handleEnd() {
        lock.lock();
        ended = true;
        stream.endHandler(null);
        stream.exceptionHandler(null);
        stream.handler(null);
        lock.unlock();
    }

    public void end(){
        if(!ended) stream.pause();
        handleEnd();
    }
}
