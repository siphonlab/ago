package org.siphonlab.ago;

import java.math.BigDecimal;

import static org.siphonlab.ago.TypeCode.UNION_VALUE;

public interface Union {

    default boolean getBooleanValue() {
        throw new IllegalStateException("getBooleanValue unsupported");
    }

    default Union setBooleanValue(boolean value) {
        throw new IllegalStateException("setBooleanValue unsupported");
    }

    default char getCharValue() {
        throw new IllegalStateException("getCharValue unsupported");
    }

    default Union setCharValue(char value) {
        throw new IllegalStateException("setCharValue unsupported");
    }

    default float getFloatValue() {
        throw new IllegalStateException("getFloatValue unsupported");
    }

    default Union setFloatValue(float value) {
        throw new IllegalStateException("setFloatValue unsupported");
    }

    default double getDoubleValue() {
        throw new IllegalStateException("getDoubleValue unsupported");
    }

    default Union setDoubleValue(double value) {
        throw new IllegalStateException("setDoubleValue unsupported");
    }

    default BigDecimal getDecimalValue() {
        throw new IllegalStateException("getDecimalValue unsupported");
    }

    default Union setDecimalValue(BigDecimal value) {
        throw new IllegalStateException("setDecimalValue unsupported");
    }

    default byte getByteValue() {
        throw new IllegalStateException("getByteValue unsupported");
    }

    default Union setByteValue(byte value) {
        throw new IllegalStateException("setByteValue unsupported");
    }

    default short getShortValue() {
        throw new IllegalStateException("getShortValue unsupported");
    }

    default Union setShortValue(short value) {
        throw new IllegalStateException("setShortValue unsupported");
    }

    default int getIntValue() {
        throw new IllegalStateException("getIntValue unsupported");
    }

    default Union setIntValue(int value) {
        throw new IllegalStateException("setIntValue unsupported");
    }

    default long getLongValue() {
        throw new IllegalStateException("getLongValue unsupported");
    }

    default Union setLongValue(long value) {
        throw new IllegalStateException("setLongValue unsupported");
    }

    default Instance<?> getObjectValue() {
        throw new IllegalStateException("getObjectValue unsupported");
    }

    default Union setObjectValue(Instance<?> instance) {
        throw new IllegalStateException("setObjectValue unsupported");
    }

    default String getStringValue() {
        throw new IllegalStateException("getStringValue unsupported");
    }

    default Union setStringValue(String value) {
        throw new IllegalStateException("setStringValue unsupported");
    }

    default int getClassRefValue() {
        throw new IllegalStateException("getClassRefValue unsupported");
    }

    default Union setClassRefValue(int value) {
        throw new IllegalStateException("setClassRefValue unsupported");
    }

    default Union setNull() {
        throw new IllegalStateException("setNull unsupported");
    }

    default boolean isNull() {
        throw new IllegalStateException("isNull unsupported");
    }

    default TypeCode getType() {
        throw new IllegalStateException("getType unsupported");
    }
}