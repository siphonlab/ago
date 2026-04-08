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

import org.siphonlab.ago.TypeCode;

import java.text.MessageFormat;

public class UnionCast implements GenericOpCode {
    public static final int KIND_UNION_CAST = 0x34_000000;
    public static final int OP                  = 0x34;

    public static final int o2u = 0x34_01_0e_03;
    public static final int n2u = 0x34_02_0e_03;
    public static final int S2u = 0x34_03_0e_03;
    public static final int B2u = 0x34_04_0e_03;
    public static final int c2u = 0x34_05_0e_03;
    public static final int f2u = 0x34_06_0e_03;
    public static final int d2u = 0x34_07_0e_03;
    public static final int b2u = 0x34_08_0e_03;
    public static final int s2u = 0x34_09_0e_03;
    public static final int i2u = 0x34_0a_0e_03;
    public static final int l2u = 0x34_0b_0e_03;
    public static final int C2u = 0x34_0c_0e_03;
    public static final int D2u = 0x34_0d_0e_03;

    public static final int u2o = 0x34_0e_01_03;
    public static final int u2S = 0x34_0e_03_03;
    public static final int u2B = 0x34_0e_04_03;
    public static final int u2c = 0x34_0e_05_03;
    public static final int u2f = 0x34_0e_06_03;
    public static final int u2d = 0x34_0e_07_03;
    public static final int u2b = 0x34_0e_08_03;
    public static final int u2s = 0x34_0e_09_03;
    public static final int u2i = 0x34_0e_0a_03;
    public static final int u2l = 0x34_0e_0b_03;
    public static final int u2C = 0x34_0e_0c_03;
    public static final int u2D = 0x34_0e_0d_03;

    public static final int u2u = 0x34_0e_0e_03;

//    // cast object with type validation
//    // cast_object(target, src, to_type_code, to_type_class)
//    public static final int cast_object_vvtC = 0x34_01_00_04;
//
//    // cast to any, cast_to_any(target, target_type_code, target class, src, src_type_code, src class)
//    public static final int cast_to_any_vtCvtC = 0x34_01_01_06;
//
//    // Class to parameterized ScopedClassInterval
//    // cls2sbr_oCC(target, parameterized ScopedClassInterval class, a Scoped Class instance)
//    public static final int C2sbr_oCC = 0x34_01_05_03;
//
//    // extract Class instance from sbr, it's always at the 2nd filed "boxedClass",
//    // but sometimes the ClassInterval Instance may be null, we need handle it manually
//    // sbr2C(target, ScopedClassInterval instance, field)
//    public static final int sbr2C_Cov = 0x34_01_06_03;

    public static String getName(int code) {
//        switch (code) {
//            case cast_object_vvtC: return "cast_object_vvtC";
//            case cast_to_any_vtCvtC: return "cast_to_any_vtCvtC";
//            case C2sbr_oCC: return "C2sbr_oCC";
//            case sbr2C_Cov: return "sbr2C_Cov";
//        }

        var type1 = (code & 0x00ff0000) >> 16;
        var type2 = (code & 0x0000ff00) >> 8;
        String t1 = type1 >= TypeCode.GENERIC_TYPE_START ? "generic code (%d)".formatted(type1) : TypeCode.of(type1).toShortString();
        String t2 = type2 >= TypeCode.GENERIC_TYPE_START ? "generic code (%d)".formatted(type2) : TypeCode.of(type2).toShortString();
        return MessageFormat.format("{0}2{1}", t1, t2);
    }
}
