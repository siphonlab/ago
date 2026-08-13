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
package org.siphonlab.ago.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.compiler.CompilationErrorsException;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.siphonlab.ago.test.Util.run;

public class TypesTests {

    @Test
    public void stringMethods() throws CompilationError, CompilationErrorsException, IOException {
        run("types/string_methods.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted(
                // length
                "5", "5",
                // charAt
                "H", "H",
                // substring
                "ello", "ello",
                // concat
                "HelloWorld", "HelloWorld",
                // contains
                "true", "true",
                // startsWith
                "true", "true",
                // endsWith
                "true", "true",
                // indexOf char
                "1", "1",
                // indexOf str
                "6", "6",
                // lastIndexOf char
                "4", "4",
                // trim
                "Hello", "Hello",
                // toLowerCase
                "hello world", "hello world",
                // toUpperCase
                "HELLO WORLD", "HELLO WORLD",
                // replace char
                "Heiio", "Heiio",
                // replace str
                "Hi World", "Hi World",
                // split (split("", -1) produces trailing empty element)
                "[H,e,l,l,o,]", "[H,e,l,l,o,]",
                // isEmpty
                "false", "true", "false", "true",
                // equalsIgnoreCase
                "true", "true"
        ));
    }

    @Test
    public void intMethods() throws CompilationError, CompilationErrorsException, IOException {
        run("types/int_methods.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted(
                // abs
                "42", "42",
                // negate
                "-42", "-7",
                // clamp
                "42", "0", "100",
                // hexString
                "2a", "2a",
                // toBinaryString
                "101010", "101010",
                // bitCount
                "3", "3",
                // reverseBytes
                "704643072", "704643072",
                // metaclass statics
                "-2147483648", "2147483647"
        ));
    }

    @Test
    public void longMethods() throws CompilationError, CompilationErrorsException, IOException {
        run("types/long_methods.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted(
                "42", "42", "-42", "-7", "42", "0", "100", "2a", "2a", "101010", "101010", "3", "3", "3026418949592973312", "3026418949592973312", "-9223372036854775808", "9223372036854775807"
        ));
    }

    @Test
    public void floatMethods() throws CompilationError, CompilationErrorsException, IOException {
        run("types/float_methods.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted(
                // abs
                "3.14", "2.5",
                // isFinite
                "true", "true",
                // isInfinite
                "true", "true",
                // isNaN
                "true", "false",
                // roundToInt
                "3", "3",
                // roundToLong
                "3", "3",
                // ceil
                "4.0", "4.0",
                // floor
                "3.0", "3.0",

                "3.14", "3.14", "true"
        ));
    }

    @Test
    public void doubleMethods() throws CompilationError, CompilationErrorsException, IOException {
        run("types/double_methods.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted(
                // abs
                "3.14", "2.5",
                // isFinite
                "true", "true",
                // isInfinite
                "true", "true",
                // isNaN
                "true", "false",
                // roundToInt
                "3", "3",
                // roundToLong
                "3", "3",
                // ceil
                "4.0", "4.0",
                // floor
                "3.0", "3.0",
                "3.14", "3.14", "true"
        ));
    }

    @Test
    public void byteMethods() throws CompilationError, CompilationErrorsException, IOException {
        run("types/byte_methods.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted(
                // abs
                "64", "127",
                // clamp
                "50", "0", "50",
                // toHexString
                "40", "40",

                "-128", "127"
        ));
    }

    @Test
    public void shortMethods() throws CompilationError, CompilationErrorsException, IOException {
        run("types/short_methods.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted(
                // abs
                "-32768", "100",
                // clamp
                "0", "0", "500",
                // toHexString
                "8000", "8000",

                "-32768", "32767"
        ));
    }

    @Test
    public void decimalMethods() throws CompilationError, CompilationErrorsException, IOException {
        run("types/decimal_methods.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted(
                // abs
                "123.456", "99",
                // isFinite
                "true", "true",
                // isInfinite
                "false", "false",
                // isNaN
                "false", "false",
                // roundToInt
                "123", "123",
                // roundToLong
                "123", "123",
                // scale
                "3", "3",

                "123.46", "123.5", "1", "-1", "0", "0"
        ));
    }

    @Test
    public void charMethods() throws CompilationError, CompilationErrorsException, IOException {
        run("types/char_methods.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted(
                // isDigit
                "false", "true",
                // isLetter
                "true", "false",
                // isWhitespace
                "false", "true",
                // isUpperCase
                "false", "true",
                // isLowerCase
                "true", "false",
                // toLowerCaseChar
                "z", "m",
                // toUpperCaseChar
                "A", "Z",
                // digitValue
                "7", "3",

                "0", "65535"
        ));
    }
}
