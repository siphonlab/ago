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

import org.jspecify.annotations.NonNull;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.Compiler;
import org.siphonlab.ago.compiler.CompilingStage;
import org.siphonlab.ago.compiler.Variable;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.ArrayList;
import java.util.List;

// it's a name scope too
public class QueryResult {
    @NonNull
    String name;

    List<ColumnDesc> columns = new ArrayList<>();

    QueryScope scope;

    /**
     * find column only inside my columns
     * @param columnName
     * @return
     */
    public ColumnDesc findColumn(String columnName) {
        return columns.stream().filter(c -> c.name.equals(columnName)).findFirst().orElse(null);
    }

    public List<ColumnDesc> getColumns() {
        return columns;
    }

    public QueryScope getScope() {
        return scope;
    }

    public @NonNull String getName() {
        return name;
    }

    public static class ColumnDesc {

        @NonNull
        String name;

        ClassDef type;

        public ColumnDesc alias(String name) {
            var r = new ColumnDesc();
            r.name = name;
            r.type = this.type;
            return r;
        }

        public String getName() {
            return this.name;
        }

        public ClassDef getType() {
            return type;
        }
    }

    public static class FieldColumnDesc extends ColumnDesc {

        @NonNull
        Variable srcVariable;
        ClassDef ownerClass;

// shouldn't preserve field after alias set, for `mapColumn` cannot work on the alias
// i.e. `select n from (select name n from User) u`, the result is `select n from (select ${mapColumn<User>('name')} n from User) u`, in the outer select, shouldn't invoke mapColumn again
//        public ColumnDef alias(String name) {
//            var r = new FieldColumnDef();
//            r.name = name;
//            r.type = this.type;
//            r.srcVariable = this.srcVariable;
//            r.ownerClass = this.ownerClass;
//            return r;
//        }
    }

    public static class IdColumnDesc extends ColumnDesc {

        @NonNull
        ClassDef ownerClass;

    }

}

