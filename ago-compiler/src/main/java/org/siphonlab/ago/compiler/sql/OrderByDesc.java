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

import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.Variable;

public class OrderByDesc {

    private final QueryResult result;
    private final QueryScope scope;
    private final boolean mustOutputOrderBy;
    private int id;
    private Variable isOrderByOutputted;
    private FunctionDef sortMappingFunction;

    public OrderByDesc(QueryResult result, QueryScope scope, boolean mustOutputOrderBy) {
        this.result = result;
        this.scope = scope;
        this.mustOutputOrderBy = mustOutputOrderBy;
    }

    public QueryResult getQueryResult() {
        return result;
    }

    public QueryScope getScope() {
        return scope;
    }

    public boolean isMustOutputOrderBy() {
        return mustOutputOrderBy;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Variable getIsOrderByOutputted() {
        return isOrderByOutputted;
    }

    public void setIsOrderByOutputted(Variable isOrderByOutputted) {
        this.isOrderByOutputted = isOrderByOutputted;
    }

    public FunctionDef getSortMappingFunction(){
        return this.sortMappingFunction;
    }

    public void setSortMappingFunction(FunctionDef sortMappingFunction) {
        this.sortMappingFunction = sortMappingFunction;
    }

    public String composeIsOrderByOutputtedVariableName() {
        return "isOrderByOutputted_%d".formatted(this.id);
    }
}
