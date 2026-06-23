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
package org.siphonlab.ago.compiler;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.array.ArrayLiteral;
import org.siphonlab.ago.compiler.expression.invoke.Invoke;
import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;
import org.siphonlab.ago.compiler.expression.logic.OrExpr;
import org.siphonlab.ago.compiler.parser.AgoLexer;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.siphonlab.ago.compiler.sql.*;
import org.siphonlab.ago.compiler.statement.ExpressionStmt;
import org.siphonlab.ago.compiler.statement.Return;
import org.siphonlab.ago.compiler.statement.Statement;
import org.siphonlab.ago.opcode.logic.Or;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class QueryDef extends FunctionDef implements ManualCreatedFunction{

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryDef.class);

    private AgoParser.QueryDeclarationContext queryDeclaration;

    private ClassDef queryResult;
    private ClassDef queryArgs;
    private SchemaLineager schemaLineager;

    public QueryDef(Root root, String name, AgoParser.QueryDeclarationContext queryDeclaration) {
        super(root, name, null);
        this.queryDeclaration = queryDeclaration;
        createSubClasses();
    }

    public QueryDef(Root root, String name, AgoParser.QueryDeclarationContext queryDeclaration, int modifiers) {
        super(root, name, null, modifiers);
        this.queryDeclaration = queryDeclaration;
        createSubClasses();
    }

    private void createSubClasses(){
        // TODO for DML, the result type is int
        var result = new ClassDef(this.getRoot(), "Result");
        result.setModifiers(AgoClass.PUBLIC | AgoClass.FINAL);
        this.addChild(result);
        this.queryResult = result;
        result.setCompilingStage(CompilingStage.AllocateSlots);

        var args = new ClassDef(this.getRoot(), "Args");
        args.setModifiers(AgoClass.PUBLIC | AgoClass.FINAL);
        this.addChild(args);
        this.queryArgs = args;
        args.setCompilingStage(CompilingStage.AllocateSlots);
    }

    public ClassDef getQueryResult() {
        return queryResult;
    }

    public ClassDef getQueryArgs() {
        return queryArgs;
    }

    @Override
    public void resolveHierarchicalClasses() throws CompilationError {
        super.resolveHierarchicalClasses();
        queryResult.setSuperClass(root.findByFullname("QueryResult"));
        queryArgs.setSuperClass(root.getObjectClass());
    }

    @Override
    public AgoParser.GenericTypeParametersContext getGenericTypeParametersContextAST() {
        return queryDeclaration.genericTypeParameters();
    }

    @Override
    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;
        if(this.isInGenericInstantiation()){
            this.nextCompilingStage(CompilingStage.ValidateHierarchy);
            return true;
        }

        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: parse function fields".formatted(this));

        if(!executeParseFieldsOfHierarchyClasses()) return false;

        var formalParameters = queryDeclaration.formalParameters();
        unit.parseFormalParameters(this, formalParameters);
        this.processFieldParameters();
        this.createDefaultValueFunForParameters();

        var it = getOrCreateGenericInstantiationClassDef(getRoot().findByFullname("QueryResultIterator"), new ClassRefLiteral[]{queryResult.toClassRefLiteral()}, null);
        this.setResultType(it);
        this.registerConcreteType(it);

