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

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.siphonlab.ago.compiler.ClassFile;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.Unit;

import java.io.File;
import java.io.IOException;

public class CompilerTest {

    @Test
    public void hello_world() throws IOException, CompilationError {
        Util.compile("bootstrap/hello_world.ago");
    }

    @Test
    public void _1st() throws IOException, CompilationError {
        Util.compile("bootstrap/0.add.ago");
    }

    @Test @Disabled
    public void langCompile() throws CompilationError, IOException {
        Compiler compiler = new Compiler();
        Unit[] units = compiler.compile(new File[]{
                new File("../ago-sdk/src/lang/lang.ago"),
                new File("../ago-sdk/src/lang/collection.ago"),
                new File("../ago-sdk/src/lang/runspace.ago"),
                new File("../ago-sdk/src/lang/atomic.ago")
        });
        ClassFile.saveToDirectory(units, "../ago-sdk/compiled/lang/");
    }



}
