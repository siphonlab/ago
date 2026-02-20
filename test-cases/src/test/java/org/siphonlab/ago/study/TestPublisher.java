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

import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServiceRequestContext;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Arrays;
import java.util.Iterator;

public class TestPublisher {
    public static void main(String[] args) {

        var ls  = Arrays.asList(1,2,3,4,5,6,7);
        Iterator<Integer> iterator = ls.iterator();

        var pub = new Publisher<HttpObject>() {

            @Override
            public void subscribe(Subscriber<? super HttpObject> subscriber) {
                while(iterator.hasNext()){
                    subscriber.onNext(HttpData.ofUtf8(iterator.next().toString()));
                }
                subscriber.onComplete();
            }

        };


        var serverBuilder = Server.builder().port(8080, SessionProtocol.HTTP);
        serverBuilder.service("/test", new HttpService() {
            @Override
            public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
//                return HttpResponse.of(Flux.fromIterable(ls).map(i -> HttpData.ofUtf8(i.toString())));
                ResponseHeaders headers = ResponseHeaders.builder(HttpStatus.OK).contentType(MediaType.PLAIN_TEXT).build();

                return HttpResponse.of(headers, pub);
            }
        });

        serverBuilder.build().start().join();


    }
}
