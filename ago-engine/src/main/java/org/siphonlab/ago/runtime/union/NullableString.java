package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;

/**
 * Nullable wrapper for {@code String}.
 */
public class NullableString extends Nullable {

    private final String value;

    public NullableString(String value) {
        this.value = value;
    }

    @Override
    public NullableString setNull() {
        return new NullableString(null);
    }

    @Override
    public String getStringValue() {
        return value;
    }

    @Override
    public NullableString setStringValue(String value) {
        return new NullableString(value);
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    @Override
    public TypeCode getType() {
        if (value == null) return TypeCode.NULL;
        return TypeCode.STRING;
    }
}
