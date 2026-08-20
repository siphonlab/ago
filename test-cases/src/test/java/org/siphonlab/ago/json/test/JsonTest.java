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
package org.siphonlab.ago.json.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.compiler.CompilationErrorsException;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;
import org.siphonlab.ago.test.Util;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonTest {
    @Test
    public void serialize() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("json/serialize.ago");
    }

    @Test
    public void serialize_test1() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("json/serialize_test1.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("{\"city\":\"Beijing\",\"zip\":100000}", "{\"name\":\"Alice\",\"age\":30}", "{\"name\":\"Bob\",\"addr\":{\"city\":\"Shanghai\",\"zip\":200000},\"age\":25}"));
    }

    @Test
    public void serialize_types() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("json/serialize_types.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("{\"b\":42,\"s\":100,\"d\":2.71828,\"ch\":\"A\",\"f\":3.14}", "{\"dec\":99.99}"));
    }

    @Test
    public void serialize_boxed() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("json/serialize_boxed.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted(
            "{\"val\":42}",
            "{\"val\":999}",
            "{\"val\":3.14}",
            "{\"val\":2.718}",
            "{\"val\":127}",
            "{\"val\":32767}",
            "{\"val\":\"X\"}",
            "{\"val\":true}",
            "{\"val\":\"hello\"}"
        ));
    }

    @Test
    public void decode_test1() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("json/decode_test1.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("Alice", "30"));
    }
}
