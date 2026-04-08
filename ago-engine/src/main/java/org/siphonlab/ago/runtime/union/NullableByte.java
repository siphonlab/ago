package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;

/**
 * Nullable wrapper for primitive {@code byte}.
 */
public class NullableByte extends NullablePrimitive {

    private final byte value;

    public NullableByte(boolean isNull, byte value) {
        super(isNull);
        this.value = value;
    }

    @Override
    public NullableByte setNull() {
        return new NullableByte(true, (byte) 0);
    }

    @Override
    public byte getByteValue() {
        return value;
    }

    @Override
    public NullableByte setByteValue(byte value) {
        return new NullableByte(false, value);
    }

    @Override
    public TypeCode getType() {
        if (isNull) return TypeCode.NULL;
        return TypeCode.BYTE;
    }
}
