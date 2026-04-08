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
package org.siphonlab.ago.opcode.compare;

import org.siphonlab.ago.opcode.GenericOpCode;

public class UnionInstanceOf implements GenericOpCode {
    public static final int KIND_UNION_INSTANCE_OF = 0x35_000000;
    public static final int OP                  = 0x35;

    public static final int uinstanceof_g_vvC = 0x35_ff_01_03;

    // uinstanceof_i(target, v)
    // the length is 3 is for generic usage, for primitive always put 0
    public static final int uinstanceof_i_vv    = 0x35_0a_01_03;

    public static final int uinstanceof_S_vv = 0x35_03_01_03;
    public static final int uinstanceof_B_vv = 0x35_04_01_03;
    public static final int uinstanceof_c_vv = 0x35_05_01_03;
    public static final int uinstanceof_f_vv = 0x35_06_01_03;
    public static final int uinstanceof_d_vv = 0x35_07_01_03;
    public static final int uinstanceof_D_vv = 0x35_0d_01_03;
    public static final int uinstanceof_b_vv = 0x35_08_01_03;
    public static final int uinstanceof_s_vv = 0x35_09_01_03;
    public static final int uinstanceof_l_vv = 0x35_0b_01_03;


    public static final int uinstanceof_o_vvC = 0x35_01_01_03;
    public static final int uinstanceof_p_vvC = 0x35_01_02_03;       // determine is Primitive or PrimitiveNumber

    // like(target, classref, class interval)
    public static final int uinstanceof_C_vvC = 0x35_0c_02_03;

    public static String getName(int code){
        return switch(code){
            case uinstanceof_i_vv -> "uinstanceof_i_vv";
            case uinstanceof_S_vv -> "uinstanceof_S_vv";
            case uinstanceof_B_vv -> "uinstanceof_B_vv";
            case uinstanceof_c_vv -> "uinstanceof_c_vv";
            case uinstanceof_f_vv -> "uinstanceof_f_vv";
            case uinstanceof_d_vv -> "uinstanceof_d_vv";
            case uinstanceof_D_vv -> "uinstanceof_D_vv";
            case uinstanceof_b_vv -> "uinstanceof_b_vv";
            case uinstanceof_s_vv -> "uinstanceof_s_vv";
            case uinstanceof_l_vv -> "uinstanceof_l_vv";
            case uinstanceof_C_vvC -> "uinstanceof_C_vvC";
            case uinstanceof_o_vvC -> "uinstanceof_o_vvC";
            case uinstanceof_g_vvC -> "uinstanceof_g_vvC";
            case uinstanceof_p_vvC -> "uinstanceof_p_vvC";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
