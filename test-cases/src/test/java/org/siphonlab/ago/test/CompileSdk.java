package org.siphonlab.ago.test;

import org.junit.jupiter.api.Test;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.CompilationErrorsException;
import org.siphonlab.ago.compiler.ClassFile;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.module.ProjectParser;

import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompileSdk {
    @Test
    public void compile() throws CompilationError, CompilationErrorsException, IOException {
        var langModule = new ProjectParser().parse("../ago-sdk/src/lang/module.info");
        Compiler langCompiler = new Compiler(langModule);
        try {
            langCompiler.compile();
        } catch (CompilationErrorsException e) {
            System.err.println(e.getMessage());
            assertTrue(false, "SDK compilation failed: " + e.getMessage());
            return;
        }
        new ClassFile(langModule).saveToDirectory("../ago-sdk/compiled/lang/");

        var ioModule = new ProjectParser().parse("../ago-sdk/src/io/module.info");
        Compiler ioCompiler = new Compiler(ioModule);
        AgoClassLoader agoClassLoader = new AgoClassLoader();
        agoClassLoader.loadModuleFromDirectory("../ago-sdk/compiled/lang/");
        ioCompiler.load(agoClassLoader);
        try {
            ioCompiler.compile();
        } catch (CompilationErrorsException e) {
            System.err.println(e.getMessage());
            assertTrue(false, "IO module compilation failed: " + e.getMessage());
            return;
        }
        new ClassFile(ioModule).saveToDirectory("../ago-sdk/compiled/io/");

        new ClassFile(langModule).createPackage(new FileOutputStream("../ago-sdk/lang.agopkg"));
    }
}
