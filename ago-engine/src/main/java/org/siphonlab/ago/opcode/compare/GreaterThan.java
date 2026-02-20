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

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.opcode.GenericOpCode;
import org.siphonlab.ago.opcode.OpCode;

public class GreaterThan implements GenericOpCode {  
    public static final int KIND_GREATER_THAN = 0x0f_000000;
    public static final int OP                  = 0x0f;

    public static final int gt_g_vvv = 0x0f_ff_04_03;

    // gt_i_vvc(target slot, slot, const),
    public static final int gt_i_vvc = 0x0f_0a_02_03;
    // gt_i_vvv(target slot, slot1, slot2)
    public static final int gt_i_vvv = 0x0f_0a_04_03;

    public static final int gt_f_vvc   = 0x0f_06_02_03;
    public static final int gt_f_vvv   = 0x0f_06_04_03;

    // double need 8 bytes
    public static final int gt_d_vvcc  = 0x0f_07_02_04;
    public static final int gt_d_vvv   = 0x0f_07_04_03;

    public static final int gt_b_vvc   = 0x0f_08_02_03;
    public static final int gt_b_vvv   = 0x0f_08_04_03;

    public static final int gt_c_vvc   = 0x0f_05_02_03;
    public static final int gt_c_vvv   = 0x0f_05_04_03;

    public static final int gt_s_vvc   = 0x0f_09_02_03;
    public static final int gt_s_vvv   = 0x0f_09_04_03;

    // long need 8 bytes
    public static final int gt_l_vvcc  = 0x0f_0b_02_04;
    public static final int gt_l_vvv   = 0x0f_0b_04_03;

    // string
    public static final int gt_S_vvc = 0x0f_03_02_03;
    public static final int gt_S_vvv = 0x0f_03_04_03;

    public static final int gt_B_vvc = 0x0f_04_02_03;
    public static final int gt_B_vvv = 0x0f_04_04_03;

    public static final int gt_o_vvc = 0x0f_01_02_03;
    public static final int gt_o_vvv = 0x0f_01_04_03;

    public static final int gt_C_vvc = 0x0f_0c_02_03;
    public static final int gt_C_vvv = 0x0f_0c_04_03;


    public static String getName(int code){
        return switch(code){
            case gt_i_vvc -> "gt_i_vvc";
            case gt_i_vvv -> "gt_i_vvv";

            case gt_f_vvc -> "gt_f_vvc";
            case gt_f_vvv -> "gt_f_vvv";

            case gt_d_vvcc -> "gt_d_vvcc";
            case gt_d_vvv -> "gt_d_vvv";

            case gt_b_vvc -> "gt_b_vvc";
            case gt_b_vvv -> "gt_b_vvv";

            case gt_c_vvc -> "gt_c_vvc";
            case gt_c_vvv -> "gt_c_vvv";

            case gt_s_vvc -> "gt_s_vvc";
            case gt_s_vvv -> "gt_s_vvv";

            case gt_l_vvcc -> "gt_l_vvcc";
            case gt_l_vvv -> "gt_l_vvv";

            case gt_S_vvc -> "gt_S_vvc";
            case gt_S_vvv -> "gt_S_vvv";

            case gt_B_vvc -> "gt_B_vvc";
            case gt_B_vvv -> "gt_B_vvv";

            case gt_o_vvc -> "gt_o_vvc";
            case gt_o_vvv -> "gt_o_vvv";

            case gt_C_vvc -> "gt_C_vvc";
            case gt_C_vvv -> "gt_C_vvv";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case gt_g_vvv:
                            yield "gt_g[%s]_vvv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
