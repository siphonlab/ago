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

public class BitShiftLeft {
    public static final int KIND_BIT_LSHIFT = 0x28_000000;
    public static final int OP = 0x28;

    public static final int lshift_vc = 0x28_00_01_02;
    public static final int lshift_vvc = 0x28_00_02_03;
    public static final int lshift_vv = 0x28_00_03_02;
    public static final int lshift_vvv = 0x28_00_04_03;
    public static final int lshift_vcv = 0x28_00_05_03;

    public static final int lshift_i_vc = 0x28_0a_01_02;
    public static final int lshift_i_vvc = 0x28_0a_02_03;
    public static final int lshift_i_vv = 0x28_0a_03_02;
    public static final int lshift_i_vvv = 0x28_0a_04_03;
    public static final int lshift_i_vcv = 0x28_0a_05_03;

    public static final int lshift_b_vc = 0x28_08_01_02;
    public static final int lshift_b_vvc = 0x28_08_02_03;
    public static final int lshift_b_vv = 0x28_08_03_02;
    public static final int lshift_b_vvv = 0x28_08_04_03;
    public static final int lshift_b_vcv = 0x28_08_05_03;

    public static final int lshift_s_vc = 0x28_09_01_02;
    public static final int lshift_s_vvc = 0x28_09_02_03;
    public static final int lshift_s_vv = 0x28_09_03_02;
    public static final int lshift_s_vvv = 0x28_09_04_03;
    public static final int lshift_s_vcv = 0x28_09_05_03;

    public static final int lshift_l_vc = 0x28_0b_01_02;
    public static final int lshift_l_vvc = 0x28_0b_02_04;
    public static final int lshift_l_vv = 0x28_0b_03_02;
    public static final int lshift_l_vvv = 0x28_0b_04_03;
    public static final int lshift_l_vcv = 0x28_0b_05_04;

    public static String getName(int code) {
        return switch (code) {
            case lshift_vc -> "lshift_vc";
            case lshift_vvc -> "lshift_vvc";
            case lshift_vv -> "lshift_vv";
            case lshift_vvv -> "lshift_vvv";
            case lshift_vcv -> "lshift_vcv";
            case lshift_i_vc -> "lshift_i_vc";
            case lshift_i_vvc -> "lshift_i_vvc";
            case lshift_i_vv -> "lshift_i_vv";
            case lshift_i_vvv -> "lshift_i_vvv";
            case lshift_i_vcv -> "lshift_i_vcv";
            case lshift_b_vc -> "lshift_b_vc";
            case lshift_b_vvc -> "lshift_b_vvc";
            case lshift_b_vv -> "lshift_b_vv";
            case lshift_b_vvv -> "lshift_b_vvv";
            case lshift_b_vcv -> "lshift_b_vcv";
            case lshift_s_vc -> "lshift_s_vc";
            case lshift_s_vvc -> "lshift_s_vvc";
            case lshift_s_vv -> "lshift_s_vv";
            case lshift_s_vvv -> "lshift_s_vvv";
            case lshift_s_vcv -> "lshift_s_vcv";
            case lshift_l_vc -> "lshift_l_vc";
            case lshift_l_vvc -> "lshift_l_vvc";
            case lshift_l_vv -> "lshift_l_vv";
            case lshift_l_vvv -> "lshift_l_vvv";
            case lshift_l_vcv -> "lshift_l_vcv";
            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }
}
