package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.Instance;
import org.siphonlab.ago.TypeCode;

public class NullableObject extends Nullable {

    private final Instance<?> value;

    public NullableObject(Instance<?> value) {
        this.value = value;
    }

    @Override
    public NullableObject setNull() {
        return new NullableObject(null);
    }

    @Override
    public Instance<?> getObjectValue() {
        return value;
    }

    @Override
    public NullableObject setObjectValue(Instance<?> instance) {
        return new NullableObject(value);
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    @Override
    public TypeCode getType() {
        if (value == null) return TypeCode.NULL;
        return TypeCode.OBJECT;
    }
}
