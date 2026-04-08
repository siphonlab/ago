package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;

/**
 * Nullable wrapper for primitive {@code boolean}.
 */
public class NullableBoolean extends NullablePrimitive {

    private final boolean value;

    public NullableBoolean(boolean isNull, boolean value) {
        super(isNull);
        this.value = value;
    }

    @Override
    public NullableBoolean setNull() {
        return new NullableBoolean(true, false);
    }

    @Override
    public boolean getBooleanValue() {
        return value;
    }

    @Override
    public NullableBoolean setBooleanValue(boolean value) {
        return new NullableBoolean(false, value);
    }

    @Override
    public TypeCode getType() {
        if (isNull) return TypeCode.NULL;
        return TypeCode.BOOLEAN;
    }
}
