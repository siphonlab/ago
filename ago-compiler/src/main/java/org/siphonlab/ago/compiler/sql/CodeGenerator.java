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
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.MySQLIndexHint;
import net.sf.jsqlparser.expression.SQLServerHints;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.SupportsOldOracleJoinSyntax;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Pivot;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.UnPivot;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.QueryDef;
import org.siphonlab.ago.compiler.Variable;
import org.siphonlab.ago.compiler.expression.CharBuffer;

import java.util.Map;

// should extend StatementDeParser, then with each subclass of SelectDeParser, DeleteDeParser, etc. to solve different statement
public class CodeGenerator extends SelectDeParser {


    private final QueryDef queryDef;
    private final Map<Table, ClassDef> classMapping;
    private final Map<Column, QueryResult.FieldColumnDef> variableMapping;
    private final Map<Expression, Variable> nullableConditions;

    public CodeGenerator(StringBuilder stringBuilder, QueryDef queryDef,
                         Map<Table, ClassDef> classMapping,
                         Map<Column, QueryResult.FieldColumnDef> variableMapping,
                         Map<Expression, Variable> nullableConditions) {
        super(new ExprVisitor(variableMapping, stringBuilder, nullableConditions), stringBuilder);
        this.queryDef = queryDef;
        this.classMapping = classMapping;
        this.variableMapping = variableMapping;
        this.nullableConditions = nullableConditions;
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

    @Override
    protected void deparseWhereClause(PlainSelect plainSelect) {
        if (plainSelect.getWhere() != null) {
            Variable nullableVar = nullableConditions.get(plainSelect.getWhere());
            if(nullableVar != null) {
                builder.append("${if(not %s) $\"".formatted(nullableVar.getName() + "IsNull"));
                super.deparseWhereClause(plainSelect);
                builder.append("\"$}");
            } else {
                super.deparseWhereClause(plainSelect);
            }
        }
    }


    static class ExprVisitor extends ExpressionDeParser{

        private final Map<Column, QueryResult.FieldColumnDef> variableMapping;
        private final Map<Expression, Variable> nullableConditions;

        public ExprVisitor(Map<Column, QueryResult.FieldColumnDef> variableMapping, StringBuilder stringBuilder, Map<Expression, Variable> nullableConditions) {
            this.variableMapping = variableMapping;
            this.nullableConditions = nullableConditions;
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

        @Override
        public <S> StringBuilder visit(OrExpression orExpression, S context) {
            return super.visit(orExpression, context);
        }

        @Override
        public <S> StringBuilder visit(AndExpression andExpression, S context) {
            Expression left = andExpression.getLeftExpression();
            Variable nullableVar = nullableConditions.get(left);
            if(nullableVar != null) {
                builder.append("${$\"");
                left.accept(this, context);
                builder.append("\"$ if(not %s) else 'true'}".formatted(nullableVar.getName() + "IsNull"));
            } else {
                left.accept(this, context);
            }
            Expression right = andExpression.getRightExpression();
            nullableVar = nullableConditions.get(right);
            if(nullableVar != null) {
                builder.append("${if(not %s) $\"".formatted(nullableVar.getName() + "IsNull"));
                builder.append(andExpression.isUseOperator() ? " && " : " AND ");
                right.accept(this, context);
                builder.append("\"$}");
            } else {
                builder.append(andExpression.isUseOperator() ? " && " : " AND ");
                right.accept(this, context);
            }
            return builder;
        }

    }

}
