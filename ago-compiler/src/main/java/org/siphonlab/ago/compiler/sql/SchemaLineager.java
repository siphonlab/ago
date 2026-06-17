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

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.parser.Token;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Block;
import net.sf.jsqlparser.statement.Commit;
import net.sf.jsqlparser.statement.CreateFunctionalStatement;
import net.sf.jsqlparser.statement.DeclareStatement;
import net.sf.jsqlparser.statement.DescribeStatement;
import net.sf.jsqlparser.statement.ExplainStatement;
import net.sf.jsqlparser.statement.IfElseStatement;
import net.sf.jsqlparser.statement.PurgeObjectType;
import net.sf.jsqlparser.statement.PurgeStatement;
import net.sf.jsqlparser.statement.ResetStatement;
import net.sf.jsqlparser.statement.RollbackStatement;
import net.sf.jsqlparser.statement.SavepointStatement;
import net.sf.jsqlparser.statement.SessionStatement;
import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.ShowColumnsStatement;
import net.sf.jsqlparser.statement.ShowStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.UnsupportedStatement;
import net.sf.jsqlparser.statement.UseStatement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterSession;
import net.sf.jsqlparser.statement.alter.AlterSystemStatement;
import net.sf.jsqlparser.statement.alter.RenameTableStatement;
import net.sf.jsqlparser.statement.alter.sequence.AlterSequence;
import net.sf.jsqlparser.statement.analyze.Analyze;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.policy.CreatePolicy;
import net.sf.jsqlparser.statement.create.schema.CreateSchema;
import net.sf.jsqlparser.statement.create.sequence.CreateSequence;
import net.sf.jsqlparser.statement.create.synonym.CreateSynonym;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.AlterView;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.delete.ParenthesedDelete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.export.Export;
import net.sf.jsqlparser.statement.grant.Grant;
import net.sf.jsqlparser.statement.imprt.Import;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.insert.OracleMultiInsertBranch;
import net.sf.jsqlparser.statement.insert.OracleMultiInsertClause;
import net.sf.jsqlparser.statement.insert.ParenthesedInsert;
import net.sf.jsqlparser.statement.lock.LockStatement;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.piped.FromQuery;
import net.sf.jsqlparser.statement.refresh.RefreshMaterializedViewStatement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.ParenthesedFromItem;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.TableStatement;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.show.ShowIndexStatement;
import net.sf.jsqlparser.statement.show.ShowTablesStatement;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.ParenthesedUpdate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import net.sf.jsqlparser.statement.upsert.Upsert;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.QueryDef;
import org.siphonlab.ago.compiler.Variable;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.parser.AgoLexer;
import org.siphonlab.ago.compiler.parser.AgoParser;

