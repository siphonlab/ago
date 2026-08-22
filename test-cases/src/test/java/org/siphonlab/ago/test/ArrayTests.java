package org.siphonlab.ago.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.compiler.CompilationErrorsException;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.siphonlab.ago.test.Util.run;

public class ArrayTests {

    @Test
    public void mapFilterReduce() throws CompilationError, CompilationErrorsException, IOException {
        run("array/map_filter_reduce.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("2", "4", "6", "8", "10", "12",
                "2", "4", "6",
                "21",
                "48",
                "n1-n2-n3-n4-n5-n6"));
    }

    @Test
    public void castTo() throws CompilationError, CompilationErrorsException, IOException {
        run("array/cast_to.ago");
        assertTrue(Trace.outputted("10", "20", "30",
                "10.0", "20.0", "30.0",
                "10.0", "20.0", "30.0"));
    }

    @Test
    public void array_field() throws CompilationError, CompilationErrorsException, IOException {
        run("array/array_field.ago");

    }

    @Test
    public void instance_of() throws CompilationError, CompilationErrorsException, IOException {
        run("array/instanceof.ago");
        Trace.printOutput();
        assertTrue(Trace.outputted("it's an array, length is 3", "1", "2", "3"));
    }


}
