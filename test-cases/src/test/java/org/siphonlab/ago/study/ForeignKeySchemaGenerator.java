package org.siphonlab.ago.study;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.*;

import java.sql.Types;

public class ForeignKeySchemaGenerator {

    public static void main(String[] args) {
        // 1. 启动 Cayenne 运行时 (使用 H2 内存数据库)
        ServerRuntime runtime = ServerRuntime.builder()
                .jdbcDriver("org.h2.Driver")
                .url("jdbc:h2:mem:testdb-fk;DB_CLOSE_DELAY=-1")
                .user("sa")
                .password("")
                .build();

        System.out.println("--- Cayenne 运行时已启动 ---");

        // 2. 以编程方式创建 DataMap 和实体
        DataMap dataMap = new DataMap("my_relational_map");

        // 2a. 创建 ARTIST 表 (目标表)
        DbEntity artistEntity = createArtistEntity(dataMap);
        System.out.println("--- DbEntity 'ARTIST' 定义完成 ---");

        // 2b. 创建 PAINTING 表 (源表)
        DbEntity paintingEntity = createPaintingEntity(dataMap);
        System.out.println("--- DbEntity 'PAINTING' 定义完成 ---");

        // 3. 创建外键关系 (DbRelationship)
        //    定义一个从 PAINTING 到 ARTIST 的关系
        System.out.println("--- 开始定义从 PAINTING 到 ARTIST 的外键关系 ---");
        // 创建关系，名称可以自定义，通常用小写的目标实体名
        DbRelationship relToArtist = new DbRelationship("toArtist");
        relToArtist.setSourceEntity(paintingEntity);  // 关系所在的表 (源)
        relToArtist.setTargetEntityName(artistEntity);    // 关系指向的表 (目标)
        relToArtist.setToMany(false); // 这是一个 to-one 关系 (一个 Painting 属于一个 Artist)

        // 定义连接的列：PAINTING.ARTIST_ID -> ARTIST.ID
        DbJoin join = new DbJoin(relToArtist);
        join.setSourceName("ARTIST_ID"); // 源表中的外键列
        join.setTargetName("ID");        // 目标表中的主键列
        relToArtist.addJoin(join);

        // 将关系添加到源实体中
        paintingEntity.addRelationship(relToArtist);
        System.out.println("--- 外键关系定义完成 ---");

        // 4. 将 DataMap 添加到运行时
        runtime.getDataDomain().addDataMap(dataMap);
        runtime.getDataDomain().getEntityResolver().addDataMap(dataMap);

        // 5. 使用 DbGenerator 生成 Schema
        System.out.println("--- 开始生成数据库 Schema ---");
        DataNode dataNode = runtime.getDataDomain().getDefaultNode();
        DbAdapter adapter = dataNode.getAdapter();

        // 创建 DbGenerator，这次它需要处理外键
        DbGenerator generator = new DbGenerator(adapter, dataMap, runtime.getDataDomain().getDefaultNode().getJdbcEventLogger());
        generator.setShouldCreateTables(true);        // 允许创建表
        generator.setShouldCreatePKSupport(true);   // 允许创建主键
        generator.setShouldCreateFKConstraints(true); // **允许创建外键约束**

        try {
            // 运行生成器
            generator.runGenerator(runtime.getDataSource());
            System.out.println("--- Schema 生成成功！ARTIST 和 PAINTING 表及外键已创建。---");
        } catch (Exception e) {
            System.err.println("Schema 生成失败！");
            e.printStackTrace();
        } finally {
            // 6. 关闭运行时
            System.out.println("--- 关闭 Cayenne 运行时 ---");
            runtime.shutdown();
        }
    }

    /**
     * 辅助方法，用于创建 ARTIST DbEntity
     */
    private static DbEntity createArtistEntity(DataMap dataMap) {
        DbEntity artistEntity = new DbEntity("ARTIST");

        DbAttribute idAttr = new DbAttribute("ID", Types.INTEGER, artistEntity);
        idAttr.setPrimaryKey(true);
        idAttr.setGenerated(true);
        idAttr.setMandatory(true);

        DbAttribute nameAttr = new DbAttribute("NAME", Types.VARCHAR, artistEntity);
        nameAttr.setMaxLength(255);
        nameAttr.setMandatory(true);

        artistEntity.addAttribute(idAttr);
        artistEntity.addAttribute(nameAttr);
        dataMap.addDbEntity(artistEntity);
        return artistEntity;
    }

    /**
     * 辅助方法，用于创建 PAINTING DbEntity
     */
    private static DbEntity createPaintingEntity(DataMap dataMap) {
        DbEntity paintingEntity = new DbEntity("PAINTING");

        DbAttribute idAttr = new DbAttribute("ID", Types.INTEGER, paintingEntity);
        idAttr.setPrimaryKey(true);
        idAttr.setGenerated(true);
        idAttr.setMandatory(true);

        DbAttribute nameAttr = new DbAttribute("NAME", Types.VARCHAR, paintingEntity);
        nameAttr.setMaxLength(255);
        nameAttr.setMandatory(true);

        // 这是外键列，它的类型必须与它引用的主键列(ARTIST.ID)兼容
        DbAttribute artistIdAttr = new DbAttribute("ARTIST_ID", Types.INTEGER, paintingEntity);
        artistIdAttr.setMandatory(true); // 通常外键是必须的

        paintingEntity.addAttribute(idAttr);
        paintingEntity.addAttribute(nameAttr);
        paintingEntity.addAttribute(artistIdAttr);
        dataMap.addDbEntity(paintingEntity);
        return paintingEntity;
    }
}