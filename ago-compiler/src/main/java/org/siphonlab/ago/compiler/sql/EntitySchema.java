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
package org.siphonlab.ago.compiler.sql;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.materialize.Lattice;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.*;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.siphonlab.ago.compiler.ClassDef;

import java.util.*;

public class EntitySchema implements SchemaPlus {

    private final ClassDef scopeClass;
    private final List<ClassDef> entityClasses;
    private Map<String, Table> tableMap;

    public EntitySchema(ClassDef scopeClass) {
        this.scopeClass = scopeClass;
        this.entityClasses = scopeClass.getRoot().getEntityClasses();
    }

    protected Map<String, Table> getTableMap() {
        if(this.tableMap == null){
            this.tableMap = createTableMap();
        }
        return tableMap;
    }

    private Map<String, Table> createTableMap() {
        return new AbstractMap<String, Table>() {

            @Override
            public Table get(Object key) {
                return super.get(key);
            }

            @Override
            public Set<Entry<String, Table>> entrySet() {
                return Set.of();
            }
        };
    }

    @Override
    public @Nullable SchemaPlus getParentSchema() {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public @Nullable Table getTable(String name) {
        return null;
    }

    @Override
    public Set<String> getTableNames() {
        return tableMap.keySet();
    }

    @Override
    public @Nullable RelProtoDataType getType(String name) {
        return null;
    }

    @Override
    public Set<String> getTypeNames() {
        return Set.of();
    }

    @Override
    public Collection<Function> getFunctions(String name) {
        return List.of();
    }

    @Override
    public Set<String> getFunctionNames() {
        return Set.of();
    }

    @Override
    public @Nullable SchemaPlus getSubSchema(String name) {
        return null;
    }

    @Override
    public Set<String> getSubSchemaNames() {
        return Set.of();
    }

    @Override
    public Expression getExpression(@Nullable SchemaPlus parentSchema, String name) {
        return null;
    }

    @Override
    public SchemaPlus add(String name, Schema schema) {
        return null;
    }

    @Override
    public void add(String name, Table table) {

    }

    @Override
    public void add(String name, Function function) {

    }

    @Override
    public void add(String name, RelProtoDataType type) {

    }

    @Override
    public void add(String name, Lattice lattice) {

    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Schema snapshot(SchemaVersion version) {
        return null;
    }

    @Override
    public @org.jspecify.annotations.Nullable <T> T unwrap(Class<T> clazz) {
        return null;
    }

    @Override
    public void setPath(ImmutableList<ImmutableList<String>> path) {

    }

    @Override
    public void setCacheEnabled(boolean cache) {

    }

    @Override
    public boolean isCacheEnabled() {
        return false;
    }
}
