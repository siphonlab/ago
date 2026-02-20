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

public class InstanceOf implements GenericOpCode {  
    public static final int KIND_INSTANCE_OF = 0x31_000000;
    public static final int OP                  = 0x31;

    public static final int instanceof_g_vvC = 0x31_ff_01_03;

    // instanceof_i(target, v)
    // the length is 3 is for generic usage, for primitive always put 0
    public static final int instanceof_i_vv    = 0x31_0a_01_03;

    public static final int instanceof_S_vv = 0x31_03_01_03;
    public static final int instanceof_B_vv = 0x31_04_01_03;
    public static final int instanceof_c_vv = 0x31_05_01_03;
    public static final int instanceof_f_vv = 0x31_06_01_03;
    public static final int instanceof_d_vv = 0x31_07_01_03;
    public static final int instanceof_b_vv = 0x31_08_01_03;
    public static final int instanceof_s_vv = 0x31_09_01_03;
    public static final int instanceof_l_vv = 0x31_0b_01_03;


    public static final int instanceof_o_vvC = 0x31_01_01_03;
    public static final int instanceof_p_vvC = 0x31_01_02_03;       // determine is Primitive or PrimitiveNumber

    // like(target, classref, class interval)
    public static final int instanceof_C_vvC = 0x31_0c_02_03;

    public static String getName(int code){
        return switch(code){
            case instanceof_i_vv -> "instanceof_i_vv";
            case instanceof_S_vv -> "instanceof_S_vv";
            case instanceof_B_vv -> "instanceof_B_vv";
            case instanceof_c_vv -> "instanceof_c_vv";
            case instanceof_f_vv -> "instanceof_f_vv";
            case instanceof_d_vv -> "instanceof_d_vv";
            case instanceof_b_vv -> "instanceof_b_vv";
            case instanceof_s_vv -> "instanceof_s_vv";
            case instanceof_l_vv -> "instanceof_l_vv";
            case instanceof_C_vvC -> "instanceof_C_vvC";
            case instanceof_o_vvC -> "instanceof_o_vvC";
            case instanceof_g_vvC -> "instanceof_g_vvC";
            case instanceof_p_vvC -> "instanceof_p_vvC";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
