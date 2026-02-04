package org.siphonlab.ago.test;

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
        assertTrue(Trace.outputted("pause", "resume caller", "done"));
    }

    @Test
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
