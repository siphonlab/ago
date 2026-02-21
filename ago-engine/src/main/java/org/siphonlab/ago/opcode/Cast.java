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
package org.siphonlab.ago.opcode;

import java.text.MessageFormat;

import org.siphonlab.ago.TypeCode;

public class Cast implements GenericOpCode {
    public static final int KIND_CAST = 0x07_000000;
    public static final int OP                  = 0x07;

    // all cast operator must be cast src_slot, target_slot
    public static final int c2c = 0x07_05_05_02;
    public static final int c2f = 0x07_05_06_02;
    public static final int c2d = 0x07_05_07_02;
    public static final int c2b = 0x07_05_08_02;
    public static final int c2s = 0x07_05_09_02;
    public static final int c2i = 0x07_05_0a_02;
    public static final int c2l = 0x07_05_0b_02;
    public static final int c2S = 0x07_05_03_02;
    public static final int c2B = 0x07_05_04_02;

    public static final int f2c = 0x07_06_05_02;
    public static final int f2f = 0x07_06_06_02;
    public static final int f2d = 0x07_06_07_02;
    public static final int f2b = 0x07_06_08_02;
    public static final int f2s = 0x07_06_09_02;
    public static final int f2i = 0x07_06_0a_02;
    public static final int f2l = 0x07_06_0b_02;
    public static final int f2S = 0x07_06_03_02;
    public static final int f2B = 0x07_06_04_02;

    public static final int d2c = 0x07_07_05_02;
    public static final int d2f = 0x07_07_06_02;
    public static final int d2d = 0x07_07_07_02;
    public static final int d2b = 0x07_07_08_02;
    public static final int d2s = 0x07_07_09_02;
    public static final int d2i = 0x07_07_0a_02;
    public static final int d2l = 0x07_07_0b_02;
    public static final int d2S = 0x07_07_03_02;
    public static final int d2B = 0x07_07_04_02;

    public static final int b2c = 0x07_08_05_02;
    public static final int b2f = 0x07_08_06_02;
    public static final int b2d = 0x07_08_07_02;
    public static final int b2b = 0x07_08_08_02;
    public static final int b2s = 0x07_08_09_02;
    public static final int b2i = 0x07_08_0a_02;
    public static final int b2l = 0x07_08_0b_02;
    public static final int b2S = 0x07_08_03_02;
    public static final int b2B = 0x07_08_04_02;

    public static final int s2c = 0x07_09_05_02;
    public static final int s2f = 0x07_09_06_02;
    public static final int s2d = 0x07_09_07_02;
    public static final int s2b = 0x07_09_08_02;
    public static final int s2s = 0x07_09_09_02;
    public static final int s2i = 0x07_09_0a_02;
    public static final int s2l = 0x07_09_0b_02;
    public static final int s2S = 0x07_09_03_02;
    public static final int s2B = 0x07_09_04_02;

    public static final int i2c = 0x07_0a_05_02;
    public static final int i2f = 0x07_0a_06_02;
    public static final int i2d = 0x07_0a_07_02;
    public static final int i2b = 0x07_0a_08_02;
    public static final int i2s = 0x07_0a_09_02;
    public static final int i2i = 0x07_0a_0a_02;
    public static final int i2l = 0x07_0a_0b_02;
    public static final int i2S = 0x07_0a_03_02;
    public static final int i2B = 0x07_0a_04_02;

    public static final int l2c = 0x07_0b_05_02;
    public static final int l2f = 0x07_0b_06_02;
    public static final int l2d = 0x07_0b_07_02;
    public static final int l2b = 0x07_0b_08_02;
    public static final int l2s = 0x07_0b_09_02;
    public static final int l2i = 0x07_0b_0a_02;
    public static final int l2l = 0x07_0b_0b_02;
    public static final int l2S = 0x07_0b_03_02;
    public static final int l2B = 0x07_0b_04_02;

    public static final int B2S = 0x07_04_03_02;
    public static final int B2c = 0x07_04_05_02;
    public static final int B2i = 0x07_04_0a_02;
    public static final int B2f = 0x07_04_06_02;
    public static final int B2d = 0x07_04_07_02;
    public static final int B2B = 0x07_04_04_02;
    public static final int B2l = 0x07_04_0b_02;
    public static final int B2b = 0x07_04_08_02;
    public static final int B2s = 0x07_04_09_02;

    public static final int C2S = 0x07_0c_03_02;

    public static final int S2B = 0x07_03_04_02;
    public static final int S2c = 0x07_03_05_02;
    public static final int S2f = 0x07_03_06_02;
    public static final int S2d = 0x07_03_07_02;
    public static final int S2b = 0x07_03_08_02;
    public static final int S2s = 0x07_03_09_02;
    public static final int S2i = 0x07_03_0a_02;
    public static final int S2l = 0x07_03_0b_02;
    public static final int S2S = 0x07_03_03_02;

    public static final int o2B = 0x07_01_04_02;

    // cast object with type validation
    // cast_object(target, src, to_type_code, to_type_class)
    public static final int cast_object_vvtC = 0x07_01_00_04;

    // cast to any, cast_to_any(target, target_type_code, target class, src, src_type_code, src class)
    public static final int cast_to_any_vtCvtC = 0x07_01_01_06;

    // Class to parameterized ScopedClassInterval
    // cls2sbr_oCC(target, parameterized ScopedClassInterval class, a Scoped Class instance)
    public static final int C2sbr_oCC = 0x07_01_05_03;

    // extract Class instance from sbr, it's always at the 2nd filed "boxedClass",
    // but sometimes the ClassInterval Instance may be null, we need handle it manually
    // sbr2C(target, ScopedClassInterval instance, field)
    public static final int sbr2C_Cov = 0x07_01_06_03;

    public static String getName(int code) {
        switch (code) {
            case cast_object_vvtC: return "cast_object_vvtC";
            case cast_to_any_vtCvtC: return "cast_to_any_vtCvtC";
            case C2sbr_oCC: return "C2sbr_oCC";
            case sbr2C_Cov: return "sbr2C_Cov";
        }

        var type1 = (code & 0x00ff0000) >> 16;
        var type2 = (code & 0x0000ff00) >> 8;
        String t1 = type1 >= TypeCode.GENERIC_TYPE_START ? "generic code (%d)".formatted(type1) : TypeCode.of(type1).toShortString();
        String t2 = type2 >= TypeCode.GENERIC_TYPE_START ? "generic code (%d)".formatted(type2) : TypeCode.of(type2).toShortString();
        return MessageFormat.format("{0}2{1}", t1, t2);
    }
}
