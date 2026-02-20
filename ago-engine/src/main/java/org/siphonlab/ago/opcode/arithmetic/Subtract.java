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

public class Subtract {  
    public static final int KIND_SUBTRACT = 0x13_000000;
    public static final int OP                  = 0x13;

    public static final int sub_vc = 0x13_00_01_02;
    public static final int sub_vvc = 0x13_00_02_03;
    public static final int sub_vv = 0x13_00_03_02;
    public static final int sub_vvv = 0x13_00_04_03;
    public static final int sub_vcv = 0x13_00_05_03;

    public static final int sub_g_vv = 0x13_ff_03_02;
    public static final int sub_g_vvv = 0x13_ff_04_03;

    // sub_i_vc(slot, const)
    public static final int sub_i_vc    = 0x13_0a_01_02;
    // sub_i_vvc(target slot, slot, const),
    public static final int sub_i_vvc = 0x13_0a_02_03;
    // sub_i_vv(slot1, slot2) result put into slot1
    public static final int sub_i_vv    = 0x13_0a_03_02;
    // sub_i_vvv(target slot, slot1, slot2)
    public static final int sub_i_vvv   = 0x13_0a_04_03;
    public static final int sub_i_vcv   = 0x13_0a_05_03;        

    public static final int sub_f_vc    = 0x13_06_01_02;
    public static final int sub_f_vvc   = 0x13_06_02_03;
    public static final int sub_f_vv    = 0x13_06_03_02;
    public static final int sub_f_vvv   = 0x13_06_04_03;
    public static final int sub_f_vcv   = 0x13_06_05_03;

    // double need 8 bytes
    public static final int sub_d_vc    = 0x13_07_01_03;
    public static final int sub_d_vvc   = 0x13_07_02_04;
    public static final int sub_d_vv    = 0x13_07_03_02;
    public static final int sub_d_vvv   = 0x13_07_04_03;
    public static final int sub_d_vcv   = 0x13_07_05_04;

    public static final int sub_b_vc    = 0x13_08_01_02;
    public static final int sub_b_vvc   = 0x13_08_02_03;
    public static final int sub_b_vv    = 0x13_08_03_02;
    public static final int sub_b_vvv   = 0x13_08_04_03;
    public static final int sub_b_vcv   = 0x13_08_05_03;

    public static final int sub_s_vc    = 0x13_09_01_02;
    public static final int sub_s_vvc   = 0x13_09_02_03;
    public static final int sub_s_vv    = 0x13_09_03_02;
    public static final int sub_s_vvv   = 0x13_09_04_03;
    public static final int sub_s_vcv   = 0x13_09_05_03;

    // long need 8 bytes
    public static final int sub_l_vc    = 0x13_0b_01_03;
    public static final int sub_l_vvc   = 0x13_0b_02_04;
    public static final int sub_l_vv    = 0x13_0b_03_02;
    public static final int sub_l_vvv   = 0x13_0b_04_03;
    public static final int sub_l_vcv   = 0x13_0b_05_04;

    public static String getName(int code){
        return switch(code){
            case 0x13_05_01_02 ->  "sub_c_vc";
            case 0x13_05_02_03 ->  "sub_c_vvc";
            case 0x13_05_03_02 ->  "sub_c_vv";
            case 0x13_05_04_03 ->  "sub_c_vvv";
            case 0x13_05_05_03 ->  "sub_c_vcv";

            case 0x13_06_01_02 ->  "sub_f_vc";
            case 0x13_06_02_03 ->  "sub_f_vvc";
            case 0x13_06_03_02 ->  "sub_f_vv";
            case 0x13_06_04_03 ->  "sub_f_vvv";
            case 0x13_06_05_03 ->  "sub_f_vcv";

            case 0x13_07_01_03 ->  "sub_d_vc";
            case 0x13_07_02_04 ->  "sub_d_vvc";
            case 0x13_07_03_02 ->    "sub_d_vv";
            case 0x13_07_04_03 ->    "sub_d_vvv";
            case 0x13_07_05_04 ->  "sub_d_vcv";

            case 0x13_08_01_02 ->  "sub_b_vc";
            case 0x13_08_02_03 ->  "sub_b_vvc";
            case 0x13_08_03_02 ->    "sub_b_vv";
            case 0x13_08_04_03 ->    "sub_b_vvv";
            case 0x13_08_05_03 ->  "sub_b_vcv";

            case 0x13_09_01_02 ->  "sub_s_vc";
            case 0x13_09_02_03 ->  "sub_s_vvc";
            case 0x13_09_03_02 ->    "sub_s_vv";
            case 0x13_09_04_03 ->    "sub_s_vvv";
            case 0x13_09_05_03 ->  "sub_s_vcv";

            case 0x13_0a_01_02 ->  "sub_i_vc";
            case 0x13_0a_02_03 ->  "sub_i_vvc";
            case 0x13_0a_03_02 ->    "sub_i_vv";
            case 0x13_0a_04_03 ->    "sub_i_vvv";
            case 0x13_0a_05_03 ->  "sub_i_vcv";

            case 0x13_0b_01_03 ->  "sub_l_vc";
            case 0x13_0b_02_04 ->  "sub_l_vvc";
            case 0x13_0b_03_02 ->    "sub_l_vv";
            case 0x13_0b_04_03 ->    "sub_l_vvv";
            case 0x13_0b_05_04 ->  "sub_l_vcv";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case sub_g_vv:
                            yield "sub_g[%s]_vv".formatted(t);
                        case sub_g_vvv:
                            yield "sub_g[%s]_vvv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }
}
