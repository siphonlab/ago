package org.siphonlab.ago.test;

import io.vertx.core.Vertx;
import org.apache.commons.io.file.PathUtils;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.ClassFile;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.Unit;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.runtime.vertx.VertxRunSpaceHost;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class Util {

    public static void compile(String filename) throws IOException, CompilationError {
        Compiler compiler = new Compiler();
        Collection<ClassDef> rtClasses = null;
        AgoClassLoader agoClassLoader = new AgoClassLoader();
        agoClassLoader.loadClasses("output/rt");

        rtClasses = compiler.load(agoClassLoader);
        Unit[] units = compiler.compile(new File[]{new File("examples/%s".formatted(filename))}, rtClasses.toArray(new ClassDef[0]));

        var dir = new File("output/%s".formatted(filename));
        if (!dir.exists()) dir.mkdirs();
        else PathUtils.cleanDirectory(dir.toPath());
        ClassFile.saveToDirectory(units, dir.getAbsolutePath());
    }

    public static void run(String filename) throws CompilationError, IOException {
        run(filename, "main#");
    }

    public static void run(String filename, String entrance) throws CompilationError, IOException {
        compile(filename);

        AgoEngine engine = new AgoEngine();
        AgoClassLoader agoClassLoader = new AgoClassLoader();

        agoClassLoader.loadClasses("output/rt", "output/%s".formatted(filename));

        engine.load(agoClassLoader);

        engine.run(entrance);
    }

    public static void runInVertxSpace(String filename, String entrance) throws CompilationError, IOException {
        Util.compile(filename);

        AgoEngine engine = new AgoEngine(new VertxRunSpaceHost(Vertx.vertx()));
        AgoClassLoader agoClassLoader = new AgoClassLoader();
        agoClassLoader.loadClasses("output/rt", "output/%s".formatted(filename));

        engine.load(agoClassLoader);

        engine.run(entrance);
    }


}
