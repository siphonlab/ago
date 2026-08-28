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
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbJsonTest {

    {
        SLF4JBridgeHandler.install();
    }

    @Test
    public void encodePrimitives() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/encode_primitives.ago");
        Trace.printOutput();
        assertTrue(Trace.outputtedMatch(
            "\\[.*Alice.*,.*30.*,.*true.*,.*2\\.718.*\\]"
        ));
    }

    @Test
    public void encodeObjectRef() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/encode_object_ref.ago");
        Trace.printOutput();
        assertTrue(Trace.outputtedMatch(
            "\\[.*Beijing.*,.*100000.*\\]",
            "\\[.*Bob.*,.*25.*,.*Address:\\d+.*\\]"
        ));
    }

    @Test
    public void encodeNullable() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/encode_nullable.ago");
        Trace.printOutput();
        assertTrue(Trace.outputtedMatch(
            "\\[.*Widget.*,.*42.*,.*null.*\\]"
        ));
    }

    @Test
    public void encodeBoxer() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/encode_boxer.ago");
        Trace.printOutput();
        assertTrue(Trace.outputtedMatch(
            "\\[.*42.*,.*\\{.*type.*lang\\.Integer.*value.*99.*\\}.*\\]"
        ));
    }

    @Test
    public void encodeArray() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/encode_array.ago");
        Trace.printOutput();
        assertTrue(Trace.outputtedMatch(
            "\\[.*Alpha.*,.*lang\\.Array<int>:\\d+.*\\]"
        ));
    }

    @Test
    public void encodeList() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/encode_list.ago");
        Trace.printOutput();
        assertTrue(Trace.outputtedMatch(
            "\\[.*Q1.*,.*"
        ));
    }

    @Test
    public void encodeMap() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/encode_map.ago");
        Trace.printOutput();
        assertTrue(Trace.outputtedMatch(
            "\\[.*\\]"
        ));
    }

    @Test
    public void encodeClassRef() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/encode_classref.ago");
        Trace.printOutput();
        assertTrue(Trace.outputtedMatch(
            "\\[.*test.*,.*int.*\\]"
        ));
    }
}
