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
import org.siphonlab.ago.compiler.exception.CompilationError;

public class TableResult extends QueryResult {

    @NonNull
    ClassDef classDef;

    TableResult(ClassDef classDef) throws CompilationError {
        super();
        this.classDef = classDef;
        this.name = classDef.getFullname();
        Compiler.processClassTillStage(classDef, CompilingStage.InheritsFields);

        var idType = classDef.getRoot().getAnyEntityClass().asThatOrSuperOfThat(classDef).getGenericSource().typeArguments()[1];
        var id = new IdColumnDesc();
        id.name = "id";
        id.type = idType.getClassDefValue();
        id.ownerClass = classDef;
        columns.add(id);

        for(var field : classDef.getFields().values()){
            var c = new FieldColumnDesc();
            c.name = field.getName();
            c.srcVariable = field;
            c.ownerClass = classDef;
            c.type = field.getType();
            columns.add(c);
        }
    }

    public @NonNull ClassDef getClassDef() {
        return classDef;
    }
}
