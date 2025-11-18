package org.siphonlab.ago.web;

import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpObject;
import org.reactivestreams.Subscriber;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.UnhandledException;

public class SimplePublisher implements org.reactivestreams.Publisher<HttpObject> {

    private Subscriber<? super HttpObject> subscriber;
    private Runnable publisher;

    @Override
    public void subscribe(Subscriber<? super HttpObject> subscriber) {
        this.subscriber = subscriber;
        if (publisher != null) {
            publisher.run();
        }
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public static void create(AgoEngine agoEngine, CallFrame<?> callFrame, Instance<?> instance, Instance<?> scopedPublishFun) {
        SimplePublisher simplePublisher = new SimplePublisher();
        ((NativeInstance) instance).setNativePayload(simplePublisher);
        AgoRunSpace runSpace = callFrame.getRunSpace();
        CallFrame<?> frame = (CallFrame<?>) agoEngine.createInstanceFromScopedClass((AgoClass) scopedPublishFun, callFrame, runSpace);
        simplePublisher.publisher = () -> runSpace.fork(frame);
    }

    public static void onNext(AgoEngine agoEngine, CallFrame<?> callFrame, Instance<?> instance, Instance<?> value) {
        SimplePublisher publisher = (SimplePublisher) ((NativeInstance) instance).getNativePayload();
        publisher.subscriber.onNext(HttpData.ofUtf8(value.toString()));
    }

    public static void onComplete(AgoEngine agoEngine, CallFrame<?> callFrame, Instance<?> instance) {
        SimplePublisher publisher = (SimplePublisher) ((NativeInstance) instance).getNativePayload();
        publisher.subscriber.onComplete();
    }

    public static void onError(AgoEngine agoEngine, CallFrame<?> callFrame, Instance<?> instance, Instance<?> error) {
        SimplePublisher publisher = (SimplePublisher) ((NativeInstance) instance).getNativePayload();
        publisher.subscriber.onError(new UnhandledException(agoEngine, error));
    }


}
