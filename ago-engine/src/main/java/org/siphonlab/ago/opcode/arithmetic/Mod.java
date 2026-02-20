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

public class Mod {
    public static final int KIND_MOD = 0x17_000000;
    public static final int OP                  = 0x17;

    public static final int mod_vc = 0x17_00_01_02;
    public static final int mod_vvc = 0x17_00_02_03;
    public static final int mod_vv = 0x17_00_03_02;
    public static final int mod_vvv = 0x17_00_04_03;
    public static final int mod_vcv = 0x17_00_05_03;

    public static final int mod_g_vv = 0x17_ff_03_02;
    public static final int mod_g_vvv = 0x17_ff_04_03;

    // mod_i_vc(slot, const)
    public static final int mod_i_vc    = 0x17_0a_01_02;
    // mod_i_vvc(target slot, slot, const),
    public static final int mod_i_vvc = 0x17_0a_02_03;
    // mod_i_vv(slot1, slot2) result put into slot1
    public static final int mod_i_vv    = 0x17_0a_03_02;
    // mod_i_vvv(target slot, slot1, slot2)
    public static final int mod_i_vvv   = 0x17_0a_04_03;
    public static final int mod_i_vcv   = 0x17_0a_05_03;

    public static final int mod_f_vc    = 0x17_06_01_02;
    public static final int mod_f_vvc   = 0x17_06_02_03;
    public static final int mod_f_vv    = 0x17_06_03_02;
    public static final int mod_f_vvv   = 0x17_06_04_03;
    public static final int mod_f_vcv   = 0x17_06_05_03;

    // double need 8 bytes
    public static final int mod_d_vc    = 0x17_07_01_03;
    public static final int mod_d_vvc   = 0x17_07_02_04;
    public static final int mod_d_vv    = 0x17_07_03_02;
    public static final int mod_d_vvv   = 0x17_07_04_03;
    public static final int mod_d_vcv   = 0x17_07_05_04;

    public static final int mod_b_vc    = 0x17_08_01_02;
    public static final int mod_b_vvc   = 0x17_08_02_03;
    public static final int mod_b_vv    = 0x17_08_03_02;
    public static final int mod_b_vvv   = 0x17_08_04_03;
    public static final int mod_b_vcv   = 0x17_08_05_03;

    public static final int mod_s_vc    = 0x17_09_01_02;
    public static final int mod_s_vvc   = 0x17_09_02_03;
    public static final int mod_s_vv    = 0x17_09_03_02;
    public static final int mod_s_vvv   = 0x17_09_04_03;
    public static final int mod_s_vcv   = 0x17_09_05_03;

    // long need 8 bytes
    public static final int mod_l_vc    = 0x17_0b_01_03;
    public static final int mod_l_vvc   = 0x17_0b_02_04;
    public static final int mod_l_vv    = 0x17_0b_03_02;
    public static final int mod_l_vvv   = 0x17_0b_04_03;
    public static final int mod_l_vcv   = 0x17_0b_05_04;

    public static String getName(int code){
        return switch(code){
            case 0x17_05_01_02 ->  "mod_c_vc";
            case 0x17_05_02_03 ->  "mod_c_vvc";
            case 0x17_05_03_02 ->  "mod_c_vv";
            case 0x17_05_04_03 ->  "mod_c_vvv";
            case 0x17_05_05_03 ->  "mod_c_vcv";

            case 0x17_06_01_02 ->  "mod_f_vc";
            case 0x17_06_02_03 ->  "mod_f_vvc";
            case 0x17_06_03_02 ->  "mod_f_vv";
            case 0x17_06_04_03 ->  "mod_f_vvv";
            case 0x17_06_05_03 ->  "mod_f_vcv";

            case 0x17_07_01_03 ->  "mod_d_vc";
            case 0x17_07_02_04 ->  "mod_d_vvc";
            case 0x17_07_03_02 ->    "mod_d_vv";
            case 0x17_07_04_03 ->    "mod_d_vvv";
            case 0x17_07_05_04 ->  "mod_d_vcv";

            case 0x17_08_01_02 ->  "mod_b_vc";
            case 0x17_08_02_03 ->  "mod_b_vvc";
            case 0x17_08_03_02 ->    "mod_b_vv";
            case 0x17_08_04_03 ->    "mod_b_vvv";
            case 0x17_08_05_03 ->  "mod_b_vcv";

            case 0x17_09_01_02 ->  "mod_s_vc";
            case 0x17_09_02_03 ->  "mod_s_vvc";
            case 0x17_09_03_02 ->    "mod_s_vv";
            case 0x17_09_04_03 ->    "mod_s_vvv";
            case 0x17_09_05_03 ->  "mod_s_vcv";

            case 0x17_0a_01_02 ->  "mod_i_vc";
            case 0x17_0a_02_03 ->  "mod_i_vvc";
            case 0x17_0a_03_02 ->    "mod_i_vv";
            case 0x17_0a_04_03 ->    "mod_i_vvv";
            case 0x17_0a_05_03 ->  "mod_i_vcv";

            case 0x17_0b_01_03 ->  "mod_l_vc";
            case 0x17_0b_02_04 ->  "mod_l_vvc";
            case 0x17_0b_03_02 ->    "mod_l_vv";
            case 0x17_0b_04_03 ->    "mod_l_vvv";
            case 0x17_0b_05_04 ->  "mod_l_vcv";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case mod_g_vv:
                            yield "mod_g[%s]_vv".formatted(t);
                        case mod_g_vvv:
                            yield "mod_g[%s]_vvv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
