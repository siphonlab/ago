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
package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;

import java.math.BigDecimal;

import static org.siphonlab.ago.TypeCode.*;

public class VertXNativeFrameHandler<T> implements Handler<AsyncResult<T>> {
    private final NativeFrame frame;
    private final String agoExceptionClass;

    private Instance<?> objectResult;

    public VertXNativeFrameHandler(NativeFrame frame, String agoExceptionClass) {
        this.frame = frame;
        this.agoExceptionClass = agoExceptionClass;
    }

    public VertXNativeFrameHandler(NativeFrame frame){
        this(frame, "lang.NativeException");
    }

    @Override
    public void handle(AsyncResult<T> result) {
        if(result.succeeded()){
            switch (frame.getAgoClass().getResultTypeCode().value){
                case INT_VALUE:     frame.finishIntAsync((Integer) result.result()); break;
                case LONG_VALUE:    frame.finishLongAsync((Long) result.result()); break;
                case FLOAT_VALUE:   frame.finishFloatAsync((Float) result.result()); break;
                case DOUBLE_VALUE:  frame.finishDoubleAsync((Double) result.result()); break;
                case DECIMAL_VALUE: frame.finishDecimalAsync((BigDecimal) result.result()); break;
                case BOOLEAN_VALUE: frame.finishBooleanAsync((Boolean) result.result()); break;
                case STRING_VALUE:  frame.finishStringAsync((String) result.result()); break;
                case SHORT_VALUE:   frame.finishShortAsync((Short) result.result()); break;
                case BYTE_VALUE:    frame.finishByteAsync((Byte) result.result()); break;
                case CHAR_VALUE:    frame.finishCharAsync((Character) result.result()); break;
                case OBJECT_VALUE:  {
                    Object r = result.result();
                    if(r instanceof Instance<?> instance){
                        frame.finishObjectAsync(instance);
                    } else {
                        AgoEngine agoEngine = frame.getAgoEngine();
                        var instance = agoEngine.createNativeInstance(null, ClassMapping.map(r.getClass(), agoEngine), frame.getRunSpace());
                        instance.setNativePayload(r);
                        frame.finishObjectAsync(instance);

//                        AgoFunction constructor = instance.getAgoClass().getEmptyArgsConstructor();
//                        if(constructor != null){
//                            var fun = agoEngine.createFunctionInstance(instance, constructor, frame.getRunSpace());
//                            frame.setNativePayload(instance);
//                            frame.getRunSpace().resumeByAcceptResult();
//                            frame.invokeFrame(fun, NativeFrame.REENTER_CREATE_INSTANCE);
//                        } else {
//                            frame.finishObjectAsync(instance);
//                        }
                    }
                } break;
                case NULL_VALUE, VOID_VALUE:    frame.finishVoidAsync(); break;
                case CLASS_REF_VALUE:   throw new IllegalStateException("impossible");
                case UNION_VALUE:   {
                    Object value = result.result();
                    switch (Union.extractUnionType(value).value){
                        case INT_VALUE, LONG_VALUE, FLOAT_VALUE, DECIMAL_VALUE, DOUBLE_VALUE, STRING_VALUE,
                             BOOLEAN_VALUE, BYTE_VALUE, SHORT_VALUE, CHAR_VALUE:
                                frame.finishUnionAsync(value); break;
                        case OBJECT_VALUE:  {
                            Object r = result.result();
                            if(r instanceof Instance<?> instance){
                                frame.finishUnionAsync(instance);
                            } else {
                                AgoEngine agoEngine = frame.getAgoEngine();
                                var instance = agoEngine.createNativeInstance(null, ClassMapping.map(r.getClass(), agoEngine), frame.getRunSpace());
                                instance.setNativePayload(r);
                                frame.finishUnionAsync(instance);
                            }
                        } break;
                        case NULL_VALUE, VOID_VALUE:    frame.finishUnionAsync(null); break;
                        case CLASS_REF_VALUE:   throw new IllegalStateException("impossible");
                    }
                }
            }
        } else {
            frame.raiseJavaException(frame.self(), result.cause(), agoExceptionClass, true);
        }
    }
}
