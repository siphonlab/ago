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

public class LittleThan implements GenericOpCode {
    public static final int KIND_LITTLE_THAN = 0x10_000000;
    public static final int OP                  = 0x10;

    public static final int lt_g_vvv = 0x10_ff_04_03;

    // lt_i_vvc(target slot, slot, const),
    public static final int lt_i_vvc = 0x10_0a_02_03;
    // lt_i_vvv(target slot, slot1, slot2)
    public static final int lt_i_vvv = 0x10_0a_04_03;

    public static final int lt_f_vvc   = 0x10_06_02_03;
    public static final int lt_f_vvv   = 0x10_06_04_03;

    // double need 8 bytes
    public static final int lt_d_vvcc  = 0x10_07_02_04;
    public static final int lt_d_vvv   = 0x10_07_04_03;

    public static final int lt_b_vvc   = 0x10_08_02_03;
    public static final int lt_b_vvv   = 0x10_08_04_03;

    public static final int lt_c_vvc   = 0x10_05_02_03;
    public static final int lt_c_vvv   = 0x10_05_04_03;

    public static final int lt_s_vvc   = 0x10_09_02_03;
    public static final int lt_s_vvv   = 0x10_09_04_03;

    // long need 8 bytes
    public static final int lt_l_vvcc  = 0x10_0b_02_04;
    public static final int lt_l_vvv   = 0x10_0b_04_03;

    // string
    public static final int lt_S_vvc = 0x10_03_02_03;
    public static final int lt_S_vvv = 0x10_03_04_03;

    public static final int lt_B_vvc = 0x10_04_02_03;
    public static final int lt_B_vvv = 0x10_04_04_03;

    public static final int lt_o_vvc = 0x10_01_02_03;
    public static final int lt_o_vvv = 0x10_01_04_03;

    public static final int lt_C_vvc = 0x10_0c_02_03;
    public static final int lt_C_vvv = 0x10_0c_04_03;


    public static String getName(int code){
        return switch(code){
            case lt_i_vvc -> "lt_i_vvc";
            case lt_i_vvv -> "lt_i_vvv";

            case lt_f_vvc -> "lt_f_vvc";
            case lt_f_vvv -> "lt_f_vvv";

            case lt_d_vvcc -> "lt_d_vvcc";
            case lt_d_vvv -> "lt_d_vvv";

            case lt_b_vvc -> "lt_b_vvc";
            case lt_b_vvv -> "lt_b_vvv";

            case lt_c_vvc -> "lt_c_vvc";
            case lt_c_vvv -> "lt_c_vvv";

            case lt_s_vvc -> "lt_s_vvc";
            case lt_s_vvv -> "lt_s_vvv";

            case lt_l_vvcc -> "lt_l_vvcc";
            case lt_l_vvv -> "lt_l_vvv";

            case lt_S_vvc -> "lt_S_vvc";
            case lt_S_vvv -> "lt_S_vvv";

            case lt_B_vvc -> "lt_B_vvc";
            case lt_B_vvv -> "lt_B_vvv";

            case lt_o_vvc -> "lt_o_vvc";
            case lt_o_vvv -> "lt_o_vvv";

            case lt_C_vvc -> "lt_C_vvc";
            case lt_C_vvv -> "lt_C_vvv";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case lt_g_vvv:
                            yield "lt_g[%s]_vvv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
