package org.siphonlab.ago.test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.siphonlab.ago.compiler.ClassFile;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.Unit;

import java.io.File;
import java.io.IOException;

public class CompilerTest {

    @Test
    public void hello_world() throws IOException, CompilationError {
        Util.compile("bootstrap/hello_world.ago");
    }

    @Test
    public void _1st() throws IOException, CompilationError {
        Util.compile("bootstrap/0.add.ago");
    }

    @Test @Disabled
    public void langCompile() throws CompilationError, IOException {
        Compiler compiler = new Compiler();
        Unit[] units = compiler.compile(new File[]{
                new File("../ago-sdk/src/lang/lang.ago"),
                new File("../ago-sdk/src/lang/runspace.ago"),
                new File("../ago-sdk/src/lang/atomic.ago")
        });
        ClassFile.saveToDirectory(units, "../ago-sdk/src/compiled/lang/");
    }



}
