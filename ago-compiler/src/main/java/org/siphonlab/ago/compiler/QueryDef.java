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
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.invoke.Invoke;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.parser.AgoLexer;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.siphonlab.ago.compiler.sql.CodeGenerator;
import org.siphonlab.ago.compiler.sql.QueryResult;
import org.siphonlab.ago.compiler.sql.SchemaLineager;
import org.siphonlab.ago.compiler.statement.ExpressionStmt;
import org.siphonlab.ago.compiler.statement.Return;
import org.siphonlab.ago.compiler.statement.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class QueryDef extends FunctionDef{

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
                var isNull = new Variable();
                isNull.setName(parameter.getName() + "IsNull");
                isNull.setType(root.BOOLEAN());
                isNull.setOwnerClass(this);
                isNull.setModifiers(AgoClass.PRIVATE);
                this.addLocalVariable(isNull);
            }
        }

        // put to each dialect later
        var sql = new Variable();
        sql.setName("sql");
        sql.setType(root.STRING());
        sql.setOwnerClass(this);
        sql.setModifiers(AgoClass.PRIVATE);
        this.addLocalVariable(sql);

        var args = new Variable();
        args.setName("args");
        args.setType(queryArgs);
        args.setOwnerClass(this);
        args.setModifiers(AgoClass.PRIVATE);
        this.addLocalVariable(args);
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
                buildArgs(schemaLineager.getBindParameters());
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
        for (QueryResult.ColumnDef column : queryResult.getColumns()) {
            var field = new Field(this.queryResult, column.getName(), null);
            field.setModifiers(AgoClass.PUBLIC | AgoClass.FINAL);
            ClassDef type = column.getType();
            if(type == null) type = getRoot().getAnyClass();
            field.setType(type);
            this.queryResult.addField(field);
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

        var codeGen = new CodeGenerator(new StringBuilder(), this, schemaLineager.getClassMapping(), schemaLineager.getFieldMapping());
        codeGen.visit((PlainSelect) schemaLineager.getStatement());
        String code = "$\"" + codeGen.getBuilder().toString() + "\"$";
        var tempStr = new AgoParser(new CommonTokenStream(new AgoLexer(CharStreams.fromString(code)))).templateStringLiteral();

        List<Statement> statements = new ArrayList<>();
        BlockCompiler blockCompiler = new BlockCompiler(this.getUnit(), this, Collections.emptyList());
        Expression templated = blockCompiler.templateString(tempStr);

        Var.LocalVar sql = new Var.LocalVar(this, getVariable("sql"), Var.LocalVar.VarMode.Existed);
        statements.add(new ExpressionStmt(this, assign(sql, templated)));

        ClassDef execQuery = getRoot().findByFullname("executeQuery#");
        var execQueryInstantiation = this.getOrCreateGenericInstantiationClassDef(execQuery, new ClassRefLiteral[]{this.getQueryResult().toClassRefLiteral()}, null);
        registerConcreteType(execQueryInstantiation);

        Expression queryArgs;
        if(!this.queryArgs.getFields().isEmpty()) {
            Var.LocalVar args = new Var.LocalVar(this, getVariable("args"), Var.LocalVar.VarMode.Existed);
            statements.add(new ExpressionStmt(this, assign(args, new Creator(this, ClassUnder.create(this, new Scope.Local(this), this.queryArgs), Collections.emptyList(), null))));
            for (Variable parameter : schemaLineager.getBindParameters()) {
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
}
