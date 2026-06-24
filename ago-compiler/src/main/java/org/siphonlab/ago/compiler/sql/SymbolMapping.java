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

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.Variable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SymbolMapping {

    private final Set<Variable> bindParameters = new HashSet<>();

    private Map<Table, ClassDef> classMapping = new HashMap<>();

    private Map<Column, QueryResult.ColumnDesc> fieldMapping = new HashMap<>();

    private Map<Expression, Variable> nullableConditions = new HashMap<>();

    private Map<Select, QueryResult> selectMapping = new HashMap<>();

    private Map<Select, OrderByDesc> orderByClauses = new HashMap<>();

    public void addClassMapping(Table table, ClassDef classDef) {
        classMapping.put(table, classDef);
    }

    public void addFiledMapping(Column tableColumn, QueryResult.FieldColumnDesc fieldColumnDef){
        fieldMapping.put(tableColumn, fieldColumnDef);
    }

    public void addIdMapping(Column tableColumn, QueryResult.IdColumnDesc idColumnDef){
        fieldMapping.put(tableColumn, idColumnDef);
    }

    public Map<Table, ClassDef> getClassMapping() {
        return classMapping;
    }

    public Map<Column, QueryResult.ColumnDesc> getFieldMapping() {
        return fieldMapping;
    }

    public Set<Variable> getBindParameters() {
        return bindParameters;
    }

    public Map<Expression, Variable> getNullableConditions() {
        return nullableConditions;
    }

    public void addBindParameter(Variable variable) {
        bindParameters.add(variable);
    }

    public void addNullableCondition(Expression topCondition, Variable variable) {
        nullableConditions.put(topCondition, variable);
    }

    public QueryResult.ColumnDesc getMappedField(Column tableColumn) {
        return fieldMapping.get(tableColumn);
    }

    public Variable getKnownNullableCondition(Expression expression) {
        return nullableConditions.get(expression);
    }

    public ClassDef getMappedTable(Table table) {
        return classMapping.get(table);
    }

    public void addSelectMapping(Select select, QueryResult queryResult) {
        this.selectMapping.put(select, queryResult);
    }

    public QueryResult getMappedSelect(Select select){
        return selectMapping.get(select);
    }

    public void addOrderBy(PlainSelect plainSelect, OrderByDesc orderByDesc) {
        this.orderByClauses.put(plainSelect, orderByDesc);
        orderByDesc.setId(this.orderByClauses.size());
    }

    public Map<Select, OrderByDesc> getOrderByClauses() {
        return orderByClauses;
    }
}
