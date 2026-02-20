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
package org.siphonlab.ago.compiler;

import java.util.regex.Pattern;

public class StringUtils {
    private static final String BACKSLASH = "\\";
    private static final Pattern HEX_ESCAPES_PATTERN = Pattern.compile("(\\\\*)\\\\u([0-9abcdefABCDEF]{4})");
    private static final Pattern OCTAL_ESCAPES_PATTERN = Pattern.compile("(\\\\*)\\\\([0-3]?[0-7]?[0-7])");
    private static final Pattern STANDARD_ESCAPES_PATTERN = Pattern.compile("(\\\\*)\\\\([btnfrs\"'])");
    private static final Pattern LINE_ESCAPE_PATTERN = Pattern.compile("(\\\\*)\\\\\r?\n");


    public static String trimQuotations(String text, int quotationLength) {
        int length = text.length();

        return length == quotationLength << 1 ? "" : text.substring(quotationLength, length - quotationLength);
    }

}
