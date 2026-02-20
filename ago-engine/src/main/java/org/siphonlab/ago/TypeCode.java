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

public class TypeCode {
    public static final int VOID_VALUE = 0;      // -1 in java
    public static final int BOOLEAN_VALUE = 4;
    public static final int CHAR_VALUE = 5;
    public static final int FLOAT_VALUE = 6;
    public static final int DOUBLE_VALUE = 7;
    public static final int BYTE_VALUE = 8;
    public static final int SHORT_VALUE = 9;
    public static final int INT_VALUE = 10;
    public static final int LONG_VALUE = 11;
    public static final int OBJECT_VALUE = 1;  // -1 in java
    public static final int NULL_VALUE = 2;
    public static final int STRING_VALUE = 3;
    public static final int CLASS_REF_VALUE = 12;  // 0x0c

    public static final int MAX_VALUE = 0x0f;   // 0 - 0x0f, from 0x10 it's generic type variable index (n-0x10)
    public static final int GENERIC_TYPE_START = 0x10;

    public static final TypeCode VOID = new TypeCode(VOID_VALUE, "void");
    public static final TypeCode BOOLEAN = new TypeCode(BOOLEAN_VALUE, "boolean");
    public static final TypeCode CHAR = new TypeCode(CHAR_VALUE, "char");
    public static final TypeCode FLOAT = new TypeCode(FLOAT_VALUE, "float");
    public static final TypeCode DOUBLE = new TypeCode(DOUBLE_VALUE, "double");
    public static final TypeCode BYTE = new TypeCode(BYTE_VALUE, "byte");
    public static final TypeCode SHORT = new TypeCode(SHORT_VALUE, "short");
    public static final TypeCode INT = new TypeCode(INT_VALUE, "int");
    public static final TypeCode LONG = new TypeCode(LONG_VALUE, "long");
    public static final TypeCode OBJECT = new TypeCode(OBJECT_VALUE, "object");
    public static final TypeCode NULL = new TypeCode(NULL_VALUE, "null");
    public static final TypeCode STRING = new TypeCode(STRING_VALUE, "string");
    public static final TypeCode CLASS_REF = new TypeCode(CLASS_REF_VALUE, "classref");

    private final String description;
    public final int value;

    public TypeCode(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static TypeCode of(int value) {
        return switch (value) {
            case 0 -> VOID;
            case 4 -> BOOLEAN;
            case 5 -> CHAR;
            case 6 -> FLOAT;
            case 7 -> DOUBLE;
            case 8 -> BYTE;
            case 9 -> SHORT;
            case 10 -> INT;
            case 11 -> LONG;
            case 1 -> OBJECT;
            case 2 -> NULL;
            case 3 -> STRING;
            case 12 -> CLASS_REF;
            default -> throw new IllegalArgumentException("Invalid type value: " + value);
        };
    }

    public static TypeCode[] values() {
        return new TypeCode[]{VOID, BOOLEAN, CHAR, FLOAT, DOUBLE, BYTE, SHORT, INT, LONG, OBJECT, NULL, STRING, CLASS_REF };
    }

    public int getValue() {
        return value;
    }

    public String toShortString(){
        if (this == VOID) {
            return "V";
        } else if (this == BOOLEAN) {
            return "B";
        } else if (this == CHAR) {
            return "c";
        } else if (this == FLOAT) {
            return "f";
        } else if (this == DOUBLE) {
            return "d";
        } else if (this == BYTE) {
            return "b";
        } else if (this == SHORT) {
            return "s";
        } else if (this == INT) {
            return "i";
        } else if (this == LONG) {
            return "l";
        } else if (this == OBJECT) {
            return "o";
        } else if (this == NULL) {
            return "n";
        } else if (this == STRING) {
            return "S";
        } else if (this == CLASS_REF) {
            return "C";
        } else if(this.value >= GENERIC_TYPE_START){
            return this.description;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * for primary type, is this higher than another, if higher than another, another can cast to this
     * @param another another type
     * @return
     */
    public boolean isHigherThan(TypeCode another){
        if (this == LONG) {
            return another == TypeCode.INT || another == TypeCode.SHORT || another == TypeCode.BYTE || another == TypeCode.CHAR;
        } else if (this == INT) {
            return another == TypeCode.SHORT || another == TypeCode.BYTE || another == TypeCode.CHAR;
        } else if (this == SHORT) {
            return another == TypeCode.BYTE;
        } else if (this == CHAR) {
            return TypeCode.BYTE == another;
        } else if (this == DOUBLE) {
            return another == TypeCode.FLOAT || another == TypeCode.LONG || another == TypeCode.INT || another == TypeCode.SHORT || another == TypeCode.BYTE || another == TypeCode.CHAR;
        } else if (this == FLOAT) {
            return another == TypeCode.INT || another == TypeCode.SHORT || another == TypeCode.BYTE || another == TypeCode.CHAR;
        } else if (this == STRING) {
            return true;
        } else if (this == OBJECT) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isGeneric(){
        return this.value >= GENERIC_TYPE_START;
    }

    @Override
    public String toString() {
        return this.description;
    }

    @Override
    public int hashCode() {
        return this.value;
    }

    public boolean isNumber() {
        return this == INT || this == LONG || this == FLOAT || this == DOUBLE || this == BYTE || this == SHORT;
    }

    public boolean isObjectOrNull(){
        return this == OBJECT || this == NULL;
    }

    public boolean isObject(){
        return this == OBJECT;
    }
    public boolean isNull(){
        return this == NULL;
    }

    public boolean isIntFamily() {
        return this == INT || this == LONG || this == BYTE || this == SHORT;
    }

    public static Object defaultValue(TypeCode typeCode) {
        return switch (typeCode.getValue()) {
            case TypeCode.INT_VALUE -> 0;
            case TypeCode.LONG_VALUE -> 0L;
            case TypeCode.SHORT_VALUE -> (short) 0;
            case TypeCode.BYTE_VALUE -> (byte) 0;
            case TypeCode.BOOLEAN_VALUE -> false;
            case TypeCode.FLOAT_VALUE -> (float) 0;
            case TypeCode.DOUBLE_VALUE -> 0.0;
            case TypeCode.STRING_VALUE -> "";
            case TypeCode.CLASS_REF_VALUE -> -1;
            default -> null;
        };
    }

    public static Object defaultValueForDb(TypeCode typeCode) {
        return switch (typeCode.getValue()) {
            case TypeCode.INT_VALUE -> 0;
            case TypeCode.LONG_VALUE -> 0L;
            case TypeCode.SHORT_VALUE -> (short) 0;
            case TypeCode.BYTE_VALUE -> (byte) 0;
            case TypeCode.BOOLEAN_VALUE -> false;
            case TypeCode.FLOAT_VALUE -> (float) 0;
            case TypeCode.DOUBLE_VALUE -> 0.0;
            case TypeCode.STRING_VALUE -> "";
            case TypeCode.CLASS_REF_VALUE -> null;
            default -> null;
        };
    }

}
