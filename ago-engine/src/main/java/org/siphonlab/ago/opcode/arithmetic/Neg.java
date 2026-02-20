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

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.opcode.OpCode;

public class Neg {
    public static final int KIND_NEG = 0x19_000000;
    public static final int OP                  = 0x19;

    public static final int neg_vv = 0x19_00_01_02;

    public static final int neg_g_vv = 0x19_ff_01_02;

    public static final int neg_i_vv= 0x19_0a_01_02;

    public static final int neg_f_vv = 0x19_06_01_02;

    public static final int neg_d_vv = 0x19_07_01_02;

    public static final int neg_b_vv = 0x19_08_01_02;

    public static final int neg_s_vv = 0x19_09_01_02;

    public static final int neg_l_vv = 0x19_0b_01_02;

    public static String getName(int code){
        return switch(code){
            case neg_i_vv -> "neg_i_vv";
            case neg_f_vv -> "neg_f_vv";
            case neg_d_vv -> "neg_d_vv";
            case neg_b_vv -> "neg_b_vv";
            case neg_s_vv  -> "neg_s_vv";
            case neg_l_vv -> "neg_l_vv";
            default -> {
                var t = OpCode.extractType(code);
                if (t >= TypeCode.GENERIC_TYPE_START) {
                    t -= TypeCode.GENERIC_TYPE_START;
                    switch (code | OpCode.DTYPE_MASK) {
                        case neg_g_vv:
                            yield "neg_g[%s]_vv".formatted(t);
                    }
                }
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
