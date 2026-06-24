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
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.ASTNodeAccessImpl;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.piped.FromQuery;
import net.sf.jsqlparser.statement.select.*;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.NullableClassDef;
import org.siphonlab.ago.compiler.QueryDef;

public class ValueVisitor implements ExpressionVisitor<QueryValue> {

    private final QueryDef queryDef;

    private final SymbolMapping symbolMapping;

    public ValueVisitor(QueryDef queryDef, SymbolMapping symbolMapping) {
        this.queryDef = queryDef;
        this.symbolMapping = symbolMapping;
    }

    @Override
    public <S> QueryValue visit(Column tableColumn, S context) {
//        if (allowColumnProcessing && tableColumn.getTable() != null
//                && tableColumn.getTable().getName() != null) {
//            visit(tableColumn.getTable(), context);
//        }
        QueryResult.ColumnDesc col;
        QueryScope scope = (QueryScope) context;
        if (tableColumn.getTable() != null) {
//            var r = tableColumn.getTable().accept(this, context);
            var r = scope.resolve(tableColumn.getTable().getFullyQualifiedName());
            col = r.findColumn(tableColumn.getColumnName());
        } else {
            col = scope.resolveColumn(tableColumn.getColumnName());
        }
        if (col == null) return null;
        if (col instanceof QueryResult.FieldColumnDesc fieldColumnDef) {
            symbolMapping.addFiledMapping(tableColumn, fieldColumnDef);
        } else if(col instanceof QueryResult.IdColumnDesc idColumnDef) {
            symbolMapping.addIdMapping(tableColumn, idColumnDef);
        }
        return new QueryValue.ColumnValue(col);
    }

    public <S> void visitBinaryExpression(BinaryExpression binaryExpression, S context) {
        binaryExpression.getLeftExpression().accept(this, context);
        binaryExpression.getRightExpression().accept(this, context);
    }

