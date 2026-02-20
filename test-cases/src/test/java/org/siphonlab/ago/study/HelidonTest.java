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

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.*;

public class HelidonTest {
    public static void main(String[] args) {
        // helidon4 is based on virtual thread now
        HttpRouting.Builder routing = HttpRouting.builder().register("/", new HttpService() {
            @Override
            public void routing(HttpRules httpRules) {
                httpRules.get("/greeting", new Handler() {
                    @Override
                    public void handle(ServerRequest serverRequest, ServerResponse serverResponse) throws Exception {
                        var paras = serverRequest.path().pathParameters();
                        serverResponse.send("hello world");
                        System.out.println(1);
                    }
                });
            }
        });

        WebServer server = WebServer.builder().routing(routing).port(8080).build();

        server.start();
    }
}
