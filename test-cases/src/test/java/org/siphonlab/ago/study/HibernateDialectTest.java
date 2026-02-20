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
package org.siphonlab.ago.study;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.*;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.*;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

public class HibernateDialectTest {
    public static void main(String[] args) {
        // 使用 hibernate 内部的 Column Table 等模型生成 DDL
        // 能成功，但是有各种封装，静态函数多，难以覆盖，达不到完美。
        // 例如 Column 会得到一个 Value，这个 Value 很难自己搞出来
        PostgreSQLDialect dialect = new PostgreSQLDialect();
        Exporter<Table> tableExporter = dialect.getTableExporter();

        Configuration cfg = new Configuration();

        // 设置基本的数据库连接属性
        cfg.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        cfg.setProperty("hibernate.connection.url", "jdbc:h2:mem:programmaticdb;DB_CLOSE_DELAY=-1");
        cfg.setProperty("hibernate.connection.username", "sa");
        cfg.setProperty("hibernate.connection.password", "");
        cfg.setProperty("hibernate.hbm2ddl.auto", "update"); // 自动建表
        cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        cfg.setProperty("hibernate.default_entity_mode", "dynamic-map");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build();
        MetadataSources metadataSources = new MetadataSources(registry);
        Metadata metadata = metadataSources.buildMetadata();

        Table table = new Table("nobody", "book");
        Column id = new Column("id");
        id.setValue(new ValueKiller(table, id, metadata, StandardBasicTypes.LONG));
        PrimaryKey primaryKey = new PrimaryKey(table);
        primaryKey.addColumn(id);
        table.setPrimaryKey(primaryKey);
        table.addColumn(id);

        Column name = new Column("name");
        name.setValue(new ValueKiller(table, id, metadata, StandardBasicTypes.STRING));
        name.setLength(200);
        table.addColumn(name);

        SqlStringGenerationContext sqlStringGenerationContext = SqlStringGenerationContextImpl.forTests(metadata.getDatabase().getJdbcEnvironment());
        var arr = tableExporter.getSqlCreateStrings(table, metadata, sqlStringGenerationContext);

        for (String s : arr) {
            System.out.println(s);
        }

    }

    private static class ValueKiller implements Value {

        private final Table table;
        private final Column column;
        private final Metadata metadata;
        private final BasicTypeReference<?> basicType;
        private String referenceTable;

        public ValueKiller(Table table, Column column, Metadata metadata,
                           BasicTypeReference<?> basicType) {
            this.table = table;
            this.column = column;
            this.metadata = metadata;
            this.basicType = basicType;
        }

        public ValueKiller(Table table, Column column, Metadata metadata,
                                BasicTypeReference<?> basicType, String referenceTable) {
            this.table = table;
            this.column = column;
            this.metadata = metadata;
            this.basicType = basicType;
            this.referenceTable = referenceTable;
        }

        @Override
        public int getColumnSpan() {
            return 1;
        }

        @Override
        public List<Selectable> getSelectables() {
            return List.of(column);
        }

        @Override
        public List<Column> getColumns() {
            return List.of(column);
        }

        @Override
        public Type getType() throws MappingException {
//            final BasicType<?> type = basicTypeRegistry.resolve(StandardBasicTypes.LONG);
//			final BasicTypeRegistry basicTypeRegistry = database.getTypeConfiguration().getBasicTypeRegistry();
//			// todo : not sure the best solution here.  do we add the columns if missing?  other?
//			final DdlTypeRegistry ddlTypeRegistry = database.getTypeConfiguration().getDdlTypeRegistry();
//			final Column segmentColumn = ExportableColumnHelper.column(
//					database,
//					table,
//					segmentColumnName,
//					basicTypeRegistry.resolve( StandardBasicTypes.STRING ),
//					ddlTypeRegistry.getTypeName( Types.VARCHAR, Size.length( segmentValueLength ) )     here can pass length
//			);
            TypeConfiguration typeConfiguration = metadata.getDatabase().getTypeConfiguration();
            BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
            DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();

            if(referenceTable != null){
                return new ManyToOneType(referenceTable,typeConfiguration);
            }

            return basicTypeRegistry.resolve(this.basicType);
        }

        @Override
        public MetadataBuildingContext getBuildingContext() {
            return metadata.getDatabase().getTypeConfiguration().getMetadataBuildingContext();
        }

        @Override
        public FetchMode getFetchMode() {
            return null;
        }

        @Override
        public Table getTable() {
            return table;
        }

        @Override
        public boolean hasFormula() {
            return false;
        }

        @Override
        public boolean isAlternateUniqueKey() {
            return false;
        }

        @Override
        public boolean isNullable() {
            return column.isNullable();
        }

        @Override
        public void createForeignKey() {

        }

        @Override
        public void createUniqueKey(MetadataBuildingContext context) {

        }

        @Override
        public boolean isSimpleValue() {
            return true;
        }

        @Override
        public boolean isValid(MappingContext mappingContext) throws MappingException {
            return false;
        }

        @Override
        public void setTypeUsingReflection(String className, String propertyName) throws MappingException {

        }

        @Override
        public Object accept(ValueVisitor visitor) {
            return null;
        }

        @Override
        public boolean isSame(Value other) {
            return this == other;
        }

        @Override
        public boolean[] getColumnInsertability() {
            return ArrayHelper.TRUE;
        }

        @Override
        public boolean hasAnyInsertableColumns() {
            return true;
        }

        @Override
        public boolean[] getColumnUpdateability() {
            return ArrayHelper.TRUE;
        }

        @Override
        public boolean hasAnyUpdatableColumns() {
            return true;
        }

        @Override
        public ServiceRegistry getServiceRegistry() {
            return metadata.getDatabase().getServiceRegistry();
        }

        @Override
        public Value copy() {
            return this;
        }

        @Override
        public boolean isColumnInsertable(int index) {
            return true;
        }

        @Override
        public boolean isColumnUpdateable(int index) {
            return true;
        }
    }
}
