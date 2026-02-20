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


import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.*;
import org.siphonlab.ago.*;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.native_.AgoNativeFunction;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.siphonlab.ago.TypeCode.*;

public class RestfulService {

    private final static Logger LOGGER = LoggerFactory.getLogger(RestfulService.class);

    private final ServerBuilder serverBuilder;

//    public RestfulService() {
//        this(Config.builder().build());
//    }

    public RestfulService(){
        serverBuilder = Server.builder().port(8080, SessionProtocol.HTTP);
    }
    public void start(){
        var server = serverBuilder.build();

        server.start().join();
    }

    public void installServices(AgoClassLoader classLoader, AgoEngine agoEngine){
        AgoClass restService = classLoader.getClass("RestService");
        AgoClass publisherProvider = classLoader.getClass("PublisherProvider");
        AgoEnum HttpMethodEnum = (AgoEnum) agoEngine.getClass("HttpMethod");
        Instance<?> HttpGet = HttpMethodEnum.findMember("Get");
        Instance<?> HttpPost = HttpMethodEnum.findMember("Post");
        Instance<?> HttpPut = HttpMethodEnum.findMember("Put");
        Instance<?> HttpDelete = HttpMethodEnum.findMember("Delete");

        for (AgoClass agoClass : classLoader.getClasses()) {
            if(agoClass instanceof AgoFunction fun && agoClass.isThatOrDerivedFrom(restService)){
                AgoClass restServiceTrait = Arrays.stream(fun.getInterfaces()).filter(funInterface -> funInterface.isThatOrDerivedFrom(restService)).findFirst().get();     // trait always be the first, no multi traits support yet
                ParameterizedClassInfo typeInfo = (ParameterizedClassInfo) restServiceTrait.getConcreteTypeInfo();

                String path = restServiceTrait.getSlots().getString(restServiceTrait.getAgoClass().findField("path").getSlotIndex());
                Instance<?> httpMethod = restServiceTrait.getSlots().getObject(restServiceTrait.getAgoClass().findField("httpMethod").getSlotIndex());
                HttpMethod armeriaHttpMethod;
                if(httpMethod == HttpGet){
                    armeriaHttpMethod = HttpMethod.GET;
                } else if(httpMethod == HttpPost){
                    armeriaHttpMethod = HttpMethod.POST;
                } else if (httpMethod == HttpPut) {
                    armeriaHttpMethod = HttpMethod.PUT;
                } else if (httpMethod == HttpDelete) {
                    armeriaHttpMethod = HttpMethod.DELETE;
                } else {
                    armeriaHttpMethod = HttpMethod.GET;
                }

                var traitField = Arrays.stream(fun.getFields()).filter(f -> f.getAgoClass() == restServiceTrait).findFirst().get();

                serverBuilder.service(Route.builder().path(path).methods(armeriaHttpMethod).build(), new HttpService() {
                    @Override
                    public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
                        CompletableFuture<HttpResponse> completableFuture = new CompletableFuture<>();

                        var frame = agoEngine.createFunctionInstance(null, fun, null, null);
                        Map<String, String> pathParams = ctx.pathParams();
                        Slots slots = frame.getSlots();
                        if(pathParams != null && !pathParams.isEmpty()){
                            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                                var field = fun.findParameter(entry.getKey());
                                String value = entry.getValue();
                                if(field != null){
                                    switch (field.getTypeCode().getValue()){
                                        case INT_VALUE:     slots.setInt(field.getSlotIndex(), Integer.parseInt(value)); break;
                                        case LONG_VALUE:    slots.setLong(field.getSlotIndex(), Long.parseLong(value)); break;
                                        case DOUBLE_VALUE:  slots.setDouble(field.getSlotIndex(), Double.parseDouble(value));
                                        case BOOLEAN_VALUE: slots.setBoolean(field.getSlotIndex(), Boolean.parseBoolean(value)); break;
                                        case STRING_VALUE:  slots.setString(field.getSlotIndex(), value); break;
                                        case CHAR_VALUE:    slots.setChar(field.getSlotIndex(), value.charAt(0)); break;
                                        case SHORT_VALUE:   slots.setShort(field.getSlotIndex(), Short.parseShort(value)); break;
                                        case BYTE_VALUE:    slots.setByte(field.getSlotIndex(), Byte.parseByte(value)); break;
                                        case FLOAT_VALUE:   slots.setFloat(field.getSlotIndex(), Float.parseFloat(value)); break;
                                        default: throw new IllegalArgumentException("Unknown type code: " + field.getTypeCode().getValue());
                                    }
                                }
                            }
                        }
                        var constructor = fun.getEmptyArgsConstructor();
                        frame.invokeMethod(frame, frame.getRunSpace(),constructor);

                        NativeInstance traitInstance = (NativeInstance) slots.getObject(traitField.getSlotIndex());
                        traitInstance.setNativePayload(req);

                        //TODO remove DummyCallFrame
//                        var waiter = new DummyCallFrame() {
//                            @Override
//                            protected void finish() {
//                                Object result = getResult();
//                                try {
//                                    if (result instanceof NativeInstance nativeInstance) {
//                                        if (nativeInstance.getNativePayload() instanceof ResultSetMapper resultSetMapper) {
//                                            result = resultSetMapper;
//                                        }
//                                    }
//                                    var bytes = agoEngine.getJsonObjectMapper().writeValueAsBytes(result);
//                                    completableFuture.complete(HttpResponse.of(HttpStatus.OK, MediaType.JSON, bytes));
//                                } catch (JsonProcessingException e) {
//                                    LOGGER.error("unexpected error", e);
//                                    completableFuture.complete(HttpResponse.ofJson(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));
//                                }
//                            }
//                        };
//                        waiter.setRunSpace(agoEngine.getRunSpace());    //TODO handle the result
//
//                        frame.setCaller(waiter);
//                        frame.setCreator(waiter);
                        agoEngine.getRunSpace().fork(frame);
                        return HttpResponse.of(completableFuture);
                    }
                });
            }
        }
    }

    // scope is RestService instance
    public static void payload(NativeFrame nativeFrame) {
        NativeInstance restService = (NativeInstance) nativeFrame.getParentScope();
        AgoEngine agoEngine = nativeFrame.getAgoEngine();
        HttpRequest httpRequest = (HttpRequest) restService.getNativePayload();
        AgoNativeFunction function = nativeFrame.getAgoClass();
        GenericArgumentsInfo genericArgumentsInfo = (GenericArgumentsInfo) function.getConcreteTypeInfo();
        TypeInfo expectedType = genericArgumentsInfo.getArguments()[genericArgumentsInfo.getArguments().length - 1];
        httpRequest.aggregate().thenAcceptAsync(r ->{
            Reader reader = r.content().toReader(StandardCharsets.UTF_8);
            try {
                Instance<?> instance = agoEngine.jsonDeserialize(expectedType.getAgoClass(), nativeFrame, reader, false);
                nativeFrame.finishObject(instance);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
