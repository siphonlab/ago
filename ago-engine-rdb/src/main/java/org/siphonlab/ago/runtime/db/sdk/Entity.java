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
package org.siphonlab.ago.runtime.db.sdk;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.ArrayUtils;
import org.siphonlab.ago.*;
import org.siphonlab.ago.native_.NativeFrame;
import org.siphonlab.ago.native_.NativeInstance;
import org.siphonlab.ago.runtime.StringArrayInstance;
import org.siphonlab.ago.runtime.db.DbSlots;
import org.siphonlab.ago.runtime.db.ObjectRef;
import org.siphonlab.ago.runtime.rdb.DbEngine;
import org.siphonlab.ago.runtime.rdb.RdbAdapter;
import org.siphonlab.ago.runtime.rdb.ResultSetToEntityMapper;
import org.siphonlab.ago.runtime.rdb.ResultSetToQueryResultMapper;

import java.util.Map;

import static net.sf.jsqlparser.parser.CCJSqlParserUtil.parseCondExpression;
import static org.siphonlab.ago.runtime.db.EntityRunSpace.retrieveEntityAdapter;

public class Entity {

    public static void getEntityById(NativeFrame callFrame, long id) {
        AgoClass entityClass = callFrame.getAgoClass().getResultClass();
        var adapter = retrieveEntityAdapter(callFrame.getRunSpace());
        callFrame.finishObject(adapter.getById(ObjectRef.create(entityClass.getFullname(), id), callFrame.getRunSpace()));
    }

    public static void getId(NativeFrame callFrame) {
        var instance = callFrame.getParentScope();
        DbSlots<?> slots = (DbSlots<?>) instance.getSlots();
        Object id = slots.getObjectRef().id();
        if(id instanceof Long l) {
            callFrame.finishLong(l);
        } else if(id instanceof String s){
            callFrame.finishString(s);
        } else if(id instanceof Integer i){
            callFrame.finishInt(i);
        } else {
            callFrame.raiseException(callFrame, "lang.ClassCastException", "can't cast to '%s'".formatted(callFrame.getAgoClass().getResultTypeCode()));
        }
    }

    public static void fetchAll(NativeFrame callFrame) {
        DbEngine<?> dbEngine = (DbEngine<?>) callFrame.getAgoEngine();

        AgoClass queryResultClass = callFrame.getAgoClass().getResultClass();
        var entityClass = queryResultClass.getConcreteTypeInfoAsGenericArguments().getArguments()[0];

        //TODO parentScope
        var queryResultInstance = (NativeInstance) dbEngine.createNativeInstance(null, queryResultClass, callFrame.getRunSpace());
        RdbAdapter<?> dbAdapter = (RdbAdapter<?>) dbEngine.getDbAdapter();
        System.out.println(1);
//        queryResultInstance.setNativePayload((ResultSetMapper) ((DbAdapter<?>) dbAdapter).(entityClass, callFrame));
//
//        return queryResultInstance;
    }

    public static void executeQuery(NativeFrame frame, String sql, Object arguments){
        var adapter = retrieveEntityAdapter(frame.getRunSpace());
        AgoClass queryResultIteratorClass = frame.getAgoClass().getResultClass();
        var resultClass = queryResultIteratorClass.getConcreteTypeInfoAsGenericArguments().getArguments()[0];
        var queryResultIteratorInstance = (NativeInstance) frame.getAgoEngine().createNativeInstance(null, queryResultIteratorClass, frame.getRunSpace());
        Map<String,Object> argMap;
        if(arguments != null){
            argMap = new InstanceAsMap((Instance<?>) arguments);
        } else {
            argMap = null;
        }
        ResultSetToQueryResultMapper<Object> mapper = adapter.executeQuery(sql, argMap, resultClass, frame.getRunSpace());
        mapper.setAgoEngine(frame.getAgoEngine());
        queryResultIteratorInstance.setNativePayload(mapper);
        frame.finishObject(queryResultIteratorInstance);
    }

    public static void mapColumn(NativeFrame frame, int slotIndex){
        var adapter = retrieveEntityAdapter(frame.getRunSpace());
        var entityClass = frame.getAgoClass().getConcreteTypeInfoAsGenericArguments().getArguments()[0];
        var columnDesc = adapter.getColumnDesc(entityClass.getFullname(), slotIndex);
        if(columnDesc.getAdditional() == null) {
            frame.finishString(columnDesc.getName());
        } else {
            frame.finishString(columnDesc.getName() + "," + columnDesc.getAdditional().getName());
        }
    }

    public static void mapTable(NativeFrame frame){
        var adapter = retrieveEntityAdapter(frame.getRunSpace());
        var entityClass = frame.getAgoClass().getConcreteTypeInfoAsGenericArguments().getArguments()[0];
        frame.finishString(adapter.tableName(entityClass));
    }

    public static void tableSortScope(NativeFrame frame, String alias, Instance<?> sortArray){
        System.out.println(1);
    }

    public static void tableSortScope(NativeFrame frame, String alias, Instance<?> additionColumns, Instance<?> sortInstance){
        var adapter = retrieveEntityAdapter(frame.getRunSpace());
        var entityClass = frame.getAgoClass().getConcreteTypeInfoAsGenericArguments().getArguments()[0];
        String[] additionColumnNames = ((StringArrayInstance) additionColumns).value;
        String sortCol = sortInstance.getStringField("column");
        String sortDirection = sortInstance.getStringField("direction");
        var tableName = adapter.tableName(entityClass);
        try {
            Column column = (Column) CCJSqlParserUtil.parseCondExpression(sortCol);
            if(column.getTableName() != null){
                if(alias.equals(column.getTableName()) || alias.equals(column.getUnquotedTableName()) || alias.equals(tableName)) {
                    var field = entityClass.findField(column.getColumnName());
                    if(field == null) {
                        field = entityClass.findField(column.getUnquotedColumnName());
                    }
                    if (field != null) {
                        var mapped = adapter.getColumnDesc(entityClass.getFullname(), field.getSlotIndex());
                        frame.finishUnion(column.getTableName() + column.getTableDelimiter() + mapped.getName() + " " + sortDirection);      // ORDER BY ...
                        return;
                    } else if (additionColumnNames != null && ArrayUtils.containsAny(additionColumnNames, column.getColumnName(), column.getUnquotedColumnName())) {
                        frame.finishUnion(sortCol + " " + sortDirection);
                        return;
                    }
                }
            } else {
                var field = entityClass.findField(column.getColumnName());
                if(field == null) {
                    field = entityClass.findField(column.getUnquotedColumnName());
                }
                if (field != null) {
                    var mapped = adapter.getColumnDesc(entityClass.getFullname(), field.getSlotIndex());
                    frame.finishUnion(mapped.getName() + " " + sortDirection);      // ORDER BY ...
                    return;
                } else if (additionColumnNames != null && ArrayUtils.containsAny(additionColumnNames, column.getColumnName(), column.getUnquotedColumnName())) {
                    frame.finishUnion(sortCol + " " + sortDirection);
                    return;
                }
            }
            frame.finishUnion(null);
        } catch (JSQLParserException e) {
            throw new RuntimeException(e);
        }
    }

    public static void querySortScope(NativeFrame frame, String alias, Instance<?> otherColumns, Instance<?> sortArray){
        System.out.println(1);
    }
}
