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
package org.siphonlab.ago.test;

import io.netty.handler.codec.http.HttpRequest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.ClassFile;
import org.siphonlab.ago.compiler.CompilationErrorsException;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.module.UnnamedProject;
import org.siphonlab.ago.lang.Trace;
import org.siphonlab.ago.runtime.vertx.VertxRunSpaceHost;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.siphonlab.ago.test.Util.run;

public class IOTest {

    @Test
    public void open_file() throws IOException, CompilationError, CompilationErrorsException {
        Util.run("io/filesystem.ago", "main#");
    }

    @Test
    public void backpressure() throws IOException, CompilationError, CompilationErrorsException {
        Util.run("io/backpressure.ago", "io_test.main#");
    }

    @Test @Disabled
    public void socket_server() throws IOException, CompilationError, CompilationErrorsException {
        Util.run("io/net_server.ago", "io_test.main#");
        System.in.read();
    }

    @Test @Disabled
    public void http_server() throws IOException, CompilationError, CompilationErrorsException {
        Util.run("io/http_server.ago", "io_test.main#");
        HttpServerRequest request;
//        Future<Buffer> body = request.body();
        System.in.read();
    }


}
