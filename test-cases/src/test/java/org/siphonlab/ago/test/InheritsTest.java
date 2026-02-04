package org.siphonlab.ago.test;

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.siphonlab.ago.test.Util.run;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InheritsTest {

    @Test
    public void child_class_test() throws CompilationError, IOException {
        run("inherits/child_class.ago");
        assertTrue(Trace.outputted("test"));
    }

}