@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.UncommentedEmptyMethodBody"})
public class SchemaLineager
        implements SelectVisitor<QueryResult>, FromItemVisitor<QueryResult>,
        StatementVisitor<QueryResult> {

    private final ClassDef scopeClass;
    private final Set<Variable> bindParameters = new HashSet<>();

    private ValueVisitor valueVisitor;

    private QueryScope currentScope = new QueryScope();
    private Map<Table, ClassDef> classMapping = new HashMap<>();
    private Map<Column, QueryResult.FieldColumnDef> fieldMapping = new HashMap<>();
    private Statement statement;

    private static <T> void throwUnsupported(T type) {
        throw new UnsupportedOperationException(String.format(
                "Finding tables from %s is not supported", type.getClass().getSimpleName()));
    }

    public SchemaLineager(QueryDef scopeClass){
        this.scopeClass = scopeClass;
        this.valueVisitor = new ValueVisitor(scopeClass, fieldMapping, bindParameters);
    }

    public QueryResult resolve(Statement statement) {
        this.statement = statement;
        return statement.accept(this, currentScope);
    }

    public Statement getStatement() {
        return statement;
    }

    QueryResult resolveTableName(Table table) throws CompilationError {
        String name = table.getName();
        var r = currentScope.resolve(name);
        if(r == null) {
            ClassDef classDef = scopeClass.getUnit().parseTypeName(scopeClass, new AgoParser(new CommonTokenStream(new AgoLexer(CharStreams.fromString(name)))).namePath(), true);
            if(classDef != null){
                r = new TableResult(classDef);
                this.classMapping.put(table, classDef);
                return r;
            }
        }
        Token first = table.getASTNode().jjtGetFirstToken();
        Token last = table.getASTNode().jjtGetLastToken();
        throw new TypeMismatchError("type '%s' not found".formatted(name), new SourceLocation(scopeClass.getUnit().getFilename(),
                first.beginLine,first.beginColumn,last.absoluteEnd - last.absoluteBegin, first.absoluteBegin, last.absoluteEnd));       // TODO offset from sql begin
    }

    @Override
    public <S> QueryResult visit(Select select, S context) {
        List<WithItem<?>> withItemsList = select.getWithItemsList();
        processWithItems((QueryScope) context, withItemsList);
        return select.accept((SelectVisitor<QueryResult>) this, context);
    }

    private <S> void processWithItems(QueryScope context, List<WithItem<?>> withItemsList) {
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem<?> withItem : withItemsList) {
                var alias = withItem.getAlias().getName();
                var r = withItem.accept((SelectVisitor<QueryResult>) this, context);
                context.registerQueryResult(alias, r);     // `with` means step in in sequence
            }
        }
    }

    @Override
    public void visit(Select select) {
        StatementVisitor.super.visit(select);
    }

    @Override
    public <S> QueryResult visit(WithItem<?> withItem, S context) {
//        if (withItem.getAlias() != null) {
//            otherItemNames.add(withItem.getAlias().getName());
//        }
        if (withItem.getSelect() != null) {
            withItem.getSelect().accept((SelectVisitor<QueryResult>) this, context);
        }
        return null;
    }

    @Override
    public void visit(WithItem<?> withItem) {
        SelectVisitor.super.visit(withItem);
    }

    @Override
    public <S> QueryResult visit(ParenthesedSelect select, S context) {
        processWithItems((QueryScope) context, select.getWithItemsList());
        var r = select.getSelect().accept((SelectVisitor<QueryResult>) this, context);
        if (select.getAlias() != null) {
            ((QueryScope)context).registerQueryResult(select.getAlias().getName(), r);
        } else {
            ((QueryScope)context).registerQueryResult(r);
        }
        return r;
    }

    @Override
    public void visit(ParenthesedSelect parenthesedSelect) {
        SelectVisitor.super.visit(parenthesedSelect);
    }

    public Map<Table, ClassDef> getClassMapping() {
        return classMapping;
    }

    public Map<Column, QueryResult.FieldColumnDef> getFieldMapping() {
        return fieldMapping;
    }

    public Set<Variable> getBindParameters() {
        return bindParameters;
    }

    public Map<Expression, Variable> getNullableConditions() {
        return valueVisitor.getNullableConditions();
    }

    @Override
    public <S> QueryResult visit(PlainSelect plainSelect, S context) {
//        String alias = plainSelect.getAlias().getName();

        QueryScope scope = new QueryScope((QueryScope) context);

        processWithItems(scope, plainSelect.getWithItemsList());

        // resolve scope
        QueryResult fromResult;
        if (plainSelect.getFromItem() != null) {
            fromResult = plainSelect.getFromItem().accept(this, scope);
        } else {
            fromResult = null;
        }
        var joins = visitJoins(plainSelect.getJoins(), scope);
        registerJoins(plainSelect.getFromItem(), fromResult, plainSelect.getJoins(), joins, scope);

//        if(scope.size() == 1){      // single table, handle *
//
//        }

        var result = new QueryResult();
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                var v = item.getExpression().accept(valueVisitor, scope);
                String cname = item.getAliasName();
                if(cname == null) {
                    if(item.getExpression() instanceof Column column){
                        cname = column.getColumnName();
                    } else {
                        throw new IllegalArgumentException("column name expected");      // TODO generate a name
                    }
                }
                QueryResult.ColumnDef columnDef;
                if(v instanceof QueryValue.ColumnValue columnValue){
                    columnDef = columnValue.columnDef.alias(cname);
                } else {
                    columnDef = new QueryResult.ColumnDef();
                    columnDef.name = cname;
                    columnDef.type = scopeClass.getRoot().getAnyClass();
                }
                result.columns.add(columnDef);

                // TODO handle QueryValue depends on Variables
//                if(item.getExpression() instanceof Column column){
//                    if(columnDef instanceof QueryResult.VariableColumnDef variableColumnDef){
//                        this.variableMapping.put(column, variableColumnDef);
//                    }
//                }
            }
        }


        visitJoinOn(plainSelect.getJoins(), scope);

        if (plainSelect.getPreWhere() != null) {
            plainSelect.getPreWhere().accept(valueVisitor, scope);
        }
        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(valueVisitor, scope);
        }

        if (plainSelect.getHaving() != null) {
            plainSelect.getHaving().accept(valueVisitor, scope);
        }

        if (plainSelect.getOracleHierarchical() != null) {
            plainSelect.getOracleHierarchical().accept(valueVisitor, scope);
        }
        return result;
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        SelectVisitor.super.visit(plainSelect);
    }

    @Override
    public <S> QueryResult visit(FromQuery fromQuery, S context) {
        return null;
    }

    protected String extractTableName(Table table) {
        return table.getFullyQualifiedName();
    }

    @Override
    public <S> QueryResult visit(Table table, S context) {
//        String tableWholeName = extractTableName(table);
        try {
            return resolveTableName(table);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(Table tableName) {
        this.visit(tableName, null);
    }

    @Override
    public <S> QueryResult visit(SetOperationList list, S context) {
        List<WithItem<?>> withItemsList = list.getWithItemsList();
        if (withItemsList != null && !withItemsList.isEmpty()) {
            for (WithItem<?> withItem : withItemsList) {
                withItem.accept((SelectVisitor<QueryResult>) this, context);
            }
        }
        for (Select selectBody : list.getSelects()) {
            selectBody.accept((SelectVisitor<QueryResult>) this, context);
        }
        return null;
    }

    @Override
    public void visit(SetOperationList setOpList) {
        SelectVisitor.super.visit(setOpList);
    }

    @Override
    public <S> QueryResult visit(LateralSubSelect lateralSubSelect, S context) {
//        if (lateralSubSelect.getAlias() != null) {
//            otherItemNames.add(lateralSubSelect.getAlias().getName());
//        }
        lateralSubSelect.getSelect().accept((SelectVisitor<QueryResult>) this, context);
        return null;
    }

    @Override
    public void visit(LateralSubSelect lateralSubSelect) {
        SelectVisitor.super.visit(lateralSubSelect);
    }

    @Override
    public <S> QueryResult visit(TableFunction tableFunction, S context) {
        return null;
    }

    @Override
    public <S> QueryResult visit(TableStatement tableStatement, S context) {
        tableStatement.getTable().accept(this, null);
        return null;
    }

    @Override
    public void visit(TableStatement tableStatement) {
        SelectVisitor.super.visit(tableStatement);
    }

    @Override
    public <S> QueryResult visit(Delete delete, S context) {
        visit(delete.getTable(), context);

        if (delete.getUsingFromItemList() != null) {
            for (FromItem usingFromItem : delete.getUsingFromItemList()) {
                usingFromItem.accept(this, context);
            }
        }

        visitJoins(delete.getJoins(), context);

        if (delete.getWhere() != null) {
            delete.getWhere().accept(valueVisitor, context);
        }
        return null;
    }

    @Override
    public void visit(Delete delete) {
        StatementVisitor.super.visit(delete);
    }

    @Override
    public <S> QueryResult visit(ParenthesedDelete delete, S context) {
        return visit(delete.getDelete(), context);
    }

    @Override
    public <S> QueryResult visit(SessionStatement sessionStatement, S context) {
        return null;
    }

    @Override
    public <S> QueryResult visit(Update update, S context) {
        if (update.getWithItemsList() != null) {
            for (WithItem<?> withItem : update.getWithItemsList()) {
                withItem.accept((SelectVisitor<QueryResult>) this, context);
            }
        }

        visit(update.getTable(), context);

        if (update.getStartJoins() != null) {
            for (Join join : update.getStartJoins()) {
                join.getRightItem().accept(this, context);
            }
        }

        if (update.getUpdateSets() != null) {
            for (UpdateSet updateSet : update.getUpdateSets()) {
                updateSet.getColumns().accept(valueVisitor, context);
                updateSet.getValues().accept(valueVisitor, context);
            }
        }

        if (update.getFromItem() != null) {
            update.getFromItem().accept(this, context);
        }

        if (update.getJoins() != null) {
            for (Join join : update.getJoins()) {
                join.getRightItem().accept(this, context);
                for (Expression expression : join.getOnExpressions()) {
                    expression.accept(valueVisitor, context);
                }
            }
        }

        if (update.getWhere() != null) {
            update.getWhere().accept(valueVisitor, context);
        }
        return null;
    }

    @Override
    public <S> QueryResult visit(ParenthesedUpdate update, S context) {
        return visit(update.getUpdate(), context);
    }

    @Override
    public void visit(Update update) {
        StatementVisitor.super.visit(update);
    }

    @Override
    public <S> QueryResult visit(Insert insert, S context) {
        if (insert.isOracleMultiInsert() && insert.getOracleMultiInsertBranches() != null) {
            for (OracleMultiInsertBranch branch : insert.getOracleMultiInsertBranches()) {
                if (branch.getWhenExpression() != null) {
                    branch.getWhenExpression().accept(valueVisitor, context);
                }
                if (branch.getClauses() == null) {
                    continue;
                }
                for (OracleMultiInsertClause clause : branch.getClauses()) {
                    visit(clause.getTable(), context);
                    if (clause.getSelect() != null) {
                        visit(clause.getSelect(), context);
                    }
                }
            }
        } else if (insert.getTable() != null) {
            visit(insert.getTable(), context);
        }
        if (insert.getWithItemsList() != null) {
            for (WithItem<?> withItem : insert.getWithItemsList()) {
                withItem.accept((SelectVisitor<QueryResult>) this, context);
            }
        }
        if (insert.getSelect() != null) {
            visit(insert.getSelect(), context);
        }
        return null;
    }

    @Override
    public <S> QueryResult visit(ParenthesedInsert insert, S context) {
        return visit(insert.getInsert(), context);
    }

    @Override
    public void visit(Insert insert) {
        StatementVisitor.super.visit(insert);
    }

    @Override
    public <S> QueryResult visit(Analyze analyze, S context) {
        visit(analyze.getTable(), context);
        return null;
    }

    @Override
    public void visit(Analyze analyze) {
        StatementVisitor.super.visit(analyze);
    }

    @Override
    public <S> QueryResult visit(Drop drop, S context) {
        visit(drop.getName(), context);
        return null;
    }

    @Override
    public void visit(Drop drop) {
        StatementVisitor.super.visit(drop);
    }

    @Override
    public <S> QueryResult visit(Truncate truncate, S context) {
        visit(truncate.getTable(), context);
        return null;
    }

    @Override
    public void visit(Truncate truncate) {
        StatementVisitor.super.visit(truncate);
    }

    @Override
    public <S> QueryResult visit(CreateIndex createIndex, S context) {
        throwUnsupported(createIndex);
        return null;
    }

    @Override
    public void visit(CreateIndex createIndex) {
        StatementVisitor.super.visit(createIndex);
    }

    @Override
    public <S> QueryResult visit(CreateSchema createSchema, S context) {
        throwUnsupported(createSchema);
        return null;
    }

    @Override
    public void visit(CreateSchema createSchema) {
        StatementVisitor.super.visit(createSchema);
    }

    @Override
    public <S> QueryResult visit(CreateTable create, S context) {
        visit(create.getTable(), null);
        if (create.getSelect() != null) {
            create.getSelect().accept((SelectVisitor<QueryResult>) this, context);
        }
        return null;
    }

    @Override
    public void visit(CreateTable createTable) {
        StatementVisitor.super.visit(createTable);
    }

    @Override
    public <S> QueryResult visit(CreateView create, S context) {
        visit(create.getView(), null);
        if (create.getSelect() != null) {
            create.getSelect().accept((SelectVisitor<QueryResult>) this, context);
        }
        return null;
    }

    @Override
    public void visit(CreateView createView) {
        StatementVisitor.super.visit(createView);
    }

    @Override
    public <S> QueryResult visit(Alter alter, S context) {
        return alter.getTable().accept(this, context);
    }

    @Override
    public void visit(Alter alter) {
        alter.getTable().accept(this, null);
    }

    @Override
    public <S> QueryResult visit(Statements statements, S context) {
        for (Statement statement : statements) {
            statement.accept(this, context);
        }
        return null;
    }

    @Override
    public void visit(Statements statements) {
        StatementVisitor.super.visit(statements);
    }

    @Override
    public <S> QueryResult visit(Execute execute, S context) {
        throwUnsupported(execute);
        return null;
    }

    @Override
    public void visit(Execute execute) {
        StatementVisitor.super.visit(execute);
    }

    @Override
    public <S> QueryResult visit(SetStatement setStatement, S context) {
        throwUnsupported(setStatement);
        return null;
    }

    @Override
    public void visit(SetStatement set) {
        StatementVisitor.super.visit(set);
    }

    @Override
    public <S> QueryResult visit(ResetStatement reset, S context) {
        throwUnsupported(reset);
        return null;
    }

    @Override
    public void visit(ResetStatement reset) {
        StatementVisitor.super.visit(reset);
    }

    @Override
    public <S> QueryResult visit(ShowColumnsStatement showColumnsStatement, S context) {
        throwUnsupported(showColumnsStatement);
        return null;
    }

    @Override
    public void visit(ShowColumnsStatement showColumns) {
        StatementVisitor.super.visit(showColumns);
    }

    @Override
    public <S> QueryResult visit(ShowIndexStatement showIndex, S context) {
        throwUnsupported(showIndex);
        return null;
    }

    @Override
    public void visit(ShowIndexStatement showIndex) {
        StatementVisitor.super.visit(showIndex);
    }

    @Override
    public <S> QueryResult visit(Merge merge, S context) {
        visit(merge.getTable(), context);
        if (merge.getWithItemsList() != null) {
            for (WithItem<?> withItem : merge.getWithItemsList()) {
                withItem.accept((SelectVisitor<QueryResult>) this, context);
            }
        }

        if (merge.getFromItem() != null) {
            merge.getFromItem().accept(this, context);
        }
        return null;
    }

    @Override
    public void visit(Merge merge) {
        StatementVisitor.super.visit(merge);
    }

//    @Override
//    public <S> QueryResult visit(TableFunction tableFunction, S context) {
//        visit(tableFunction.getFunction(), null);
//        return null;
//    }

    @Override
    public void visit(TableFunction tableFunction) {
        FromItemVisitor.super.visit(tableFunction);
    }

    @Override
    public <S> QueryResult visit(AlterView alterView, S context) {
        throwUnsupported(alterView);
        return null;
    }

    @Override
    public void visit(AlterView alterView) {
        StatementVisitor.super.visit(alterView);
    }

    @Override
    public <S> QueryResult visit(RefreshMaterializedViewStatement materializedView, S context) {
        visit(materializedView.getView(), context);
        return null;
    }

    @Override
    public void visit(RefreshMaterializedViewStatement materializedView) {
        StatementVisitor.super.visit(materializedView);
    }

    @Override
    public <S> QueryResult visit(Commit commit, S context) {
        return null;
    }

    @Override
    public void visit(Commit commit) {
        StatementVisitor.super.visit(commit);
    }

    @Override
    public <S> QueryResult visit(Upsert upsert, S context) {
        visit(upsert.getTable(), context);
        if (upsert.getExpressions() != null) {
            upsert.getExpressions().accept(valueVisitor, context);
        }
        if (upsert.getSelect() != null) {
            visit(upsert.getSelect(), context);
        }
        return null;
    }

    @Override
    public void visit(Upsert upsert) {
        StatementVisitor.super.visit(upsert);
    }

    @Override
    public <S> QueryResult visit(UseStatement use, S context) {
        return null;
    }

    @Override
    public void visit(UseStatement use) {
        StatementVisitor.super.visit(use);
    }

    @Override
    public <S> QueryResult visit(ParenthesedFromItem parenthesis, S context) {
        parenthesis.getFromItem().accept(this, context);
        visitJoins(parenthesis.getJoins(), context);
        return null;
    }

    @Override
    public void visit(ParenthesedFromItem parenthesedFromItem) {
        FromItemVisitor.super.visit(parenthesedFromItem);
    }

    private <S> List<QueryResult> visitJoins(List<Join> joins, S context) {
        if (joins == null) {
            return null;
        }
        List<QueryResult> ls = new ArrayList<>();
        for (Join join : joins) {
            ls.add(join.getFromItem().accept(this, context));
        }
        return ls;
    }

    private void registerJoins(FromItem baseFrom, QueryResult queryResult, List<Join> joins, List<QueryResult> joinResults, QueryScope context) {
        if(baseFrom != null) {
            Alias alias = baseFrom.getAlias();
            if (alias != null) {
                context.registerQueryResult(alias.getName(), queryResult);
            } else {
                context.registerQueryResult(queryResult);
            }
        }
        if(joins != null) {
            for (int i = 0; i < joins.size(); i++) {
                Join join = joins.get(i);
                var alias = join.getFromItem().getAlias();
                queryResult = joinResults.get(i);
                if (alias != null) {
                    context.registerQueryResult(alias.getName(), queryResult);
                } else {
                    context.registerQueryResult(queryResult);
                }
            }
        }
    }

    private <S> void visitJoinOn(List<Join> joins, S context) {
        if(joins != null) {
            for (Join join : joins) {
                for (Expression expression : join.getOnExpressions()) {
                    expression.accept(valueVisitor, context);
                }
            }
        }
    }

    @Override
    public <S> QueryResult visit(Block block, S context) {
        if (block.getStatements() != null) {
            visit(block.getStatements(), context);
        }
        return null;
    }

    @Override
    public void visit(Block block) {
        StatementVisitor.super.visit(block);
    }

    @Override
    public <S> QueryResult visit(Comment comment, S context) {
        if (comment.getTable() != null) {
            visit(comment.getTable(), context);
        }
        if (comment.getColumn() != null) {
            Table table = comment.getColumn().getTable();
            if (table != null) {
                visit(table, context);
            }
        }
        return null;
    }

    @Override
    public void visit(Comment comment) {
        StatementVisitor.super.visit(comment);
    }

    @Override
    public <S> QueryResult visit(Values values, S context) {
        values.getExpressions().accept(valueVisitor, context);
        return null;
    }

    @Override
    public void visit(Values values) {
        SelectVisitor.super.visit(values);
    }

    @Override
    public <S> QueryResult visit(DescribeStatement describe, S context) {
        describe.getTable().accept(this, context);
        return null;
    }

    @Override
    public void visit(DescribeStatement describe) {
        StatementVisitor.super.visit(describe);
    }

    @Override
    public <S> QueryResult visit(ExplainStatement explainStatement, S context) {
        if (explainStatement.getStatement() != null) {
            explainStatement.getStatement().accept((StatementVisitor<?>) this, context);
        }
        return null;
    }

    @Override
    public void visit(ExplainStatement explainStatement) {
        StatementVisitor.super.visit(explainStatement);
    }

    @Override
    public <S> QueryResult visit(ShowStatement showStatement, S context) {
        return null;
    }

    @Override
    public void visit(ShowStatement showStatement) {
        StatementVisitor.super.visit(showStatement);
    }

    @Override
    public <S> QueryResult visit(DeclareStatement declareStatement, S context) {
        return null;
    }

    @Override
    public void visit(DeclareStatement declareStatement) {
        StatementVisitor.super.visit(declareStatement);
    }

    @Override
    public <S> QueryResult visit(Grant grant, S context) {
        return null;
    }

    @Override
    public void visit(Grant grant) {
        StatementVisitor.super.visit(grant);
    }

    @Override
    public <S> QueryResult visit(CreateSequence createSequence, S context) {
        throwUnsupported(createSequence);
        return null;
    }

    @Override
    public void visit(CreateSequence createSequence) {
        StatementVisitor.super.visit(createSequence);
    }

    @Override
    public <S> QueryResult visit(AlterSequence alterSequence, S context) {
        throwUnsupported(alterSequence);
        return null;
    }

    @Override
    public void visit(AlterSequence alterSequence) {
        StatementVisitor.super.visit(alterSequence);
    }

    @Override
    public <S> QueryResult visit(CreateFunctionalStatement createFunctionalStatement, S context) {
        throwUnsupported(createFunctionalStatement);
        return null;
    }

    @Override
    public void visit(CreateFunctionalStatement createFunctionalStatement) {
        StatementVisitor.super.visit(createFunctionalStatement);
    }

    @Override
    public <S> QueryResult visit(ShowTablesStatement showTables, S context) {
        throwUnsupported(showTables);
        return null;
    }

    @Override
    public void visit(ShowTablesStatement showTables) {
        StatementVisitor.super.visit(showTables);
    }

    @Override
    public <S> QueryResult visit(CreateSynonym createSynonym, S context) {
        throwUnsupported(createSynonym);
        return null;
    }

    @Override
    public void visit(CreateSynonym createSynonym) {
        StatementVisitor.super.visit(createSynonym);
    }

    @Override
    public <S> QueryResult visit(SavepointStatement savepointStatement, S context) {
        return null;
    }

    @Override
    public void visit(SavepointStatement savepointStatement) {
        StatementVisitor.super.visit(savepointStatement);
    }

    @Override
    public <S> QueryResult visit(RollbackStatement rollbackStatement, S context) {

        return null;
    }

    @Override
    public void visit(RollbackStatement rollbackStatement) {
        StatementVisitor.super.visit(rollbackStatement);
    }

    @Override
    public <S> QueryResult visit(AlterSession alterSession, S context) {

        return null;
    }

    @Override
    public void visit(AlterSession alterSession) {
        StatementVisitor.super.visit(alterSession);
    }

    @Override
    public <S> QueryResult visit(IfElseStatement ifElseStatement, S context) {
        ifElseStatement.getIfStatement().accept(this, context);
        if (ifElseStatement.getElseStatement() != null) {
            ifElseStatement.getElseStatement().accept(this, context);
        }
        return null;
    }

    @Override
    public void visit(IfElseStatement ifElseStatement) {
        StatementVisitor.super.visit(ifElseStatement);
    }

    @Override
    public <S> QueryResult visit(RenameTableStatement renameTableStatement, S context) {
        for (Map.Entry<Table, Table> e : renameTableStatement.getTableNames()) {
            e.getKey().accept(this, context);
            e.getValue().accept(this, context);
        }
        return null;
    }

    @Override
    public void visit(RenameTableStatement renameTableStatement) {
        StatementVisitor.super.visit(renameTableStatement);
    }

    @Override
    public <S> QueryResult visit(PurgeStatement purgeStatement, S context) {
        if (purgeStatement.getPurgeObjectType() == PurgeObjectType.TABLE) {
            ((Table) purgeStatement.getObject()).accept(this, context);
        }
        return null;
    }

    @Override
    public void visit(PurgeStatement purgeStatement) {
        StatementVisitor.super.visit(purgeStatement);
    }

    @Override
    public <S> QueryResult visit(AlterSystemStatement alterSystemStatement, S context) {
        return null;
    }

    @Override
    public void visit(AlterSystemStatement alterSystemStatement) {
        StatementVisitor.super.visit(alterSystemStatement);
    }

    @Override
    public <S> QueryResult visit(UnsupportedStatement unsupportedStatement, S context) {
        return null;
    }

    @Override
    public void visit(UnsupportedStatement unsupportedStatement) {
        StatementVisitor.super.visit(unsupportedStatement);
    }

    @Override
    public <S> QueryResult visit(Import imprt, S context) {
        throwUnsupported(imprt);
        return null;
    }

    @Override
    public void visit(Import imprt) {
        StatementVisitor.super.visit(imprt);
    }

    @Override
    public <S> QueryResult visit(Export export, S context) {
        throwUnsupported(export);
        return null;
    }

    @Override
    public void visit(Export export) {
        StatementVisitor.super.visit(export);
    }

    @Override
    public <S> QueryResult visit(LockStatement lock, S context) {
        lock.getTable().accept(this);
        return null;
    }

    @Override
    public void visit(LockStatement lock) {
        StatementVisitor.super.visit(lock);
    }

    @Override
    public <S> QueryResult visit(CreatePolicy createPolicy, S context) {
        if (createPolicy.getTable() != null) {
            visit(createPolicy.getTable(), context);
        }

        if (createPolicy.getUsingExpression() != null) {
            createPolicy.getUsingExpression().accept(valueVisitor, context);
        }

        if (createPolicy.getWithCheckExpression() != null) {
            createPolicy.getWithCheckExpression().accept(valueVisitor, context);
        }

        return null;
    }

    @Override
    public void visit(CreatePolicy createPolicy) {
        StatementVisitor.super.visit(createPolicy);
    }
}
