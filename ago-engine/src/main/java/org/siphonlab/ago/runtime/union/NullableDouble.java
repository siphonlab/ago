package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;

/**
 * Nullable wrapper for primitive {@code double}.
 */
public class NullableDouble extends NullablePrimitive {

    private final double value;

    public NullableDouble(boolean isNull, double value) {
        super(isNull);
        this.value = value;
    }

    @Override
    public NullableDouble setNull() {
        return new NullableDouble(true, 0d);
    }

    @Override
    public double getDoubleValue() {
        return value;
    }

    @Override
    public NullableDouble setDoubleValue(double value) {
        return new NullableDouble(false, value);
    }

    @Override
    public TypeCode getType() {
        if (isNull) return TypeCode.NULL;
        return TypeCode.DOUBLE;
    }
}
