/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago;

import org.siphonlab.ago.classloader.ClassRefValue;

import java.math.BigDecimal;

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
    private BigDecimal decimalValue;
    private byte byteValue;
    private short shortValue;
    private int intValue;
    private long longValue;
    protected Instance<?> objectValue;
    protected Object unionValue;
    protected int unionValueType;
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

    public BigDecimal getDecimalValue() {
        return decimalValue;
    }

    public void setDecimalValue(BigDecimal value) {
        decimalValue = value;
        dataType = TypeCode.DECIMAL_VALUE;
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

    public Instance<?> takeObjectValue() {
        var r = objectValue;
        objectValue = null;
        return r;
    }

    public Object takeUnionValue() {
        var r= this.unionValue;
        unionValue = null;
        return r;
    }

    public Object getUnionValue() {
        return unionValue;
    }

    public void setObjectValue(Instance<?> value) {
        objectValue = value;
        dataType = TypeCode.OBJECT_VALUE;
    }

    public void setUnionValue(Object value) {
        this.unionValue = value;
        dataType = UNION_VALUE;
        unionValueType = Union.extractUnionType(value).value;
    }

    public int getUnionValueType() {
        return unionValueType;
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
        int type = getDataType();
        return castAnyToObject(boxer, type);
    }

    private Instance<? extends AgoClass> castAnyToObject(Boxer boxer, int type) {
        return switch (type) {
            case VOID_VALUE, NULL_VALUE -> null;
            case OBJECT_VALUE -> getObjectValue();
            case INT_VALUE -> boxer.boxInt(getIntValue());
            case BYTE_VALUE -> boxer.boxByte(getByteValue());
            case SHORT_VALUE -> boxer.boxShort(getShortValue());
            case LONG_VALUE -> boxer.boxLong(getLongValue());
            case FLOAT_VALUE -> boxer.boxFloat(getFloatValue());
            case DOUBLE_VALUE -> boxer.boxDouble(getDoubleValue());
            case DECIMAL_VALUE -> boxer.boxDecimal(getDecimalValue());
            case BOOLEAN_VALUE -> boxer.boxBoolean(getBooleanValue());
            case CHAR_VALUE -> boxer.boxChar(getCharValue());
            case STRING_VALUE -> boxer.boxString(getStringValue());
            case CLASS_REF_VALUE -> boxer.boxClassRef(getClassRefValue().getClassId());
            case UNION_VALUE -> castAnyToObject(boxer, getUnionValueType());
            default -> throw new UnsupportedOperationException("unexpected data type " + getDataType());
        };
    }

    public Object takeResultAsUnion(){
        switch (this.getDataType()) {
            case VOID_VALUE:
            case NULL_VALUE:
                return null;
            case OBJECT_VALUE:
                return this.takeObjectValue();
            case INT_VALUE:
                return this.getIntValue();
            case BYTE_VALUE:
                return (this.getByteValue());
            case SHORT_VALUE:
                return (this.getShortValue());
            case LONG_VALUE:
                return (this.getLongValue());
            case FLOAT_VALUE:
                return (this.getFloatValue());
            case DOUBLE_VALUE:
                return (this.getDoubleValue());
            case DECIMAL_VALUE:
                return (this.getDecimalValue());
            case BOOLEAN_VALUE:
                return (this.getBooleanValue());
            case CHAR_VALUE:
                return (this.getCharValue());
            case STRING_VALUE:
                return (this.getStringValue());
            case CLASS_REF_VALUE:
                return new ClassRefValue(this.getClassRefValue().getFullname());
            case UNION_VALUE:
                if(this.unionValue instanceof Instance<?>){
                    var r = this.unionValue;
                    this.unionValue = null;
                    return r;
                }
                return this.unionValue;
            default:
                throw new UnsupportedOperationException("unexpected data type " + this.getDataType());
        }

    }

    public Object getResultAsJavaObject() {
        switch (this.getDataType()) {
            case VOID_VALUE:
            case NULL_VALUE:
                return null;
            case OBJECT_VALUE:
                return this.getObjectValue();
            case INT_VALUE:
                return this.getIntValue();
            case BYTE_VALUE:
                return (this.getByteValue());
            case SHORT_VALUE:
                return (this.getShortValue());
            case LONG_VALUE:
                return (this.getLongValue());
            case FLOAT_VALUE:
                return (this.getFloatValue());
            case DOUBLE_VALUE:
                return (this.getDoubleValue());
            case DECIMAL_VALUE:
                return (this.getDecimalValue());
            case BOOLEAN_VALUE:
                return (this.getBooleanValue());
            case CHAR_VALUE:
                return (this.getCharValue());
            case STRING_VALUE:
                return (this.getStringValue());
            case CLASS_REF_VALUE:
                return this.getClassRefValue();
            case UNION_VALUE:
                return this.unionValue;
            default:
                throw new UnsupportedOperationException("unexpected data type " + this.getDataType());
        }
    }

    public Object getResultAsJavaObject(BoxTypes boxTypes, ClassManager classManager) {
        switch (this.getDataType()) {
            case VOID_VALUE:
            case NULL_VALUE:
                return null;
            case OBJECT_VALUE: {
                var object = this.getObjectValue();
                return boxTypes.unbox(classManager, object);
            }
            case INT_VALUE:
                return this.getIntValue();
            case BYTE_VALUE:
                return (this.getByteValue());
            case SHORT_VALUE:
                return (this.getShortValue());
            case LONG_VALUE:
                return (this.getLongValue());
            case FLOAT_VALUE:
                return (this.getFloatValue());
            case DOUBLE_VALUE:
                return (this.getDoubleValue());
            case DECIMAL_VALUE:
                return (this.getDecimalValue());
            case BOOLEAN_VALUE:
                return (this.getBooleanValue());
            case CHAR_VALUE:
                return (this.getCharValue());
            case STRING_VALUE:
                return (this.getStringValue());
            case CLASS_REF_VALUE:
                return this.getClassRefValue();
            case UNION_VALUE:
                return this.unionValue;
            default:
                throw new UnsupportedOperationException("unexpected data type " + this.getDataType());
        }
    }
}
