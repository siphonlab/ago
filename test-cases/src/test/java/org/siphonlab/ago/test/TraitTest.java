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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.siphonlab.ago.test.Util.run;

public class TraitTest {

    @Test
    public void permit_test() throws CompilationError, IOException {
        run("trait/permit.ago");
        assertTrue(Trace.outputted("delay: 1000", "1000", "delay: 1000", "1000", "delay: 3000", "3000", "delay: 3000", "3000"));
    }

    @Test
    public void inherits_test() throws CompilationError, IOException {
        run("trait/inherits.ago");
        assertTrue(Trace.outputted("A:I'm a", "A:I'm b", "A:I'm b"));
    }

    @Test
    public void generic_test() throws CompilationError, IOException {
        run("trait/generic.ago");
        assertTrue(Trace.outputted("setItem", "printItemInfo", "Samoyed", "test", "printItemInfo", "Samoyed", "printItemInfo", "Samoyed"));
    }
}
