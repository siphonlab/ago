package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;

/**
 * Nullable wrapper for primitive {@code float}.
 */
public class NullableFloat extends NullablePrimitive {

    private final float value;

    public NullableFloat(boolean isNull, float value) {
        super(isNull);
        this.value = value;
    }

    @Override
    public NullableFloat setNull() {
        return new NullableFloat(true, 0f);
    }

    @Override
    public float getFloatValue() {
        return value;
    }

    @Override
    public NullableFloat setFloatValue(float value) {
        return new NullableFloat(false, value);
    }

    @Override
    public TypeCode getType() {
        if (isNull) return TypeCode.NULL;
        return TypeCode.FLOAT;
    }
}
