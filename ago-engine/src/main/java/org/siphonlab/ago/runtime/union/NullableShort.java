package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;

/**
 * Nullable wrapper for primitive {@code short}.
 */
public class NullableShort extends NullablePrimitive {

    private final short value;

    public NullableShort(boolean isNull, short value) {
        super(isNull);
        this.value = value;
    }

    @Override
    public NullableShort setNull() {
        return new NullableShort(true, (short) 0);
    }

    @Override
    public short getShortValue() {
        return value;
    }

    @Override
    public NullableShort setShortValue(short value) {
        return new NullableShort(false, value);
    }

    @Override
    public TypeCode getType() {
        if (isNull) return TypeCode.NULL;
        return TypeCode.SHORT;
    }
}
