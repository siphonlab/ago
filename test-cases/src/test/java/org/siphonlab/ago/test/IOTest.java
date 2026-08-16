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
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
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
    public void http_server_test1() throws IOException, CompilationError, CompilationErrorsException {
        Util.run("io/http_server_test1.ago", "io_test.main#");
        System.in.read();
    }

    @Test @Disabled
    public void http_server_test2() throws IOException, CompilationError, CompilationErrorsException {
        Util.run("io/http_server_test2.ago", "io_test.main#");
        System.in.read();
    }

    @Test @Disabled
    public void http_client_test1() throws IOException, CompilationError, CompilationErrorsException {
        Util.run("io/http_client_test1.ago", "main#");
        System.in.read();
    }

    public static void main(String[] args) throws IOException {
//        Vertx vertx = Vertx.vertx();
//        var router = Router.router(vertx);
//        router.route().handler(BodyHandler.create());
//
//        router.post("/echo").handler(new Handler<RoutingContext>() {
//            @Override
//            public void handle(RoutingContext event) {
//                var s = event.body().asString();
//                event.end(s);
//            }
//        });
//        vertx.createHttpServer().requestHandler(router).listen(8080);

        var httpClient = Vertx.vertx().createHttpClient();

// 1. 发起请求
        Future<HttpClientRequest> req = httpClient.request(HttpMethod.GET, 8080, "127.0.0.1", "/hello");

        req.compose(request -> {
                    // 2. 发送请求体并结束请求，同时立即把流转接到 response 的 Future 上
                    return request.end().compose( _ -> request.response());
                })
                .compose(response -> {
                    // 3. 【最核心的关键点】
                    // 此时 response 刚刚就绪，在同一个事件循环内，**必须立即**调用 body()。
                    // compose 会等待 bodyFuture 完成，并把最终的 Buffer 传给下一步。
                    return response.body();
                })
                .onSuccess(buffer -> {
                    // 4. 完美、安全地拿到数据
                    System.out.println("底层 HttpClient 100% 抓住 body: " + buffer.toString());
                })
                .onFailure(err -> {
                    err.printStackTrace();
                });


//        httpClient.request(HttpMethod.POST, 80, "example.com", "/hello")
//                .compose(request -> request.send("hello world"))
//                .onSuccess(response -> {
//                    // 必须在入口处立刻调用 body() 转换为 Future
//                    Future<Buffer> bodyFuture = response.body();
//
//                    bodyFuture.onSuccess(buffer -> {
//                        System.out.println("底层 HttpClient 抓住 body: " + buffer.toString());
//                    });
//                });

        System.in.read();
    }


}
