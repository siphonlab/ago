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
        Trace.printOutput();
        Thread.sleep(200);
        assertTrue(Trace.outputted("have a rest", "task1", "subtask 1", "subtask 2", "subtask 3", "move on", "subtask 2 done", "subtask 3 done", "subtask 1 done"));
    }


}
