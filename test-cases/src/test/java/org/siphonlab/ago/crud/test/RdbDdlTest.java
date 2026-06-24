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
package org.siphonlab.ago.crud.test;

import io.ebean.platform.h2.H2Platform;
import io.ebean.platform.postgres.PostgresPlatform;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.siphonlab.ago.BoxTypes;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.runtime.db.DbSlotsCreatorFactory;
import org.siphonlab.ago.runtime.db.SnowflakeIdGenerator;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.runtime.rdb.h2.H2Adapter;
import org.siphonlab.ago.runtime.rdb.json.JsonPGAdapter;
import org.siphonlab.ago.runtime.rdb.json.PGJsonDDLGenerator;
import org.siphonlab.ago.runtime.rdb.pg.PGEntityAdapter;
import org.siphonlab.ago.test.Util;
import org.siphonlab.ago.web.RestfulService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RdbDdlTest {

    @Test
    public void test_generate_sql() throws IOException, CompilationError {
        Util.compile("restful/crud.ago");

        String output = "output/restful/crud.ago";
        generateDDL(output);
    }

    @Test
    public void test_run() throws IOException, CompilationError {
        runWithH2Db("output/restful/crud");
        System.in.read();
    }


    public void runWithH2Db(String output) throws IOException {
        var agoClassLoader = new AgoClassLoader(new DbSlotsCreatorFactory<Long>());
        agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream("../ago-sdk/lang.agopkg")));
        agoClassLoader.loadClasses(output);

        H2Platform platform = new H2Platform();
        var outputSqlFile = new File(new File(output), "create_tables_%s.sql".formatted(platform.name()));
        var outputMapFile = new File(new File(output), "db_map_" + platform.name() + ".yml");

        BasicDataSource ds = new org.apache.commons.dbcp2.BasicDataSource();
        ds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        //ds.setUrl("jdbc:h2:%s".formatted("./" + output + "/log.h2.db"));
        ds.setUsername("sa");
        ds.setDriverClassName("org.h2.Driver");

        var rdbAdapter = new H2Adapter<Long>(agoClassLoader, TypeCode.LONG, new SnowflakeIdGenerator(1), agoClassLoader.getBoxTypes(), ds);
        rdbAdapter.loadTableMap(FileUtils.openInputStream(outputMapFile));
        // for first run
        rdbAdapter.executeDDL(FileUtils.readFileToString(outputSqlFile, "utf-8"));

//        DbEngine dbEngine = new (rdbAdapter);
//        dbEngine.load(agoClassLoader);
//        dbEngine.run("main#");
//
//        RestfulService restfulService = new RestfulService();
//        restfulService.installServices(agoClassLoader, dbEngine);
//        restfulService.start();
    }

    public static void generateDDL(String output) throws IOException {
        var agoClassLoader = new AgoClassLoader();
        agoClassLoader.loadClasses(new ZipInputStream(new FileInputStream("../ago-sdk/lang.agopkg")));
        agoClassLoader.loadClasses(output);

        var platform = new PostgresPlatform();
        var outputSqlFile = new File(new File(output), "create_tables_%s.sql".formatted(platform.name()));
        FileOutputStream fileOutputStream = new FileOutputStream(outputSqlFile);
        var outputMapFile = new File(new File(output), "db_map_" + platform.name() + ".yml");

        BasicDataSource ds = new org.apache.commons.dbcp2.BasicDataSource();
        ds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        //ds.setUrl("jdbc:h2:%s".formatted("./" + output + "/log.h2.db"));
        ds.setUsername("sa");
        ds.setDriverClassName("org.h2.Driver");

        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1);
        var workflowAdapter = new JsonPGAdapter<>(agoClassLoader, TypeCode.LONG, idGenerator, agoClassLoader.getBoxTypes(), ds, 1);

        var entityAdapter = new PGEntityAdapter<>(agoClassLoader, TypeCode.LONG, idGenerator, agoClassLoader.getBoxTypes(), ds);

        new PGJsonDDLGenerator<Long>(agoClassLoader, workflowAdapter, platform).generate(fileOutputStream);      // workflow tables

        var entityRdbDDLGenerator = new EntityRdbDDLGenerator<Long>(agoClassLoader, entityAdapter, platform);
        entityRdbDDLGenerator.generate(fileOutputStream);

        entityRdbDDLGenerator.dumpClassMapper(new FileOutputStream(outputMapFile));
    }
}
