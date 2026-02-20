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

public class Not {
    public static final int KIND_NOT = 0x22_000000;
    public static final int OP                  = 0x22;

    public static final int not_v  = 0x22_00_01_01;
    public static final int not_vv = 0x22_00_02_02;
//    public static final int not_vov = 0x20_04_02_03;


    public static String getName(int code){
        return switch(code){
            case not_v -> "not_v";
            case not_vv -> "not_vv";
//            case not_vov -> "not_vov";
            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
