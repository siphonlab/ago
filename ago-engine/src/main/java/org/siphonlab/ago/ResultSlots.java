package org.siphonlab.ago;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.FLOAT_VALUE;
import static org.siphonlab.ago.TypeCode.INT_VALUE;
import static org.siphonlab.ago.TypeCode.LONG_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;

public class ResultSlots {
    private boolean booleanValue;
    private char charValue;
    private float floatValue;
    private double doubleValue;
    private byte byteValue;
    private short shortValue;
    private int intValue;
    private long longValue;
    private Instance<?> objectValue;
    private String stringValue;
    private AgoClass classRefValue;

    private int dataType;

    public Object getVoidValue() {
        return null;
    }

    public void setVoidValue() {    // should be null
        dataType = TypeCode.VOID_VALUE;
    }

    public boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(boolean value) {
        booleanValue = value;
        dataType = TypeCode.BOOLEAN_VALUE;
    }

    public char getCharValue() {
        return charValue;
    }

    public void setCharValue(char value) {
        charValue = value;
        dataType = TypeCode.CHAR_VALUE;
    }

    public float getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(float value) {
        floatValue = value;
        dataType = TypeCode.FLOAT_VALUE;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(double value) {
        doubleValue = value;
        dataType = TypeCode.DOUBLE_VALUE;
    }

    public byte getByteValue() {
        return byteValue;
    }

    public void setByteValue(byte value) {
        byteValue = value;
        dataType = TypeCode.BYTE_VALUE;
    }

    public short getShortValue() {
        return shortValue;
    }

    public void setShortValue(short value) {
        shortValue = value;
        dataType = TypeCode.SHORT_VALUE;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int value) {
        intValue = value;
        dataType = TypeCode.INT_VALUE;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long value) {
        longValue = value;
        dataType = TypeCode.LONG_VALUE;
    }

    public Instance<?> getObjectValue() {
        return objectValue;
    }

    public void setObjectValue(Instance<?> value) {
        objectValue = value;
        dataType = TypeCode.OBJECT_VALUE;
    }

    public Object getNullValue() {
        return null;
    }

    public void setNullValue() {
        dataType = TypeCode.OBJECT_VALUE;
        objectValue = null;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String value) {
        this.stringValue = value;
        dataType = TypeCode.STRING_VALUE;
    }

    public AgoClass getClassRefValue() {
        return classRefValue;
    }

    public void setClassRefValue(AgoClass value) {
        this.classRefValue = value;
        dataType = TypeCode.CLASS_REF_VALUE;
    }

    public int getDataType() {
        return dataType;
    }

    public Instance<?> castAnyToObject(Boxer boxer) {
        switch (getDataType()) {
            case VOID_VALUE:
            case NULL_VALUE:
                return null;
            case OBJECT_VALUE:
                return getObjectValue();
            case INT_VALUE:
                return boxer.boxInt(getIntValue());
            case BYTE_VALUE:
                return boxer.boxByte(getByteValue());
            case SHORT_VALUE:
                return boxer.boxShort(getShortValue());
            case LONG_VALUE:
                return boxer.boxLong(getLongValue());
            case FLOAT_VALUE:
                return boxer.boxFloat(getFloatValue());
            case DOUBLE_VALUE:
                return boxer.boxDouble(getDoubleValue());
            case BOOLEAN_VALUE:
                return boxer.boxBoolean(getBooleanValue());
            case CHAR_VALUE:
                return boxer.boxChar(getCharValue());
            case STRING_VALUE:
                return boxer.boxString(getStringValue());
            case CLASS_REF_VALUE:
                return boxer.boxClassRef(getClassRefValue().getClassId());
            default:
                throw new UnsupportedOperationException("unexpected data type " + getDataType());
        }
    }

}
