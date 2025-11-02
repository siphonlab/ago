package org.siphonlab.ago.runtime;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import org.siphonlab.ago.AgoRunSpace;
import org.siphonlab.ago.RunSpaceHost;

import java.util.concurrent.TimeUnit;

public class NettyEventLoopRunSpaceHost implements RunSpaceHost {
    private final EventLoop eventLoop;
    private final EventLoopGroup eventLoopGroup;

    public NettyEventLoopRunSpaceHost(EventLoopGroup eventLoopGroup) {
        this.eventLoop = eventLoopGroup.next();
        this.eventLoopGroup = eventLoopGroup;
    }

    @Override
    public void execute(AgoRunSpace runSpace) {
        this.eventLoop.execute(runSpace);
    }


    @Override
    public Object setTimer(long delay, Runnable handler) {
        return eventLoop.schedule(handler,delay, TimeUnit.MILLISECONDS);
    }
}
