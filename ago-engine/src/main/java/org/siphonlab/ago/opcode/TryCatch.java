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

public class TryCatch {
    public static final int KIND_TRY_CATCH = 0x23_000000;
    public static final int OP                  = 0x23;

    public static final int except_store_v = 0x23_00_01_01;
    public static final int except_clean   = 0x23_00_02_00;
    // set final exit, works like const_i_vc, separate as an individual instruction
    public static final int set_final_exit_vc = 0x23_0a_04_02;

    public static final int except_throw_v = 0x23_00_03_01;
    public static final int except_throw_if_v = 0x23_00_04_01;

    public static String getName(int code){
        return switch(code){
            case except_store_v ->  "except_store_v";
            case except_clean ->  "except_clean";
            case except_throw_v ->  "except_throw_v";
            case set_final_exit_vc -> "set_final_exit_vc";
            case except_throw_if_v -> "except_throw_if_v";
            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
