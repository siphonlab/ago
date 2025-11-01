package org.siphonlab.ago.crud.test;

import org.siphonlab.ago.compiler.exception.CompilationError;

import java.io.IOException;

import static org.siphonlab.ago.test.Util.run;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RestfulTest {

    @Test
    public void crud() throws CompilationError, IOException {
        run("restful/crud.ago");
    }


}
