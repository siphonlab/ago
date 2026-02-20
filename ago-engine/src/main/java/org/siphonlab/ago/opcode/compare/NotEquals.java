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

public class NotEquals implements GenericOpCode {  
    public static final int KIND_NOT_EQUALS = 0x0a_000000;
    public static final int OP                  = 0x0a;

    public static final int ne_g_vvv = 0x0a_ff_04_03;

    public static final int ne_i_vvc = 0x0a_0a_02_03;
    public static final int ne_i_vvv = 0x0a_0a_04_03;

    public static final int ne_f_vvc   = 0x0a_06_02_03;
    public static final int ne_f_vvv   = 0x0a_06_04_03;

    // double need 8 bytes
    public static final int ne_d_vvcc  = 0x0a_07_02_04;
    public static final int ne_d_vvv   = 0x0a_07_04_03;

    public static final int ne_b_vvc   = 0x0a_08_02_03;
    public static final int ne_b_vvv   = 0x0a_08_04_03;

    public static final int ne_c_vvc   = 0x0a_05_02_03;
    public static final int ne_c_vvv   = 0x0a_05_04_03;

    public static final int ne_s_vvc   = 0x0a_09_02_03;
    public static final int ne_s_vvv   = 0x0a_09_04_03;

    // long need 8 bytes
    public static final int ne_l_vvcc  = 0x0a_0b_02_04;
    public static final int ne_l_vvv   = 0x0a_0b_04_03;

    // string
    public static final int ne_S_vvc = 0x0a_03_02_03;
    public static final int ne_S_vvv = 0x0a_03_04_03;

    public static final int ne_B_vvc = 0x0a_04_02_03;
    public static final int ne_B_vvv = 0x0a_04_04_03;

    public static final int ne_o_vvn = 0x0a_01_02_02;
    public static final int ne_o_vvv = 0x0a_01_04_03;

    public static final int ne_C_vvc = 0x0a_0c_02_03;
    public static final int ne_C_vvv = 0x0a_0c_04_03;


    public static String getName(int code){
        return switch(code){
            case ne_i_vvc -> "ne_i_vvc";
            case ne_i_vvv -> "ne_i_vvv";

            case ne_f_vvc -> "ne_f_vvc";
            case ne_f_vvv -> "ne_f_vvv";

            case ne_d_vvcc -> "ne_d_vvcc";
            case ne_d_vvv -> "ne_d_vvv";

            case ne_b_vvc -> "ne_b_vvc";
            case ne_b_vvv -> "ne_b_vvv";

            case ne_c_vvc -> "ne_c_vvc";
            case ne_c_vvv -> "ne_c_vvv";

            case ne_s_vvc -> "ne_s_vvc";
            case ne_s_vvv -> "ne_s_vvv";

            case ne_l_vvcc -> "ne_l_vvcc";
            case ne_l_vvv -> "ne_l_vvv";

            case ne_S_vvc -> "ne_S_vvc";
            case ne_S_vvv -> "ne_S_vvv";

            case ne_B_vvc -> "ne_B_vvc";
            case ne_B_vvv -> "ne_B_vvv";

            case ne_o_vvn -> "ne_o_vvn";
            case ne_o_vvv -> "ne_o_vvv";

            case ne_C_vvc -> "ne_C_vvc";
            case ne_C_vvv -> "ne_C_vvv";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case ne_g_vvv:
                            yield "ne_g[%s]_vvv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }

        };
    }

}
