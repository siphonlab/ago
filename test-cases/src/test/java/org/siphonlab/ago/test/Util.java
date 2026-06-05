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

import io.ebean.platform.postgres.PostgresPlatform;
import io.vertx.core.Vertx;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.ClassFile;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.Unit;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.lang.Trace;
import org.siphonlab.ago.runtime.db.DbSlotsCreatorFactory;
import org.siphonlab.ago.runtime.db.SnowflakeIdGenerator;
import org.siphonlab.ago.runtime.db.lazy.JsonAgoClassLoader;
import org.siphonlab.ago.runtime.rdb.json.JsonPGAdapter;
import org.siphonlab.ago.runtime.db.task.WorkflowEngine;
import org.siphonlab.ago.runtime.vertx.VertxRunSpaceHost;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;
import java.util.zip.ZipInputStream;

import static org.siphonlab.ago.crud.test.RdbDdlTest.generateDDL;

public class Util {
    public static class TestDatabaseConfig {
        private final String database;
        private final String user;
        private final String password;
        private final int port;
        private final String host;

        public TestDatabaseConfig() {
            this.database = "ago";
            this.user = "ago";
            this.password = "ago";
            this.host = "127.0.0.1";
            this.port = 5432;
        }

        public TestDatabaseConfig(Properties props) {
            this.database = props.getProperty("database", "ago");
            this.user = props.getProperty("user", "ago");
            this.password = props.getProperty("password", "ago");
            this.host = props.getProperty("host", "127.0.0.1");

            var port = props.getProperty("port");
            var defPort = 5432;
            if (port != null) {
                try {
                    defPort = Integer.parseInt(port);
                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
            this.port = defPort;
        }

        public BasicDataSource createDataSource() {
            var url = String.format("jdbc:postgresql://%s:%d/%s", this.host, this.port, this.database);
            var ds = new BasicDataSource();
            ds.setDriverClassName("org.postgresql.Driver");
            ds.setUrl(url);
            ds.setUsername(this.user);
            ds.setPassword(this.password);

            return ds;
        }
    }

    public static BasicDataSource connectDataSource() {
        var props = new Properties();
        var url = ClassLoader.getSystemResource("database.properties");
         if(url == null){
            var config = new TestDatabaseConfig();
            return config.createDataSource();
        }
        try {
            props.load(url.openStream());
            var config = new TestDatabaseConfig(props);
            return config.createDataSource();
        } catch (IOException e) {
            return new TestDatabaseConfig().createDataSource();
        }
    }


    public enum RunEngine{
        NettyEngine,
        VertxEngine,
        WorkflowEngine,
    }

    public static RunEngine parseEngine(){
        String s = System.getenv("engine");
        if("vertx".equalsIgnoreCase(s)){
            return RunEngine.VertxEngine;
        } else if("netty".equalsIgnoreCase(s) || StringUtils.isEmpty(s)){
            return RunEngine.NettyEngine;
        } else if ("workflow".equalsIgnoreCase(s)) {
            return RunEngine.WorkflowEngine;
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
        Trace.clear();
        var selectedEngine = parseEngine();
        switch (selectedEngine){
            case NettyEngine:
                runInNettySpace(filename, entrance);
                break;

            case VertxEngine:
                runInVertxSpace(filename,entrance);
                break;

            case WorkflowEngine:
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

        var slotsCreatorFactory = new DbSlotsCreatorFactory<Long>();
        var agoClassLoader = new AgoClassLoader(slotsCreatorFactory);
        String output = "output/%s".formatted(filename);
        if(new File("../ago-sdk/compiled/lang/").exists()) {
            agoClassLoader.loadClasses("../ago-sdk/compiled/lang/", output);
        } else {
            agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream("../ago-sdk/lang.agopkg")));
            agoClassLoader.loadClasses(output);
        }

        var ds = connectDataSource();
        ds.setDefaultAutoCommit(true);
        ds.setMaxTotal(20);

        var rdbAdapter = new JsonPGAdapter<>(agoClassLoader, TypeCode.LONG, new SnowflakeIdGenerator(1),
                agoClassLoader.getBoxTypes(), ds, applicationId);
        slotsCreatorFactory.setAdapter(rdbAdapter);

        generateDDL(output);

        var platform = new PostgresPlatform();
        var outputMapFile = new File(new File(output), "db_map_" + platform.name() + ".yml");
        rdbAdapter.loadTableMap(new FileInputStream(outputMapFile));

        var rdbEngine = new WorkflowEngine<>(rdbAdapter, rdbAdapter, new VertxRunSpaceHost(Vertx.vertx()), 0L);
        rdbAdapter.setClassManager(rdbEngine);
        rdbEngine.load(agoClassLoader);
        rdbEngine.run(entrance);
    }

    public static void resumeWithPGJsonLazy() throws IOException, CompilationError, SQLException {
        var ds = connectDataSource();
        ds.setDefaultAutoCommit(true);

        var slotsCreatorFactory = new DbSlotsCreatorFactory<Long>();
        var agoClassLoader = new JsonAgoClassLoader(slotsCreatorFactory);
        agoClassLoader.loadClasses(ds, applicationId);      // load classes from db

        var rdbAdapter = new JsonPGAdapter<Long>(agoClassLoader, TypeCode.LONG, new SnowflakeIdGenerator(1),
                agoClassLoader.getBoxTypes(), ds, applicationId);

        slotsCreatorFactory.setAdapter(rdbAdapter);

        var rdbEngine = new WorkflowEngine<>(rdbAdapter, rdbAdapter, new VertxRunSpaceHost(Vertx.vertx()), 0L);
        rdbEngine.load(agoClassLoader);
        rdbEngine.resume();

    }

}
