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

public class Debug {
    public static final int KIND_DEBUG = 0xff_000000;
    public static final int OP                  = 0xff;
    // print(slot)
    public static final int print_S_c = 0x0ff_03_01_01;
    public static final int print_S_v = 0xff_03_02_01;

    public static String getName(int code) {
        return switch(code){
            case 0xff_03_01_01 -> "print_S_c";
            case 0xff_03_02_01 -> "print_S_v";
            default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
        };
    }

    public static void main(String[] args) {
        for (TypeCode typeCode : TypeCode.values()) {
            String type = StringUtils.leftPad(Integer.toHexString(typeCode.getValue()), 2, '0');
            var shorts = typeCode.toShortString();
            String s = MessageFormat.format("""
            	    public static final int print_{0}_c = 0x0ff_{1}_01_01;
            	    public static final int print_{0}_v = 0xff_{1}_02_01;
            	""",
            	    shorts, type
            	);
            System.out.println(s);
        }

        StringBuffer sb = new StringBuffer();
        sb.append("""
            public String getName(int code){
                return switch(code){
            """);

        for (TypeCode typeCode : TypeCode.values()) {
            String type = StringUtils.leftPad(Integer.toHexString(typeCode.getValue()), 2, '0');
            var shorts = typeCode.toShortString();
            String s = MessageFormat.format("""
            	    case 0xff_{1}_01_01 -> "print_{0}_c";
            	    case 0xff_{1}_02_01 -> "print_{0}_v";
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
