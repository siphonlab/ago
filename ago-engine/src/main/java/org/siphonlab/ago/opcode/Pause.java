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

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.TypeCode;

import java.text.MessageFormat;

public class Pause {
    public static final int KIND_PAUSE = 0x32_000000;
    public static final int OP                  = 0x32;

    public static final int pause = 0x32_000000;

    public static String getName(int code) {
        return switch(code){
            case 0x32_000000 -> "pause";
            default -> throw new IllegalArgumentException("illegal code " + Integer.toHexString(code));
        };
    }

}
