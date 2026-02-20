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


import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Route;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServiceRequestContext;

import java.io.IOException;

public class ArmeriaTest {
    public static void main(String[] args) throws IOException {
        Server.builder().port(8080, SessionProtocol.HTTP)
                .service(Route.builder().path("/hello/{name}").build(), new HttpService() {
                    @Override
                    public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
                        String name = ctx.pathParam("name");
                        String greeting = ctx.queryParam("greeting");
//                        Multipart.from(req).aggregate().thenApply(aggregatedMultipart -> {
//                            for (AggregatedBodyPart part : aggregatedMultipart.bodyParts()) {
//                                String name = part.headers().contentDisposition().name();
//                                String filename = part.headers().contentDisposition().filename();
//                                HttpData content = part.content();
//
//                                if (filename != null) {
//                                    // 处理文件部分
//                                    try (FileOutputStream fos = new FileOutputStream("uploaded_" + filename)) {
//                                        fos.write(content.array()); // 写入文件
//                                        //response.append("File: ").append(filename).append(", size: ").append(content.length()).append(" bytes\n");
//                                    } catch (IOException e) {
//                                        return HttpResponse.of(500, "Failed to save file: " + e.getMessage());
//                                    }
//                                } else {
//                                    // 处理非文件字段（如表单字段）
//                                    String value = content.toStringUtf8();
//                                    //response.append("Field: ").append(name).append(" = ").append(value).append("\n");
//                                }
//
//                            }
//                        });
                        return HttpResponse.of("Hello, %s! %s", name, greeting != null ? greeting : "Welcome!");
                    }
                })

//                .service(GrpcService.builder().addService(myGrpcServiceImpl).build())
//                .service("/api/thrift", ThriftService.of(myThriftServiceImpl))
//                .service("prefix:/files", FileService.of(new File("/var/www")))
//                .service("/monitor/l7check", HealthCheckService.of())
                .build()
                .start();
        System.in.read();
    }
}
