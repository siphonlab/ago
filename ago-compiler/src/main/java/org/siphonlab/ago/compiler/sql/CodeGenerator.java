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

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.MySQLIndexHint;
import net.sf.jsqlparser.expression.SQLServerHints;
import net.sf.jsqlparser.expression.operators.relational.SupportsOldOracleJoinSyntax;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Pivot;
import net.sf.jsqlparser.statement.select.UnPivot;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.QueryDef;

import java.util.Map;

// should extend StatementDeParser, then with each subclass of SelectDeParser, DeleteDeParser, etc. to solve different statement
public class CodeGenerator extends SelectDeParser {


    private final QueryDef queryDef;
    private final Map<Table, ClassDef> classMapping;
    private final Map<Column, QueryResult.FieldColumnDef> variableMapping;

    public CodeGenerator(StringBuilder stringBuilder, QueryDef queryDef, Map<Table, ClassDef> classMapping, Map<Column, QueryResult.FieldColumnDef> variableMapping) {
        super(new ExprVisitor(variableMapping, stringBuilder), stringBuilder);
        this.queryDef = queryDef;
        this.classMapping = classMapping;
        this.variableMapping = variableMapping;
    }

    @Override
    public <S> StringBuilder visit(Table table, S context){
        var classDef = classMapping.get(table);
        if(classDef == null) {
            builder.append(table.getFullyQualifiedName());
        } else {
            builder.append("${mapTable<%s>()}".formatted(classDef.getFullname()));
        }

        if (table.getTimeTravel() != null) {
            builder.append(" ").append(table.getTimeTravel());
        }
        Alias alias = table.getAlias();
        if (alias != null) {
            builder.append(alias);
        }
        if (table.getTimeTravelStrAfterAlias() != null) {
            builder.append(" ").append(table.getTimeTravelStrAfterAlias());
        }
        Pivot pivot = table.getPivot();
        if (pivot != null) {
            pivot.accept(this, context);
        }
        UnPivot unpivot = table.getUnPivot();
        if (unpivot != null) {
            unpivot.accept(this, context);
        }
        MySQLIndexHint indexHint = table.getIndexHint();
        if (indexHint != null) {
            builder.append(indexHint);
        }
        SQLServerHints sqlServerHints = table.getSqlServerHints();
        if (sqlServerHints != null) {
            builder.append(sqlServerHints);
        }
        return builder;
    }

    static class ExprVisitor extends ExpressionDeParser{

        private final Map<Column, QueryResult.FieldColumnDef> variableMapping;

        public ExprVisitor(Map<Column, QueryResult.FieldColumnDef> variableMapping, StringBuilder stringBuilder) {
            this.variableMapping = variableMapping;
            this.builder = stringBuilder;
        }

        @Override
        public <S> StringBuilder visit(Column tableColumn, S context) {
            var variableColumnDef = variableMapping.get(tableColumn);

            final Table table = tableColumn.getTable();
            String tableName = null;
            if (table != null) {
                if (table.getAlias() != null) {
                    tableName = table.getAlias().getName();
                } else {
                    tableName = table.getFullyQualifiedName();
                }
            }
            if (tableName != null && !tableName.isEmpty()) {
                builder.append(tableName).append(tableColumn.getTableDelimiter());
            }

            if(variableColumnDef != null){
                builder.append("${mapColumn<%s>(%d)}".formatted(variableColumnDef.ownerClass.getFullname(), variableColumnDef.srcVariable.getSlotIndex()));
            } else {

                builder.append(tableColumn.getColumnName());
            }

            if (tableColumn.getOldOracleJoinSyntax() != SupportsOldOracleJoinSyntax.NO_ORACLE_JOIN) {
                builder.append("(+)");
            }

            if (tableColumn.getArrayConstructor() != null) {
                tableColumn.getArrayConstructor().accept(this, context);
            }

            if (tableColumn.getCommentText() != null) {
                builder.append(" /* ").append(tableColumn.getCommentText()).append("*/ ");
            }

            return builder;
        }
    }

}
