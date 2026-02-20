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

public class Return implements GenericOpCode {
    public static final int KIND_RETURN = 0x06_000000;
    public static final int OP                  = 0x06;

    public static final int return_v    = 0x06_00_02_01;

    public static final int return_i_c = 0x06_0a_01_01;
    // return(slot)
    public static final int return_i_v = 0x06_0a_02_01;

    public static final int return_V   = 0x06_00_01_00;
    public static final int return_V_v = 0x06_00_02_01;

    public static final int return_B_c = 0x06_04_01_01;
    public static final int return_B_v = 0x06_04_02_01;

    public static final int return_c_c = 0x06_05_01_01;
    public static final int return_c_v = 0x06_05_02_01;

    public static final int return_f_c = 0x06_06_01_01;
    public static final int return_f_v = 0x06_06_02_01;

    public static final int return_d_c = 0x06_07_01_02;
    public static final int return_d_v = 0x06_07_02_01;

    public static final int return_b_c = 0x06_08_01_01;
    public static final int return_b_v = 0x06_08_02_01;

    public static final int return_s_c = 0x06_09_01_01;
    public static final int return_s_v = 0x06_09_02_01;

    public static final int return_l_c = 0x06_0b_01_02;
    public static final int return_l_v = 0x06_0b_02_01;

    public static final int return_o_v = 0x06_01_02_01;

    public static final int return_n   = 0x06_02_01_00;

    public static final int return_S_c = 0x06_03_01_01;
    public static final int return_S_v = 0x06_03_02_01;

    public static final int return_C_C = 0x06_0c_01_01;
    public static final int return_C_v = 0x06_0c_02_01;

    public static String getName(int code){
        return switch(code){
            case 0x06_00_01_00 -> "return_V";
            case 0x06_00_02_01 -> "return_V_v";
            case 0x06_04_01_01 -> "return_B_c";
            case 0x06_04_02_01 -> "return_B_v";
            case 0x06_05_01_01 -> "return_c_c";
            case 0x06_05_02_01 -> "return_c_v";
            case 0x06_06_01_01 -> "return_f_c";
            case 0x06_06_02_01 -> "return_f_v";
            case 0x06_07_01_02 -> "return_d_c";
            case 0x06_07_02_01 -> "return_d_v";
            case 0x06_08_01_01 -> "return_b_c";
            case 0x06_08_02_01 -> "return_b_v";
            case 0x06_09_01_01 -> "return_s_c";
            case 0x06_09_02_01 -> "return_s_v";
            case 0x06_0a_01_01 -> "return_i_c";
            case 0x06_0a_02_01 -> "return_i_v";
            case 0x06_0b_01_02 -> "return_l_c";
            case 0x06_0b_02_01 -> "return_l_v";
            case 0x06_01_01_01 -> "return_o_c";
            case 0x06_01_02_01 -> "return_o_v";
            case 0x06_02_01_00 -> "return_n";
            case 0x06_03_01_01 -> "return_S_c";
            case 0x06_03_02_01 -> "return_S_v";
            case 0x06_0c_01_01 -> "return_C_C";
            case 0x06_0c_02_01 -> "return_C_v";
            default -> {
                var t = OpCode.extractType(code);
                if(t >= TypeCode.GENERIC_TYPE_START){
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch ((code & OpCode.DTYPE_MASK_NEG)){
                        case return_v: yield "return_G[%s]_v".formatted(t);
                    };
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
