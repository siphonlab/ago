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
package org.siphonlab.ago.ruleserver.test;

import groovy.sql.Sql;
import io.ebean.platform.postgres.PostgresPlatform;
import io.vertx.core.Vertx;
import org.agrona.concurrent.SnowflakeIdGenerator;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.siphonlab.ago.MetaClass;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.runtime.rdb.RdbDDLGenerator;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;
import org.siphonlab.ago.runtime.rdb.json.PGJsonDDLGenerator;
import org.siphonlab.ago.runtime.rdb.json.lazy.JsonAgoClassLoader;
import org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonAgoEngine;
import org.siphonlab.ago.runtime.rdb.json.lazy.LazyJsonPGAdapter;
import org.siphonlab.ago.runtime.rdb.json.lazy.PGJsonSlotsCreatorFactory;
import org.siphonlab.ago.runtime.vertx.VertxRunSpaceHost;
import org.siphonlab.ago.test.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AsRuleSeverLazyInstance {

    @Test
    public void test_generate_sql() throws IOException, CompilationError {
        Util.compile("ruleserver/1.ago");

        String output = "output/ruleserver/1.ago";
        generateDDL(output);
    }

    @Test
    public void test_run() throws IOException, CompilationError {
        Util.compile("org/siphonlab/ago/ruleserver/test/1");
        runWithPG("output/ruleserver/1");
    }

    @Test
    public void test_resume() throws IOException, CompilationError, SQLException {
        resumeWithPG();
    }

    @Test
    public void test_resume_native() throws IOException, CompilationError, SQLException {
        resumeNativeFrameWithPG();
    }

    @Test
    public void test_mq() throws IOException, CompilationError, SQLException {
        Util.compile("org/siphonlab/ago/ruleserver/test/mq");
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://127.0.0.1:5432/ago");
        ds.setUsername("ago");
        ds.setPassword("ago");

        int applicationId = 1;

        PGJsonSlotsCreatorFactory slotsCreatorFactory = new PGJsonSlotsCreatorFactory();
        var agoClassLoader = new AgoClassLoader(slotsCreatorFactory);
        agoClassLoader.loadClasses("../ago-sdk/src/compiled/lang/");
        agoClassLoader.loadClasses("output/ruleserver/mq");

//        var agoClassLoader = new JsonAgoClassLoader(new MetaClass(), slotsCreatorFactory);
//        agoClassLoader.loadClasses(ds, applicationId);      // load classes from db

        var rdbAdapter = new LazyJsonPGAdapter(agoClassLoader.getBoxTypes(), agoClassLoader, applicationId, new SnowflakeIdGenerator(1));

        slotsCreatorFactory.setAdapter(rdbAdapter);
        rdbAdapter.setDataSource(ds);

        PersistentRdbEngine rdbEngine = new LazyJsonAgoEngine(rdbAdapter, new VertxRunSpaceHost(Vertx.vertx()));
        slotsCreatorFactory.setEngine(rdbEngine);
        rdbEngine.load(agoClassLoader);
        rdbEngine.run("main#");

        System.in.read();
    }

    public void runWithPG(String output) throws IOException {
        PGJsonSlotsCreatorFactory slotsCreatorFactory = new PGJsonSlotsCreatorFactory();
        var agoClassLoader = new AgoClassLoader(slotsCreatorFactory);
        agoClassLoader.loadClasses("../ago-sdk/src/compiled/lang/");
        agoClassLoader.loadClasses(output);

        PostgresPlatform platform = new PostgresPlatform();
        var outputSqlFile = new File(new File(output), "create_tables_%s.sql".formatted(platform.name()));

        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://127.0.0.1:5432/ago");
        ds.setUsername("ago");
        ds.setPassword("ago");
        ds.setDefaultAutoCommit(true);
        ds.setMaxTotal(20);

        int applicationId = 1;
        var rdbAdapter = new LazyJsonPGAdapter(agoClassLoader.getBoxTypes(), agoClassLoader, applicationId, new SnowflakeIdGenerator(1));
        slotsCreatorFactory.setAdapter(rdbAdapter);
        rdbAdapter.setDataSource(ds);
        // for first run
//        rdbAdapter.executeDDL(FileUtils.readFileToString(outputSqlFile, "utf-8"));

        PersistentRdbEngine rdbEngine = new LazyJsonAgoEngine(rdbAdapter, new VertxRunSpaceHost(Vertx.vertx()));
        slotsCreatorFactory.setEngine(rdbEngine);
        rdbEngine.load(agoClassLoader);
        rdbEngine.run("main#");
    }

    private void generateDDL(String output) throws IOException {
        var agoClassLoader = new AgoClassLoader();
        agoClassLoader.loadClasses("../ago-sdk/src/compiled/lang/");
        agoClassLoader.loadClasses(output);

        PostgresPlatform platform = new PostgresPlatform();
        var outputSqlFile = new File(new File(output), "create_tables_%s.sql".formatted(platform.name()));
        FileOutputStream fileOutputStream = new FileOutputStream(outputSqlFile);

        int applicationId = 1;
        LazyJsonPGAdapter rdbAdapter = new LazyJsonPGAdapter(agoClassLoader.getBoxTypes(), agoClassLoader, applicationId, new SnowflakeIdGenerator(1));
        RdbDDLGenerator rdbDDLGenerator = new PGJsonDDLGenerator(agoClassLoader, rdbAdapter, platform);
        rdbDDLGenerator.generate(fileOutputStream);
    }

    public void resumeWithPG() throws IOException, SQLException {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://127.0.0.1:5432/ago");
        ds.setUsername("ago");
        ds.setPassword("ago");
        ds.setDefaultAutoCommit(true);

        int applicationId = 1;

        new Sql(ds).execute("update ago_frame af set state = 1, pc = 0 where af.ago_class  = 'main#' and application =? ", List.of(applicationId));

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

        System.in.read();
    }

    public void resumeNativeFrameWithPG() throws IOException, SQLException {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://127.0.0.1:5432/ago");
        ds.setUsername("ago");
        ds.setPassword("ago");
        ds.setDefaultAutoCommit(true);

        int applicationId = 1;

        new Sql(ds).execute("update ago_frame af set state = 1 where af.ago_class = 'addNative#' and application =? ", List.of(applicationId));

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

        System.in.read();
    }

}
