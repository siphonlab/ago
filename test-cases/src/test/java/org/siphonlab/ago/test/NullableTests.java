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
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.siphonlab.ago.test.Util.run;

public class NullableTests {

    @Test
    public void and() throws CompilationError, IOException {
        run("nullable/and.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("false", "false", "false", "false", "true", "true", "false", "false", "false", "false", "null", "200", "true", "null"));
    }

    @Test
    public void or() throws CompilationError, IOException {
        run("nullable/or.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("true", "true", "true", "false", "true", "true", "false", "false", "false", "false", "200", "200", "true", "200", "null"));
    }

    @Test
    public void not() throws CompilationError, IOException {
        run("nullable/not.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("false", "true", "true", "false", "true"));
    }

    @Test
    public void if_else() throws CompilationError, IOException {
        run("nullable/if_else.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("yes", "no", "no", "yes", "no", "no", "yes", "no", "yes", "no"));
    }

    @Test
    public void equals() throws CompilationError, IOException {
        run("nullable/equals.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("true", "true", "false", "true", "true", "false", "false", "true", "true", "false", "false", "true"));
    }

    @Test
    public void compare() throws CompilationError, IOException {
        run("nullable/compare.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("true", "false", "false"));
    }

    @Test
    public void if_stmt() throws CompilationError, IOException {
        run("nullable/if_stmt.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("b yes", "b2 no", "b3 no", "i yes", "n no", "b yes", "b2 no", "b3 no", "i yes", "n no"));
    }

    @Test
    public void while_stmt() throws CompilationError, IOException {
        run("nullable/while_stmt.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("b yes", "b2 no", "b3 no", "5", "4", "3", "2", "1", "5",
                "b loop", "b loop", "b2 loop", "b3 loop", "b not loop", "b2 not loop", "b2 not loop", "b3 not loop", "b3 not loop", "5", "4", "3", "2", "1", "5"));
    }

    @Test
    public void for_each_stmt() throws CompilationError, IOException {
        run("nullable/for_each.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("1", "2", "3"));
    }

    @Test
    public void with() throws CompilationError, IOException {
        run("nullable/with.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("John", "20"));
    }

    @Test
    public void switch_stmt() throws CompilationError, IOException {
        run("nullable/switch.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("1", "null", "0 or null", "null"));
    }

    @Test
    public void via_stmt() throws CompilationError, IOException {
        run("nullable/via.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("open file sample.txt", "read file content from sample.txt", "close file sample.txt"));
    }

    @Test
    public void narrow() throws CompilationError, IOException {
        run("nullable/narrow.ago");
        Trace.printOutput();
    }

}
