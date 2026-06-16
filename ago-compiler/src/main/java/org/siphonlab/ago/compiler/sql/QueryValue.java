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

import net.sf.jsqlparser.schema.Column;
import org.siphonlab.ago.compiler.Variable;

import java.util.Objects;

public interface QueryValue {

    public class ColumnValue implements QueryValue{
        QueryResult.ColumnDef columnDef;

        public ColumnValue(QueryResult.ColumnDef columnDef) {
            this.columnDef = Objects.requireNonNull(columnDef);
        }
    }

    public class VariableValue implements QueryValue{

        final Variable variable;

        public VariableValue(Variable variable) {
            this.variable = Objects.requireNonNull(variable);
        }
    }

}
