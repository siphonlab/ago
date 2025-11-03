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
