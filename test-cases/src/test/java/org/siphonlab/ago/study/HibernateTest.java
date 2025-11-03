package org.siphonlab.ago.study;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.*;
import org.hibernate.boot.jaxb.mapping.spi.*;
import org.hibernate.boot.jaxb.mapping.spi.ObjectFactory;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.type.SqlTypes;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

public class HibernateTest {

    public static void main(String[] args) {
        // 1. 使用 Configuration 开始，这是传统的入口，非常适合这种编程方式的配置
        Configuration cfg = new Configuration();

        // 设置基本的数据库连接属性
        cfg.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        cfg.setProperty("hibernate.connection.url", "jdbc:h2:mem:programmaticdb;DB_CLOSE_DELAY=-1");
        cfg.setProperty("hibernate.connection.username", "sa");
        cfg.setProperty("hibernate.connection.password", "");
        cfg.setProperty("hibernate.hbm2ddl.auto", "update"); // 自动建表
        cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        cfg.setProperty("hibernate.default_entity_mode","dynamic-map");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build();



        MetadataSources metadataSources = new MetadataSources(registry);
        //Metadata metadata = metadataSources.addAnnotatedClass(Bok.class).buildMetadata();
//
        addWithXmlString(metadataSources);
//        addWithHbmHibernateMapping(metadataSources);
//        addWithJaxbEntityDynamic(metadataSources);
//        addWithXmlStringComplex(metadataSources);     // failed, fk need class

        Metadata metadata = metadataSources.buildMetadata();

        new SchemaExport().setFormat(true).setDelimiter(";").createOnly(EnumSet.of(TargetType.DATABASE), metadata);

//        try (SessionFactory factory = metadata.buildSessionFactory(); Session session = factory.openSession()) {
//
//            Transaction tx = session.beginTransaction();
//            var book = new HashMap<String, Object>();
//            book.put("name", "first book");
//            session.persist("Book", book);
//
//
//            tx.commit();
//        }
//
    }

    private static void addWithHbmHibernateMapping(MetadataSources metadataSources) {
        JaxbHbmHibernateMapping hibernateMapping = new JaxbHbmHibernateMapping();
        hibernateMapping.setDefaultAccess("property");
        hibernateMapping.setDefaultCascade("none");
        hibernateMapping.setDefaultLazy(true);
        hibernateMapping.setAutoImport(true);

        JaxbHbmRootEntityType rootEntityType = new JaxbHbmRootEntityType();
//        rootEntityType.setName("book");
        rootEntityType.setEntityName("Book");
        rootEntityType.setDynamicInsert(true);
        rootEntityType.setDynamicUpdate(true);

        JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
        id.setName("id");
        JaxbHbmTypeSpecificationType type = new JaxbHbmTypeSpecificationType();
        type.setName("long");
        id.setType(type);
        id.setColumnAttribute("id");
        JaxbHbmGeneratorSpecificationType generator = new JaxbHbmGeneratorSpecificationType();
        generator.setClazz("native");
        id.setGenerator(generator);
        rootEntityType.setId(id);

        JaxbHbmBasicAttributeType name = new JaxbHbmBasicAttributeType();
        JaxbHbmTypeSpecificationType type1 = new JaxbHbmTypeSpecificationType();
        type1.setName("string");
        name.setName("name");
        name.setColumnAttribute("name");
        name.setType(type1);
        rootEntityType.getAttributes().add(name);

        hibernateMapping.getClazz().add(rootEntityType);


        metadataSources.addXmlBinding(new Binding<>(hibernateMapping, new Origin(SourceType.INPUT_STREAM, "test")));

    }


    private static void addWithJaxbEntity(MetadataSources metadataSources) {
        // 需要对应实际类
        ObjectFactory objectFactory = new ObjectFactory();
        JaxbEntityMappingsImpl jaxbEntityMappings = objectFactory.createJaxbEntityMappingsImpl();

        JaxbEntityImpl entity = new JaxbEntityImpl();
        entity.setName("com.example.Person");       // real class required
        JaxbTableImpl table = new JaxbTableImpl();
        table.setName("person");
        entity.setTable(table);

        JaxbAttributesContainerImpl jaxbAttributesContainer = new JaxbAttributesContainerImpl();
        entity.setAttributes(jaxbAttributesContainer);

        JaxbIdImpl id = new JaxbIdImpl();
        id.setName("id");
        id.setJdbcTypeCode(SqlTypes.BIGINT);
        JaxbSequenceGeneratorImpl sequenceGenerator = new JaxbSequenceGeneratorImpl();
        sequenceGenerator.setName("native");
        id.setSequenceGenerator(sequenceGenerator);
        JaxbColumnImpl column = new JaxbColumnImpl();
        column.setName("id");
        id.setColumn(column);
        entity.getAttributes().getIdAttributes().add(id);

        JaxbBasicImpl name = new JaxbBasicImpl();

        name.setName("name");
        name.setJdbcTypeCode(SqlTypes.VARCHAR);
        JaxbColumnImpl col = new JaxbColumnImpl();
        col.setName("name");
        col.setLength(200);
        col.setNullable(false);
        name.setColumn(col);
        entity.getAttributes().getBasicAttributes().add(name);

        jaxbEntityMappings.getEntities().add(entity);

        metadataSources.addXmlBinding(new Binding<>(jaxbEntityMappings, new Origin(SourceType.DOM, "test")));

    }

    private static void addWithXmlString(MetadataSources metadataSources) {
        // 对应 JaxbHbmHibernateMapping
        String s = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <!DOCTYPE hibernate-mapping PUBLIC
                                "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                                "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
                        <hibernate-mapping>
                            <class name="com.example.YourEntity" table="your_table">
                                <!-- 主键 -->
                                <id name="id" column="id" type="long">
                                    <generator class="native"/>
                                </id>
                                <!-- 其他字段 -->
                                <property name="name" column="name" type="string" length="100"/>
                                <property name="age" column="age" type="integer"/>
                            </class>
                        </hibernate-mapping>
                """;
        metadataSources.addInputStream(new ByteArrayInputStream(s.trim().getBytes(StandardCharsets.UTF_8)));
    }

    private static void addWithXmlStringComplex(MetadataSources metadataSources) {
        // 对应 JaxbHbmHibernateMapping
        String s = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <!DOCTYPE hibernate-mapping PUBLIC
                                "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                                "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
                        <hibernate-mapping>
                            <!-- Customer 映射 -->
                            <class name="com.example.Customer" table="CUSTOMERS">
                                <id name="id" column="CUSTOMER_ID">
                                    <generator class="native"/>
                                </id>
                                <property name="name" column="CUSTOMER_NAME"/>
                                <set name="orders" inverse="true">
                                    <key column="CUSTOMER_ID"/>
                                    <one-to-many class="com.example.Order"/>
                                </set>
                            </class>
                        
                            <!-- Order 映射 -->
                            <class name="com.example.Order" table="ORDERS">
                                <id name="id" column="ORDER_ID">
                                    <generator class="native"/>
                                </id>
                                <property name="orderDate" column="ORDER_DATE"/>
                                <many-to-one name="customer" 
                                             class="com.example.Customer" 
                                             column="CUSTOMER_ID" 
                                             not-null="true"/>
                            </class>
                        </hibernate-mapping>
                """;
        metadataSources.addInputStream(new ByteArrayInputStream(s.trim().getBytes(StandardCharsets.UTF_8)));
    }

}
