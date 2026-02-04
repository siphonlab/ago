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
