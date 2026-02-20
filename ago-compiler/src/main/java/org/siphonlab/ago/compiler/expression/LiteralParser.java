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
package org.siphonlab.ago.compiler.expression;

public class LiteralParser {
    /**
     * Parse a JavaScript string literal, handling escape sequences,
     * Unicode (\\uXXXX or \\u{…}) and hex escapes (\xXX).
     *
     * The input must include the surrounding quotes.
     */
    public static String parseJsStringLiteral(String text) {
        if (text == null || text.length() < 2) return "";

        char quote = text.charAt(0);          // STRING_LITERAL: ('"' … | '\'' …)
        int i = 1, end = text.length() - 1;   // skip opening and closing quotes
        StringBuilder out = new StringBuilder();

        while (i < end) {
            char c = text.charAt(i);
            if (c == '\\') {                  // EscapeSequence
                i++;
                if (i >= end) break;
                char esc = text.charAt(i);

                switch (esc) {
                    /* SingleEscapeCharacter */
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'v': out.append('\u000B'); break; // vertical tab
                    case '\\': out.append('\\'); break;
                    case '\'': out.append('\''); break;
                    case '"':  out.append('"');  break;

                    /* 0 – no digit ahead! TODO – simplified as NUL */
                    case '0':
                        out.append('\u0000');
                        break;

                    /* HexEscapeSequence: \xHH */
                    case 'x':
                        if (i + 2 < end) {
                            int val = Integer.parseInt(text.substring(i + 1, i + 3), 16);
                            out.append((char) val);
                            i += 2;               // skip the two hex digits
                        }
                        break;

                    /* UnicodeEscapeSequence: \\uXXXX or \\u{…} */
                    case 'u':
                        if (i + 1 < end && text.charAt(i + 1) == '{') {   // \\u{…}
                            int j = i + 2;
                            while (j < end && text.charAt(j) != '}') j++;
                            String hex = text.substring(i + 2, j);
                            int code = Integer.parseInt(hex, 16);
                            out.append((char) code);
                            i = j; // will be incremented by the loop
                        } else if (i + 4 < end) {                         // \\uXXXX
                            String hex = text.substring(i + 1, i + 5);
                            int code = Integer.parseInt(hex, 16);
                            out.append((char) code);
                            i += 4; // skip the four hex digits
                        }
                        break;

                    /* NonEscapeCharacter – just copy it */
                    default:
                        out.append(esc);
                }
            } else {
                /* DoubleStringCharacter / SingleStringCharacter */
                out.append(c);
            }
            i++;
        }

        return out.toString();
    }

    /**
     * Parse a JavaScript integer literal and return the smallest boxed
     * numeric type that can hold its value.
     *
     * The returned instance is one of:
     *   java.lang.Byte, java.lang.Short,
     *   java.lang.Integer or java.lang.Long
     *
     * This method follows the same lexical rules as the ANTLR fragment
     * for integerLiteral (iDec, iHex, iOct, iBin).
     */
    public static Number parseIntegerLiteral(String text) {
        // Optional long suffix (l/L/b/B) – ignore it
        int len = text.length();
        char last = text.charAt(len - 1);
        if (last == 'l' || last == 'L' || last == 'b' || last == 'B')
            text = text.substring(0, len - 1);

        // Detect radix and strip leading markers
        int radix;
        String digits = text;

        if (digits.startsWith("0x") || digits.startsWith("0X")) {
            radix = 16;          // HEX_LITERAL
            digits = digits.substring(2);
        } else if (digits.startsWith("0b") || digits.startsWith("0B")) {
            radix = 2;           // BINARY_LITERAL
            digits = digits.substring(2);
        } else if (digits.length() > 1 && digits.charAt(0) == '0') {
            radix = 8;           // OCT_LITERAL
            int i = 1;
            while (i < digits.length() && digits.charAt(i) == '_')
                i++;
            digits = digits.substring(i - 1);   // keep the leading zero
        } else {
            radix = 10;          // DECIMAL_LITERAL
        }

        // Remove underscore separators
        StringBuilder sb = new StringBuilder(digits.length());
        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            if (c != '_') sb.append(c);
        }
        digits = sb.toString();

        if (digits.isEmpty())
            throw new NumberFormatException("no digits in literal");

