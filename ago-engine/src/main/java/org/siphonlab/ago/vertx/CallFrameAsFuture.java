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
package org.siphonlab.ago.vertx;

import io.vertx.core.impl.future.FutureImpl;
import org.siphonlab.ago.*;

import static org.siphonlab.ago.TypeCode.*;

public class CallFrameAsFuture<T> extends FutureImpl<T> implements CallFrameStateHandler<T>{

    @Override
    public boolean complete(T result) {
        return tryComplete(result);
    }

    @Override
    public boolean fail(Throwable cause) {
        return tryFail(cause);
    }

    public static CallFrameAsFuture<?> createFuture(CallFrame<?> callFrame){
        AgoFunction function = callFrame.getAgoClass();
        switch (function.getResultTypeCode().getValue()){
            case INT_VALUE:
                return new CallFrameAsFuture<Integer>();
            case LONG_VALUE:
                return new CallFrameAsFuture<Long>();
            case FLOAT_VALUE:
                return new CallFrameAsFuture<Float>();
            case DOUBLE_VALUE:
                return new CallFrameAsFuture<Double>();
            case BOOLEAN_VALUE:
                return new CallFrameAsFuture<Boolean>();
            case STRING_VALUE:
                return new CallFrameAsFuture<String>();
            case SHORT_VALUE:
                return new CallFrameAsFuture<Short>();
            case BYTE_VALUE:
                return new CallFrameAsFuture<Byte>();
            case CHAR_VALUE:
                return new CallFrameAsFuture<Character>();
            case OBJECT_VALUE:
                return new CallFrameAsFuture<Instance<?>>();
            case NULL_VALUE:
                throw new UnsupportedOperationException("null??");
            case CLASS_REF_VALUE:
                return new CallFrameAsFuture<AgoClass>();
            default:
                throw new UnsupportedOperationException("unknown type code " + function.getResultTypeCode());
        }
    }
}
