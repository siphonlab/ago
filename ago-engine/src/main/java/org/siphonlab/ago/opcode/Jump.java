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

public class Jump {
    public static final int KIND_JUMP = 0x0e_000000;
    public static final int OP                  = 0x0e;

    public static final int jump_c =  0x0e_00_01_01;
    public static final int jnz_v  = 0x0e_00_02_01;     // jump to variable if its value is not zero

    // switch(tableId, v)
    public static final int switch_dense_cv =  0x0e_00_05_02;

    public static final int switch_sparse_cv =  0x0e_00_06_02;

    public static final int jump_t_vc = 0x0e_00_03_02;
    public static final int jump_f_vc = 0x0e_00_04_02;

    public static final int jump_t_B_vc = 0x0e_04_03_02;
    public static final int jump_f_B_vc = 0x0e_04_04_02;
    public static final int jump_t_i_vc = 0x0e_0a_03_02;
    public static final int jump_f_i_vc = 0x0e_0a_04_02;
    public static final int jump_t_C_vc = 0x0e_0c_03_02;
    public static final int jump_f_C_vc = 0x0e_0c_04_02;
    public static final int jump_t_c_vc = 0x0e_05_03_02;
    public static final int jump_f_c_vc = 0x0e_05_04_02;
    public static final int jump_t_f_vc = 0x0e_06_03_02;
    public static final int jump_f_f_vc = 0x0e_06_04_02;
    public static final int jump_t_d_vc = 0x0e_07_03_02;
    public static final int jump_f_d_vc = 0x0e_07_04_02;
    public static final int jump_t_b_vc = 0x0e_08_03_02;
    public static final int jump_f_b_vc = 0x0e_08_04_02;
    public static final int jump_t_s_vc = 0x0e_09_03_02;
    public static final int jump_f_s_vc = 0x0e_09_04_02;
    public static final int jump_t_l_vc = 0x0e_0b_03_02;
    public static final int jump_f_l_vc = 0x0e_0b_04_02;
    public static final int jump_t_o_vc = 0x0e_01_03_02;
    public static final int jump_f_o_vc = 0x0e_01_04_02;
    public static final int jump_t_S_vc = 0x0e_03_03_02;
    public static final int jump_f_S_vc = 0x0e_03_04_02;

    public static String getName(int code) {
        return switch (code) {
            case jump_c -> "jump_c";
            case jnz_v -> "jnz_v";

            case switch_dense_cv -> "switch_dense_cv";
            case switch_sparse_cv -> "switch_sparse_cv";

            case jump_t_B_vc -> "jump_t_B_vc";
            case jump_f_B_vc -> "jump_f_B_vc";
            case jump_t_i_vc -> "jump_t_i_vc";
            case jump_f_i_vc -> "jump_f_i_vc";
            case jump_t_C_vc -> "jump_t_C_vc";
            case jump_f_C_vc -> "jump_f_C_vc";
            case jump_t_c_vc -> "jump_t_c_vc";
            case jump_f_c_vc -> "jump_f_c_vc";
            case jump_t_f_vc -> "jump_t_f_vc";
            case jump_f_f_vc -> "jump_f_f_vc";
            case jump_t_d_vc -> "jump_t_d_vc";
            case jump_f_d_vc -> "jump_f_d_vc";
            case jump_t_b_vc -> "jump_t_b_vc";
            case jump_f_b_vc -> "jump_f_b_vc";
            case jump_t_s_vc -> "jump_t_s_vc";
            case jump_f_s_vc -> "jump_f_s_vc";
            case jump_t_l_vc -> "jump_t_l_vc";
            case jump_f_l_vc -> "jump_f_l_vc";
            case jump_t_o_vc -> "jump_t_o_vc";
            case jump_f_o_vc -> "jump_f_o_vc";
            case jump_t_S_vc -> "jump_t_S_vc";
            case jump_f_S_vc -> "jump_f_S_vc";


            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch ((code & OpCode.DTYPE_MASK_NEG)) {
                        case jump_t_vc:
                            yield "jump_t_[%s]_vv".formatted(t);
                        case jump_f_vc:
                            yield "jump_f_[%s]_vv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
