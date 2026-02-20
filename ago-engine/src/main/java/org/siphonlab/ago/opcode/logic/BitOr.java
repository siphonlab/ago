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

public class BitOr {
    public static final int KIND_BITOR = 0x25_000000;
    public static final int OP                  = 0x25;

    public static final int bor_vc  = 0x25_00_01_02;
    public static final int bor_vvc = 0x25_00_02_03;
    public static final int bor_vv  = 0x25_00_03_02;
    public static final int bor_vvv = 0x25_00_04_03;

    public static final int bor_i_vc    = 0x25_0a_01_02;
    public static final int bor_i_vvc = 0x25_0a_02_03;
    public static final int bor_i_vv    = 0x25_0a_03_02;
    public static final int bor_i_vvv   = 0x25_0a_04_03;

    public static final int bor_b_vc    = 0x25_08_01_02;
    public static final int bor_b_vvc   = 0x25_08_02_03;
    public static final int bor_b_vv    = 0x25_08_03_02;
    public static final int bor_b_vvv   = 0x25_08_04_03;

    public static final int bor_s_vc    = 0x25_09_01_02;
    public static final int bor_s_vvc   = 0x25_09_02_03;
    public static final int bor_s_vv    = 0x25_09_03_02;
    public static final int bor_s_vvv   = 0x25_09_04_03;

    // long need 8 bytes
    public static final int bor_l_vc    = 0x25_0b_01_03;
    public static final int bor_l_vvc   = 0x25_0b_02_04;
    public static final int bor_l_vv    = 0x25_0b_03_02;
    public static final int bor_l_vvv   = 0x25_0b_04_03;

    public static String getName(int code){
        return switch(code){
            case bor_i_vc->  "bor_i_vc";
            case bor_i_vvc ->  "bor_i_vvc";
            case bor_i_vv->  "bor_i_vv";
            case bor_i_vvv ->  "bor_i_vvv";

            case bor_b_vc->  "bor_b_vc";
            case bor_b_vvc ->  "bor_b_vvc";
            case bor_b_vv->    "bor_b_vv";
            case bor_b_vvv ->    "bor_b_vvv";

            case bor_s_vc->  "bor_s_vc";
            case bor_s_vvc ->  "bor_s_vvc";
            case bor_s_vv->    "bor_s_vv";
            case bor_s_vvv ->    "bor_s_vvv";

            case bor_l_vc->  "bor_l_vc";
            case bor_l_vvc ->  "bor_l_vvc";
            case bor_l_vv->    "bor_l_vv";
            case bor_l_vvv ->    "bor_l_vvv";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }
}
