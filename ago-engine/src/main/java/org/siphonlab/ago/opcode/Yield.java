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

public class Yield implements GenericOpCode {
    public static final int KIND_YIELD = 0x35_000000;
    public static final int OP                  = 0x35;

    public static final int yield_v    = 0x35_00_02_01;

    public static final int yield_i_c = 0x35_0a_01_01;
    // yield(slot)
    public static final int yield_i_v = 0x35_0a_02_01;

    public static final int yield_B_c = 0x35_04_01_01;
    public static final int yield_B_v = 0x35_04_02_01;

    public static final int yield_c_c = 0x35_05_01_01;
    public static final int yield_c_v = 0x35_05_02_01;

    public static final int yield_f_c = 0x35_06_01_01;
    public static final int yield_f_v = 0x35_06_02_01;

    public static final int yield_d_c = 0x35_07_01_02;
    public static final int yield_d_v = 0x35_07_02_01;

    public static final int yield_D_c = 0x35_0d_01_02;
    public static final int yield_D_v = 0x35_0d_02_01;

    public static final int yield_b_c = 0x35_08_01_01;
    public static final int yield_b_v = 0x35_08_02_01;

    public static final int yield_s_c = 0x35_09_01_01;
    public static final int yield_s_v = 0x35_09_02_01;

    public static final int yield_l_c = 0x35_0b_01_02;
    public static final int yield_l_v = 0x35_0b_02_01;

    public static final int yield_o_v = 0x35_01_02_01;

    public static final int yield_n   = 0x35_02_01_00;

    public static final int yield_S_c = 0x35_03_01_01;
    public static final int yield_S_v = 0x35_03_02_01;

    public static final int yield_C_C = 0x35_0c_01_01;
    public static final int yield_C_v = 0x35_0c_02_01;

    public static final int yield_u_v = 0x35_0e_02_01;

    public static String getName(int code){
        return switch(code){
            case 0x35_00_02_01 -> "yield_V_v";
            case 0x35_04_01_01 -> "yield_B_c";
            case 0x35_04_02_01 -> "yield_B_v";
            case 0x35_05_01_01 -> "yield_c_c";
            case 0x35_05_02_01 -> "yield_c_v";
            case 0x35_06_01_01 -> "yield_f_c";
            case 0x35_06_02_01 -> "yield_f_v";
            case 0x35_07_01_02 -> "yield_d_c";
            case 0x35_07_02_01 -> "yield_d_v";
            case 0x35_08_01_01 -> "yield_b_c";
            case 0x35_08_02_01 -> "yield_b_v";
            case 0x35_09_01_01 -> "yield_s_c";
            case 0x35_09_02_01 -> "yield_s_v";
            case 0x35_0a_01_01 -> "yield_i_c";
            case 0x35_0a_02_01 -> "yield_i_v";
            case 0x35_0b_01_02 -> "yield_l_c";
            case 0x35_0b_02_01 -> "yield_l_v";
            case 0x35_01_01_01 -> "yield_o_c";
            case 0x35_01_02_01 -> "yield_o_v";
            case 0x35_02_01_00 -> "yield_n";
            case 0x35_03_01_01 -> "yield_S_c";
            case 0x35_03_02_01 -> "yield_S_v";
            case 0x35_0c_01_01 -> "yield_C_C";
            case 0x35_0c_02_01 -> "yield_C_v";
            case 0x35_0d_01_01 -> "yield_D_C";
            case 0x35_0d_02_01 -> "yield_D_v";
            case 0x35_0e_01_01 -> "yield_u_C";
            case 0x35_0e_02_01 -> "yield_u_v";
            default -> {
                var t = OpCode.extractType(code);
                if(t >= TypeCode.GENERIC_TYPE_START){
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch ((code & OpCode.DTYPE_MASK_NEG)){
                        case yield_v: yield "yield_G[%s]_v".formatted(t);
                    };
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
