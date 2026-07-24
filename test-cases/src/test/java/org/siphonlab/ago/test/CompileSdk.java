package org.siphonlab.ago.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.compiler.CompliationErrorsException;
import org.siphonlab.ago.compiler.ClassFile;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.module.ProjectParser;

import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompileSdk {
    @Test
    public void compile() throws CompilationError, CompliationErrorsException, IOException {
        var module = new ProjectParser().parse("../ago-sdk/src/lang/module.info");
        Compiler compiler = new Compiler(module);
        try {
            compiler.compile();
        } catch (CompliationErrorsException e) {
            System.err.println(e.getMessage());
            assertTrue(false, "SDK compilation failed: " + e.getMessage());
            return;
        }
        new ClassFile(module).saveToDirectory("../ago-sdk/compiled/lang/");
        new ClassFile(module).createPackage(new FileOutputStream("../ago-sdk/lang.agopkg"));
    }
}
