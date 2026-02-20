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

public class Move implements GenericOpCode{     // 30
    public static final int KIND_MOVE = 0x02_000000;
    public static final int OP                  = 0x02;

    public static final int move_vv = 0x02_00_01_02;
    public static final int move_fld_ovv = 0x02_00_02_03;
    public static final int move_fld_vov = 0x02_00_03_03;

    // move_i_vv(target_slot, src_slot). move from target_slot to src_slot
    public static final int move_i_vv = 0x02_0a_01_02;
    // move_fld_i_ovv(slot_of_target_instance, target_slot, src_slot)
    public static final int move_fld_i_ovv = 0x02_0a_02_03;
    // move_fld_i_vov(target_slot, src_instance, src_slot)
    public static final int move_fld_i_vov = 0x02_0a_03_03;

    // classref
    public static final int move_C_vv = 0x02_0c_01_02;
    public static final int move_fld_C_ovv = 0x02_0c_02_03;
    public static final int move_fld_C_vov = 0x02_0c_03_03;

    public static final int move_B_vv = 0x02_04_01_02;
    public static final int move_fld_B_ovv = 0x02_04_02_03;
    public static final int move_fld_B_vov = 0x02_04_03_03;

    public static final int move_c_vv = 0x02_05_01_02;
    public static final int move_fld_c_ovv = 0x02_05_02_03;
    public static final int move_fld_c_vov = 0x02_05_03_03;

    public static final int move_f_vv = 0x02_06_01_02;
    public static final int move_fld_f_ovv = 0x02_06_02_03;
    public static final int move_fld_f_vov = 0x02_06_03_03;

    public static final int move_d_vv = 0x02_07_01_02;
    public static final int move_fld_d_ovv = 0x02_07_02_03;
    public static final int move_fld_d_vov = 0x02_07_03_03;

    public static final int move_b_vv = 0x02_08_01_02;
    public static final int move_fld_b_ovv = 0x02_08_02_03;
    public static final int move_fld_b_vov = 0x02_08_03_03;

    public static final int move_s_vv = 0x02_09_01_02;
    public static final int move_fld_s_ovv = 0x02_09_02_03;
    public static final int move_fld_s_vov = 0x02_09_03_03;

    public static final int move_V_vv       = 0x02_00_01_02;
    public static final int move_fld_V_ovv  = 0x02_00_02_03;
    public static final int move_fld_V_vov  = 0x02_00_03_03;

    public static final int move_l_vv = 0x02_0b_01_02;
    public static final int move_fld_l_ovv = 0x02_0b_02_03;
    public static final int move_fld_l_vov = 0x02_0b_03_03;

    public static final int move_o_vv = 0x02_01_01_02;
    public static final int move_fld_o_ovv = 0x02_01_02_03;
    public static final int move_fld_o_vov = 0x02_01_03_03;

    public static final int move_S_vv = 0x02_03_01_02;
    public static final int move_fld_S_ovv = 0x02_03_02_03;
    public static final int move_fld_S_vov = 0x02_03_03_03;

    public static final int move_copy_ooC = 0x02_01_04_03;

    public static String getName(int code){
        return switch(code){
            case 0x02_00_01_02 -> "move_V_vv";
            case 0x02_00_02_03 -> "move_fld_V_ovv";
            case 0x02_00_03_03 -> "move_fld_V_vov";
            case 0x02_04_01_02 -> "move_B_vv";
            case 0x02_04_02_03 -> "move_fld_B_ovv";
            case 0x02_04_03_03 -> "move_fld_B_vov";
            case 0x02_05_01_02 -> "move_c_vv";
            case 0x02_05_02_03 -> "move_fld_c_ovv";
            case 0x02_05_03_03 -> "move_fld_c_vov";
            case 0x02_06_01_02 -> "move_f_vv";
            case 0x02_06_02_03 -> "move_fld_f_ovv";
            case 0x02_06_03_03 -> "move_fld_f_vov";
            case 0x02_07_01_02 -> "move_d_vv";
            case 0x02_07_02_03 -> "move_fld_d_ovv";
            case 0x02_07_03_03 -> "move_fld_d_vov";
            case 0x02_08_01_02 -> "move_b_vv";
            case 0x02_08_02_03 -> "move_fld_b_ovv";
            case 0x02_08_03_03 -> "move_fld_b_vov";
            case 0x02_09_01_02 -> "move_s_vv";
            case 0x02_09_02_03 -> "move_fld_s_ovv";
            case 0x02_09_03_03 -> "move_fld_s_vov";
            case 0x02_0a_01_02 -> "move_i_vv";
            case 0x02_0a_02_03 -> "move_fld_i_ovv";
            case 0x02_0a_03_03 -> "move_fld_i_vov";
            case 0x02_0b_01_02 -> "move_l_vv";
            case 0x02_0b_02_03 -> "move_fld_l_ovv";
            case 0x02_0b_03_03 -> "move_fld_l_vov";
            case 0x02_01_01_02 -> "move_o_vv";
            case 0x02_01_02_03 -> "move_fld_o_ovv";
            case 0x02_01_03_03 -> "move_fld_o_vov";
            case 0x02_02_01_02 -> "move_n_vv";
            case 0x02_02_02_03 -> "move_fld_n_ovv";
            case 0x02_02_03_03 -> "move_fld_n_vov";
            case 0x02_03_01_02 -> "move_S_vv";
            case 0x02_03_02_03 -> "move_fld_S_ovv";
            case 0x02_03_03_03 -> "move_fld_S_vov";
            case 0x02_0c_01_02 -> "move_i_vv";
            case 0x02_0c_02_03 -> "move_fld_i_ovv";
            case 0x02_0c_03_03 -> "move_fld_i_vov";
            case move_copy_ooC -> "move_batch_ooC";
            default -> {
                var t = OpCode.extractType(code);
                if(t >= TypeCode.GENERIC_TYPE_START){
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch ((code & OpCode.DTYPE_MASK_NEG)){
                        case move_vv: yield "move_G[%s]_vv".formatted(t);
                        case move_fld_ovv: yield  "move_fld_G[%s]_ovv".formatted(t);
                        case move_fld_vov: yield "move_fld_G[%s]_vov".formatted(t);
                    };
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

    public static void main(String[] args) {
        for (TypeCode typeCode : TypeCode.values()) {
            String type = StringUtils.leftPad(Integer.toHexString(typeCode.getValue()), 2, '0');
            String s = MessageFormat.format("""
                    public static final int move_{0}_vv = 0x02_{1}_01_02;
                    public static final int move_{0}_ovv = 0x02_{1}_02_03;
                    public static final int move_{0}_vov = 0x02_{1}_03_03;
                  """, typeCode.toShortString(), type);
            System.out.println(s);
        }
        StringBuffer sb = new StringBuffer();
        sb.append("""
            public static String getName(int code){
                return switch(code){
            """);

        for (TypeCode typeCode : TypeCode.values()) {
            String type = StringUtils.leftPad(Integer.toHexString(typeCode.getValue()), 2, '0');
            var shorts = typeCode.toShortString();
            String s = MessageFormat.format("""
                    case 0x02_{0}_01_02 -> "move_{1}_vv";
                    case 0x02_{0}_02_03 -> "move_{1}_ovv";
                    case 0x02_{0}_03_03 -> "move_{1}_vov";
            """, type, shorts);
            sb.append(s);
        }
        String defaultBranch = """
                default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            };
        }""";
        sb.append(defaultBranch).append("\n");
        System.out.println(sb);
    }

}
