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
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Router {

    public static void create(NativeFrame creator, Instance<?> serverInst){
        VertxRunSpaceHost host = (VertxRunSpaceHost) creator.getRunSpace().getRunSpaceHost();
        var inst = creator.getParentScope();
        io.vertx.ext.web.Router router = io.vertx.ext.web.Router.router(host.getVertx());

        // enable body handler
        router.route()
            .method(HttpMethod.POST)
            .method(HttpMethod.PUT)
            .method(HttpMethod.PATCH)
            .method(HttpMethod.DELETE)
            .handler(BodyHandler.create());

        inst.setNativePayload(router);

        io.vertx.core.http.HttpServer server = (io.vertx.core.http.HttpServer) serverInst.getNativePayload();
        server.requestHandler(router);

        creator.finishVoid();
    }

    private static Map<Instance<?>, RoutingContext> functionsToRoute = new ConcurrentHashMap<>();

    // routeClass is a scoped class interval
    public static void route(NativeFrame frame, Instance<?> routeClass){
        AgoEngine engine = frame.getAgoEngine();
        var fun = (AgoFunction) engine.getBoxer().unbox(routeClass);

        var parameterizedRouteClass = engine.getClass("io.Route").asThatOrSuperOfThat(fun);
        String path = parameterizedRouteClass.getStringField("path");
        String method = parameterizedRouteClass.getStringField("method");
        io.vertx.ext.web.Router router = (io.vertx.ext.web.Router) frame.getParentScope().getNativePayload();

        router.route(HttpMethod.valueOf(method), path).handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                ScopedClassIntervalExtractor.Result scopedClassInterval = ScopedClassIntervalExtractor.extract(routeClass);
                var handler = engine.createFunctionInstance(scopedClassInterval.scope(), (AgoFunction) scopedClassInterval.agoClass(), frame.getRunSpace());
                functionsToRoute.put(handler, event);
                handler.setCaller(frame);
                frame.getRunSpace().fork(handler);       // pass RoutingContext through fork context
            }
        });
        frame.finishVoid();
    }

    public static void Route_bindRoutingContext(NativeFrame frame, Instance<?> handler){
        RoutingContext routingContext = functionsToRoute.remove(handler);
        assert routingContext != null;
        frame.finishObject(ClassMapping.mapObject(routingContext, frame));
    }

    static class RoutingForkContext {
        final RoutingContext routingContext;

        RoutingForkContext(RoutingContext routingContext) {
            this.routingContext = routingContext;
        }

        public RoutingContext getRoutingContext() {
            return routingContext;
        }
    }

}
