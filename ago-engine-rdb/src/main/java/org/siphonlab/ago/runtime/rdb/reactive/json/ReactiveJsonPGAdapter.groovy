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
package org.siphonlab.ago.runtime.rdb.reactive.json


import groovy.transform.CompileStatic
import org.agrona.concurrent.IdGenerator
import org.siphonlab.ago.*
import org.siphonlab.ago.runtime.rdb.RdbSlots
import org.siphonlab.ago.runtime.rdb.RdbType
import org.siphonlab.ago.runtime.rdb.ObjectRef
import org.siphonlab.ago.runtime.rdb.reactive.SlotsAdapter
import org.siphonlab.ago.runtime.rdb.json.JsonPGAdapter

@CompileStatic
public class ReactiveJsonPGAdapter extends JsonPGAdapter {

    SlotsAdapter slotsAdapter

    public ReactiveJsonPGAdapter(BoxTypes boxTypes, ClassManager classManager, int applicationId, IdGenerator idGenerator) {
        super(boxTypes, classManager, applicationId, idGenerator);
        this.applicationId = applicationId;
        this.slotsAdapter = new ReactivePGJsonSlotsAdapter(this)
    }

    @Override
    public RdbType idType() {
        return mapType(TypeCode.LONG, null);
    }

    @Override
    protected void insert(Instance<?> instance, RdbSlots rdbSlots, AgoClass agoClass) {
        ReactiveJsonRefSlots jsonRefSlots = rdbSlots as ReactiveJsonRefSlots;
        if (jsonRefSlots.isSaved())
            return

        // it's new created instance
        if (instance instanceof AgoFrame) {
            saveAgoFrame((AgoFrame) instance)
        } else if (instance instanceof AgoFunction) {
            saveAgoFunction((AgoFunction) instance)
        } else if (instance instanceof AgoClass) {
            saveAgoClass((AgoClass) instance)
        } else {
            saveAgoInstance(instance)
        }
        jsonRefSlots.setSaved(true);
    }

    Slots restoreSlots(ObjectRef objectRef, Map<String, Object> dbRow, AgoClass agoClass) {
        return new ReactiveJsonRefSlots(objectRef, this.getSlotsAdapter(), agoClass.getSlotDefs());       // don't restore value, only a ref
    }

    void move(ObjectRef dbRef, String destField, String srcField) {
        this.sql.execute("UPDATE ${getTableName(dbRef.className())} SET slots['${destField}'] = slots['${srcField}'] WHERE id = ?", [dbRef.id() as Object])
    }

    void move(ObjectRef destRef, String destField, ObjectRef srcRef, String srcField) {
        this.sql.execute("UPDATE ${getTableName(destRef.className())} a SET slots['${destField}'] = b.slots['${srcField}'] FROM ${getTableName(srcRef.className())} b WHERE b.id=? AND a.id = ?", [srcRef.id() as Object, destRef.id()])
    }

    void copyAssign(ObjectRef destRef, ObjectRef srcRef, List<String> fields) {
        String sql = """
            UPDATE ${getTableName(destRef.className())} a 
                SET ${fields.collect {"a.slots['${it}'] = b.slots['${it}']"}.join(",")} 
            FROM ${getTableName(srcRef.className())} b WHERE a.id = ? AND b.id=? 
        """
        this.sql.execute(sql, [destRef.id() as Object, srcRef.id()])
    }

    void binaryOp(String op, RdbType type, ObjectRef dbRef, String destField, String srcField, Object value) {
        String sql = "UPDATE ${getTableName(dbRef.className())} SET slots['$destField'] = to_jsonb((slots['$srcField'] :: ${type.typeName}) $op ?) WHERE id = ?"
        this.sql.execute(sql, [value, dbRef.id()])
    }

    void binaryOp(String op, RdbType type, ObjectRef dbRef, String destField, String a, String b) {
        String sql = "UPDATE ${getTableName(dbRef.className())} SET slots['$destField'] = to_jsonb((slots['$a'] :: ${type.typeName}) $op (slots['$b'] :: ${type.typeName})) WHERE id = ?"
        this.sql.execute(sql, [dbRef.id() as Object])
    }

    void selfOp(String op, RdbType type, ObjectRef dbRef, String destField, String incField) {
        String sql = "UPDATE ${getTableName(dbRef.className())} SET slots['$destField'] = to_jsonb((slots['$destField'] :: ${type.typeName}) $op (slots['$incField'] :: ${type.typeName})) WHERE id = ?"
        this.sql.execute(sql, [dbRef.id() as Object])
    }

    void selfOp(String op, RdbType type, ObjectRef dbRef, String destField, Object value) {
        String sql = "UPDATE ${getTableName(dbRef.className())} SET slots['$destField'] = to_jsonb((slots['$destField'] :: ${type.typeName}) $op (?::${type.typeName}))) WHERE id = ?"
        this.sql.execute(sql, [value, dbRef.id()])
    }

}
