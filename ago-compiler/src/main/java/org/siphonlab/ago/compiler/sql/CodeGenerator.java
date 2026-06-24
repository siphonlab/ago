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

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.SupportsOldOracleJoinSyntax;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.Pivot;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.UnPivot;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.OrderByDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import org.siphonlab.ago.compiler.QueryDef;
import org.siphonlab.ago.compiler.Variable;

import java.util.Iterator;
import java.util.List;

// should extend StatementDeParser, then with each subclass of SelectDeParser, DeleteDeParser, etc. to solve different statement
public class CodeGenerator extends SelectDeParser {


    private final QueryDef queryDef;
    private final SymbolMapping symbolMapping;

    public CodeGenerator(StringBuilder stringBuilder, QueryDef queryDef,
                         SymbolMapping symbolMapping) {
        super(new ExprVisitor(stringBuilder, symbolMapping), stringBuilder);
        this.queryDef = queryDef;
        this.symbolMapping = symbolMapping;
    }

    @Override
    public <S> StringBuilder visit(Table table, S context){
        var classDef = symbolMapping.getMappedTable(table);
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
            Variable nullableVar = symbolMapping.getKnownNullableCondition(plainSelect.getWhere());
            if(nullableVar != null) {
                builder.append("${if(not %s) $\"".formatted(nullableVar.getName() + "IsNull"));
                super.deparseWhereClause(plainSelect);
                builder.append("\"$}");
            } else {
                super.deparseWhereClause(plainSelect);
            }
        }
    }

    @Override
    protected void deparseOrderByElementsClause(PlainSelect plainSelect, List<OrderByElement> orderByElements) {
        if (orderByElements != null) {
            var orderByDesc = symbolMapping.getOrderByClauses().get(plainSelect);
            new MyOrderByDeParser(this.getExpressionVisitor(), builder, orderByDesc, symbolMapping)
                    .deParse(plainSelect.isOracleSiblings(),orderByElements);
        }
    }

    static class ExprVisitor extends ExpressionDeParser{

        private SymbolMapping symbolMapping;

        public ExprVisitor(StringBuilder stringBuilder, SymbolMapping symbolMapping) {
            this.symbolMapping = symbolMapping;
            this.builder = stringBuilder;
        }

        @Override
        public <S> StringBuilder visit(Column tableColumn, S context) {
            var variableColumnDef = symbolMapping.getMappedField(tableColumn);

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

            if(variableColumnDef instanceof QueryResult.FieldColumnDesc fieldColumnDef) {
                builder.append("${mapColumn<%s>(%d)}".formatted(fieldColumnDef.ownerClass.getFullname(), fieldColumnDef.srcVariable.getSlotIndex()));
            } else if(variableColumnDef instanceof QueryResult.IdColumnDesc idColumnDef) {
                builder.append("${mapIdColumn<%s>()}".formatted(idColumnDef.ownerClass.getFullname()));
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
        public <S> StringBuilder visit(AndExpression andExpression, S context) {
            Expression left = andExpression.getLeftExpression();
            Expression right = andExpression.getRightExpression();
            deParseLogical(left, right, andExpression.isUseOperator() ? " && " : " AND ", context);
            return builder;
        }

        @Override
        public <S> StringBuilder visit(OrExpression orExpression, S context) {
            Expression left = orExpression.getLeftExpression();
            Expression right = orExpression.getRightExpression();
            deParseLogical(left, right, " OR ", context);
            return builder;
        }

        @Override
        public <S> StringBuilder visit(XorExpression xorExpression, S context) {
            Expression left = xorExpression.getLeftExpression();
            Expression right = xorExpression.getRightExpression();
            deParseLogical(left, right, " XOR ", context);
            return builder;
        }

        protected <S> void deParseLogical(Expression left, Expression right,
                                          String operator, S context) {
            Variable nullableVar = symbolMapping.getKnownNullableCondition(left);
            if(nullableVar != null) {
                builder.append("${$\"");
                left.accept(this, context);
                builder.append("\"$ if(not %s) else 'true'}".formatted(nullableVar.getName() + "IsNull"));
            } else {
                left.accept(this, context);
            }
            nullableVar = symbolMapping.getKnownNullableCondition(right);
            if(nullableVar != null) {
                builder.append("${if(not %s) $\"".formatted(nullableVar.getName() + "IsNull"));
                builder.append(operator);
                right.accept(this, context);
                builder.append("\"$}");
            } else {
                builder.append(operator);
                right.accept(this, context);
            }
        }

        @Override
        public <S> StringBuilder visit(EqualsTo equalsTo, S context) {
            if(equalsTo.getLeftExpression() instanceof Column leftCol && equalsTo.getRightExpression() instanceof Column rightCol) {
                var left = symbolMapping.getMappedField(leftCol);
                var right = symbolMapping.getMappedField(rightCol);
                if(left instanceof QueryResult.FieldColumnDesc leftField && leftField.getType().isObjectOrNullableObject()) {
                    if(right instanceof QueryResult.IdColumnDesc rightId) {
                        builder.append("${idEquals<%s,%s>('%s', '%s', %d)}".formatted(
                                rightId.ownerClass.getFullname(),
                                leftField.ownerClass.getFullname(),
                                rightCol.getTableName(), leftCol.getTableName(), leftField.srcVariable.getSlotIndex()
                        ));
                        return builder;
                    } else if(right instanceof QueryResult.FieldColumnDesc rightField) {
                        builder.append("${objectEquals<%s,%s>('%s', %d, '%s', %d)}".formatted(
                                leftField.ownerClass.getFullname(),
                                rightField.ownerClass.getFullname(),
                                leftCol.getTableName(), leftField.srcVariable.getSlotIndex(),
                                rightCol.getTableName(),rightField.srcVariable.getSlotIndex()
                        ));
                        return builder;
                    }
                } else if(left instanceof QueryResult.IdColumnDesc leftId) {
                    if(right instanceof QueryResult.FieldColumnDesc rightField) {
                        builder.append("${idEquals<%s,%s>('%s', '%s', %d)}".formatted(
                                leftId.ownerClass.getFullname(),
                                rightField.ownerClass.getFullname(),
                                leftCol.getTableName(),
                                rightCol.getTableName(), rightField.srcVariable.getSlotIndex()
                        ));
                        return builder;
                    }
                }
            }
            return super.visit(equalsTo, context);
        }
    }

    static class MyOrderByDeParser extends OrderByDeParser {

        private final OrderByDesc orderByDesc;
        private final SymbolMapping symbolMapping;
        private ExpressionVisitor<StringBuilder> expressionVisitor;

        public MyOrderByDeParser(ExpressionVisitor<StringBuilder> expressionVisitor,
                                 StringBuilder buffer, OrderByDesc orderByDesc, SymbolMapping symbolMapping) {
            super(expressionVisitor, buffer);
            this.orderByDesc = orderByDesc;
            this.symbolMapping = symbolMapping;
        }

        public void deParse(boolean oracleSiblings, List<OrderByElement> orderByElementList) {
            boolean mustOutput = orderByElementList.stream().anyMatch(o -> o.getExpression() instanceof Column);
            if(mustOutput) {
                if (oracleSiblings) {
                    builder.append(" ORDER SIBLINGS BY ");
                } else {
                    builder.append(" ORDER BY ");
                }
            }

            int index = 0;
            for (Iterator<OrderByElement> iterator = orderByElementList.iterator(); iterator.hasNext();) {
                OrderByElement orderByElement = iterator.next();
                deParseElement(orderByElement, oracleSiblings);
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
        }

        public void deParseElement(OrderByElement orderBy, boolean oracleSiblings) {

            Expression expression = orderBy.getExpression();

            if(expression instanceof Column tableColumn) {
                var variableColumnDef = this.symbolMapping.getMappedField(tableColumn);

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

                if(variableColumnDef instanceof QueryResult.FieldColumnDesc fieldColumnDef) {
                    builder.append("${mapColumn<%s>(%d)}".formatted(fieldColumnDef.ownerClass.getFullname(), fieldColumnDef.srcVariable.getSlotIndex()));
                } else if(variableColumnDef instanceof QueryResult.IdColumnDesc idColumnDef) {
                    builder.append("${mapIdColumn<%s>()}".formatted(idColumnDef.ownerClass.getFullname()));
                } else {
                    builder.append(tableColumn.getColumnName());
                }
                if (!orderBy.isAsc()) {
                    builder.append(" DESC");
                } else if (orderBy.isAscDescPresent()) {
                    builder.append(" ASC");
                }
                if (orderBy.getNullOrdering() != null) {
                    builder.append(' ');
                    builder.append(orderBy.getNullOrdering() == OrderByElement.NullOrdering.NULLS_FIRST
                            ? "NULLS FIRST"
                            : "NULLS LAST");
                }
                if (orderBy.isMysqlWithRollup()) {
                    builder.append(" WITH ROLLUP");
                }
            } else if(expression instanceof JdbcNamedParameter jdbcNamedParameter) {
                // if(:name != nul and not isOrderByOutputed_{orderByDesc.index}) "ORDER BY"
                // isOrderByOutputed_orderByDesc.index = true
                builder.append("${");
                String outputtedVarName = orderByDesc.getIsOrderByOutputted().getName();
                builder.append("if(%s != null and (not %s and (%s = true))) ".formatted(jdbcNamedParameter.getName(), outputtedVarName, outputtedVarName));
                if (oracleSiblings) {
                    builder.append(" $\" ORDER SIBLINGS BY \"$ ");
                } else {
                    builder.append(" $\" ORDER BY \"$ ");
                }
                builder.append("}");

                builder.append("${if(%s != null) %s!.map<>(%s).join(',')}".formatted(jdbcNamedParameter.getName(), jdbcNamedParameter.getName(), orderByDesc.getSortMappingFunction().getName()));
            }
        }

    }

}