    @Override
    public <S> QueryValue visit(BitwiseRightShift bitwiseRightShift, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(BitwiseLeftShift bitwiseLeftShift, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(NullValue nullValue, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Function function, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(SignedExpression signedExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(JdbcParameter jdbcParameter, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(JdbcNamedParameter jdbcNamedParameter, S context) {
        var name = jdbcNamedParameter.getName();
        var variable = queryDef.getVariable(name);
        if (variable == null) {
            variable = queryDef.getFields().get(name);
        }
        assert variable != null;
        symbolMapping.addBindParameter(variable);
        ClassDef type = variable.getType();
        if (type instanceof NullableClassDef nullableClassDef) {
            symbolMapping.addNullableCondition(findTopCondition(jdbcNamedParameter), variable);
        }
        return new QueryValue.VariableValue(variable);
    }

    private Expression findTopCondition(JdbcNamedParameter jdbcNamedParameter) {
        Expression prev = jdbcNamedParameter;
        for (var p = jdbcNamedParameter.getParent(); p instanceof ASTNodeAccessImpl p2; p = p2.getParent()) {
            if (p instanceof PlainSelect plainSelect) {
                if (prev == plainSelect.getWhere()) {
                    return prev;
                } else if (prev == plainSelect.getPreWhere()) {
                    return prev;
                }
            }
            if (p instanceof Join join) {
                return prev;
            }
            if (p instanceof AndExpression) {
                return prev;
            }
            if (p instanceof OrExpression or) {
                return prev;
            }
            if (p instanceof XorExpression xor) {
                return prev;
            }
            prev = (Expression) p;
        }
        return null;
    }

    @Override
    public <S> QueryValue visit(DoubleValue doubleValue, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(LongValue longValue, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(HexValue hexValue, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(DateValue dateValue, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(TimeValue timeValue, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(TimestampValue timestampValue, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(StringValue stringValue, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(BooleanValue booleanValue, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Addition addition, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Division division, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(IntegerDivision integerDivision, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Multiplication multiplication, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Subtraction subtraction, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(AndExpression andExpression, S context) {
        visitBinaryExpression(andExpression, context);
        return null;
    }

    @Override
    public <S> QueryValue visit(OrExpression orExpression, S context) {
        visitBinaryExpression(orExpression, context);
        return null;
    }

    @Override
    public <S> QueryValue visit(XorExpression xorExpression, S context) {
        visitBinaryExpression(xorExpression, context);
        return null;
    }

    @Override
    public <S> QueryValue visit(Between between, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(OverlapsCondition overlapsCondition, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(EqualsTo equalsTo, S context) {

        visitBinaryExpression(equalsTo, context);
        return null;
    }

    @Override
    public <S> QueryValue visit(GreaterThan greaterThan, S context) {
        visitBinaryExpression(greaterThan, context);
        return null;
    }

    @Override
    public <S> QueryValue visit(GreaterThanEquals greaterThanEquals, S context) {
        visitBinaryExpression(greaterThanEquals, context);
        return null;
    }

    @Override
    public <S> QueryValue visit(InExpression inExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(IncludesExpression includesExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(ExcludesExpression excludesExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(FullTextSearch fullTextSearch, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(IsNullExpression isNullExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(IsBooleanExpression isBooleanExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(IsUnknownExpression isUnknownExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(LikeExpression likeExpression, S context) {
        visitBinaryExpression(likeExpression, context);
        return null;
    }

    @Override
    public <S> QueryValue visit(MinorThan minorThan, S context) {
        visitBinaryExpression(minorThan, context);
        return null;
    }

    @Override
    public <S> QueryValue visit(MinorThanEquals minorThanEquals, S context) {
        visitBinaryExpression(minorThanEquals, context);
        return null;
    }

    @Override
    public <S> QueryValue visit(NotEqualsTo notEqualsTo, S context) {
        visitBinaryExpression(notEqualsTo, context);
        return null;
    }

    @Override
    public <S> QueryValue visit(DoubleAnd doubleAnd, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Contains contains, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(ContainedBy containedBy, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(ParenthesedSelect select, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(CaseExpression caseExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(WhenClause whenClause, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(ExistsExpression existsExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(MemberOfExpression memberOfExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(AnyComparisonExpression anyComparisonExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Concat concat, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Matches matches, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(BitwiseAnd bitwiseAnd, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(BitwiseOr bitwiseOr, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(BitwiseXor bitwiseXor, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(CastExpression castExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Modulo modulo, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(AnalyticExpression analyticExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(ExtractExpression extractExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(IntervalExpression intervalExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(OracleHierarchicalExpression hierarchicalExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(RegExpMatchOperator regExpMatchOperator, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(JsonExpression jsonExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(JsonOperator jsonOperator, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(UserVariable userVariable, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(NumericBind numericBind, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(KeepExpression keepExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(MySQLGroupConcat groupConcat, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(ExpressionList<? extends Expression> expressionList, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(RowConstructor<? extends Expression> rowConstructor, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(RowGetExpression rowGetExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(OracleHint hint, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(TimeKeyExpression timeKeyExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(DateTimeLiteralExpression dateTimeLiteralExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(NotExpression notExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(NextValExpression nextValExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(CollateExpression collateExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(SimilarToExpression similarToExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(ArrayExpression arrayExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(ArrayConstructor arrayConstructor, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(VariableAssignment variableAssignment, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(XMLSerializeExpr xmlSerializeExpr, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(TimezoneExpression timezoneExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(JsonAggregateFunction jsonAggregateFunction, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(JsonFunction jsonFunction, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(ConnectByRootOperator connectByRootOperator, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(ConnectByPriorOperator connectByPriorOperator, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(OracleNamedFunctionParameter oracleNamedFunctionParameter, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(AllColumns allColumns, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(FunctionAllColumns functionColumns, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(AllTableColumns allTableColumns, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(AllValue allValue, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(IsDistinctExpression isDistinctExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(GeometryDistance geometryDistance, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Select select, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(TranscodingFunction transcodingFunction, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(TrimFunction trimFunction, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(RangeExpression rangeExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(TSQLLeftJoin tsqlLeftJoin, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(TSQLRightJoin tsqlRightJoin, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(StructType structType, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(LambdaExpression lambdaExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(HighExpression highExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(LowExpression lowExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Plus plus, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(PriorTo priorTo, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(Inverse inverse, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(CosineSimilarity cosineSimilarity, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(FromQuery fromQuery, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(DateUnitExpression dateUnitExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(KeyExpression keyExpression, S context) {
        return null;
    }

    @Override
    public <S> QueryValue visit(PostgresNamedFunctionParameter postgresNamedFunctionParameter, S context) {
        return null;
    }
}
