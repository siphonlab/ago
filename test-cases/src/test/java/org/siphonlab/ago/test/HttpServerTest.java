/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.web.RestfulService;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

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
        agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream("../ago-sdk/lang.agopkg")));
        agoClassLoader.loadClasses("output/%s".formatted(filename));

        engine.load(agoClassLoader);

        RestfulService restfulService = new RestfulService();
        restfulService.installServices(agoClassLoader,engine);
        restfulService.start();
        System.in.read();
    }

}
