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
package org.siphonlab.ago.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.*;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import org.apache.commons.dbcp2.Utils;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.Instance;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.runtime.json.AgoJsonConfig;
import org.siphonlab.ago.runtime.rdb.ObjectRef;
import org.siphonlab.ago.runtime.rdb.ObjectRefOwner;
import org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine;
import org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonPGAdapter;
import org.siphonlab.ago.runtime.vertx.VertxRunSpaceHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class TestFunctions {

    public static Logger logger = LoggerFactory.getLogger(TestFunctions.class);

    public static void add(NativeFrame frame, int a, int b){
        System.err.println("let's think a long while");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.err.println("output result");
        frame.finishInt(a + b);
    }

    public static void mustTryAgain(NativeFrame frame){
        if(frame.getPayload() == null){
            System.err.println("failed this time, please try again");
            frame.setPayload("you will success this time");
            System.exit(0);
        } else {
            System.err.println("now success!");
            frame.finishVoid();
        }
    }

    //TODO auto convert enum to primitive type
    public static void jsonSerialize(NativeFrame nativeFrame,
                                     Instance<?> input,
                                     Instance<?> writeType,
                                     boolean writeId ,
                                     Instance<?> objectAsReference,
                                     boolean writeSlots){
        AgoJsonConfig.WriteTypeMode typeMode = AgoJsonConfig.WriteTypeMode.valueOf(writeType.getSlots().getInt(0));
        AgoJsonConfig.ObjectAsReferenceMode objectAsReferenceMode = AgoJsonConfig.ObjectAsReferenceMode.valueOf(objectAsReference.getSlots().getInt(0));
        var engine = nativeFrame.getAgoEngine();
        var objectMapper = engine.getObjectMapper(new AgoJsonConfig(typeMode, writeId, objectAsReferenceMode, writeSlots));
        try {
            nativeFrame.finishString(objectMapper.writeValueAsString(input));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessage(NativeFrame nativeFrame, Instance<?> destination, Instance<?> message){
        VertxRunSpaceHost vertxRunSpaceHost = (VertxRunSpaceHost) nativeFrame.getRunSpace().getRunSpaceHost();
//        vertxRunSpaceHost.getVertx().eventBus().send(ObjectRefOwner.extractObjectRef(destination).toString(), ObjectRefOwner.extractObjectRef(message));
        try {
            vertxRunSpaceHost.getVertx().eventBus().send(String.valueOf(destination.hashCode()), nativeFrame.getAgoEngine()
                    .jsonStringify(message, AgoJsonConfig.RPC_OBJECT_REF));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        nativeFrame.finishVoid();
    }

    public static void receiveMessage(NativeFrame nativeFrame, Instance<?> source) {
        VertxRunSpaceHost vertxRunSpaceHost = (VertxRunSpaceHost) nativeFrame.getRunSpace().getRunSpaceHost();
        AgoClass resultClass = nativeFrame.getAgoClass().getResultClass();
        nativeFrame.beginAsync();
        vertxRunSpaceHost.getVertx().eventBus().consumer(String.valueOf(source.hashCode()), event -> {
            String json = (String) event.body();
            try {
                var instance = nativeFrame.getAgoEngine().jsonDeserialize(resultClass, nativeFrame, new StringReader(json), true);
                nativeFrame.finishObjectAsync(instance);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void sendMessageMQ(NativeFrame nativeFrame, Instance<?> destination, Instance<?> message) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (var connection = factory.newConnection();
            var channel = connection.createChannel()) {
            ObjectRef objectRef = ObjectRefOwner.extractObjectRef(destination);

            channel.exchangeDeclare(objectRef.className(), BuiltinExchangeType.DIRECT.getType());
//            channel.queueDeclare(objectRef.toString(), true,false,false,null);
//            channel.queueBind(objectRef.toString(), objectRef.className(), objectRef.toString());

            ObjectRef messageRef = ObjectRefOwner.extractObjectRef(message);
            String json = new JsonBuilder(messageRef).toString();
            channel.basicPublish(objectRef.className(), objectRef.toString(), null, json.getBytes(StandardCharsets.UTF_8));

            nativeFrame.finishVoid();
        }
    }

    public static void receiveMessageMQ(NativeFrame nativeFrame, Instance<?> source) throws IOException, TimeoutException {
        ObjectRef objectRef = ObjectRefOwner.extractObjectRef(source);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        LazyJsonAgoEngine engine = (LazyJsonAgoEngine) nativeFrame.getAgoEngine();
        LazyJsonPGAdapter adapter = (LazyJsonPGAdapter) engine.getRdbAdapter();

        var connection = factory.newConnection();
        var channel = connection.createChannel();
        channel.exchangeDeclare(objectRef.className(), BuiltinExchangeType.DIRECT.getType());
        String queueName = channel.queueDeclare().getQueue();

        channel.queueBind(queueName, objectRef.className(), objectRef.toString());

        channel.basicConsume(queueName, true, new DeliverCallback(){
            @Override
            public void handle(String consumerTag, Delivery message) throws IOException {
                Map<String,Object> o = (Map<String, Object>) new JsonSlurper().parse(message.getBody());
                ObjectRef messageRef = new ObjectRef((String) o.get("className"), (Long) o.get("id"));
                var instance = adapter.restoreInstance(messageRef);
                Utils.closeQuietly(channel);
                Utils.closeQuietly(connection);
                nativeFrame.finishObject(instance);
            }
        }, consumerTag -> {});
    }

}
