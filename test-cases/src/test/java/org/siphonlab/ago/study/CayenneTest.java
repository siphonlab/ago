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

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.*;
import org.apache.cayenne.map.*;
import org.apache.commons.dbcp2.BasicDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Types;
import java.util.Map;

public class CayenneTest {

    private static final int JSON_TYPE = Types.OTHER + 1;

    public static void main(String[] args) throws Exception {
        // 建立 DataSource（使用 H2 内存数据库举例）
        String url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
        String user = "sa";
        String password = "";

        BasicDataSource ds = new org.apache.commons.dbcp2.BasicDataSource();
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setDriverClassName("org.h2.Driver");

        ServerRuntime runtime = ServerRuntime.builder().dataSource(ds).build();

        // 设置 DbAdapter（H2）
        DbAdapter adapter = runtime.getDataDomain().getDefaultNode().getAdapter();

        // inject JSONB type
        Method getAdapter = AutoAdapter.class.getDeclaredMethod("getAdapter");
        getAdapter.setAccessible(true);
        JdbcAdapter jdbcAdapter = (JdbcAdapter) getAdapter.invoke(adapter);
        Field typesHandlerFld = JdbcAdapter.class.getDeclaredField("typesHandler");
        typesHandlerFld.setAccessible(true);
        TypesHandler typesHandler = (TypesHandler) typesHandlerFld.get(jdbcAdapter);

        Field typesMapFld = TypesHandler.class.getDeclaredField("typesMap");
        typesMapFld.setAccessible(true);
        Map<Integer, String[]> typesMap = (Map<Integer, String[]>) typesMapFld.get(typesHandler);
        typesMap.put(JSON_TYPE,new String[]{"JSON"});

        // 创建 DataMap 和 DbEntity
        DbEntity entity = new DbEntity("user");

        // 添加 id 字段
        DbAttribute id = new DbAttribute("id");
        id.setType(TypesMapping.getSqlTypeByName("INTEGER"));
        id.setPrimaryKey(true);
        id.setMandatory(true);
        id.setGenerated(true); // 自动增长

        // 添加 name 字段
        DbAttribute name = new DbAttribute("name");
        name.setType(TypesMapping.getSqlTypeByName("VARCHAR"));
        name.setMaxLength(100);
        name.setMandatory(false);

        DbAttribute jsonAttr = new DbAttribute("DATA", JSON_TYPE, entity);
        entity.addAttribute(jsonAttr);

        // 加入字段到实体
        entity.addAttribute(id);
        entity.addAttribute(name);

        String sql = adapter.createTable(entity);
        System.out.println(sql);

//        var map = new DataMap("sample");
//        ObjEntity objEntity = new ObjEntity("test");
//        var attr = new ObjAttribute("data", "org.apache.cayenne.value.GeoJson", objEntity);
//        DbEntity test = new DbEntity("test");
//        objEntity.setDbEntity(test);
//        map.addObjEntity(objEntity);
//        map.addDbEntity(test);
//        objEntity.addAttribute(attr);
//        runtime.getDataDomain().addDataMap(map);
//        System.out.println();

//        // 创建表
//        var node = runtime.getDataDomain().getDefaultNode();
//        node.getSchemaUpdateStrategy().updateSchema(node);

//        System.out.println("表已创建");
    }
}
