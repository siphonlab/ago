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

import org.junit.jupiter.api.Disabled;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.siphonlab.ago.test.Util.run;
import static org.siphonlab.ago.test.Util.runInVertxSpace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FunctionTest {

    @Test
    public void pause() throws CompilationError, IOException {
        run("function/pause.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("pause", "resume caller", "done"));
    }

    @Test @Disabled
    public void mq() throws CompilationError, IOException {
        runInVertxSpace("function/mq.ago", "main#");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue(Trace.outputted("5"));
    }

    @Test
    public void functor() throws CompilationError, IOException {
        runInVertxSpace("function/functor.ago", "main#");
        try {
            Thread.sleep(3000);     // the main function cannot prevent the event loop shutdown for the EntranceCallframe already exit, and sleep cannot make vertx keep alive
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue(Trace.outputted("3", "5", "test spawn", "test fork"));
    }


}
