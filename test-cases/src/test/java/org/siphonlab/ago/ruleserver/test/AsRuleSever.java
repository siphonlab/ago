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

import io.ebean.platform.postgres.PostgresPlatform;
import org.agrona.concurrent.SnowflakeIdGenerator;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.reactive.PersistentRdbEngine;
import org.siphonlab.ago.runtime.rdb.reactive.json.ReactiveJsonSlotsCreatorFactory;
import org.siphonlab.ago.runtime.rdb.reactive.json.ReactiveJsonAgoEngine;
import org.siphonlab.ago.runtime.rdb.reactive.json.ReactiveJsonPGAdapter;
import org.siphonlab.ago.runtime.rdb.json.PGJsonDDLGenerator;
import org.siphonlab.ago.test.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

public class AsRuleSever {

    @Test
    public void test_generate_sql() throws IOException, CompilationError {
        Util.compile("examples/bootstrap/0.add.ago");

        String output = "output/ruleserver/1";
        generateDDL(output);
    }

    @Test
    public void test_run() throws IOException, CompilationError {
        runWithPG("output/ruleserver/1");
    }

    public void runWithPG(String output) throws IOException {
        ReactiveJsonSlotsCreatorFactory slotsCreatorFactory = new ReactiveJsonSlotsCreatorFactory(null);
        var agoClassLoader = new AgoClassLoader(slotsCreatorFactory);
        agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream("../ago-sdk/lang.agopkg")));
        agoClassLoader.loadClasses(output);

        PostgresPlatform platform = new PostgresPlatform();
        var outputSqlFile = new File(new File(output), "create_tables_%s.sql".formatted(platform.name()));

        BasicDataSource ds = new org.apache.commons.dbcp2.BasicDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://127.0.0.1:5432/ago");
        ds.setUsername("ago");
        ds.setPassword("ago");
        ds.setDefaultAutoCommit(true);

        int applicationId = 1;
        var rdbAdapter = new ReactiveJsonPGAdapter(agoClassLoader.getBoxTypes(), agoClassLoader, applicationId, new SnowflakeIdGenerator(1));
        slotsCreatorFactory.setAdapter(rdbAdapter);
        rdbAdapter.setDataSource(ds);
        // for first run
//        rdbAdapter.executeDDL(FileUtils.readFileToString(outputSqlFile, "utf-8"));

        PersistentRdbEngine rdbEngine = new ReactiveJsonAgoEngine(rdbAdapter);
        rdbEngine.load(agoClassLoader);
        rdbEngine.run("main#");
    }

    private void generateDDL(String output) throws IOException {
        var agoClassLoader = new AgoClassLoader();
        agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream("../ago-sdk/lang.agopkg")));
        agoClassLoader.loadClasses(output);

        PostgresPlatform platform = new PostgresPlatform();
        var outputSqlFile = new File(new File(output), "create_tables_%s.sql".formatted(platform.name()));
        FileOutputStream fileOutputStream = new FileOutputStream(outputSqlFile);

        int applicationId = 1;
        ReactiveJsonPGAdapter rdbAdapter = new ReactiveJsonPGAdapter(agoClassLoader.getBoxTypes(), agoClassLoader, applicationId, new SnowflakeIdGenerator(1));
        RdbDDLGenerator rdbDDLGenerator = new PGJsonDDLGenerator(agoClassLoader, rdbAdapter, platform);
        rdbDDLGenerator.generate(fileOutputStream);
    }
}
