package org.siphonlab.ago.study;

import io.ebean.config.DatabaseConfig;
import io.ebean.platform.h2.H2Platform;
import io.ebeaninternal.dbmigration.ddlgeneration.DdlWrite;
import io.ebeaninternal.dbmigration.ddlgeneration.PlatformDdlBuilder;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.BaseTableDdl;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.PlatformDdl;
import io.ebeaninternal.dbmigration.migration.Column;
import io.ebeaninternal.dbmigration.migration.CreateTable;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.SQLException;
import java.util.List;

public class EbeanTest {
    private static final PlatformDdl h2ddl = PlatformDdlBuilder.create(new H2Platform());

    public static void main(String[] args) throws SQLException {
        String url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
        String user = "sa";
        String password = "";

        BasicDataSource ds = new org.apache.commons.dbcp2.BasicDataSource();
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setDriverClassName("org.h2.Driver");


        DatabaseConfig config = new DatabaseConfig();
        config.setDataSource(ds);

        BaseTableDdl ddlGen = new BaseTableDdl(config, h2ddl);

        DdlWrite writer = new DdlWrite();
        ddlGen.generate(writer, createTable());
        String apply = writer.apply().getBuffer();
        String applyLast = writer.applyForeignKeys().getBuffer();

        System.out.println(apply);
        System.out.println(applyLast);
    }

    private static CreateTable createTable() {
        CreateTable createTable = new CreateTable();
        createTable.setName("mytable");
        createTable.setPkName("pk_mytable");
        List<Column> columns = createTable.getColumn();
        Column col = new Column();
        col.setName("id");
        col.setType("integer");
        col.setPrimaryKey(true);

        columns.add(col);

        Column col2 = new Column();
        col2.setName("status");
        col2.setType("varchar(1)");
        col2.setNotnull(true);
        col2.setCheckConstraint("check (status in ('A','B'))");
        col2.setCheckConstraintName("ck_mytable_status");

        columns.add(col2);

        Column col3 = new Column();
        col3.setName("order_id");
        col3.setType("integer");
        col3.setNotnull(true);
        col3.setReferences("orders.id");
        col3.setForeignKeyName("fk_mytable_order_id");
//        col3.setForeignKeyIndex("ix_mytable_order_id");

        columns.add(col3);

        return createTable;
    }

}
