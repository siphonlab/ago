package org.siphonlab.ago.study;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;

public class VertXTest {

    static void main() {
        Vertx vertx = Vertx.vertx();
        FileSystem fs = vertx.fileSystem();

        Future<Void> future = fs
                .createFile("/foo")
                .compose(v -> {
                    // When the file is created (fut1), execute this:
                    return fs.writeFile("/foo", Buffer.buffer());
                })
                .compose(v -> {
                    // When the file is written (fut2), execute this:
                    return fs.move("/foo", "/bar");
                });

    }
}
