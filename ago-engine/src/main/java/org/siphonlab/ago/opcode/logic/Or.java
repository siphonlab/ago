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

public class Or {
    public static final int KIND_OR = 0x21_000000;
    public static final int OP                  = 0x21;

    public static final int or_vv = 0x21_04_03_02;
    public static final int or_vvv = 0x21_04_04_03;


    public static String getName(int code){
        return switch(code){
            case or_vv -> "or_vv";
            case or_vvv -> "or_vvv";

            default -> {
                throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
            }
        };
    }

}
