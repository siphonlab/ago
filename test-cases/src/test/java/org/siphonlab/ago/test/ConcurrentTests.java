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
import static org.siphonlab.ago.test.Util.runInVertxSpace;

public class ConcurrentTests {

    @Test
    public void race() throws IOException, CompilationError, InterruptedException {
        runInVertxSpace("concurrent/race.ago", "main#");
        assertTrue(Trace.outputted("f1", "f2", "f3", "put the elephant in"));
    }

    @Test
    public void create_runspace() throws IOException, CompilationError, InterruptedException {
        runInVertxSpace("concurrent/create_runspace.ago", "main#");
        Trace.printOutput();
        assertTrue(Trace.outputted("test", "3"));
    }

    @Test
    public void spawn_many() throws IOException, CompilationError, InterruptedException {
        runInVertxSpace("concurrent/spawn_many.ago", "main#");
        assertTrue(Trace.outputted("f1", "f2", "f3", "put the elephant in", "close the door", "open the door"));
    }

    @Test
    public void await_many() throws IOException, CompilationError, InterruptedException {
        runInVertxSpace("concurrent/await_many.ago", "main#");
        assertTrue(Trace.outputted("f1", "f2", "f3"));
    }

    @Test
    public void structured() throws IOException, CompilationError, InterruptedException {
        runInVertxSpace("concurrent/structured.ago", "main#");
        Thread.sleep(200);
        Trace.printOutput();
        assertTrue(Trace.outputted("have a rest", "task1", "subtask 1", "subtask 2", "subtask 3", "move on", "subtask 2 done", "subtask 3 done", "subtask 1 done"));
    }

    @Test
    public void forkContext() throws IOException, CompilationError, InterruptedException {
        runInVertxSpace("concurrent/fork_context.ago", "main#");
    }


}
