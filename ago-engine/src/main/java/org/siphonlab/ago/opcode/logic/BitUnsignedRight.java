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

public class BitUnsignedRight {
    public static final int KIND_BIT_URSHIFT = 0x30_000000;
    public static final int OP                  = 0x30;

    public static final int urshift_vc  = 0x30_00_01_02;
    public static final int urshift_vvc = 0x30_00_02_03;
    public static final int urshift_vv  = 0x30_00_03_02;
    public static final int urshift_vvv = 0x30_00_04_03;
    public static final int urshift_vcv = 0x30_00_05_03;

    public static final int urshift_i_vc    = 0x30_0a_01_02;
    public static final int urshift_i_vvc = 0x30_0a_02_03;
    public static final int urshift_i_vv    = 0x30_0a_03_02;
    public static final int urshift_i_vvv   = 0x30_0a_04_03;
    public static final int urshift_i_vcv    = 0x30_0a_05_03;

    public static final int urshift_b_vc    = 0x30_08_01_02;
    public static final int urshift_b_vvc   = 0x30_08_02_03;
    public static final int urshift_b_vv    = 0x30_08_03_02;
    public static final int urshift_b_vvv   = 0x30_08_04_03;
    public static final int urshift_b_vcv    = 0x30_08_05_03;

    public static final int urshift_s_vc    = 0x30_09_01_02;
    public static final int urshift_s_vvc   = 0x30_09_02_03;
    public static final int urshift_s_vv    = 0x30_09_03_02;
    public static final int urshift_s_vvv   = 0x30_09_04_03;
    public static final int urshift_s_vcv    = 0x30_09_05_03;

    public static final int urshift_l_vc    = 0x30_0b_01_02;
    public static final int urshift_l_vvc   = 0x30_0b_02_04;
    public static final int urshift_l_vv    = 0x30_0b_03_02;
    public static final int urshift_l_vvv   = 0x30_0b_04_03;
    public static final int urshift_l_vcv    = 0x30_0b_05_04;

    public static String getName(int code){
        return switch(code){
            case urshift_i_vc->  "urshift_i_vc";
            case urshift_i_vvc ->  "urshift_i_vvc";
            case urshift_i_vv->  "urshift_i_vv";
            case urshift_i_vvv ->  "urshift_i_vvv";
            case urshift_i_vcv -> "urshift_i_vcv";

            case urshift_b_vc->  "urshift_b_vc";
            case urshift_b_vvc ->  "urshift_b_vvc";
            case urshift_b_vv->    "urshift_b_vv";
            case urshift_b_vvv ->    "urshift_b_vvv";
            case urshift_b_vcv -> "urshift_b_vcv";

            case urshift_s_vc->  "urshift_s_vc";
            case urshift_s_vvc ->  "urshift_s_vvc";
            case urshift_s_vv->    "urshift_s_vv";
            case urshift_s_vvv ->    "urshift_s_vvv";
            case urshift_s_vcv -> "urshift_s_vcv";

            case urshift_l_vc->  "urshift_l_vc";
            case urshift_l_vvc ->  "urshift_l_vvc";
            case urshift_l_vv->    "urshift_l_vv";
            case urshift_l_vvv ->    "urshift_l_vvv";
            case urshift_l_vcv -> "urshift_l_vcv";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }
}
