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
import org.siphonlab.ago.compiler.CompilationErrorsException;
import org.siphonlab.ago.compiler.ClassFile;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.module.ProjectParser;

import java.io.FileOutputStream;
import java.io.IOException;

public class CompilerTest {

    @Test
    public void hello_world() throws IOException, CompilationError, CompilationErrorsException {
        Util.compile("bootstrap/hello_world.ago");
    }

    @Test
    public void _1st() throws IOException, CompilationError, CompilationErrorsException {
        Util.compile("bootstrap/0.add.ago");
    }

    @Test @Disabled
    public void langCompile() throws CompilationError, CompilationErrorsException, IOException {
        var module = new ProjectParser().parse("../ago-sdk/src/lang/module.info");
        Compiler compiler = new Compiler(module);
        try {
            compiler.compile();
        } catch (CompilationErrorsException e) {
            System.err.println(e.getMessage());
            return;
        }
        new ClassFile(module).saveToDirectory("../ago-sdk/compiled/lang/");
        new ClassFile(module).createPackage(new FileOutputStream("../ago-sdk/lang.agopkg"));
    }

    @Test @Disabled
    public void ioCompile() throws CompilationError, CompilationErrorsException, IOException {
        var module = new ProjectParser().parse("../ago-sdk/src/io/module.info");
        Compiler compiler = new Compiler(module);
        try {
            compiler.compile();
        } catch (CompilationErrorsException e) {
            System.err.println(e.getMessage());
            return;
        }
        new ClassFile(module).saveToDirectory("../ago-sdk/compiled/io/");
        new ClassFile(module).createPackage(new FileOutputStream("../ago-sdk/io.agopkg"));
    }


}
