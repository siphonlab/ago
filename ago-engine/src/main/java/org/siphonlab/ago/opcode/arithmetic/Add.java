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

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.opcode.OpCode;

public class Add {  
    public static final int KIND_ADD = 0x03_000000;
    public static final int OP                  = 0x03;

    public static final int add_g_vv = 0x03_ff_03_02;
    public static final int add_g_vvv = 0x03_ff_04_03;

    public static final int add_vc  = 0x03_00_01_02;
    public static final int add_vvc = 0x03_00_02_03;
    public static final int add_vv  = 0x03_00_03_02;
    public static final int add_vvv = 0x03_00_04_03;

    // add_i_vc(slot, const)
    public static final int add_i_vc    = 0x03_0a_01_02;
    // add_i_vvc(target slot, slot, const),
    public static final int add_i_vvc = 0x03_0a_02_03;
    // add_i_vv(slot1, slot2) result put into slot1
    public static final int add_i_vv    = 0x03_0a_03_02;
    // add_i_vvv(target slot, slot1, slot2)
    public static final int add_i_vvv   = 0x03_0a_04_03;

    public static final int add_f_vc    = 0x03_06_01_02;
    public static final int add_f_vvc   = 0x03_06_02_03;
    public static final int add_f_vv    = 0x03_06_03_02;
    public static final int add_f_vvv   = 0x03_06_04_03;

    // double need 8 bytes
    public static final int add_d_vc    = 0x03_07_01_03;
    public static final int add_d_vvc   = 0x03_07_02_04;
    public static final int add_d_vv    = 0x03_07_03_02;
    public static final int add_d_vvv   = 0x03_07_04_03;

    public static final int add_b_vc    = 0x03_08_01_02;
    public static final int add_b_vvc   = 0x03_08_02_03;
    public static final int add_b_vv    = 0x03_08_03_02;
    public static final int add_b_vvv   = 0x03_08_04_03;

    public static final int add_s_vc    = 0x03_09_01_02;
    public static final int add_s_vvc   = 0x03_09_02_03;
    public static final int add_s_vv    = 0x03_09_03_02;
    public static final int add_s_vvv   = 0x03_09_04_03;

    // long need 8 bytes
    public static final int add_l_vc    = 0x03_0b_01_03;
    public static final int add_l_vvc   = 0x03_0b_02_04;
    public static final int add_l_vv    = 0x03_0b_03_02;
    public static final int add_l_vvv   = 0x03_0b_04_03;

    public static String getName(int code){
        return switch(code){
            case add_f_vc ->  "add_f_vc";
            case add_f_vvc ->  "add_f_vvc";
            case add_f_vv ->  "add_f_vv";
            case add_f_vvv ->  "add_f_vvv";

            case add_d_vc ->  "add_d_vc";
            case add_d_vvc ->  "add_d_vvc";
            case add_d_vv ->    "add_d_vv";
            case add_d_vvv ->    "add_d_vvv";

            case add_b_vc ->  "add_b_vc";
            case add_b_vvc ->  "add_b_vvc";
            case add_b_vv ->    "add_b_vv";
            case add_b_vvv ->    "add_b_vvv";

            case add_s_vc ->  "add_s_vc";
            case add_s_vvc ->  "add_s_vvc";
            case add_s_vv ->    "add_s_vv";
            case add_s_vvv ->    "add_s_vvv";

            case add_i_vc ->  "add_i_vc";
            case add_i_vvc ->  "add_i_vvc";
            case add_i_vv ->    "add_i_vv";
            case add_i_vvv ->    "add_i_vvv";

            case add_l_vc ->  "add_l_vc";
            case add_l_vvc ->  "add_l_vvc";
            case add_l_vv ->    "add_l_vv";
            case add_l_vvv ->    "add_l_vvv";

            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case add_g_vv:
                            yield "add_g[%s]_vv".formatted(t);
                        case add_g_vvv:
                            yield "add_g[%s]_vvv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

    public static void main(String[] args) {
        for (TypeCode typeCode : TypeCode.values()) {
            String type = StringUtils.leftPad(Integer.toHexString(typeCode.getValue()), 2, '0');
            int addition = 0;
            if(typeCode == TypeCode.LONG || typeCode == TypeCode.DOUBLE){
                addition = 1;
            }
            String s = MessageFormat.format(
                "public static final int add_{0}_vc    = 0x03_{1}_01_0{2};\n" +
                "public static final int add_{0}_vvc   = 0x03_{1}_02_0{3};\n" +
                "public static final int add_{0}_vv    = 0x03_{1}_03_02;\n" +
                "public static final int add_{0}_vvv   = 0x03_{1}_04_03;\n" +
                "public static final int add_{0}_cv    = 0x03_{1}_05_0{4};\n" +
                "public static final int add_{0}_vcv   = 0x03_{1}_06_0{5};",
                typeCode.toShortString(), type, 
                2 + addition,
                3 + addition, 
                2 + addition, 
                3 + addition
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
            int addition = 0;
            if(typeCode == TypeCode.LONG || typeCode == TypeCode.DOUBLE){
                addition = 1;
            }
            String s = MessageFormat.format("""
            	    case 0x03_{1}_01_0{2} ->  "add_{0}_vc";
            	    case 0x03_{1}_02_0{3} ->  "add_{0}_vvc";
            	    case 0x03_{1}_03_02 ->    "add_{0}_vv";
            	    case 0x03_{1}_04_03 ->    "add_{0}_vvv";
            	    case 0x03_{1}_05_0{4} ->  "add_{0}_cv";
            	    case 0x03_{1}_06_0{5} ->  "add_{0}_vcv";
            	""",
            	    typeCode.toShortString(), type, 2 + addition, 3 + addition, 2 + addition, 3 + addition
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
