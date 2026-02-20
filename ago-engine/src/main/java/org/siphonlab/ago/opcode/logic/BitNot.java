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

public class BitNot {
    public static final int KIND_BITNOT = 0x27_000000;
    public static final int OP                  = 0x27;

    public static final int bnot_vv = 0x27_00_01_02;

    public static final int bnot_i_vv= 0x27_0a_01_02;

    public static final int bnot_b_vv = 0x27_08_01_02;

    public static final int bnot_s_vv = 0x27_09_01_02;

    public static final int bnot_l_vv = 0x27_0b_01_02;


    public static String getName(int code){
        return switch(code){
            case bnot_vv -> "bnot_vv";
            case bnot_i_vv -> "bnot_i_vv";
            case bnot_b_vv -> "bnot_b_vv";
            case bnot_s_vv -> "bnot_s_vv";
            case bnot_l_vv -> "bnot_l_vv";
            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