//        parseThrows(this.methodDecl.throwsPhrase());
        createLocalVarsForQuery();

        this.setCompilingStage(CompilingStage.InheritsFields);      // skip ValidateHierarchy
        return true;
    }

    private void createLocalVarsForQuery() {
        for (Parameter parameter : this.getParameters()) {
            if(parameter.getType() instanceof NullableClassDef nullableClassDef) {
                declareVariable(parameter.getName() + "IsNull", root.BOOLEAN(), AgoClass.PRIVATE);
            }
        }

        // put to each dialect later
        declareVariable("sql", root.STRING(), AgoClass.PRIVATE);

        declareVariable("args", queryArgs, AgoClass.PRIVATE);
    }

    @Override
    public void inheritsFields() throws CompilationError {
        if(this.isInGenericInstantiation()) {
            this.instantiateFields();
            return;
        }
        if(this.compilingStage == CompilingStage.InheritsFields) {
            if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: parse query fields".formatted(this));

            AgoParser.SqlBlockContext sqlBlock = this.queryDeclaration.sqlBlock().getFirst();
            try {
                String sql = sqlBlock.SQL_ATOM().getFirst().getText();
                this.schemaLineager = new SchemaLineager(this);
                var queryResult = schemaLineager.resolve(CCJSqlParserUtil.parse(sql));
                buildQueryResult(queryResult);
                SymbolMapping symbolMapping = schemaLineager.getSymbolMapping();
                buildArgs(symbolMapping.getBindParameters());
                buildSortOutputtedVariables(symbolMapping);
            } catch (Exception e) {
                throw new SyntaxError(e.getMessage(), getUnit().sourceLocation(sqlBlock));
            }

            setCompilingStage(CompilingStage.ValidateNewFunctions);
        }
    }

    private void buildArgs(Set<Variable> bindParameters) {
        for (Variable variable : bindParameters) {
            var field = new Field(this.queryArgs, variable.getName(), null);
            field.setModifiers(AgoClass.PUBLIC);
            ClassDef type = variable.getType();
            field.setType(type);
            this.queryArgs.addField(field);
        }
    }

    private void buildQueryResult(QueryResult queryResult) {
        for (QueryResult.ColumnDesc column : queryResult.getColumns()) {
            var field = new Field(this.queryResult, column.getName(), null);
            field.setModifiers(AgoClass.PUBLIC | AgoClass.FINAL);
            ClassDef type = column.getType();
            if(type == null) type = getRoot().getAnyClass();
            field.setType(type);
            this.queryResult.addField(field);
        }
    }

    private void buildSortOutputtedVariables(SymbolMapping symbolMapping) {
        // sort functions, each select a function
        for (Map.Entry<Select, OrderByDesc> entry : symbolMapping.getOrderByClauses().entrySet()) {
            var select = entry.getKey();
            var orderByDesc = entry.getValue();

            if(!orderByDesc.isMustOutputOrderBy()) {
                var isOrderByOutputted = this.declareVariable(orderByDesc.composeIsOrderByOutputtedVariableName(), getRoot().BOOLEAN(), AgoClass.PRIVATE);
                orderByDesc.setIsOrderByOutputted(isOrderByOutputted);
            }
        }
    }

    @Override
    public void inheritsChildClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.InheritsInnerClasses) return;

        super.inheritsChildClasses();

        // sort functions, each select a function
        var symbolMapping = schemaLineager.getSymbolMapping();
        for (Map.Entry<Select, OrderByDesc> entry : symbolMapping.getOrderByClauses().entrySet()) {
            var select = entry.getKey();
            var orderByDesc = entry.getValue();

            FunctionDef functionDef = generateSortFunction(symbolMapping, select, orderByDesc);
            orderByDesc.setSortMappingFunction(functionDef);
        }
    }



    @Override
    public void compileBody() throws CompilationError {
        if(this.getCompilingStage() != CompilingStage.CompileMethodBody) return;
        if(this.isGenericInstantiation()){
            this.nextCompilingStage(CompilingStage.Compiled);
            return;
        }

        /*
            var nameIsNull as boolean = (name == null);
            var key = dialect + nameIsNull;
            var existed = cache.get(key);
            if(existed != null) return existed;
            if(dialect == 'pg'){
                var s = pg(name != null);
                cache.put(key, s)
                return s;
            }

            fun pg() as string{
                var sql = $"
                    select ${mapColumn<User>('id')}, ${mapColumn<User>('name')}
                    from ${mapTable<User>()}
                "$;
                if(!nameIsNUll){
                    sql += $"where ${mapColumn<User>('name')} = ${name}"$;
                }
                return sql;
            }
         */

        List<Statement> statements = new ArrayList<>();
        for (Parameter parameter : this.getParameters()) {
            if(parameter.getType() instanceof NullableClassDef nullableClassDef) {
                var isNull = getVariable(parameter.getName() + "IsNull") ;
                statements.add(new ExpressionStmt(this, assign(new Var.LocalVar(this, isNull, Var.LocalVar.VarMode.Existed),
                        new Equals(this, Var.of(this, new Scope.Local(this), parameter), getRoot().nullLiteral(), Equals.Type.Equals))));
            }
        }
        SymbolMapping symbolMapping = schemaLineager.getSymbolMapping();

        for (Map.Entry<Select, OrderByDesc> entry : symbolMapping.getOrderByClauses().entrySet()) {
            var orderByDesc = entry.getValue();

            if(!orderByDesc.isMustOutputOrderBy()) {
                statements.add(new ExpressionStmt(this,
                        assign(new Var.LocalVar(this, orderByDesc.getIsOrderByOutputted(), Var.LocalVar.VarMode.Existed),
                                new BooleanLiteral(getRoot().BOOLEAN(), false)).transform()));
            }
        }

        var codeGen = new CodeGenerator(new StringBuilder(), this, symbolMapping);
        codeGen.visit((PlainSelect) schemaLineager.getStatement());
        String code = "$\"" + codeGen.getBuilder().toString() + "\"$";
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("%s transformed sql code: %s".formatted(this, code));
        }
        var tempStr = new AgoParser(new CommonTokenStream(new AgoLexer(CharStreams.fromString(code)))).templateStringLiteral();

        BlockCompiler blockCompiler = new BlockCompiler(this.getUnit(), this, Collections.emptyList());
        Expression templated = blockCompiler.templateString(tempStr);

        Var.LocalVar sql = new Var.LocalVar(this, getVariable("sql"), Var.LocalVar.VarMode.Existed);
        statements.add(new ExpressionStmt(this, assign(sql, templated)));

        ClassDef execQuery = getRoot().findByFullname("executeQuery#");
        var execQueryInstantiation = this.getOrCreateGenericInstantiationClassDef(execQuery, new ClassRefLiteral[]{this.getQueryResult().toClassRefLiteral()}, null);
        registerConcreteType(execQueryInstantiation);

        // init query args
        Expression queryArgs;
        if(!this.queryArgs.getFields().isEmpty()) {
            Var.LocalVar args = new Var.LocalVar(this, getVariable("args"), Var.LocalVar.VarMode.Existed);
            statements.add(new ExpressionStmt(this, assign(args, new Creator(this, ClassUnder.create(this, new Scope.Local(this), this.queryArgs), Collections.emptyList(), null))));
            for (Variable parameter : symbolMapping.getBindParameters()) {
                Var.Field field = new Var.Field(this, args, this.queryArgs.getFields().get(parameter.getName()));
                Var value;
                if(parameter instanceof Field f){
                    value = new Var.Field(this, new Scope.Local(this), f);
                } else {
                    value = new Var.LocalVar(this, parameter, Var.LocalVar.VarMode.Existed);
                }
                statements.add(new ExpressionStmt(this, assign(field, value)));
            }
            queryArgs = args;
        } else {
            queryArgs = getRoot().nullLiteral();
        }


        statements.add(new Return(this, invoke(Invoke.InvokeMode.Invoke,
                    new ConstClass(execQueryInstantiation),
                    List.of(sql, queryArgs), unit.sourceLocation(this.queryDeclaration)
                )));

        blockCompiler.compileExpressions(statements.stream().map(st -> (Expression)st).toList());

        this.nextCompilingStage(CompilingStage.Compiled);   // Compiled
    }

    private FunctionDef generateSortFunction(SymbolMapping symbolMapping, Select select, OrderByDesc orderByDesc) {
        QueryResult queryResult = symbolMapping.getMappedSelect(select);
        QueryScope scope = queryResult.getScope();
        StringBuilder funName = new StringBuilder();
        for (Map.Entry<String, QueryResult> entry : scope.getNames().entrySet()) {
            String name = entry.getKey();
            funName.append(name).append('_');
        }
        funName.append(orderByDesc.getId()).append("_scope_sort");       // u_scope_1_sort()

        var fun = new SortDef(root, funName.toString(), queryResult);
        this.addChild(fun);
        return fun;
    }


    class SortDef extends FunctionDef implements ManualCreatedFunction{

        private final QueryResult queryResult;

        public SortDef(Root root, String name, QueryResult queryResult) {
            super(root, name, null);
            this.queryResult = queryResult;
            this.setCompilingStage(CompilingStage.ParseFields);
        }

        @Override
        public boolean parseFields() throws CompilationError {
            if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
            if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;
            if(this.isInGenericInstantiation()){
                this.nextCompilingStage(CompilingStage.ValidateHierarchy);
                return true;
            }
            this.declareParameter("sort", root.findByFullname("Sort"), AgoClass.PRIVATE);
            this.setResultType(this.getOrCreateNullableType(getRoot().STRING(), null));

            this.createFunctionInterface();
            this.createFieldsOfTrait();

            this.setCompilingStage(CompilingStage.AllocateSlots);
            return true;
        }

        @Override
        public void compileBody() throws CompilationError {
            if(this.getCompilingStage() != CompilingStage.CompileMethodBody) return;
            if(this.isGenericInstantiation()){
                this.nextCompilingStage(CompilingStage.Compiled);
                return;
            }

            var scope = queryResult.getScope();
            Expression expr = null;
            Var sort = Var.of(this, new Scope.Local(this), this.getParameters().getFirst());
            for (Map.Entry<String, QueryResult> entry : scope.getNames().entrySet()) {
                String name = entry.getKey();
                QueryResult qr = entry.getValue();
                Invoke invokeSort;
                if(qr instanceof TableResult tableResult){
                    // tableSortScope<User>('u', sort)
                    FunctionDef tableSortScope = getRoot().findByFullname("tableSortScope#");
                    var instantiated = this.getOrCreateGenericInstantiationClassDef(tableSortScope, new ClassRefLiteral[]{
                            tableResult.getClassDef().toClassRefLiteral()
                    }, null);
                    this.registerConcreteType(instantiated);
                    invokeSort = invoke(Invoke.InvokeMode.Invoke, new ConstClass(instantiated),
                            List.of(new StringLiteral(root.STRING(), name),sort),
                            QueryDef.this.getSourceLocation());
                } else {
                    // querySortScope('q', ['col1', 'col2', 'col3', ...], sort)
                    var columns = qr.getColumns().stream().map(QueryResult.ColumnDesc::getName)
                            .map(s -> (Expression)new StringLiteral(root.STRING(), s)).toList();
                    invokeSort = invoke(Invoke.InvokeMode.Invoke, new ConstClass(root.findByFullname("querySortScope#")),
                            List.of(
                                    new StringLiteral(root.STRING(), name),
                                    new ArrayLiteral(this, getOrCreateArrayType(root.STRING(), null), columns),
                                    sort
                            ),
                            QueryDef.this.getSourceLocation());
                }
                if(expr == null){
                    expr = invokeSort;
                } else {
                    expr = new OrExpr(this, expr, invokeSort);
                }
            }
            // the output cols of select
            var columns = this.queryResult.getColumns().stream().map(QueryResult.ColumnDesc::getName)
                    .map(s -> (Expression)new StringLiteral(root.STRING(), s)).toList();
            var querySortSelect = invoke(Invoke.InvokeMode.Invoke, new ConstClass(root.findByFullname("querySortSelect#")),
                    List.of(
                            new ArrayLiteral(this, getOrCreateArrayType(root.STRING(), null), columns),
                            sort
                    ),
                    QueryDef.this.getSourceLocation());
            if(expr == null){
                expr = querySortSelect;
            } else {
                expr = new OrExpr(this, expr, querySortSelect);
            }


            List<Expression> exprs = List.of(new Return(this, expr.transform()));

            new BlockCompiler(QueryDef.this.getUnit(), this, Collections.emptyList()).compileExpressions(exprs);

            this.nextCompilingStage(CompilingStage.Compiled);   // Compiled

        }
    }
}
