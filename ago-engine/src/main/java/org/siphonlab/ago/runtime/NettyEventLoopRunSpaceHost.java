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
package org.siphonlab.ago.runtime;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import org.siphonlab.ago.RunSpace;
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
    public void execute(RunSpace runSpace) {
        this.eventLoop.execute(runSpace);
    }


    @Override
    public Object setTimer(long delay, Runnable handler) {
        return eventLoop.schedule(handler,delay, TimeUnit.MILLISECONDS);
    }
}
