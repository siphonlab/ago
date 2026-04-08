package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;

public class NullableInt extends NullablePrimitive {

    private final int value;

    public NullableInt(boolean isNull, int value) {
        super(isNull);
        this.value = value;
    }

    @Override
    public NullableInt setNull() {
        return new NullableInt(true, 0);
    }

    @Override
    public int getIntValue() {
        return value;
    }

    @Override
    public NullableInt setIntValue(int value) {
        return new NullableInt(false, value);
    }

    @Override
    public TypeCode getType() {
        if(isNull) return  TypeCode.NULL;
        return TypeCode.INT;
    }
}
