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
import org.siphonlab.ago.runtime.rdb.reactive.semischema.PGJsonSlotsCreatorFactory;
import org.siphonlab.ago.runtime.rdb.reactive.semischema.SemiSchemaEngine;
import org.siphonlab.ago.runtime.rdb.reactive.semischema.SemiSchemaPGAdapter;
import org.siphonlab.ago.runtime.rdb.semischema.SemiSchemaPGDDLGenerator;
import org.siphonlab.ago.test.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AsRuleSever {

    @Test
    public void test_generate_sql() throws IOException, CompilationError {
        Util.compile("org/siphonlab/ago/ruleserver/test/1");

        String output = "output/ruleserver/1";
        generateDDL(output);
    }

    @Test
    public void test_run() throws IOException, CompilationError {
        runWithPG("output/ruleserver/1");
    }

    public void runWithPG(String output) throws IOException {
        PGJsonSlotsCreatorFactory slotsCreatorFactory = new PGJsonSlotsCreatorFactory(null);
        var agoClassLoader = new AgoClassLoader(slotsCreatorFactory);
        agoClassLoader.loadClasses("output/rt");
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
        var rdbAdapter = new SemiSchemaPGAdapter(agoClassLoader.getBoxTypes(), agoClassLoader, applicationId, new SnowflakeIdGenerator(1));
        slotsCreatorFactory.setAdapter(rdbAdapter);
        rdbAdapter.setDataSource(ds);
        // for first run
//        rdbAdapter.executeDDL(FileUtils.readFileToString(outputSqlFile, "utf-8"));

        PersistentRdbEngine rdbEngine = new SemiSchemaEngine(rdbAdapter);
        rdbEngine.load(agoClassLoader);
        rdbEngine.run("main#");
    }

    private void generateDDL(String output) throws IOException {
        var agoClassLoader = new AgoClassLoader();
        agoClassLoader.loadClasses("output/rt");
        agoClassLoader.loadClasses(output);

        PostgresPlatform platform = new PostgresPlatform();
        var outputSqlFile = new File(new File(output), "create_tables_%s.sql".formatted(platform.name()));
        FileOutputStream fileOutputStream = new FileOutputStream(outputSqlFile);

        int applicationId = 1;
        SemiSchemaPGAdapter rdbAdapter = new SemiSchemaPGAdapter(agoClassLoader.getBoxTypes(), agoClassLoader, applicationId, new SnowflakeIdGenerator(1));
        RdbDDLGenerator rdbDDLGenerator = new SemiSchemaPGDDLGenerator(agoClassLoader, rdbAdapter, platform);
        rdbDDLGenerator.generate(fileOutputStream);
    }
}
