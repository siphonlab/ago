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
import org.siphonlab.ago.compiler.CompliationErrorsException;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.siphonlab.ago.test.Util.run;

public class TypesTests {

    @Test
    public void stringMethods() throws CompilationError, CompliationErrorsException, IOException {
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
}
