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
package org.siphonlab.ago.opcode.logic;

public class BitShiftRight {
    public static final int KIND_BIT_RSHIFT = 0x29_000000;
    public static final int OP                  = 0x29;

    public static final int rshift_vc  = 0x29_00_01_02;
    public static final int rshift_vvc = 0x29_00_02_03;
    public static final int rshift_vv  = 0x29_00_03_02;
    public static final int rshift_vvv = 0x29_00_04_03;
    public static final int rshift_vcv = 0x29_00_05_03;

    public static final int rshift_i_vc    = 0x29_0a_01_02;
    public static final int rshift_i_vvc = 0x29_0a_02_03;
    public static final int rshift_i_vv    = 0x29_0a_03_02;
    public static final int rshift_i_vvv   = 0x29_0a_04_03;
    public static final int rshift_i_vcv = 0x29_0a_05_03;

    public static final int rshift_b_vc    = 0x29_08_01_02;
    public static final int rshift_b_vvc   = 0x29_08_02_03;
    public static final int rshift_b_vv    = 0x29_08_03_02;
    public static final int rshift_b_vvv   = 0x29_08_04_03;
    public static final int rshift_b_vcv = 0x29_08_05_03;

    public static final int rshift_s_vc    = 0x29_09_01_02;
    public static final int rshift_s_vvc   = 0x29_09_02_03;
    public static final int rshift_s_vv    = 0x29_09_03_02;
    public static final int rshift_s_vvv   = 0x29_09_04_03;
    public static final int rshift_s_vcv = 0x29_09_05_03;

    public static final int rshift_l_vc    = 0x29_0b_01_02;
    public static final int rshift_l_vvc   = 0x29_0b_02_04;
    public static final int rshift_l_vv    = 0x29_0b_03_02;
    public static final int rshift_l_vvv   = 0x29_0b_04_03;
    public static final int rshift_l_vcv = 0x29_0b_05_04;

    public static String getName(int code){
        return switch(code){
            case rshift_i_vc->  "rshift_i_vc";
            case rshift_i_vvc ->  "rshift_i_vvc";
            case rshift_i_vv->  "rshift_i_vv";
            case rshift_i_vvv ->  "rshift_i_vvv";
            case rshift_i_vcv -> "rshift_i_vcv";

            case rshift_b_vc->  "rshift_b_vc";
            case rshift_b_vvc ->  "rshift_b_vvc";
            case rshift_b_vv->    "rshift_b_vv";
            case rshift_b_vvv ->    "rshift_b_vvv";
            case rshift_b_vcv -> "rshift_b_vcv";

            case rshift_s_vc->  "rshift_s_vc";
            case rshift_s_vvc ->  "rshift_s_vvc";
            case rshift_s_vv->    "rshift_s_vv";
            case rshift_s_vvv ->    "rshift_s_vvv";
            case rshift_s_vcv -> "rshift_s_vcv";

            case rshift_l_vc->  "rshift_l_vc";
            case rshift_l_vvc ->  "rshift_l_vvc";
            case rshift_l_vv->    "rshift_l_vv";
            case rshift_l_vvv ->    "rshift_l_vvv";
            case rshift_l_vcv -> "rshift_l_vcv";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }
}
