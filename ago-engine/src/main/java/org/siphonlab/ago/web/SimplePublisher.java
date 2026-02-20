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
        RunSpace runSpace = callFrame.getRunSpace();
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
