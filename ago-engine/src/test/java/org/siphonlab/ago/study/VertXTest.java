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
