package org.siphonlab.ago;

import io.vertx.core.*;
import io.vertx.core.impl.future.FailedFuture;
import io.vertx.core.impl.future.FutureBase;
import io.vertx.core.impl.future.FutureImpl;
import io.vertx.core.impl.future.SucceededFuture;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;


public interface CallFrameStateHandler<T> {


    boolean complete(T result);

    boolean fail(Throwable cause);

}
