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
