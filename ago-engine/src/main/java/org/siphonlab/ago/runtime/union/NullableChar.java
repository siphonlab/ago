package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;

/**
 * Nullable wrapper for primitive {@code char}.
 */
public class NullableChar extends NullablePrimitive {

    private final char value;

    public NullableChar(boolean isNull, char value) {
        super(isNull);
        this.value = value;
    }

    @Override
    public NullableChar setNull() {
        return new NullableChar(true, '\u0000');
    }

    @Override
    public char getCharValue() {
        return value;
    }

    @Override
    public NullableChar setCharValue(char value) {
        return new NullableChar(false, value);
    }

    @Override
    public TypeCode getType() {
        if (isNull) return TypeCode.NULL;
        return TypeCode.CHAR;
    }
}
