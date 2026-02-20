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

public class GreaterEquals implements GenericOpCode {  
    public static final int KIND_GREATER_EQUALS = 0x11_000000;
    public static final int OP                  = 0x11;

    public static final int ge_g_vvv = 0x11_ff_04_03;

    // ge_i_vvc(target slot, slot, const),
    public static final int ge_i_vvc = 0x11_0a_02_03;
    // ge_i_vvv(target slot, slot1, slot2)
    public static final int ge_i_vvv = 0x11_0a_04_03;

    public static final int ge_f_vvc   = 0x11_06_02_03;
    public static final int ge_f_vvv   = 0x11_06_04_03;

    // double need 8 bytes
    public static final int ge_d_vvcc  = 0x11_07_02_04;
    public static final int ge_d_vvv   = 0x11_07_04_03;

    public static final int ge_b_vvc   = 0x11_08_02_03;
    public static final int ge_b_vvv   = 0x11_08_04_03;

    public static final int ge_c_vvc   = 0x11_05_02_03;
    public static final int ge_c_vvv   = 0x11_05_04_03;

    public static final int ge_s_vvc   = 0x11_09_02_03;
    public static final int ge_s_vvv   = 0x11_09_04_03;

    // long need 8 bytes
    public static final int ge_l_vvcc  = 0x11_0b_02_04;
    public static final int ge_l_vvv   = 0x11_0b_04_03;

    // string
    public static final int ge_S_vvc = 0x11_03_02_03;
    public static final int ge_S_vvv = 0x11_03_04_03;

    public static final int ge_B_vvc = 0x11_04_02_03;
    public static final int ge_B_vvv = 0x11_04_04_03;

    public static final int ge_o_vvc = 0x11_01_02_03;
    public static final int ge_o_vvv = 0x11_01_04_03;

    public static final int ge_C_vvc = 0x11_0c_02_03;
    public static final int ge_C_vvv = 0x11_0c_04_03;


    public static String getName(int code){
        return switch(code){
            case ge_i_vvc -> "ge_i_vvc";
            case ge_i_vvv -> "ge_i_vvv";

            case ge_f_vvc -> "ge_f_vvc";
            case ge_f_vvv -> "ge_f_vvv";

            case ge_d_vvcc -> "ge_d_vvcc";
            case ge_d_vvv -> "ge_d_vvv";

            case ge_b_vvc -> "ge_b_vvc";
            case ge_b_vvv -> "ge_b_vvv";

            case ge_c_vvc -> "ge_c_vvc";
            case ge_c_vvv -> "ge_c_vvv";

            case ge_s_vvc -> "ge_s_vvc";
            case ge_s_vvv -> "ge_s_vvv";

            case ge_l_vvcc -> "ge_l_vvcc";
            case ge_l_vvv -> "ge_l_vvv";

            case ge_S_vvc -> "ge_S_vvc";
            case ge_S_vvv -> "ge_S_vvv";

            case ge_B_vvc -> "ge_B_vvc";
            case ge_B_vvv -> "ge_B_vvv";

            case ge_o_vvc -> "ge_o_vvc";
            case ge_o_vvv -> "ge_o_vvv";

            case ge_C_vvc -> "ge_C_vvc";
            case ge_C_vvv -> "ge_C_vvv";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case ge_g_vvv:
                            yield "ge_g[%s]_vvv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
