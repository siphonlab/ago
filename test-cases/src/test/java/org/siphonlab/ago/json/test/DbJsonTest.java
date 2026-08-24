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

public class DbJsonTest {

    @Test
    public void dumpEntity() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/dump_entity.ago");
        Trace.printOutput();
    }

    @Test
    public void dumpEntityWithObjectRef() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/dump_object_ref.ago");
        Trace.printOutput();
    }

    @Test
    public void dumpArrayInSlot() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/dump_array_slot.ago");
        Trace.printOutput();
    }

    @Test
    public void dumpNullableSlot() throws CompilationError, CompilationErrorsException, IOException {
        Util.run("db_json/dump_nullable_slot.ago");
        Trace.printOutput();
    }
}
