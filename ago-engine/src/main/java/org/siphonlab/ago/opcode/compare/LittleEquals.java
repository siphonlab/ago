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

public class LittleEquals implements GenericOpCode {  
    public static final int KIND_LITTLE_EQUALS = 0x12_000000;
    public static final int OP                  = 0x12;

    public static final int le_g_vvv = 0x12_ff_04_03;

    // le_i_vvc(target slot, slot, const),
    public static final int le_i_vvc = 0x12_0a_02_03;
    // le_i_vvv(target slot, slot1, slot2)
    public static final int le_i_vvv = 0x12_0a_04_03;

    public static final int le_f_vvc   = 0x12_06_02_03;
    public static final int le_f_vvv   = 0x12_06_04_03;

    // double need 8 bytes
    public static final int le_d_vvcc  = 0x12_07_02_04;
    public static final int le_d_vvv   = 0x12_07_04_03;

    public static final int le_b_vvc   = 0x12_08_02_03;
    public static final int le_b_vvv   = 0x12_08_04_03;

    public static final int le_c_vvc   = 0x12_05_02_03;
    public static final int le_c_vvv   = 0x12_05_04_03;

    public static final int le_s_vvc   = 0x12_09_02_03;
    public static final int le_s_vvv   = 0x12_09_04_03;

    // long need 8 bytes
    public static final int le_l_vvcc  = 0x12_0b_02_04;
    public static final int le_l_vvv   = 0x12_0b_04_03;

    // string
    public static final int le_S_vvc = 0x12_03_02_03;
    public static final int le_S_vvv = 0x12_03_04_03;

    public static final int le_B_vvc = 0x12_04_02_03;
    public static final int le_B_vvv = 0x12_04_04_03;

    public static final int le_o_vvc = 0x12_01_02_03;
    public static final int le_o_vvv = 0x12_01_04_03;

    public static final int le_C_vvc = 0x12_0c_02_03;
    public static final int le_C_vvv = 0x12_0c_04_03;


    public static String getName(int code){
        return switch(code){
            case le_i_vvc -> "le_i_vvc";
            case le_i_vvv -> "le_i_vvv";

            case le_f_vvc -> "le_f_vvc";
            case le_f_vvv -> "le_f_vvv";

            case le_d_vvcc -> "le_d_vvcc";
            case le_d_vvv -> "le_d_vvv";

            case le_b_vvc -> "le_b_vvc";
            case le_b_vvv -> "le_b_vvv";

            case le_c_vvc -> "le_c_vvc";
            case le_c_vvv -> "le_c_vvv";

            case le_s_vvc -> "le_s_vvc";
            case le_s_vvv -> "le_s_vvv";

            case le_l_vvcc -> "le_l_vvcc";
            case le_l_vvv -> "le_l_vvv";

            case le_S_vvc -> "le_S_vvc";
            case le_S_vvv -> "le_S_vvv";

            case le_B_vvc -> "le_B_vvc";
            case le_B_vvv -> "le_B_vvv";

            case le_o_vvc -> "le_o_vvc";
            case le_o_vvv -> "le_o_vvv";

            case le_C_vvc -> "le_C_vvc";
            case le_C_vvv -> "le_C_vvv";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case le_g_vvv:
                            yield "le_g[%s]_vvv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
