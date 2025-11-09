package org.siphonlab.ago.json.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.test.Util;

import java.io.IOException;

public class JsonTest {
    @Test
    public void serialize() throws CompilationError, IOException {
        Util.run("json/serialize.ago");
    }
}
