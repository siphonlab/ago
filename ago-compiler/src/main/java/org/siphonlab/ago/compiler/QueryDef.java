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
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.config.Lex;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.babel.SqlBabelParserImpl;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.siphonlab.ago.compiler.sql.EntitySchema;
import org.siphonlab.ago.compiler.sql.SchemaLineager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryDef extends FunctionDef{

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryDef.class);

    private AgoParser.QueryDeclarationContext queryDeclaration;

    private ClassDef queryResult;
    private ClassDef queryArgs;

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

//        parseThrows(this.methodDecl.throwsPhrase());

        this.setCompilingStage(CompilingStage.InheritsFields);      // skip ValidateHierarchy
        return true;
    }

    @Override
    public void inheritsFields() throws CompilationError {
        if(this.isInGenericInstantiation()) {
            this.instantiateFields();
            return;
        }
        if(this.compilingStage == CompilingStage.InheritsFields) {
            if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: parse query fields".formatted(this));

            var schema = new EntitySchema(this);
            Planner planner = Frameworks.getPlanner(
                    Frameworks.newConfigBuilder()
                            .defaultSchema(schema)
                            .parserConfig(SqlParser.config()
                                            .withCaseSensitive(true)
//                                .withQuotedCasing(Casing.UNCHANGED)
//                                .withLex(Lex.SQL_SERVER)
                                            .withParserFactory(SqlBabelParserImpl.FACTORY)
                                            .withCharLiteralStyles(java.util.Collections.emptySet())
                            )
                            .build());

            SqlNode root;
            AgoParser.SqlBlockContext sqlBlock = this.queryDeclaration.sqlBlock().getFirst();
            try {
                String sql = sqlBlock.SQL_ATOM().getFirst().getText();
//            root = planner.parse(sql);
//
//            SqlNode validated = planner.validate(root);
//            RelNode rel = planner.rel(validated).rel;
//            System.out.println(RelOptUtil.toString(rel));
                var r = new SchemaLineager(this).resolve(CCJSqlParserUtil.parse(sql));
                System.out.println(r);
            } catch (Exception e) {
                throw new SyntaxError(e.getMessage(), getUnit().sourceLocation(sqlBlock));
            }

            setCompilingStage(CompilingStage.ValidateNewFunctions);
        }
    }

}
