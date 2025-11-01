package org.siphonlab.ago.test;

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;

import java.io.IOException;

import static org.siphonlab.ago.test.Util.run;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InterfaceTest {

    @Test @Tag("parameterized")
    public void interface_test() throws CompilationError, IOException {
        run("interface/generic.ago");
        assertTrue(Trace.outputted("meow", "meow"));
    }

    @Test
    public void meta_test() throws CompilationError, IOException {
        run("interface/meta.ago");
        assertTrue(Trace.outputted("timeout: 100",
                "timeout: 100",
                "100",
                "timeout: 100",
                "timeout: 200",
                "timeout: 100"));
    }

    @Test
    public void configurable_fun_test() throws CompilationError, IOException {
        run("interface/configurable_fun.ago");
        assertTrue(Trace.outputted("200", "prod:1000", "dev:3000"));
    }


}
