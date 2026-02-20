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
package org.siphonlab.ago.opcode;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.TypeCode;

public class Box implements GenericOpCode{

    public static final int KIND_BOX = 0x0d_000000;
    public static final int OP                  = 0x0d;

    public static final int box_g_vv = 0x0d_ff_02_02;
    public static final int unbox_g_vo = 0x0d_ff_03_02;

    public static final int box_vc    = 0x0d_00_01_02;
    public static final int box_vv    = 0x0d_00_02_02;
    public static final int unbox_vo  = 0x0d_00_03_02;

    // box_i_vc(slot, const)
    public static final int box_i_vc    = 0x0d_0a_01_02;
    public static final int box_i_vv    = 0x0d_0a_02_02;
    public static final int unbox_i_vo  = 0x0d_0a_03_02;

    public static final int box_B_vc    = 0x0d_04_01_02;
    public static final int box_B_vv   = 0x0d_04_02_02;
    public static final int unbox_B_vo    = 0x0d_04_03_02;

    public static final int box_c_vc    = 0x0d_05_01_02;
    public static final int box_c_vv   = 0x0d_05_02_02;
    public static final int unbox_c_vo    = 0x0d_05_03_02;

    public static final int box_f_vc    = 0x0d_06_01_02;
    public static final int box_f_vv   = 0x0d_06_02_02;
    public static final int unbox_f_vo    = 0x0d_06_03_02;

    public static final int box_d_vc    = 0x0d_07_01_03;
    public static final int box_d_vv   = 0x0d_07_02_02;
    public static final int unbox_d_vo    = 0x0d_07_03_02;

    public static final int box_b_vc    = 0x0d_08_01_02;
    public static final int box_b_vv   = 0x0d_08_02_02;
    public static final int unbox_b_vo    = 0x0d_08_03_02;

    public static final int box_s_vc    = 0x0d_09_01_02;
    public static final int box_s_vv   = 0x0d_09_02_02;
    public static final int unbox_s_vo    = 0x0d_09_03_02;

    public static final int box_l_vc    = 0x0d_0b_01_03;
    public static final int box_l_vv   = 0x0d_0b_02_02;
    public static final int unbox_l_vo    = 0x0d_0b_03_02;

    // box anything
    public static final int box_o_vv    = 0x0d_01_02_02;
    // object has no unbox, but for generic usage, leave it
    public static final int unbox_o_vo    = 0x0d_01_03_02;

    public static final int box_S_vc    = 0x0d_03_01_02;
    public static final int box_S_vv   = 0x0d_03_02_02;
    public static final int unbox_S_vo    = 0x0d_03_03_02;

    public static final int box_C_vC = 0x0d_0c_01_02;
    public static final int box_C_vv   = 0x0d_0c_02_02;
    public static final int unbox_C_vo    = 0x0d_0c_03_02;

    // box to ClassRef/ClassInterval/ScopedClassInterval/GenericTypeParameter
    public static final int box_C_vCC = 0x0d_0c_04_03;
    public static final int box_C_vvC = 0x0d_0c_05_03;

    public static final int unbox_force_vot = 0x0d_01_04_02;

    public static String getName(int code){
        return switch (code){
            case 0x0d_0a_01_02 -> "box_i_vc";
            case 0x0d_0a_02_02 -> "box_i_vv";
            case 0x0d_0a_03_02 -> "unbox_i_vo";
            case 0x0d_04_01_02 -> "box_B_vc";
            case 0x0d_04_02_02 -> "box_B_vv";
            case 0x0d_04_03_02 -> "unbox_B_vo";
            case 0x0d_05_01_02 -> "box_c_vc";
            case 0x0d_05_02_02 -> "box_c_vv";
            case 0x0d_05_03_02 -> "unbox_c_vo";
            case 0x0d_06_01_02 -> "box_f_vc";
            case 0x0d_06_02_02 -> "box_f_vv";
            case 0x0d_06_03_02 -> "unbox_f_vo";
            case 0x0d_07_01_03 -> "box_d_vc";
            case 0x0d_07_02_02 -> "box_d_vv";
            case 0x0d_07_03_02 -> "unbox_d_vo";
            case 0x0d_08_01_02 -> "box_b_vc";
            case 0x0d_08_02_02 -> "box_b_vv";
            case 0x0d_08_03_02 -> "unbox_b_vo";
            case 0x0d_09_01_02 -> "box_s_vc";
            case 0x0d_09_02_02 -> "box_s_vv";
            case 0x0d_09_03_02 -> "unbox_s_vo";
            case 0x0d_0b_01_03 -> "box_l_vc";
            case 0x0d_0b_02_02 -> "box_l_vv";
            case 0x0d_0b_03_02 -> "unbox_l_vo";
            case 0x0d_01_02_02 -> "box_o_vv";
            case 0x0d_01_03_02 -> "unbox_o_vo";
            case 0x0d_03_01_02 -> "box_S_vc";
            case 0x0d_03_02_02 -> "box_S_vv";
            case 0x0d_03_03_02 -> "unbox_S_vo";
            case 0x0d_0c_01_02 -> "box_C_vc";
            case 0x0d_0c_02_02 -> "box_C_vv";
            case 0x0d_0c_03_02 -> "unbox_C_vo";

            case 0x0d_0c_04_03 -> "box_C_vCC";
            case 0x0d_0c_05_03 -> "box_C_vvC";

            case unbox_force_vot -> "unbox_force_vot";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch ((code & OpCode.DTYPE_MASK_NEG)) {
                        case box_vc:
                            yield "box_G[%s]_vc".formatted(t);
                        case box_vv:
                            yield "box_G[%s]_vv".formatted(t);
                        case unbox_vo:
                            yield "unbox_G[%s]_vo".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
