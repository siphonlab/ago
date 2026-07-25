package org.siphonlab.ago.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.compiler.CompliationErrorsException;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.siphonlab.ago.test.Util.run;

public class ArrayTests {

    @Test
    public void mapFilterReduce() throws CompilationError, CompliationErrorsException, IOException {
        run("array/map_filter_reduce.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("2", "4", "6", "8", "10", "12",
                "2", "4", "6",
                "21",
                "48",
                "n1-n2-n3-n4-n5-n6"));
    }

    @Test
    public void castTo() throws CompilationError, CompliationErrorsException, IOException {
        run("array/cast_to.ago");
        assertTrue(Trace.outputted("10", "20", "30",
                "10.0", "20.0", "30.0",
                "10.0", "20.0", "30.0"));
    }
}
