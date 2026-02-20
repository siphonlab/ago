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

public class Concat {
    public static final int KIND_CONCAT = 0x0b_000000;
    public static final int OP                  = 0x0b;

    public static final int concat_S_vc     = 0x0b_03_01_02;
    public static final int concat_S_vvc    = 0x0b_03_02_03;
    public static final int concat_S_vv     = 0x0b_03_03_02;
    public static final int concat_S_vvv    = 0x0b_03_04_03;
    public static final int concat_S_vcv    = 0x0b_03_05_03;


    public static String getName(int code){
        return switch(code){
            case 0x0b_03_01_02 -> "concat_S_vc";
            case 0x0b_03_02_03 -> "concat_S_vvc";
            case 0x0b_03_03_02 -> "concat_S_vv";
            case 0x0b_03_04_03 -> "concat_S_vvv";
            case 0x0b_03_05_03 -> "concat_S_vcv";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
