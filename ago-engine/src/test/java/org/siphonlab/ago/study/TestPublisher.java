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
