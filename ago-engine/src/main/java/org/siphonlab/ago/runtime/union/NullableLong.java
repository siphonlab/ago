package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;

/**
 * Nullable wrapper for primitive {@code long}.
 */
public class NullableLong extends NullablePrimitive {

    private final long value;

    public NullableLong(boolean isNull, long value) {
        super(isNull);
        this.value = value;
    }

    @Override
    public NullableLong setNull() {
        return new NullableLong(true, 0L);
    }

    @Override
    public long getLongValue() {
        return value;
    }

    @Override
    public NullableLong setLongValue(long value) {
        return new NullableLong(false, value);
    }

    @Override
    public TypeCode getType() {
        if (isNull) return TypeCode.NULL;
        return TypeCode.LONG;
    }
}
