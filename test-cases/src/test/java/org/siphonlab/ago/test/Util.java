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

import groovy.sql.Sql;
import io.vertx.core.Vertx;
import org.agrona.concurrent.SnowflakeIdGenerator;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.ClassFile;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.Unit;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.runtime.rdb.json.lazy.JsonAgoClassLoader;
import org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine;
import org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonPGAdapter;
import org.siphonlab.ago.runtime.rdb.json.lazy.PGJsonSlotsCreatorFactory;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;
import org.siphonlab.ago.runtime.vertx.VertxRunSpaceHost;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipInputStream;

public class Util {

    public enum RunEngine{
        NettyEngine,
        VertxEngine,
        PGJsonReactiveEngine,
        PGJsonLazyEngine,
    }

    public static RunEngine parseEngine(){
        String s = System.getenv("engine");
        if("vertx".equalsIgnoreCase(s)){
            return RunEngine.VertxEngine;
        } else if("PGJsonLazy".equalsIgnoreCase(s)){
            return RunEngine.PGJsonLazyEngine;
        } else if("PGJsonReactive".equalsIgnoreCase(s)){
            return RunEngine.PGJsonReactiveEngine;
        } else if("netty".equalsIgnoreCase(s) || StringUtils.isEmpty(s)){
            return RunEngine.NettyEngine;
        }
        throw new IllegalArgumentException("unknown engine '%s'".formatted(s));
    }

    public static void compile(String filename) throws IOException, CompilationError {
        Compiler compiler = new Compiler();
        Collection<ClassDef> rtClasses = null;
        AgoClassLoader agoClassLoader = new AgoClassLoader();
        if(new File("../ago-sdk/compiled/lang/").exists()) {
            agoClassLoader.loadClasses("../ago-sdk/compiled/lang/");
        } else {
            agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream("../ago-sdk/lang.agopkg")));
        }

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
        var selectedEngine = parseEngine();
        switch (selectedEngine){
            case NettyEngine:
                runInNettySpace(filename, entrance);
                break;

            case VertxEngine:
                runInVertxSpace(filename,entrance);
                break;

            case PGJsonLazyEngine:
                runWithPGJsonLazy(filename, entrance);
                break;
        }

    }

    private static void runInNettySpace(String filename, String entrance) throws IOException, CompilationError {
        compile(filename);

        AgoEngine engine = new AgoEngine();
        AgoClassLoader agoClassLoader = new AgoClassLoader();

        if(new File("../ago-sdk/compiled/lang/").exists()) {
            agoClassLoader.loadClasses("../ago-sdk/compiled/lang/", "output/%s".formatted(filename));
        } else {
            agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream("../ago-sdk/lang.agopkg")));
            agoClassLoader.loadClasses("output/%s".formatted(filename));
        }

        engine.load(agoClassLoader);

        engine.run(entrance);
    }

    public static void runInVertxSpace(String filename, String entrance) throws CompilationError, IOException {
        Util.compile(filename);

        AgoEngine engine = new AgoEngine(new VertxRunSpaceHost(Vertx.vertx()));
        AgoClassLoader agoClassLoader = new AgoClassLoader();
        if(new File("../ago-sdk/compiled/lang/").exists()) {
            agoClassLoader.loadClasses("../ago-sdk/compiled/lang/", "output/%s".formatted(filename));
        } else {
            agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream("../ago-sdk/lang.agopkg")));
            agoClassLoader.loadClasses("output/%s".formatted(filename));
        }

        engine.load(agoClassLoader);

        engine.run(entrance);
    }

    public static int applicationId = 0;
    public static void runWithPGJsonLazy(String filename, String entrance) throws IOException, CompilationError {
        compile(filename);

        if (applicationId == 0) applicationId = RandomUtils.insecure().randomInt();

        PGJsonSlotsCreatorFactory slotsCreatorFactory = new PGJsonSlotsCreatorFactory();
        var agoClassLoader = new AgoClassLoader(slotsCreatorFactory);
        if(new File("../ago-sdk/compiled/lang/").exists()) {
            agoClassLoader.loadClasses("../ago-sdk/compiled/lang/", "output/%s".formatted(filename));
        } else {
            agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream("../ago-sdk/lang.agopkg")));
            agoClassLoader.loadClasses("output/%s".formatted(filename));
        }

        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://127.0.0.1:5432/ago");
        ds.setUsername("ago");
        ds.setPassword("ago");
        ds.setDefaultAutoCommit(true);
        ds.setMaxTotal(20);

        var rdbAdapter = new LazyJsonPGAdapter(agoClassLoader.getBoxTypes(), agoClassLoader,
                                applicationId,
                                new SnowflakeIdGenerator(1));
        slotsCreatorFactory.setAdapter(rdbAdapter);
        rdbAdapter.setDataSource(ds);

        PersistentRdbEngine rdbEngine = new LazyJsonAgoEngine(rdbAdapter, new VertxRunSpaceHost(Vertx.vertx()));
        slotsCreatorFactory.setEngine(rdbEngine);
        rdbEngine.load(agoClassLoader);
        rdbEngine.run(entrance);
    }

    public static void resumeWithPGJsonLazy() throws IOException, CompilationError, SQLException {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://127.0.0.1:5432/ago");
        ds.setUsername("ago");
        ds.setPassword("ago");
        ds.setDefaultAutoCommit(true);

        PGJsonSlotsCreatorFactory slotsCreatorFactory = new PGJsonSlotsCreatorFactory();
        var agoClassLoader = new JsonAgoClassLoader(slotsCreatorFactory);
        agoClassLoader.loadClasses(ds, applicationId);      // load classes from db

        var rdbAdapter = new LazyJsonPGAdapter(agoClassLoader.getBoxTypes(), agoClassLoader, applicationId, new SnowflakeIdGenerator(1));

        slotsCreatorFactory.setAdapter(rdbAdapter);
        rdbAdapter.setDataSource(ds);

        PersistentRdbEngine rdbEngine = new LazyJsonAgoEngine(rdbAdapter, new VertxRunSpaceHost(Vertx.vertx()));
        slotsCreatorFactory.setEngine(rdbEngine);
        rdbEngine.load(agoClassLoader);
        rdbEngine.resume();

    }


}
