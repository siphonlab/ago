package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.siphonlab.ago.AgoRunSpace;
import org.siphonlab.ago.RunSpaceHost;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class VertxRunSpaceHost implements RunSpaceHost {
    private final Vertx vertx;

    public VertxRunSpaceHost(Vertx vertx){
        this.vertx = vertx;
    }
    @Override
    public void execute(AgoRunSpace runSpace) {
        vertx.runOnContext(event -> runSpace.run());
    }

    public Vertx getVertx() {
        return vertx;
    }

    public Object setTimer(long delay, Runnable handler){
        return vertx.setTimer(delay,l -> handler.run());
    }
}
