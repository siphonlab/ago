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

public class IncDec {  
    public static final int KIND_INC_DEC = 0x18_000000;
    public static final int OP                  = 0x18;

    public static final int inc_ovc = 0x18_00_01_03;
    public static final int inc_ovv = 0x18_00_02_03;

    // `inc_v` is `add_vc x,1`,  `inc_vc` is `add_vc`
    // so here is only `inc_ovc` `inc_ovv`

    public static final int inc_i_ovc = 0x18_0a_01_03;
    public static final int inc_i_ovv = 0x18_0a_02_03;

    public static final int inc_f_ov   = 0x18_06_01_03;
    public static final int inc_f_ovc  = 0x18_06_02_03;

    public static final int inc_d_ov   = 0x18_07_01_03;
    public static final int inc_d_ovcc = 0x18_07_02_04;

    public static final int inc_b_ov   = 0x18_08_01_03;
    public static final int inc_b_ovcc = 0x18_08_02_03;

    public static final int inc_s_ov   = 0x18_09_01_03;
    public static final int inc_s_ovcc = 0x18_09_02_03;

    public static final int inc_l_ov   = 0x18_0b_01_03;
    public static final int inc_l_ovcc = 0x18_0b_02_04;


    public static String getName(int code){
        return switch(code){
            case inc_i_ovc -> "inc_i_ov";
            case inc_i_ovv -> "inc_i_ovc";
            case inc_f_ov -> "inc_f_ov";
            case inc_f_ovc -> "inc_f_ovc";
            case inc_d_ov -> "inc_d_ov";
            case inc_d_ovcc -> "inc_d_ovcc";
            case inc_b_ov -> "inc_b_ov";
            case inc_b_ovcc -> "inc_b_ovcc";
            case inc_s_ov -> "inc_s_ov";
            case inc_s_ovcc -> "inc_s_ovcc";
            case inc_l_ov -> "inc_l_ov";
            case inc_l_ovcc -> "inc_l_ovcc";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
