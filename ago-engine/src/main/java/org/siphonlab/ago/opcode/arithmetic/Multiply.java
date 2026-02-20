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
package org.siphonlab.ago.opcode.arithmetic;

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.opcode.OpCode;

public class Multiply {  
    public static final int KIND_MULTIPLY = 0x15_000000;
    public static final int OP                  = 0x15;

    public static final int mul_vc = 0x15_00_01_02;
    public static final int mul_vvc = 0x15_00_02_03;
    public static final int mul_vv = 0x15_00_03_02;
    public static final int mul_vvv = 0x15_00_04_03;

    public static final int mul_g_vv = 0x15_ff_03_02;
    public static final int mul_g_vvv = 0x15_ff_04_03;

    // mul_i_vc(slot, const)
    public static final int mul_i_vc    = 0x15_0a_01_02;
    // mul_i_vvc(target slot, slot, const),
    public static final int mul_i_vvc = 0x15_0a_02_03;
    // mul_i_vv(slot1, slot2) result put into slot1
    public static final int mul_i_vv    = 0x15_0a_03_02;
    // mul_i_vvv(target slot, slot1, slot2)
    public static final int mul_i_vvv   = 0x15_0a_04_03;

    public static final int mul_f_vc    = 0x15_06_01_02;
    public static final int mul_f_vvc   = 0x15_06_02_03;
    public static final int mul_f_vv    = 0x15_06_03_02;
    public static final int mul_f_vvv   = 0x15_06_04_03;

    // double need 8 bytes
    public static final int mul_d_vc    = 0x15_07_01_03;
    public static final int mul_d_vvc   = 0x15_07_02_04;
    public static final int mul_d_vv    = 0x15_07_03_02;
    public static final int mul_d_vvv   = 0x15_07_04_03;

    public static final int mul_b_vc    = 0x15_08_01_02;
    public static final int mul_b_vvc   = 0x15_08_02_03;
    public static final int mul_b_vv    = 0x15_08_03_02;
    public static final int mul_b_vvv   = 0x15_08_04_03;

    public static final int mul_s_vc    = 0x15_09_01_02;
    public static final int mul_s_vvc   = 0x15_09_02_03;
    public static final int mul_s_vv    = 0x15_09_03_02;
    public static final int mul_s_vvv   = 0x15_09_04_03;

    // long need 8 bytes
    public static final int mul_l_vc    = 0x15_0b_01_03;
    public static final int mul_l_vvc   = 0x15_0b_02_04;
    public static final int mul_l_vv    = 0x15_0b_03_02;
    public static final int mul_l_vvv   = 0x15_0b_04_03;

    public static String getName(int code){
        return switch(code){
            case 0x15_05_01_02 ->  "mul_c_vc";
            case 0x15_05_02_03 ->  "mul_c_vvc";
            case 0x15_05_03_02 ->  "mul_c_vv";
            case 0x15_05_04_03 ->  "mul_c_vvv";
            case 0x15_05_05_03 ->  "mul_c_vcv";

            case 0x15_06_01_02 ->  "mul_f_vc";
            case 0x15_06_02_03 ->  "mul_f_vvc";
            case 0x15_06_03_02 ->  "mul_f_vv";
            case 0x15_06_04_03 ->  "mul_f_vvv";
            case 0x15_06_05_03 ->  "mul_f_vcv";

            case 0x15_07_01_03 ->  "mul_d_vc";
            case 0x15_07_02_04 ->  "mul_d_vvc";
            case 0x15_07_03_02 ->    "mul_d_vv";
            case 0x15_07_04_03 ->    "mul_d_vvv";
            case 0x15_07_05_04 ->  "mul_d_vcv";

            case 0x15_08_01_02 ->  "mul_b_vc";
            case 0x15_08_02_03 ->  "mul_b_vvc";
            case 0x15_08_03_02 ->    "mul_b_vv";
            case 0x15_08_04_03 ->    "mul_b_vvv";
            case 0x15_08_05_03 ->  "mul_b_vcv";

            case 0x15_09_01_02 ->  "mul_s_vc";
            case 0x15_09_02_03 ->  "mul_s_vvc";
            case 0x15_09_03_02 ->    "mul_s_vv";
            case 0x15_09_04_03 ->    "mul_s_vvv";
            case 0x15_09_05_03 ->  "mul_s_vcv";

            case 0x15_0a_01_02 ->  "mul_i_vc";
            case 0x15_0a_02_03 ->  "mul_i_vvc";
            case 0x15_0a_03_02 ->    "mul_i_vv";
            case 0x15_0a_04_03 ->    "mul_i_vvv";
            case 0x15_0a_05_03 ->  "mul_i_vcv";

            case 0x15_0b_01_03 ->  "mul_l_vc";
            case 0x15_0b_02_04 ->  "mul_l_vvc";
            case 0x15_0b_03_02 ->    "mul_l_vv";
            case 0x15_0b_04_03 ->    "mul_l_vvv";
            case 0x15_0b_05_04 ->  "mul_l_vcv";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case mul_g_vv:
                            yield "mul_g[%s]_vv".formatted(t);
                        case mul_g_vvv:
                            yield "mul_g[%s]_vvv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
