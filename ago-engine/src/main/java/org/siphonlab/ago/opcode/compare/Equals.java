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

public class Equals implements GenericOpCode {  
    public static final int KIND_EQUALS = 0x09_000000;
    public static final int OP                  = 0x09;

    public static final int equals_g_vvv = 0x09_ff_04_03;

    // equals_i_vvc(target slot, slot, const),
    public static final int equals_i_vvc = 0x09_0a_02_03;
    // equals_i_vvv(target slot, slot1, slot2)
    public static final int equals_i_vvv = 0x09_0a_04_03;

    public static final int equals_f_vvc   = 0x09_06_02_03;
    public static final int equals_f_vvv   = 0x09_06_04_03;

    // double need 8 bytes
    public static final int equals_d_vvcc  = 0x09_07_02_04;
    public static final int equals_d_vvv   = 0x09_07_04_03;

    public static final int equals_b_vvc   = 0x09_08_02_03;
    public static final int equals_b_vvv   = 0x09_08_04_03;

    public static final int equals_c_vvc   = 0x09_05_02_03;
    public static final int equals_c_vvv   = 0x09_05_04_03;

    public static final int equals_s_vvc   = 0x09_09_02_03;
    public static final int equals_s_vvv   = 0x09_09_04_03;

    // long need 8 bytes
    public static final int equals_l_vvcc  = 0x09_0b_02_04;
    public static final int equals_l_vvv   = 0x09_0b_04_03;

    // string
    public static final int equals_S_vvc = 0x09_03_02_03;
    public static final int equals_S_vvv = 0x09_03_04_03;

    public static final int equals_B_vvc = 0x09_04_02_03;
    public static final int equals_B_vvv = 0x09_04_04_03;

    public static final int equals_o_vvn = 0x09_01_02_02;
    public static final int equals_o_vvv = 0x09_01_04_03;

    public static final int equals_C_vvc = 0x09_0c_02_03;
    public static final int equals_C_vvv = 0x09_0c_04_03;


    public static String getName(int code){
        return switch(code){
            case equals_i_vvc -> "equals_i_vvc";
            case equals_i_vvv -> "equals_i_vvv";

            case equals_f_vvc -> "equals_f_vvc";
            case equals_f_vvv -> "equals_f_vvv";

            case equals_d_vvcc -> "equals_d_vvcc";
            case equals_d_vvv -> "equals_d_vvv";

            case equals_b_vvc -> "equals_b_vvc";
            case equals_b_vvv -> "equals_b_vvv";

            case equals_c_vvc -> "equals_c_vvc";
            case equals_c_vvv -> "equals_c_vvv";

            case equals_s_vvc -> "equals_s_vvc";
            case equals_s_vvv -> "equals_s_vvv";

            case equals_l_vvcc -> "equals_l_vvcc";
            case equals_l_vvv -> "equals_l_vvv";

            case equals_S_vvc -> "equals_S_vvc";
            case equals_S_vvv -> "equals_S_vvv";

            case equals_B_vvc -> "equals_B_vvc";
            case equals_B_vvv -> "equals_B_vvv";

            case equals_o_vvn -> "equals_o_vvn";
            case equals_o_vvv -> "equals_o_vvv";

            case equals_C_vvc -> "equals_C_vvc";
            case equals_C_vvv -> "equals_C_vvv";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case equals_g_vvv:
                            yield "equals_g[%s]_vv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
