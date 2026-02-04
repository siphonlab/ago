package org.siphonlab.ago.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.web.RestfulService;

import java.io.IOException;

@Disabled
public class HttpServerTest {

    @Test
    public void greeting() throws CompilationError, IOException {
        run("restful/greeting.ago");
    }

    @Test
    public void testArmeria() throws CompilationError, IOException {
        run("restful/armeria_publisher.ago");
    }


    public void run(String filename) throws CompilationError, IOException {
        Util.compile(filename);

        AgoEngine engine = new AgoEngine();
        AgoClassLoader agoClassLoader = new AgoClassLoader();
        agoClassLoader.loadClasses("../ago-sdk/src/compiled/lang/", "output/%s".formatted(filename));

        engine.load(agoClassLoader);

        RestfulService restfulService = new RestfulService();
        restfulService.installServices(agoClassLoader,engine);
        restfulService.start();
        System.in.read();
    }

}
