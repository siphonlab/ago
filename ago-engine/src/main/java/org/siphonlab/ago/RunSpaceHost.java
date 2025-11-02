package org.siphonlab.ago;


import java.util.concurrent.Future;

// runspace host, which can hosts run space, i.e. netty EventLoop, thread and virtual thread, java executors, remote machine
public interface RunSpaceHost {

    void execute(AgoRunSpace runSpace);

    Object setTimer(long delay, Runnable handler);

}