        return switch (last) {
            case 'l', 'L' -> Long.parseLong(digits, radix);
            case 'b', 'B' -> Byte.parseByte(digits, radix);
            default -> Integer.parseInt(digits, radix);
        };
    }

    /**
     * Parse a JavaScript/Java‑style floating point literal.
     *
     * Grammar:
     *   floatLiteral : FLOAT_LITERAL | HEX_FLOAT_LITERAL ;
     *
     * The function returns a Double or Float depending on the optional suffix
     * (f/F → Float, d/D or no suffix → Double).  Underscores are not allowed in
     * the literals as per the given rules.
     */
    public static Number parseFloatLiteral(String text) {
        if (text == null || text.isEmpty())
            throw new IllegalArgumentException("empty input");

        // Detect and strip optional suffix (f/F/d/D)
        char last = text.charAt(text.length() - 1);
        boolean isFloat = false;
        if (last == 'f' || last == 'F') {
            isFloat = true;
            text = text.substring(0, text.length() - 1);
        } else if (last == 'd' || last == 'D') {
            // explicit double suffix – no change needed
            text = text.substring(0, text.length() - 1);
        }

        // Hexadecimal floating point starts with 0x/0X and contains a p/P exponent
        boolean isHexFloat = (text.startsWith("0x") || text.startsWith("0X"))
                && (text.indexOf('p') >= 0 || text.indexOf('P') >= 0);

        try {
            if (isHexFloat) {
                // Java's Double.parseDouble understands hex floats
                if (isFloat)
                    return Float.parseFloat(text);
                return Double.parseDouble(text);
            } else {
                // Decimal float
//                double val = Double.parseDouble(text);
                if (isFloat)
                    return Float.parseFloat(text);
                return Double.parseDouble(text);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid float literal: " + text, e);
        }
    }

    /**
     * Parse a JavaScript/Java‑style character literal.
     *
     * Grammar:
     *   CHAR_LITERAL : 'c' '\'' (~['\\\r\n] | EscapeSequence) '\'' ;
     *
     * Returns the single char represented by the literal or throws an
     * IllegalArgumentException if the syntax is invalid.
     */
    public static char parseCharLiteral(String text) {
        if (text == null || !text.startsWith("c'") || !text.endsWith("'"))
            throw new IllegalArgumentException("invalid CHAR_LITERAL: " + text);

        // strip leading c and surrounding single quotes
        String body = text.substring(2, text.length() - 1);
        if (body.isEmpty())
            throw new IllegalArgumentException("empty character literal");

        // Handle escape sequences
        if (body.charAt(0) == '\\') {
            if (body.length() < 2)
                throw new IllegalArgumentException("incomplete escape sequence");

            char esc = body.charAt(1);
            switch (esc) {
                case 'b': return '\b';
                case 'f': return '\f';
                case 'n': return '\n';
                case 'r': return '\r';
                case 't': return '\t';
                case 'v': return '\u000B';
                case '\\': return '\\';
                case '\'': return '\'';     // allow escaped single quote
                case '0':
                    if (body.length() > 2 && Character.isDigit(body.charAt(2)))
                        throw new IllegalArgumentException("invalid \\0 escape");
                    return '\0';

                case 'x':
                    if (body.length() < 4)
                        throw new IllegalArgumentException("incomplete hex escape");
                    int hx = Integer.parseInt(body.substring(2, 4), 16);
                    return (char) hx;

                case 'u':
                    // two forms: \\uXXXX or \\u{…}
                    if (body.charAt(2) == '{') {
                        int close = body.indexOf('}', 3);
                        if (close < 0)
                            throw new IllegalArgumentException("unterminated \\u{ escape");
                        String hex = body.substring(3, close);
                        int code = Integer.parseInt(hex, 16);
                        if (code > 0xFFFF)
                            throw new IllegalArgumentException(
                                    "Unicode code point out of char range: U+" + Integer.toHexString(code));
                        return (char) code;
                    } else {
                        if (body.length() < 6)
                            throw new IllegalArgumentException("incomplete \\u escape");
                        int ux = Integer.parseInt(body.substring(2, 6), 16);
                        return (char) ux;
                    }

                default:
                    // treat as non‑escape character (e.g., \x etc.)
                    throw new IllegalArgumentException("unknown escape sequence: \\" + esc);
            }
        } else {
            if (body.length() != 1)
                throw new IllegalArgumentException("character literal must contain a single char");
            return body.charAt(0);
        }
    }


}
