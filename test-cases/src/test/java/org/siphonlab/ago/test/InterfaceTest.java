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

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.siphonlab.ago.test.Util.run;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InterfaceTest {

    @Test @Tag("parameterized")
    public void interface_test() throws CompilationError, IOException {
        run("interface/generic.ago");
        assertTrue(Trace.outputted("meow", "meow"));
    }

    @Test
    public void meta_test() throws CompilationError, IOException {
        run("interface/meta.ago");
        assertTrue(Trace.outputted("timeout: 100",
                "timeout: 100",
                "100",
                "timeout: 100",
                "timeout: 200",
                "timeout: 100"));
    }

    @Test
    public void configurable_fun_test() throws CompilationError, IOException {
        run("interface/configurable_fun.ago");
        assertTrue(Trace.outputted("200", "prod:1000", "dev:3000"));
    }


}
