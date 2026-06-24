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

import java.util.*;

/**
 * for WITH CTE, i.e. `WITH xxx AS a, xx AS b`, the names are `a -> xxx`, `b -> xxx`, and they are in same QueryScope, so that b can reference a, with FROM/JOIN
 * for FROM,
 *      - multiple FROM, the names are each clause
 *          behold, for multiple FROM, the scope were expanded parallel, when resolve Sub-Query of multi FROM/JOIN, the scope WASN'T add name-QueryResult pair one by one, instead, each query are works without Scope, and at last append to the scope together.
 *          therefore, `FROM tbl a, (SELECT xx FROM a) b` is illegal.
 *          however, for LATERAL JOIN, it needs Sub-Queries before append to scope then works on the updated scope.
 *      - one FROM, only one element in names
 *
 * the rule is, FROM/JOIN makes QueryScope, SELECT/CTE/TableName create QueryResult
 * and for Sub-Query, its `parent` is the outer scope
 *
 * and even for sub Scope, it can only resolve column in its scope, that means, FROM/JOIN lock current QueryResults, parent give a possible scope to find QueryResults, but only scan columns in current query scope
 *
 * Group By will create a speicial kind of QueryScope, in this scope, the columns are only the group key and accumalting function
 */
public class QueryScope{

    final QueryScope parent;

    private Map<String, QueryResult> names = new HashMap<>();

    QueryScope(){
        parent = null;
    }

    QueryScope(QueryScope parent){
        this.parent = parent;
    }

    QueryResult getQueryResult(String name){
        return names.get(name);
    }

    void registerQueryResult(QueryResult queryResult){
        names.put(queryResult.name, queryResult);
    }

    void registerQueryResult(String name, QueryResult queryResult){
        names.put(name, queryResult);
    }

    /**
     * we can resolve name(table/relation) from scope and parent scopes
     * @param name
     * @return
     */
    QueryResult resolve(String name){
        return names.getOrDefault(name, parent == null ? null : parent.resolve(name));
    }

    /**
     * but can only resolve column with this scope, cannot involve parent
     * @param columnName
     * @return
     */
    public QueryResult.ColumnDesc resolveColumn(String columnName) {
        return this.names.values().stream()
                .map(queryResult -> queryResult.findColumn(columnName))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    public int size(){
        return names.size();
    }

    public Map<String, QueryResult> getNames() {
        return names;
    }
}
