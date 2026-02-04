package org.siphonlab.ago.crud.test;

import io.ebean.platform.h2.H2Platform;
import org.agrona.concurrent.SnowflakeIdGenerator;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.siphonlab.ago.classloader.AgoClassLoader;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.runtime.rdb.*;
import org.siphonlab.ago.test.Util;
import org.siphonlab.ago.web.RestfulService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RdbDdlTest {

    @Test
    public void test_generate_sql() throws IOException, CompilationError {
        Util.compile("restful/crud");

        String output = "output/restful/crud";
        generateDDL(output);
    }

    @Test
    public void test_run() throws IOException, CompilationError {
        runWithH2Db("output/restful/crud");
        System.in.read();
    }


    public void runWithH2Db(String output) throws IOException {
        var agoClassLoader = new AgoClassLoader(new RdbSlotsCreatorFactory(null));
        agoClassLoader.loadClasses("../ago-sdk/src/compiled/lang/");
        agoClassLoader.loadClasses(output);

        H2Platform platform = new H2Platform();
        var outputSqlFile = new File(new File(output), "create_tables_%s.sql".formatted(platform.name()));
        var outputMapFile = new File(new File(output), "db_map_" + platform.name() + ".yml");

        BasicDataSource ds = new org.apache.commons.dbcp2.BasicDataSource();
        ds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        //ds.setUrl("jdbc:h2:%s".formatted("./" + output + "/log.h2.db"));
        ds.setUsername("sa");
        ds.setDriverClassName("org.h2.Driver");

        RdbAdapter rdbAdapter = new H2Adapter(agoClassLoader.getBoxTypes(), agoClassLoader, new SnowflakeIdGenerator(1));
        rdbAdapter.loadTableMap(FileUtils.openInputStream(outputMapFile));
        rdbAdapter.setDataSource(ds);
        // for first run
        rdbAdapter.executeDDL(FileUtils.readFileToString(outputSqlFile, "utf-8"));

        RdbEngine rdbEngine = new RdbEngine(rdbAdapter);
        rdbEngine.load(agoClassLoader);
        rdbEngine.run("main#");

        RestfulService restfulService = new RestfulService();
        restfulService.installServices(agoClassLoader, rdbEngine);
        restfulService.start();
    }

    private void generateDDL(String output) throws IOException {
        var agoClassLoader = new AgoClassLoader();
        agoClassLoader.loadClasses("../ago-sdk/src/compiled/lang/");
        agoClassLoader.loadClasses(output);

        H2Platform platform = new H2Platform();
        var outputSqlFile = new File(new File(output), "create_tables_%s.sql".formatted(platform.name()));
        FileOutputStream fileOutputStream = new FileOutputStream(outputSqlFile);
        var outputMapFile = new File(new File(output), "db_map_" + platform.name() + ".yml");
        H2Adapter rdbAdapter = new H2Adapter(agoClassLoader.getBoxTypes(), agoClassLoader, new SnowflakeIdGenerator(1));
        RdbDDLGenerator rdbDDLGenerator = new RdbDDLGenerator(agoClassLoader, rdbAdapter, platform);
        rdbDDLGenerator.generate(fileOutputStream);
        rdbDDLGenerator.dumpClassMapper(new FileOutputStream(outputMapFile));
    }
}
