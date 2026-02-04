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
