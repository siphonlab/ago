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

    List<ColumnDef> columns = new ArrayList<>();

    /**
     * find column only inside my columns
     * @param columnName
     * @return
     */
    public ColumnDef findColumn(String columnName) {
        return columns.stream().filter(c -> c.name.equals(columnName)).findFirst().orElse(null);
    }

    public List<ColumnDef> getColumns() {
        return columns;
    }

    public static class ColumnDef{

        @NonNull
        String name;

        ClassDef type;

        public ColumnDef alias(String name) {
            var r = new ColumnDef();
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

    public static class FieldColumnDef extends ColumnDef{

        @NonNull
        Variable srcVariable;
        ClassDef ownerClass;

        public ColumnDef alias(String name) {
            var r = new FieldColumnDef();
            r.name = name;
            r.type = this.type;
            r.srcVariable = this.srcVariable;
            r.ownerClass = this.ownerClass;
            return r;
        }
    }

    public static class IdColumnDef extends ColumnDef{

        @NonNull
        ClassDef ownerClass;

        public ColumnDef alias(String name) {
            var r = new IdColumnDef();
            r.name = name;
            r.type = this.type;
            r.ownerClass = this.ownerClass;
            return r;
        }
    }

}

class TableResult extends QueryResult {

    @NonNull
    ClassDef classDef;

    TableResult(ClassDef classDef) throws CompilationError {
        super();
        this.classDef = classDef;
        this.name = classDef.getFullname();
        Compiler.processClassTillStage(classDef, CompilingStage.InheritsFields);

        var idType = classDef.getRoot().getAnyEntityClass().asThatOrSuperOfThat(classDef).getGenericSource().typeArguments()[1];
        var id = new IdColumnDef();
        id.name = "id";
        id.type = idType.getClassDefValue();
        id.ownerClass = classDef;
        columns.add(id);

        for(var field : classDef.getFields().values()){
            var c = new FieldColumnDef();
            c.name = field.getName();
            c.srcVariable = field;
            c.ownerClass = classDef;
            c.type = field.getType();
            columns.add(c);
        }
    }
}
