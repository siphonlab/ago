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

public class BitAnd {
    public static final int KIND_BITAND = 0x24_000000;
    public static final int OP                  = 0x24;

    public static final int band_vc  = 0x24_00_01_02;
    public static final int band_vvc = 0x24_00_02_03;
    public static final int band_vv  = 0x24_00_03_02;
    public static final int band_vvv = 0x24_00_04_03;

    public static final int band_i_vc    = 0x24_0a_01_02;
    public static final int band_i_vvc = 0x24_0a_02_03;
    public static final int band_i_vv    = 0x24_0a_03_02;
    public static final int band_i_vvv   = 0x24_0a_04_03;

    public static final int band_b_vc    = 0x24_08_01_02;
    public static final int band_b_vvc   = 0x24_08_02_03;
    public static final int band_b_vv    = 0x24_08_03_02;
    public static final int band_b_vvv   = 0x24_08_04_03;

    public static final int band_s_vc    = 0x24_09_01_02;
    public static final int band_s_vvc   = 0x24_09_02_03;
    public static final int band_s_vv    = 0x24_09_03_02;
    public static final int band_s_vvv   = 0x24_09_04_03;

    // long need 8 bytes
    public static final int band_l_vc    = 0x24_0b_01_03;
    public static final int band_l_vvc   = 0x24_0b_02_04;
    public static final int band_l_vv    = 0x24_0b_03_02;
    public static final int band_l_vvv   = 0x24_0b_04_03;

    public static String getName(int code){
        return switch(code){
            case band_i_vc->  "band_i_vc";
            case band_i_vvc ->  "band_i_vvc";
            case band_i_vv->  "band_i_vv";
            case band_i_vvv ->  "band_i_vvv";

            case band_b_vc->  "band_b_vc";
            case band_b_vvc ->  "band_b_vvc";
            case band_b_vv->    "band_b_vv";
            case band_b_vvv ->    "band_b_vvv";

            case band_s_vc->  "band_s_vc";
            case band_s_vvc ->  "band_s_vvc";
            case band_s_vv->    "band_s_vv";
            case band_s_vvv ->    "band_s_vvv";

            case band_l_vc->  "band_l_vc";
            case band_l_vvc ->  "band_l_vvc";
            case band_l_vv->    "band_l_vv";
            case band_l_vvv ->    "band_l_vvv";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }
}
