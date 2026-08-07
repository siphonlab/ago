package org.siphonlab.ago.test;

import com.linecorp.armeria.internal.shaded.bouncycastle.math.raw.Nat;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.core.Handler;
import io.vertx.core.streams.Pipe;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.native_.NativeFrame;

public class BackpressureDemo extends AbstractVerticle {

    @Override
    public void start() {
        // 1. 创建一个会疯狂产生数据的上游 ReadStream
        FastProducer producer = new FastProducer();

        // 2. 创建一个写得很慢的下游 WriteStream
        SlowConsumer consumer = new SlowConsumer();

        // 3. 用 pipe 连接（自动处理背压）
        producer.pipeTo(consumer)
                .onSuccess(v -> System.out.println(">>> 全部传输完成"))
                .onFailure(err -> System.err.println("出错: " + err.getMessage()));

        // 启动生产
        producer.start();
    }

    // ==================== 上游：快速生产者 ====================
    public static class FastProducer implements ReadStream<Buffer> {
        private Handler<Buffer> dataHandler;
        private Handler<Void> endHandler;
        private Handler<Throwable> exceptionHandler;
        private boolean paused = false;
        private boolean ended = false;
        private long counter = 0;
        private long timerId = -1;

        public void start() {
            // 每 10ms 尝试发一次数据（很快）
            timerId = Vertx.currentContext().owner().setPeriodic(10, id -> {
                if (ended) return;

                if (paused) {
                    // 被下游暂停了，这里什么都不做（这就是背压生效的地方）
                    System.out.println("[上游] 已暂停，等待下游消化...");
                    return;
                }

                counter++;
                Buffer data = Buffer.buffer("数据-" + counter + " ");
                System.out.println("[上游] 发送: " + data.toString() + " (第 " + counter + " 条)");

                if (dataHandler != null) {
                    dataHandler.handle(data);
                }

                // 发 30 条就结束
                if (counter >= 30) {
                    ended = true;
                    Vertx.currentContext().owner().cancelTimer(timerId);
                    if (endHandler != null) {
                        endHandler.handle(null);
                    }
                    System.out.println("[上游] 生产结束");
                }
            });
        }

        @Override
        public ReadStream<Buffer> handler(Handler<Buffer> handler) {
            this.dataHandler = handler;
            return this;
        }

        @Override
        public ReadStream<Buffer> pause() {
            paused = true;
            System.out.println(">>> [背压] 上游被 pause 了");
            return this;
        }

        @Override
        public ReadStream<Buffer> resume() {
            paused = false;
            System.out.println(">>> [背压] 上游被 resume 了");
            return this;
        }

        @Override
        public ReadStream<Buffer> fetch(long amount) {
            // 简单实现，这里用 pause/resume 模式即可
            return this.resume();
        }

        @Override
        public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
            this.endHandler = endHandler;
            return this;
        }

        @Override
        public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
            this.exceptionHandler = handler;
            return this;
        }
    }

    // ==================== 下游：慢速消费者 ====================
    public static class SlowConsumer implements WriteStream<Buffer> {
        private Handler<Void> drainHandler;
        private Handler<Throwable> exceptionHandler;
        private boolean writeQueueFull = false;
        private int pending = 0;
        private final int MAX_PENDING = 3; // 缓冲区最多只允许 3 条

        @Override
        public Future<Void> write(Buffer data) {
            pending++;
            System.out.println("[下游] 收到: " + data.toString() + "  (当前积压: " + pending + ")");

            // 模拟写磁盘很慢：故意延迟 200ms 才真正“写完”
            Vertx.currentContext().owner().setTimer(200, id -> {
                pending--;
                System.out.println("[下游] 写完一条，剩余积压: " + pending);

                // 如果之前队列满了，现在有空位了，就通知上游可以继续
                if (writeQueueFull && pending < MAX_PENDING) {
                    writeQueueFull = false;
                    if (drainHandler != null) {
                        System.out.println(">>> [背压] 下游有空位，触发 drainHandler");
                        drainHandler.handle(null);
                    }
                }
            });

            // 如果积压达到上限，标记队列已满
            if (pending >= MAX_PENDING) {
                writeQueueFull = true;
                System.out.println(">>> [背压] 下游队列已满！");
            }

            return Future.succeededFuture();
        }

        @Override
        public boolean writeQueueFull() {
            return writeQueueFull;
        }

        @Override
        public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
            this.drainHandler = handler;
            return this;
        }

        @Override
        public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
            this.exceptionHandler = handler;
            return this;
        }

        @Override
        public Future<Void> end() {
            System.out.println("[下游] end() 被调用");
            return Future.succeededFuture();
        }

        @Override
        public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
            return this;
        }

    }

    public static void Test_FastProducer(NativeFrame frame) {
        AgoEngine agoEngine = frame.getAgoEngine();
        var inst = agoEngine.createNativeInstance(null, agoEngine.getClass("io_test.FastProducer"), frame.getRunSpace());
        inst.setNativePayload(new FastProducer());
        frame.finishObject(inst);
    }

    public static void Test_SlowConsumer(NativeFrame frame) {
        AgoEngine agoEngine = frame.getAgoEngine();
        var inst = agoEngine.createNativeInstance(null, agoEngine.getClass("io_test.SlowConsumer"), frame.getRunSpace());
        inst.setNativePayload(new SlowConsumer());
        frame.finishObject(inst);
    }

    public static void Test_startProducer(NativeFrame frame) {
        var producer = (FastProducer)frame.getParentScope().getNativePayload();
        producer.start();
        frame.finishVoid();
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new BackpressureDemo());
    }
}