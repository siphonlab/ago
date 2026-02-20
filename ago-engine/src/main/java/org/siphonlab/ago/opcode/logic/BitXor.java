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

public class BitXor {
    public static final int KIND_BITXOR = 0x26_000000;
    public static final int OP                  = 0x26;

    public static final int bxor_vc  = 0x26_00_01_02;
    public static final int bxor_vvc = 0x26_00_02_03;
    public static final int bxor_vv  = 0x26_00_03_02;
    public static final int bxor_vvv = 0x26_00_04_03;

    public static final int bxor_i_vc    = 0x26_0a_01_02;
    public static final int bxor_i_vvc = 0x26_0a_02_03;
    public static final int bxor_i_vv    = 0x26_0a_03_02;
    public static final int bxor_i_vvv   = 0x26_0a_04_03;

    public static final int bxor_b_vc    = 0x26_08_01_02;
    public static final int bxor_b_vvc   = 0x26_08_02_03;
    public static final int bxor_b_vv    = 0x26_08_03_02;
    public static final int bxor_b_vvv   = 0x26_08_04_03;

    public static final int bxor_s_vc    = 0x26_09_01_02;
    public static final int bxor_s_vvc   = 0x26_09_02_03;
    public static final int bxor_s_vv    = 0x26_09_03_02;
    public static final int bxor_s_vvv   = 0x26_09_04_03;

    // long need 8 bytes
    public static final int bxor_l_vc    = 0x26_0b_01_03;
    public static final int bxor_l_vvc   = 0x26_0b_02_04;
    public static final int bxor_l_vv    = 0x26_0b_03_02;
    public static final int bxor_l_vvv   = 0x26_0b_04_03;

    public static String getName(int code){
        return switch(code){
            case bxor_i_vc->  "bxor_i_vc";
            case bxor_i_vvc ->  "bxor_i_vvc";
            case bxor_i_vv->  "bxor_i_vv";
            case bxor_i_vvv ->  "bxor_i_vvv";

            case bxor_b_vc->  "bxor_b_vc";
            case bxor_b_vvc ->  "bxor_b_vvc";
            case bxor_b_vv->    "bxor_b_vv";
            case bxor_b_vvv ->    "bxor_b_vvv";

            case bxor_s_vc->  "bxor_s_vc";
            case bxor_s_vvc ->  "bxor_s_vvc";
            case bxor_s_vv->    "bxor_s_vv";
            case bxor_s_vvv ->    "bxor_s_vvv";

            case bxor_l_vc->  "bxor_l_vc";
            case bxor_l_vvc ->  "bxor_l_vvc";
            case bxor_l_vv->    "bxor_l_vv";
            case bxor_l_vvv ->    "bxor_l_vvv";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }
}
