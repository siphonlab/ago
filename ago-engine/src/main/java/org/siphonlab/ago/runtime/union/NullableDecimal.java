package org.siphonlab.ago.runtime.union;

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.Union;

import java.math.BigDecimal;

/**
 * Nullable wrapper for {@code String}.
 */
public class NullableDecimal extends Nullable {

    private final BigDecimal value;

    public NullableDecimal(BigDecimal value) {
        this.value = value;
    }

    @Override
    public NullableDecimal setNull() {
        return new NullableDecimal(null);
    }

    @Override
    public BigDecimal getDecimalValue() {
        return value;
    }

    @Override
    public NullableDecimal setDecimalValue(BigDecimal value) {
        return new  NullableDecimal(value);
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    @Override
    public TypeCode getType() {
        if (value == null) return TypeCode.NULL;
        return TypeCode.DECIMAL;
    }
}
