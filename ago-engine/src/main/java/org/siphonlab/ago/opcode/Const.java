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

public class Const {        

    public static final int KIND_CONST          = 0x01_000000;
    public static final int OP                  = 0x01;

    public static final int const_vc = 0x01_00_01_02;
    public static final int const_fld_ovc = 0x01_00_02_03;

    // const_i_vc(slot, int_value)
    public static final int const_i_vc          = 0x01_0a_01_02;
    // const_i_vvc(slot_of_instance, slot, int_value)
    public static final int const_fld_i_ovc     = 0x01_0a_02_03;

    public static final int const_B_vc          = 0x01_04_01_02;
    public static final int const_fld_B_ovc     = 0x01_04_02_03;

    public static final int const_c_vc          = 0x01_05_01_02;
    public static final int const_fld_c_ovc     = 0x01_05_02_03;

    public static final int const_f_vc          = 0x01_06_01_02;
    public static final int const_fld_f_ovc     = 0x01_06_02_03;

    public static final int const_d_vc          = 0x01_07_01_03;
    public static final int const_fld_d_ovc     = 0x01_07_02_04;

    public static final int const_b_vc          = 0x01_08_01_02;
    public static final int const_fld_b_ovc     = 0x01_08_02_03;

    public static final int const_s_vc          = 0x01_09_01_02;
    public static final int const_fld_s_ovc     = 0x01_09_02_03;

    public static final int const_l_vc          = 0x01_0b_01_03;
    public static final int const_fld_l_ovc     = 0x01_0b_02_04;

    public static final int const_n_vc          = 0x01_02_01_01;
    public static final int const_fld_n_ovc     = 0x01_02_02_02;

    public static final int const_S_vc          = 0x01_03_01_02;
    public static final int const_fld_S_ovc     = 0x01_03_02_03;

    public static final int const_C_vC = 0x01_0c_01_02;
    public static final int const_fld_C_ovC = 0x01_0c_02_03;

    public static String getName(int code){
        return switch(code){
            case 0x01_04_01_02 -> "const_B_vc";
            case 0x01_04_02_03 -> "const_fld_B_ovc";
            case 0x01_05_01_02 -> "const_c_vc";
            case 0x01_05_02_03 -> "const_fld_c_ovc";
            case 0x01_06_01_02 -> "const_f_vc";
            case 0x01_06_02_03 -> "const_fld_f_ovc";
            case 0x01_07_01_03 -> "const_d_vc";
            case 0x01_07_02_04 -> "const_fld_d_ovc";
            case 0x01_08_01_02 -> "const_b_vc";
            case 0x01_08_02_03 -> "const_fld_b_ovc";
            case 0x01_09_01_02 -> "const_s_vc";
            case 0x01_09_02_03 -> "const_fld_s_ovc";
            case 0x01_0a_01_02 -> "const_i_vc";
            case 0x01_0a_02_03 -> "const_fld_i_ovc";
            case 0x01_0b_01_03 -> "const_l_vc";
            case 0x01_0b_02_04 -> "const_fld_l_ovc";
            case 0x01_01_01_02 -> "const_o_vc";
            case 0x01_01_02_03 -> "const_fld_o_ovc";
            case 0x01_02_01_01 -> "const_n_vc";
            case 0x01_02_02_02 -> "const_fld_n_ovc";
            case 0x01_03_01_02 -> "const_S_vc";
            case 0x01_03_02_03 -> "const_fld_S_ovc";
            case 0x01_0c_01_02 -> "const_C_vC";
            case 0x01_0c_02_03 -> "const_fld_C_ovC";
            default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
        };
    }

    public static void main(String[] args) {
        for (TypeCode typeCode : TypeCode.values()) {
            String type = StringUtils.leftPad(Integer.toHexString(typeCode.getValue()), 2, '0');
            var shorts = typeCode.toShortString();
            String s = MessageFormat.format("""
            	    public static final int const_{0}_vc      = 0x01_{1}_01_02;
            	    public static final int const_{0}_ovc     = 0x01_{1}_02_03;
            	""",
            	    shorts, type
            	);
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
            	    case 0x01_{1}_01_02 -> "const_{0}_vc";
            	    case 0x01_{1}_02_03 -> "const_{0}_ovc";
            	""",
            	    shorts, type
            	);
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
