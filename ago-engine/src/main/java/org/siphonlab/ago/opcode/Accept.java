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

// copy result from runspace.resultSlots to my slot, invoke at caller side
public class Accept {
    public static final int KIND_ACCEPT = 0x33_000000;
    public static final int OP = 0x33;

    public static final int accept_v = 0x33_00_01_01;

    // accept(slot)

    public static final int accept_V_v = 0x33_00_01_01;     // only happened in generic, if I know it's void, no `accept_*` output

    public static final int accept_o_v = 0x33_01_01_01;

    public static final int accept_n_v = 0x33_02_01_01;

    public static final int accept_S_v = 0x33_03_01_01;

    public static final int accept_B_v = 0x33_04_01_01;

    public static final int accept_c_v = 0x33_05_01_01;

    public static final int accept_f_v = 0x33_06_01_01;

    public static final int accept_d_v = 0x33_07_01_01;

    public static final int accept_b_v = 0x33_08_01_01;

    public static final int accept_s_v = 0x33_09_01_01;

    public static final int accept_i_v = 0x33_0a_01_01;

    public static final int accept_l_v = 0x33_0b_01_01;

    public static final int accept_C_v = 0x33_0c_01_01;

    public static final int accept_any_v = 0x33_00_02_01;     // accept any and cast to Object, the original data type was recorded in result slot, and the destination type must be Object

    public static String getName(int code) {
        return switch (code) {
            case accept_V_v -> "accept_V_v";
            case accept_o_v -> "accept_o_v";
            case accept_n_v -> "accept_n_v";
            case accept_S_v -> "accept_S_v";
            case accept_B_v -> "accept_B_v";
            case accept_c_v -> "accept_c_v";
            case accept_f_v -> "accept_f_v";
            case accept_d_v -> "accept_d_v";
            case accept_b_v -> "accept_b_v";
            case accept_s_v -> "accept_s_v";
            case accept_i_v -> "accept_i_v";
            case accept_l_v -> "accept_l_v";
            case accept_C_v -> "accept_C_v";
            case accept_any_v -> "accept_any_v";
            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch ((code & OpCode.DTYPE_MASK_NEG)) {
                        case accept_v:
                            yield "accept_G[%s]_v".formatted(t);
                    }
                    ;
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
