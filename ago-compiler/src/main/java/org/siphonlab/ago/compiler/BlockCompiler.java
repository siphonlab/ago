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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.array.*;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;
import org.siphonlab.ago.compiler.expression.logic.*;
import org.siphonlab.ago.compiler.expression.math.ArithmeticExpr;
import org.siphonlab.ago.compiler.expression.math.Neg;
import org.siphonlab.ago.compiler.expression.math.Pos;
import org.siphonlab.ago.compiler.expression.math.SelfArithmetic;
import org.siphonlab.ago.compiler.generic.ClassIntervalClassDef;
import org.siphonlab.ago.compiler.resolvepath.NamePathResolver;
import org.siphonlab.ago.compiler.statement.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.siphonlab.ago.compiler.ClassDef.findCommonType;
import static org.siphonlab.ago.compiler.Unit.extractType;
import static org.siphonlab.ago.compiler.Unit.extractTypeIfPossible;
import static org.siphonlab.ago.compiler.expression.Assign.processBoundClass;
import static org.siphonlab.ago.compiler.parser.AgoParser.*;

public class BlockCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(BlockCompiler.class);

    private final Unit unit;
    private final FunctionDef functionDef;
    private final SlotsAllocator slotsAllocator;
    private final List<BlockStatementContext> blockStatements;

    private final CodeBuffer code;

    final List<Expression> compiledStatements = new ArrayList<>();
    int nextLabelId = 0;

    private List<ClassDef> handledExceptions = new LinkedList<>();

    public BlockCompiler(Unit unit, FunctionDef functionDef, List<BlockStatementContext> blockStatements) {
        this.unit = unit;
        this.functionDef = functionDef;
        this.blockStatements = blockStatements;
        this.slotsAllocator = functionDef.getSlotsAllocator();
        this.code = new CodeBuffer();
        this.handledExceptions.addAll(functionDef.getThrowsExceptions());
    }

    public FunctionDef getFunctionDef() {
        return functionDef;
    }

    public SlotsAllocator getSlotsAllocator() {
        return slotsAllocator;
    }

    public CodeBuffer getCode() {
        return code;
    }

    public void compile() throws CompilationError {
        if(functionDef instanceof ConstructorDef constructorDef){
            compileInitializerBlockForConstructor(constructorDef.getParentClass());
        }
        if (this.functionDef.getConstructor() != null) {
            // trait may create constructor for functions
            ConstructorDef constructor = this.functionDef.getConstructor();
            if (constructor != null) {
                var c = ClassUnder.create(new Scope(0, functionDef), constructor);
                var constructorInvocation = new Invoke(Invoke.InvokeMode.Invoke, c, Collections.emptyList(), functionDef.getSourceLocation()).setSourceLocation(functionDef.getSourceLocation()).transform();
                compiledStatements.add(constructorInvocation);
            }
        }
        // convert to expressions
        for (BlockStatementContext blockStatement : blockStatements) {
            Statement statement = blockStatement(blockStatement);
            if(statement != null) {
                statement.setSourceLocation(unit.sourceLocation(blockStatement));
                this.compiledStatements.add(statement);
            }
        }

        if(LOGGER.isDebugEnabled()) LOGGER.debug("generate code for " + functionDef.getFullname());
        for (int i = 0; i < compiledStatements.size(); i++) {
            var stmt = compiledStatements.get(i);
            compiledStatements.set(i, stmt = stmt.transform());
            if (LOGGER.isDebugEnabled()) LOGGER.debug("\t" + stmt);
        }
        // default return statement for `void`
        if(functionDef.getResultType() == null || functionDef.getResultType().getTypeCode() == TypeCode.VOID){
            if(compiledStatements.isEmpty() || !(compiledStatements.getLast() instanceof Return)){
                compiledStatements.add(new Return());
            }
        }
        // generate code
        compileExpressions(compiledStatements);
    }

    private Statement blockStatement(BlockStatementContext blockStatement) throws CompilationError {
        if(blockStatement instanceof DefaultBlockStmtContext defaultBlockStmt) {
            StatementContext statement = defaultBlockStmt.statement();
            return statement(statement);
        } else if(blockStatement instanceof LocalVarDeclContext localVarDecl) {
            LocalVariableDeclarationContext localVariableDeclaration = localVarDecl.localVariableDeclaration();
            return (visitLocalVariableDeclaration(localVariableDeclaration));
        } else if(blockStatement instanceof LocalTypeDeclContext localTypeDeclContext){
            // already handled
            return null;
        } else {
            // localTypeDeclaration
            throw new UnsupportedOperationException("TODO");
        }
    }

    private Statement statement(StatementContext statement) throws CompilationError {
        if (statement instanceof ReturnStmtContext returnStmt) {
            return returnStmt(returnStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if (statement instanceof ExpressionStmtContext expressionStmt) {
            var expression = expression(expressionStmt.expressionStatement().expression());
            return new ExpressionStmt(expression).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof BlockStmtContext block) {
            return this.blockStmt(block);
        } else if(statement instanceof EmptyStmtContext emptyStmt){
            return new EmptyStmt().setSourceLocation(unit.sourceLocation(emptyStmt));
        } else if(statement instanceof IfStmtContext ifStmt){
            return this.ifStmt(ifStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof ForStmtContext forStmt){
            return this.forStmt(forStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof WhileStmtContext whileStmt){
            return this.whileStmt(whileStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof DoWhileStmtContext doWhileStmt){
            return this.doWhileStmt(doWhileStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof SwitchStmtContext switchStmt){
            return this.switchStmt(switchStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof TryStmtContext tryStmt){
            return this.tryStmt(tryStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof ThrowStmtContext throwStmt){
            return this.throwStmt(throwStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof BreakStmtContext breakStmt){
            return this.breakStmt(breakStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof ContinueStmtContext continueStmt){
            return this.continueStmt(continueStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof YieldStmtContext yieldStmt){

        } else if(statement instanceof WithStmtContext withStmt){
            return this.withStmt(withStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof ViaStmtContext viaStmt) {
            return viaStmt(viaStmt);
        } else if (statement instanceof AwaitStmtContext awaitStmt) {
            return this.awaitStmt();
        } else if(statement instanceof AsyncInvokeFunctorStmtContext asyncInvokeFunctorStmt){
            return asyncInvokeFunctorStmt(asyncInvokeFunctorStmt);
        } else {
        }
        throw new UnsupportedOperationException();
    }

    private AwaitStmt awaitStmt() {
        return new AwaitStmt();
    }

    private Return returnStmt(ReturnStmtContext returnStmt) throws CompilationError {
        if(returnStmt.expression() == null){
            if(functionDef.getResultType() != PrimitiveClassDef.VOID){
                throw unit.typeError(returnStmt, "'%s' result expected".formatted(functionDef.getResultType()));
            }
            return new Return();
        } else {
            return new Return(new Cast(expression(returnStmt.expression()), functionDef.getResultType()));
        }
    }

    private void compileInitializerBlockForConstructor(ClassDef ownerClass) throws CompilationError {
        boolean hasFieldParameters = functionDef instanceof ConstructorDef c && c.hasFieldParameters();
        if(ownerClass.hasFieldInitializerOrTrait() || hasFieldParameters) {
            var stmts = new ArrayList<Expression>();

            Map<Field, Parameter> pfMap;
            if(hasFieldParameters){
                pfMap = new HashMap<>();
                for (Parameter parameter : functionDef.getParameters()) {
                    if(parameter.isField()){
                        pfMap.put(ownerClass.fields.get(parameter.getName()), parameter);
                    }
                }
            } else {
                pfMap = null;
            }

            // initializers, depends on the declaration sequence
            var me = new Scope(1, ownerClass);
            var varMe = (Var.LocalVar) me.visit(this);
            this.lockRegister(varMe);
            for (Field field : ownerClass.fields.values()) {
                if(hasFieldParameters) {
                    var parameter = pfMap.get(field);
                    if(parameter != null){      // assign to this parameter instead of the default initializer expr
                        Var.Field assignee = new Var.Field(varMe, field).setSourceLocation(unit.sourceLocation(field.getDeclaration()));
                        var parameterExpr = new Var.LocalVar(parameter, Var.LocalVar.VarMode.Existed);
                        var assign = Assign.to(assignee, parameterExpr).setSourceLocation(unit.sourceLocation(parameter.getDeclaration()));
                        stmts.add(assign);
                        continue;
                    }
                }
                if (field.getInitializer() != null) {
                    var valueExpr = expression(field.getInitializer());
                    Var.Field assignee = new Var.Field(varMe, field).setSourceLocation(unit.sourceLocation(field.getDeclaration()));
                    var assign = Assign.to(assignee, valueExpr).setSourceLocation(assignee.getSourceLocation());
                    stmts.add(assign);
                }
            }

            for (var entry : ownerClass.traitFields.entrySet()) {
                Field field = entry.getValue();
                ClassDef trait = field.getType();   // field type is TraitDefInScope, the key is TraitDef
                TraitCreator traitCreator;
                Var.Field traitField = new Var.Field(varMe, field).setSourceLocation(unit.sourceLocation(field.getDeclaration()));
                Expression bindPermit;
                if (trait.getPermitClass() != null && trait.getPermitClass() != trait.getRoot().getObjectClass()) {
                    var permitFld = new Var.Field(traitField, trait.getFieldForPermitClass());
                    bindPermit = Assign.to(permitFld, varMe);
                } else {
                    bindPermit = null;
                }
                // create a local var to accept the trait instance from `new Trait()`
                // for traitField is a field, can't accept creator result, there must be a temp var
                // even if Assign.to(field, creator), it will create a temp var too
                // now I make the temp var explicit, so that I can bind @trait_field and @permit_field before invoke trait constructor
                var createdTrait = this.acquireTempVar(new SomeInstance(trait));

                var ls = new ArrayList<Statement>();
                ls.add(new ExpressionStmt( Assign.to(traitField, createdTrait).transform()));
                if(bindPermit != null) ls.add(new ExpressionStmt(bindPermit));

                traitCreator = new TraitCreator(new ConstClass(trait), new BlockStmt(ls), unit.sourceLocation(field.getDeclaration()));
                stmts.add(Assign.to(createdTrait, traitCreator).setSourceLocation(traitField.getSourceLocation()));
            }

            if(!stmts.isEmpty()) {
                for(var stmt : stmts){
                    stmt.termVisit(this);
                }
            }
            this.releaseRegister(varMe);
        }
    }

    public void compileExpressions(List<Expression> compiledStatements) throws CompilationError {
        for(var stmt : compiledStatements){
            stmt.termVisit(this);
        }

        int[] compiledCode = code.toArray();
        functionDef.setBody(compiledCode);
        functionDef.setCompilingStage(CompilingStage.Compiled);
        functionDef.setSourceMap(code.getSourceMapEntries());

        if (LOGGER.isDebugEnabled()) LOGGER.debug(functionDef.getFullname());

        List<String> ls = new ArrayList<>();
        List<String> topStrings = functionDef.getTopStrings();
        for (int i = 0; i < topStrings.size(); i++) {
            ls.add("\t" + i + ":\t" + topStrings.get(i));
        }
        if (LOGGER.isDebugEnabled()) LOGGER.debug(ls.stream().collect(Collectors.joining("\n")));

        var slots = functionDef.getSlotsAllocator().getSlots();
        if (LOGGER.isDebugEnabled()) LOGGER.debug(slots.stream().map(slot -> "\t" + slot.getIndex() + "\t" + slot.getName() + "\t" + slot.getTypeCode() + "\t" + slot.getClassDef()).collect(Collectors.joining("\n")));

        if (LOGGER.isDebugEnabled()) LOGGER.debug(Arrays.stream(StringUtils.split(InspectUtil.inspectCode(functionDef.getBody()), '\n')).map(s -> "\t" + s).collect(Collectors.joining("\n")));

    }

    private Var.LocalVar defineLocalVar(IdentifierContext identifier, VariableModifiersContext modifiers, ClassDef type) throws CompilationError {
        Variable variable = new Variable();
        variable.setName(identifier.getText());
        variable.setOwnerClass(functionDef);
        variable.setSourceLocation(unit.sourceLocation(identifier));
        if(modifiers != null){
            int m = Compiler.variableModifiers(unit, modifiers, Compiler.ModifierTarget.Variable);
            variable.setModifiers(m);
        }
        if(type != null) {
            variable.setType(type);
        }

        Var.LocalVar localVar = new Var.LocalVar(variable, Var.LocalVar.VarMode.ToDeclare).setSourceLocation(unit.sourceLocation(identifier));
        functionDef.addLocalVariableWithSlot(variable);
        return localVar;
    }

    private ExpressionStmt visitLocalVariableDeclaration(LocalVariableDeclarationContext localVariableDeclaration) throws CompilationError {
        Variable variable = new Variable();
        variable.setName(localVariableDeclaration.identifier().getText());
        variable.setOwnerClass(functionDef);
        variable.setSourceLocation(unit.sourceLocation(localVariableDeclaration));
        variable.setModifiers(Compiler.variableModifiers(unit, localVariableDeclaration.variableModifiers(), Compiler.ModifierTarget.Variable));

        Var.LocalVar localVar = new Var.LocalVar(variable, Var.LocalVar.VarMode.ToDeclare);

        VariableInitializerContext initializer = localVariableDeclaration.variableInitializer();
        ClassDef type = null;
        if(localVariableDeclaration.typeOfVariable() != null) {
            type = unit.parseType(functionDef, localVariableDeclaration.typeOfVariable(), false);
            Compiler.processClassTillStage(type,CompilingStage.AllocateSlots);
            variable.setType(type);
            if(type.getTypeCode() == TypeCode.OBJECT) functionDef.idOfConstString(type.getFullname());
        } else {
            if(initializer == null){
                throw unit.syntaxError(localVariableDeclaration, "variable declaration has no variable type nor initializer");
            }
        }
        if(initializer != null) {
            var initializerExpr = assigner(initializer.expression(), localVar, type);
            if (type == null) {
                ClassDef inferred;
                if(initializerExpr instanceof ClassUnder
                        || initializerExpr instanceof ConstClass
                        || initializerExpr instanceof ClassOf
                ){
                    var t = initializerExpr.inferType();
                    var scopedClassIntervalClassDef = functionDef.getRoot().getOrCreateScopedClassInterval(t, t, null);
                    functionDef.registerConcreteType(scopedClassIntervalClassDef);
                    inferred = t;
                    initializerExpr = new CastToScopedClassRef(initializerExpr, scopedClassIntervalClassDef).transform();
                } else {
                    inferred = initializerExpr.inferType();
                    if (inferred == functionDef.getRoot().getAnyClass()) {
                        inferred = functionDef.getRoot().getObjectClass();
                        initializerExpr = new Cast(initializerExpr, inferred).setSourceLocation(initializerExpr.getSourceLocation()).transform();
                    }
                }
                variable.setType(inferred);
            } else {
                initializerExpr = new Cast(initializerExpr, type).setSourceLocation(initializerExpr.getSourceLocation()).transform();
            }
            functionDef.addLocalVariableWithSlot(variable);
            return new ExpressionStmt(Assign.to(localVar, initializerExpr));
        } else {
            functionDef.addLocalVariableWithSlot(variable);
            return null;
        }
    }

    Expression expression(ExpressionContext expression) throws CompilationError {
        if(expression == null) return null;
        if(expression instanceof AddSubtractExprContext addSubtractExpr){
            ArithmeticExpr.Type type = addSubtractExpr.bop.getType() == ADD ? ArithmeticExpr.Type.Add : ArithmeticExpr.Type.Substract;
            return new ArithmeticExpr(type, expression(addSubtractExpr.expression(0)), expression(addSubtractExpr.expression(1)))
                    .setSourceLocation(unit.sourceLocation(expression));
        } else if (expression instanceof PrimaryExprContext primaryExprContext){
            if(primaryExprContext.primaryExpression() instanceof LiteralExprContext literalExpr) {
                return literalExpr(literalExpr);
            } else if(primaryExprContext.primaryExpression() instanceof NamePathExprContext namePath){
                return unit.resolveNamePath(this.functionDef, namePath.namePath(), NamePathResolver.ResolveMode.ForValue);
            }
        } else if(expression instanceof MethodCallExprContext methodCallExpr){
            var methodCall = methodCallExpr.methodCall();
            return methodCall(null, methodCall);        // static instance or current instance
        } else if(expression instanceof MemberAccessExprContext memberAccessExpr){
            var left = expression(memberAccessExpr.expression());
            MethodCallContext methodCall = memberAccessExpr.methodCall();
            if(methodCall != null){
               return this.methodCall(left, methodCall);
            } else {
                var namePath = memberAccessExpr.namePath();
                var right = new NamePathResolver(NamePathResolver.ResolveMode.ForValue, unit, left.inferType(),left, (FormalNamePathContext) namePath).resolve();
                return right;
            }
        } else if(expression instanceof QuotedExprContext quotedExpr){
            return expression(quotedExpr.expression());
        } else if(expression instanceof EqualsExprContext equalsExpr){
            Equals.Type type = switch(equalsExpr.bop.getType()){
                case EQUAL -> Equals.Type.Equals;
                case NOTEQUAL -> Equals.Type.NotEquals;
//                case IDENTITY_EQUAL -> ;
//                case NOT_IDENTITY_EQUAL ->
                default -> throw new UnsupportedOperationException("'%s' not supported".formatted(equalsExpr.bop));
            };
            return new Equals(expression(equalsExpr.expression(0)), expression(equalsExpr.expression(1)), type).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof CreatorExprContext creatorExpr) {
            return creator(creatorExpr);
        } else if(expression instanceof ChainCreatorExprContext chainCreatorExpr){
            return creator(chainCreatorExpr);
        } else if(expression instanceof AssignExprContext assignExpr){
            return assign(assignExpr);
        } else if(expression instanceof CastTypeExprContext castTypeExprContext) {
            var type = unit.parseType(functionDef, castTypeExprContext.variableType(), false, false);
            ClassDef t = extractType(type);
            Compiler.processClassTillStage(t, CompilingStage.AllocateSlots);
            return new Cast(expression(castTypeExprContext.expression()), t, true);
//        } else if(expression instanceof HighPriorCastTypeExprContext highPriorCastTypeExprContext) {
//            ClassDef type;
//            if(highPriorCastTypeExprContext.primitiveType() != null){
//                type = PrimitiveClassDef.fromPrimitiveTypeAst(highPriorCastTypeExprContext.primitiveType());
//            } else {
//                Expression expr = new NamePathResolver(NamePathResolver.ResolveMode.ForTypeExpr, unit, this.functionDef, highPriorCastTypeExprContext.identifierAllowPostfix()).resolve();
//                type = extractType(expr);
//                Compiler.processClassTillStage(type, CompilingStage.AllocateSlots);
//            }
//            return new Cast(expression(highPriorCastTypeExprContext.expression()), type, true);
        } else if(expression instanceof ElementExprContext elementExpr){
            var obj = expression(elementExpr.expression(0));
            var index = expression(elementExpr.expression(1));
            Root root = functionDef.getRoot();
            if(root.getAnyArrayClass().isThatOrSuperOfThat(obj.inferType())) {
                return new ArrayElement(obj, index).setSourceLocation(unit.sourceLocation(expression));
            } else if(root.getAnyReadwriteList().isThatOrSuperOfThat(obj.inferType()) || root.getAnyReadonlyList().isThatOrSuperOfThat(obj.inferType())) {
                return new ListElement(obj, index).setSourceLocation(unit.sourceLocation(expression));
            } else if(root.getAnyReadwriteMap().isThatOrSuperOfThat(obj.inferType()) || root.getAnyReadonlyMap().isThatOrSuperOfThat(obj.inferType())){
                return new MapValue(obj, index).setSourceLocation(unit.sourceLocation(expression));
            } else {
                throw new TypeMismatchError("an array, a List or a Map expected", unit.sourceLocation(elementExpr));
            }
        } else if(expression instanceof ClassExprContext classExpr){
            //TODO
        } else if(expression instanceof WithMemberAccessExprContext withMemberAccessExprContext){
            var left = this.getCurrentWithExpr();
            NamePathContext namePathContext = withMemberAccessExprContext.namePath();
            if(namePathContext != null){
                var namePath = withMemberAccessExprContext.namePath();
                NamePathResolver namePathResolver = new NamePathResolver(NamePathResolver.ResolveMode.ForVariable, unit, left.inferType(), left, ((FormalNamePathContext) namePath));
                var r = namePathResolver.resolve();
                return r;
            } else {
                MethodCallContext methodCallContext = withMemberAccessExprContext.methodCall();
                return methodCall(left, methodCallContext);
            }
        } else if(expression instanceof SwitchExprContext switchExprContext){
            //TODO
        } else if(expression instanceof IncDecExprContext incDecExpr){
            return incDec(incDecExpr).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof PrefixExprContext prefixExpr){
            return prefixExpr(prefixExpr).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof MultiDivModExprContext multiDivModExpr){
            ArithmeticExpr.Type type = switch (multiDivModExpr.bop.getType()) {
                case MUL -> ArithmeticExpr.Type.Multi;
                case DIV -> ArithmeticExpr.Type.Div;
                case MOD -> ArithmeticExpr.Type.Mod;
                default -> throw new RuntimeException("unexpected type " + multiDivModExpr.bop);
            };
            return new ArithmeticExpr(type, expression(multiDivModExpr.expression(0)), expression(multiDivModExpr.expression(1)))
                    .setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof CompareExprContext compareExpr){
            return compareExpr(compareExpr).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof InstanceOfExprContext instanceOfExpr){
            return instanceOfExpr(instanceOfExpr).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof BitAndExprContext expr){
            return new BitOpExpr(BitOpExpr.Type.BitAnd,expression(expr.expression(0)), expression(expr.expression(1)))
                    .setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof BitXorExprContext expr){
            return new BitOpExpr(BitOpExpr.Type.BitXor,expression(expr.expression(0)), expression(expr.expression(1))).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof BitOrExprContext expr){
            return new BitOpExpr(BitOpExpr.Type.BitOr,expression(expr.expression(0)), expression(expr.expression(1))).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof AndExprContext andExpr){
            return new AndExpr(expression(andExpr.expression(0)), expression(andExpr.expression(1))).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof OrExprContext orExpr) {
            return new OrExpr(expression(orExpr.expression(0)), expression(orExpr.expression(1))).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof ShiftExprContext shiftExprContext){
            return shiftExpr(shiftExprContext).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof PostWithExprContext postWithExpr){
            return withExpr(postWithExpr).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof IfElseExprContext ifElseExpr){
            return ifElseExpr(ifElseExpr).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof LambdaExprContext lambdaExpr){          // we need name

        } else if(expression instanceof PostfixExprContext postfixExprContext){     //TODO need these?

        } else if(expression instanceof AwaitFunctorContext invokeFunctorContext) {
            return invokeFunctor(invokeFunctorContext);
        }
        throw new UnsupportedOperationException(expression.getText());
    }

    private Expression literalExpr(LiteralExprContext literalExpr) throws CompilationError {
        LiteralContext literal = literalExpr.literal();
        if(literal instanceof LArrayContext larr) {
            return arrayLiteral(larr, null, null);
        } else if(literal instanceof LTemplateStringContext lTemplateString){
            return templateString(lTemplateString);
        } else {
            return Literal.parse(literal, unit.getRoot(), unit.sourceLocation(literalExpr));
        }
    }

    private Expression templateString(LTemplateStringContext lTemplateString) throws CompilationError {
        List<Expression> expressions = new ArrayList<>();
        var sb = new StringBuilder();
        TemplateStringAtomContext startAtom = null, endAtom = null;
        var offset = lTemplateString.getStart().getCharPositionInLine() + 2;
        var newLine = false;
        var newLineWSCount = 0;
        for (var atom : lTemplateString.templateStringLiteral().templateStringAtom()) {
            ExpressionContext atomExpr = atom.expression();
            if(atomExpr != null){
                if(!sb.isEmpty()){
                    expressions.add(new StringLiteral(sb.toString()).setSourceLocation(unit.sourceLocation(startAtom, endAtom)));
                    sb.setLength(0);
                    startAtom = null;
                }
                expressions.add(this.expression(atomExpr));
            } else {
                if(startAtom == null) startAtom = atom;
                endAtom = atom;
                String text = atom.TemplateStringAtom().getText();
                for(var i=0; i<text.length(); i++){
                    var c = text.charAt(i);
                    if(newLine){
                        if(c == '\r'){
                            sb.append(c);
                            continue;
                        } else if(c == ' ' || c == '\t'){
                            newLineWSCount ++;
                            if(newLineWSCount >= offset){
                                newLine = false;
                            }
                            continue;
                        } else {
                            newLine = false;
                        }
                    } else {
                        if(c == '\n'){
                            newLine = true;
                            newLineWSCount = 0;
                        }
                    }
                    sb.append(c);
                }
            }
        }
        if(!sb.isEmpty()){
            expressions.add(new StringLiteral(LiteralParser.parseJsStringLiteral(sb.toString())).setSourceLocation(unit.sourceLocation(startAtom, endAtom)));
        }
        if(expressions.isEmpty()) return new StringLiteral("").setSourceLocation(unit.sourceLocation(lTemplateString));
        if(expressions.size() == 1) return expressions.getFirst();
        var r = expressions.getFirst();
        for (int j = 1; j < expressions.size(); j++) {
            Expression expr = expressions.get(j);
            r = new Concat(r, expr);
        }
        return r;
    }


    private Expression instanceOfExpr(InstanceOfExprContext instanceOfExpr) throws CompilationError {
        var typeExpr = unit.parseType(functionDef, instanceOfExpr.variableType(), false, false);
        var type = extractType(typeExpr);
        Compiler.processClassTillStage(type, CompilingStage.AllocateSlots);
        Var.LocalVar receiverVar;
        if(instanceOfExpr.identifier() != null){
            receiverVar = defineLocalVar(instanceOfExpr.identifier(), null, type);
        } else {
            receiverVar = null;
        }
        return new InstanceOf(expression(instanceOfExpr.expression()), type, receiverVar);
    }

    private Expression ifElseExpr(IfElseExprContext ifElseExpr) throws CompilationError {
        var ifPart = expression(ifElseExpr.ifPart);
        var elsePart = expression(ifElseExpr.elsePart);
        var cond = expression(ifElseExpr.condition);
        return new IfElseExpr(ifPart, cond, elsePart);
    }

    private Expression prefixExpr(PrefixExprContext prefixExpr) throws CompilationError {
        Expression expression = expression(prefixExpr.expression());
        switch (prefixExpr.prefix.getType()) {
            case ADD:
                return new Pos(expression);
            case SUB:
                return new Neg(expression);
            case NOT:
                return new Not(expression);
            case INC:
                return new SelfArithmetic(expression, new IntLiteral(1), SelfArithmetic.Type.Inc);
            case DEC:
                return new SelfArithmetic(expression, new IntLiteral(1), SelfArithmetic.Type.Dec);
            case BITNOT:
                return new BitNot(expression);
            default:
                throw new RuntimeException("TODO");
        }
    }

    private Expression incDec(IncDecExprContext incDecExpr) throws CompilationError {
        var expr = incDecExpr.expression();
        SelfArithmetic.Type type = incDecExpr.INC() != null ? SelfArithmetic.Type.IncPost : SelfArithmetic.Type.DecPost;
        return new SelfArithmetic(expression(expr), new IntLiteral(1), type);
    }


    private Expression assign(AssignExprContext assignExpr) throws CompilationError {
        var assignee = assignee(assignExpr.expression(0));
        var value = assigner(assignExpr.expression(1), assignee, assignee.inferType());
        int bopType = assignExpr.bop.getType();
        SourceLocation sourceLocation = unit.sourceLocation(assignExpr);
        switch (bopType) {
            case ASSIGN:
                return Assign.to((Assign.Assignee) assignee, value).setSourceLocation(sourceLocation);
            case ADD_ASSIGN:
            case SUB_ASSIGN:
            case MUL_ASSIGN:
            case DIV_ASSIGN:
            case MOD_ASSIGN:
                SelfArithmetic.Type arithType = switch (bopType){
                    case ADD_ASSIGN -> SelfArithmetic.Type.Inc;
                    case SUB_ASSIGN -> SelfArithmetic.Type.Dec;
                    case MUL_ASSIGN -> SelfArithmetic.Type.SelfMulti;
                    case DIV_ASSIGN -> SelfArithmetic.Type.SelfDiv;
                    case MOD_ASSIGN -> SelfArithmetic.Type.SelfMod;
                    default -> throw new UnsupportedOperationException("impossible");
                };
                return new SelfArithmetic(assignee, value,arithType).setSourceLocation(sourceLocation);

            case AND_ASSIGN:
                return new SelfLogicExpr(assignee, value, SelfLogicExpr.Type.And).setSourceLocation(unit.sourceLocation(assignExpr));
            case OR_ASSIGN:
                return new SelfLogicExpr(assignee, value, SelfLogicExpr.Type.Or).setSourceLocation(unit.sourceLocation(assignExpr));
            case BITAND_ASSIGN:
                return new SelfBitOpExpr(assignee,value, SelfBitOpExpr.Type.BitAnd).setSourceLocation(unit.sourceLocation(assignExpr));
            case BITOR_ASSIGN:
                return new SelfBitOpExpr(assignee,value, SelfBitOpExpr.Type.BitOr).setSourceLocation(unit.sourceLocation(assignExpr));
            case BITXOR_ASSIGN:
                return new SelfBitOpExpr(assignee,value, SelfBitOpExpr.Type.BitXor).setSourceLocation(unit.sourceLocation(assignExpr));
            case LSHIFT_ASSIGN:
                return new SelfBitShiftExpr(assignee,value, SelfBitShiftExpr.Type.LShift).setSourceLocation(unit.sourceLocation(assignExpr));
            case RSHIFT_ASSIGN:
                return new SelfBitShiftExpr(assignee,value, SelfBitShiftExpr.Type.RShift).setSourceLocation(unit.sourceLocation(assignExpr));
            case URSHIFT_ASSIGN:
                return new SelfBitShiftExpr(assignee,value, SelfBitShiftExpr.Type.URShift).setSourceLocation(unit.sourceLocation(assignExpr));
            case COPY_ASSIGN:
                ClassDef assigneeType = assignee.inferType();
                ClassDef valueType = value.inferType();
                if(assigneeType.getTypeCode() != TypeCode.OBJECT && !assigneeType.isClass()){
                    throw unit.syntaxError(assignExpr, "object type expected");
                }
                if (valueType.getTypeCode() != TypeCode.OBJECT && !valueType.isClass()) {
                    throw new TypeMismatchError("object type expected", value.getSourceLocation());
                }
                ClassDef commonType;
                if(assigneeType.isThatOrDerivedFromThat(valueType)){
                    commonType = valueType;
                } else if(assigneeType.isThatOrSuperOfThat(valueType)){
                    commonType = assigneeType;
                } else {
                    throw new TypeMismatchError("'%s' and '%s' has no explicit relation".formatted(assigneeType.getFullname(), valueType.getFullname()), value.getSourceLocation());
                }
                return new CopyAssign(assignee, value, commonType).setSourceLocation(unit.sourceLocation(assignExpr));
            case SET_VALUE:
//TODO
            default:
                throw new UnsupportedOperationException("");
        }
    }

    private Expression compareExpr(CompareExprContext compareExpr) throws CompilationError {
        Compare.Type type = switch(compareExpr.bop.getType()){
            case GT -> Compare.Type.GT;
            case LT -> Compare.Type.LT;
            case GE -> Compare.Type.GE;
            case LE -> Compare.Type.LE;
            default -> throw new UnsupportedOperationException("'%s' not supported".formatted(compareExpr.bop));
        };
        return new Compare(expression( compareExpr.expression(0)),expression(compareExpr.expression(1)), type);
    }

    private Expression shiftExpr(ShiftExprContext shiftExprContext) throws CompilationError {
        BitShiftExpr.Type type;
        if(shiftExprContext.LT().size() == 2){      // <<
            gtLtNoBroken(shiftExprContext.LT(), "<<");
            type = BitShiftExpr.Type.LShift;
        } else if(shiftExprContext.GT().size() == 2){
            gtLtNoBroken(shiftExprContext.GT(), ">>");
            type = BitShiftExpr.Type.RShift;
        } else {
            assert shiftExprContext.GT().size() == 3;
            gtLtNoBroken(shiftExprContext.GT(), ">>>");
            type = BitShiftExpr.Type.URShift;
        }
        List<ExpressionContext> expression = shiftExprContext.expression();
        return new BitShiftExpr(type, this.expression(expression.get(0)), this.expression(expression.get(1)));
    }

    private void gtLtNoBroken(List<TerminalNode> tokens, String s) throws SyntaxError {
        for(var i=0; i < tokens.size() -1; i++){
            if(tokens.get(i).getSymbol().getStopIndex() +1 != tokens.get(i + 1).getSymbol().getStartIndex()){
                throw new SyntaxError("no space allowed between '%s'".formatted(s), unit.sourceLocation(tokens.get(i)).extend(unit.sourceLocation(tokens.get(i + 1))));
            }
        }
    }

    private Expression creator(CreatorExprContext creatorExpr) throws CompilationError {
        CreatorContext creator = creatorExpr.creator();
        if(creator instanceof NormalCreatorContext normalCreatorContext) {
            var expr = unit.parseType(functionDef, normalCreatorContext.declarationType(), true, true);
            Compiler.processClassTillStage(extractTypeIfPossible(expr), CompilingStage.AllocateSlots);
            var rest = normalCreatorContext.classCreatorRest();
            if (rest != null) {
                // if want support anonymous inner class like java did, the class declaration should be handled in {Unit.parseClassDef}, not here
                return create(expr, creator, rest.arguments());
            } else {
                return create(expr, creator, null);
            }
        } else if(creator instanceof ChainingCreatorContext chainingCreator){
            ChainCreatorContext chainCreator = chainingCreator.chainCreator();
            var expr = unit.parseType(functionDef, chainCreator.declarationType(), true, true);
            Compiler.processClassTillStage(extractTypeIfPossible(expr), CompilingStage.AllocateSlots);
            var rest = chainCreator.classCreatorRest();
            if (rest != null) {
                // if want support anonymous inner class like java did, the class declaration should be handled in {Unit.parseClassDef}, not here
                return create(expr, creator, rest.arguments());
            } else {
                return create(expr, creator, null);
            }
        } else if(creator instanceof ArrayCreatorContext arrayCreator) {
            var elementType = unit.parseTypeName(functionDef, arrayCreator.declarationType().namePath(), false);
            Compiler.processClassTillStage(elementType, CompilingStage.AllocateSlots);
            var dimension = arrayCreator.LBRACK().size();
            var expressions = arrayCreator.expression();
            ArrayCreate expr = null;
            boolean freeLength = false;
            for(var i=0; i<dimension; i++){
                Expression lengthExpr;
                if(i< expressions.size()){
                    lengthExpr = expression(expressions.get(i));
                    if(freeLength){
                        throw unit.syntaxError(expressions.get(i), "unexpected array length");
                    }
                } else {
                    lengthExpr = null;
                    freeLength = true;
                }
                var arrayType = functionDef.getOrCreateArrayType(elementType, null);
                expr = new ArrayCreate(arrayType, lengthExpr);
                elementType = arrayType;
            }
            return expr;
        } else {
            throw new UnsupportedOperationException("unknown creator type " + creator);
        }
    }

    private Expression creator(ChainCreatorExprContext chainCreatorExpr) throws CompilationError {
        var current = this.expression(chainCreatorExpr.expression());
        ChainCreatorContext chainCreator = chainCreatorExpr.chainCreator();
        var expr = unit.parseType(current.inferType(), chainCreator.declarationType(), true, true);
        if (expr instanceof ClassUnder.ClassUnderScope classUnderScope) {     // if it's under scope, expr.Class
            assert ((Scope) classUnderScope.getScope()).getDepth() == 0;
            expr = ClassUnder.create(current, classUnderScope.getClassDef());
        } else {
            // i.e. class under meta, class under meta of scope
            throw new ResolveError("illegal expression for '%s' creator".formatted(chainCreator.declarationType().getText()), unit.sourceLocation(chainCreatorExpr.expression()));
        }
        Compiler.processClassTillStage(extractTypeIfPossible(expr), CompilingStage.AllocateSlots);
        var rest = chainCreator.classCreatorRest();
        TerminalNode postIdentifier = chainCreator.POST_IDENTIFIER();
        String id = postIdentifier == null ? null : "new" + postIdentifier.getText();
        if (rest != null) {
            return create(expr, chainCreatorExpr, rest.arguments(), id);
        } else {
            return create(expr, chainCreatorExpr, null, id);
        }
    }

    Expression assignee(ExpressionContext expression) throws CompilationError {
        if (expression instanceof PrimaryExprContext primaryExprContext){
            if(primaryExprContext.primaryExpression() instanceof LiteralExprContext literalExpr) {
                return literalExpr(literalExpr);
            } else if(primaryExprContext.primaryExpression() instanceof NamePathExprContext namePath){
                return unit.resolveNamePath(this.functionDef, namePath.namePath(), NamePathResolver.ResolveMode.ForVariable);
            }
        } else if(expression instanceof MemberAccessExprContext memberAccessExpr) {
            var left = expression(memberAccessExpr.expression());
            MethodCallContext methodCall = memberAccessExpr.methodCall();
            if (methodCall != null) {
                throw new SyntaxError("left side is not assignable", unit.sourceLocation(methodCall));
            } else {
                var namePath = memberAccessExpr.namePath();
                NamePathResolver namePathResolver = new NamePathResolver(NamePathResolver.ResolveMode.ForVariable, unit, left.inferType(), left, ((FormalNamePathContext) namePath));
                var r = namePathResolver.resolve();
                return r;
            }
        } else if(expression instanceof WithMemberAccessExprContext withMemberAccessExprContext){
            var left = this.getCurrentWithExpr();
            var methodCall = withMemberAccessExprContext.methodCall();
            if (methodCall != null) {
                throw new SyntaxError("left side is not assignable", unit.sourceLocation(methodCall));
            } else {
                var namePath = withMemberAccessExprContext.namePath();
                NamePathResolver namePathResolver = new NamePathResolver(NamePathResolver.ResolveMode.ForVariable, unit, left.inferType(), left, ((FormalNamePathContext) namePath));
                var r = namePathResolver.resolve();
                return r;
            }
        } else if(expression instanceof QuotedExprContext quotedExpr){
            return assignee(quotedExpr.expression());
        } else if(expression instanceof AssignExprContext assignExpr){
            return expression(assignExpr);
        } else if(expression instanceof ElementExprContext elementExpr){
            return expression(elementExpr);
        }
        throw new UnsupportedOperationException(expression.getText());
    }

    Expression assigner(ExpressionContext expression, Expression assignee, ClassDef assigneeType) throws CompilationError {
        if(expression instanceof PrimaryExprContext primaryExpr){
            if(primaryExpr.primaryExpression() instanceof LiteralExprContext literalExpr){
                LiteralContext literal = literalExpr.literal();
                // array literal and object literal can be instructed by assignee type
                if(literal instanceof LArrayContext lArrayContext){
                    return arrayLiteral(lArrayContext, assignee, assigneeType);
                } else if(literal instanceof LObjectContext objectLiteral){
                    throw new UnsupportedOperationException("object literal todo");
                }
            }
        }
        Expression value = expression(expression).transform();
        if(assigneeType == null){
            return value;
        } else {
            if(assignee instanceof Assign.Assignee a) {
                value = processBoundClass(a, value);
            }
            return new Cast(value, assigneeType).transform();
        }
    }

    private ArrayLiteral arrayLiteral(LArrayContext lArrayContext, Expression assignee, ClassDef assigneeType) throws CompilationError {
        ClassDef arrayType;
        var arrayLiteral = lArrayContext.arrayLiteral();
        if(arrayLiteral.variableType() != null){
            Expression typeExpr = unit.parseType(functionDef, arrayLiteral.variableType(), false, false);
            arrayType = extractType(typeExpr);
        } else if(assigneeType != null){
            arrayType = assigneeType;
        } else if(!arrayLiteral.elementList().isEmpty()){
            var first = arrayLiteral.elementList().arrayElement(0);
            var el = arrayElement(first, null);
            arrayType = functionDef.getOrCreateArrayType(el.inferType(), null);
        } else {
            throw unit.syntaxError(lArrayContext, "cannot predict array type");
        }
        Compiler.processClassTillStage(arrayType, CompilingStage.AllocateSlots);

        if(!functionDef.getRoot().getAnyArrayClass().isThatOrSuperOfThat(arrayType)){  //TODO allow List
            throw new TypeMismatchError("assignee type '%s' is not an array".formatted(assigneeType.getFullname()), assignee.getSourceLocation());
        }
        List<Expression> elements = new ArrayList<>();
        for (ArrayElementContext arrayElementContext : arrayLiteral.elementList().arrayElement()) {
            Expression element = arrayElement(arrayElementContext, ((ArrayClassDef) arrayType).getElementType());
            elements.add(element);
        }
        return new ArrayLiteral((ArrayClassDef) arrayType, elements).setSourceLocation(unit.sourceLocation(lArrayContext));
    }

    private Expression arrayElement(ArrayElementContext elementContext, ClassDef elementType) throws CompilationError {
        if(elementContext.expando != null){
            throw new UnsupportedOperationException("TODO");    //TODO
        }
        if(elementType != null){
            return new Cast(expression(elementContext.expression()), elementType).transform();
        } else {
            return expression(elementContext.expression());
        }
    }

    private Expression methodCall(Expression left, MethodCallContext methodCall) throws CompilationError {
        NamePathContext namePath;
        ArgumentsContext arguments;
        if(methodCall instanceof NormalInvokeContext normalInvokeContext){
            namePath = normalInvokeContext.namePath();
            arguments = normalInvokeContext.arguments();
        } else if(methodCall instanceof AsyncInvokeContext asyncInvokeContext){
            namePath = asyncInvokeContext.namePath();
            arguments = asyncInvokeContext.arguments();
        } else {
            throw unit.syntaxError(methodCall, "unexpected method call");
        }
        if(!(namePath instanceof FormalNamePathContext)){
            throw unit.syntaxError(namePath, "illegal token '%s' for method call, function expected".formatted(namePath.getText()));
        }
        NamePathResolver resolver;
        if(left != null){
            resolver = new NamePathResolver(NamePathResolver.ResolveMode.ForInvokable, this.unit, functionDef, left, ((FormalNamePathContext)namePath));
        } else {
            resolver = new NamePathResolver(NamePathResolver.ResolveMode.ForInvokable, this.unit, functionDef, ((FormalNamePathContext)namePath));
        }

        Expression invocation = resolver.resolve();
        Expression forkContext = extractForkContext(methodCall);
        if(invocation instanceof MaybeFunction maybeFunction && maybeFunction.isFunction()) {
            return invoke(maybeFunction, methodCall, arguments);
        } else {
            ClassDef inferType = invocation.inferType();
            if(inferType instanceof FunctionDef) {   // a function instance
                return new FunctionApply(extractInvokeMode(methodCall), invocation, forkContext);
            } else if(functionDef.getRoot().getFunctionBaseOfAnyClass().isThatOrSuperOfThat(inferType)){  // Function<R>
                return new InvokeFunctor(Invoke.InvokeMode.Invoke, invocation, forkContext);
            } else if(inferType instanceof ClassIntervalClassDef classIntervalClassDef) {   // `var v as [SomeFunction] = f; f()`, that means
                var lBound = classIntervalClassDef.getLBoundClass();
                if (lBound.isThatOrDerivedFromThat(lBound.getRoot().getFunctionBaseOfAnyClass())) {
                    return new InvokeExpression(this.functionDef, extractInvokeMode(methodCall), invocation, valueExpressions(arguments), forkContext, unit.sourceLocation(methodCall));
                }
            }
        }
        throw unit.syntaxError(namePath, "'%s' is not invocable".formatted(namePath.getText()));
    }

    private Expression invoke(MaybeFunction resolved, MethodCallContext methodCall, ArgumentsContext arguments) throws CompilationError {
        List<Expression> values = valueExpressions(arguments);
        Invoke.InvokeMode invokeMode = extractInvokeMode(methodCall);
        var invoke = new Invoke(invokeMode, this.functionDef, resolved, values, unit.sourceLocation(methodCall));
        invoke.setForkContext(extractForkContext(methodCall));
        return invoke;
    }

    private static Invoke.InvokeMode extractInvokeMode(MethodCallContext methodCall) {
        if(methodCall instanceof NormalInvokeContext){
            return Invoke.InvokeMode.Invoke;
        }

        if(methodCall instanceof AsyncInvokeContext asyncInvokeContext) {
            InvokeModeContext invokeModeContext = asyncInvokeContext.invokeMode();
            if (invokeModeContext.AWAIT() != null) {
                return Invoke.InvokeMode.Await;
            } else if (invokeModeContext.FORK() != null) {
                return Invoke.InvokeMode.Fork;
            } else if (invokeModeContext.SPAWN() != null) {
                return Invoke.InvokeMode.Spawn;
            }
        }
        return Invoke.InvokeMode.Invoke;
    }

    private Expression extractForkContext(MethodCallContext methodCall) throws CompilationError {
        if(methodCall instanceof NormalInvokeContext){
            return null;
        }

        if(methodCall instanceof AsyncInvokeContext asyncInvokeContext) {
            ViaForkContextContext viaForkContext = asyncInvokeContext.viaForkContext();
            if(viaForkContext != null) {
                Expression r = this.expression(viaForkContext.forkContext);
                if(!r.inferType().isDeriveFrom(this.functionDef.getRoot().getForkContextInterface())){
                    throw unit.typeError(viaForkContext.forkContext, "'lang.ForkContext' expected");
                }
                return r;
            }
        }
        return null;
    }


    private List<Expression> valueExpressions(ArgumentsContext arguments) throws CompilationError {
        List<ExpressionContext> valueExprs;
        if(arguments != null && arguments.expressionList() != null){
            ExpressionListContext expressionList = arguments.expressionList();
            valueExprs = expressionList.expression();
        } else {
            valueExprs = new ArrayList<>();
        }

        List<Expression> values = new ArrayList<>();
        for (ExpressionContext valueExpr : valueExprs) {
            values.add(expression(valueExpr));
        }
        return values;
    }

    private Expression create(Expression typeExpr, ParserRuleContext creatorContext, ArgumentsContext arguments, String constructorName) throws CompilationError {
        List<Expression> values = valueExpressions(arguments);

        return new Creator(typeExpr, values, unit.sourceLocation(creatorContext), constructorName).transform();
    }

    private Expression create(Expression typeExpr, ParserRuleContext creatorContext, ArgumentsContext arguments) throws CompilationError {
        return create(typeExpr, creatorContext, arguments, null);
    }


    private Map<Expression, Var.LocalVar> reusableTempVariables = new HashMap<>();
    public static boolean isReusableExpression(Expression expression){
        return expression instanceof Scope
                || expression instanceof ConstClass
                || expression instanceof ClassOf.ClassOfScope
                || expression instanceof ClassUnder.ClassUnderScope;
    }

    public Var.LocalVar acquireTempVar(Expression expression) throws CompilationError {
        boolean reusable;
        if(isReusableExpression(expression)) {
            var existed = reusableTempVariables.get(expression);
            if(existed != null) {
                return existed;
            }
            reusable = true;
        } else {
            reusable = false;
        }

        ClassDef type = expression.inferType();
        if(type instanceof PhantomMetaClassDef) type = functionDef.getRoot().getObjectClass();
        var v = new Variable();
        v.setType(type);
        v.setOwnerClass(this.functionDef);
        v.setModifiers(AgoClass.PRIVATE);
        SlotDef slot = getSlotsAllocator().acquireRegister(type);
        v.setSlot(slot);
        v.setName(slot.getName());
        var r = reusable ? new Var.ReusingLocalVar(v, Var.LocalVar.VarMode.Temp): new Var.LocalVar(v, Var.LocalVar.VarMode.Temp);
        if(reusable){
            getSlotsAllocator().lockRegister(slot);
            this.reusableTempVariables.put(expression, r);
        }
        return r;
    }

    public void lockRegister(TermExpression tempVar) {
        if(tempVar instanceof Var.LocalVar localVar && localVar.varMode == Var.LocalVar.VarMode.Temp){
            getSlotsAllocator().lockRegister(localVar.getVariableSlot());
        }
    }

    public void releaseRegister(TermExpression tempVar) {
        if(tempVar instanceof Var.LocalVar localVar && localVar.varMode == Var.LocalVar.VarMode.Temp){
            if(this.reusableTempVariables.containsValue(localVar)) return;
            getSlotsAllocator().releaseRegister(localVar.getVariableSlot());
        }
    }

    public void assign(Var.LocalVar assignee, Var.LocalVar localVar) {
        code.assign(assignee.getVariableSlot(), localVar.variable.getType().getTypeCode(), localVar.getVariableSlot());
    }

    public void assign(Var.LocalVar instance, Variable variable, Var.LocalVar src) {
        code.assign(instance.getVariableSlot(), variable.getSlot(), src.variable.getType().getTypeCode(), src.getVariableSlot());
    }

    public Label createLabel(){
        return new Label(nextLabelId++, this.code);
    }


    /*
        statement
            : blockLabel = block        # BlockStmt
     */
    private BlockStmt blockStmt(BlockStmtContext block) throws CompilationError {
        return block(block.block());
    }

    /*
        block:
            '{' blockStatement* '}'
        ;
     */
    private BlockStmt block(BlockContext block) throws CompilationError {
        List<Statement> statements = new ArrayList<>();
        for (BlockStatementContext blockStatementContext : block.blockStatement()) {
            var st = this.blockStatement(blockStatementContext);
            if(st != null) statements.add(st);
        }
        return new BlockStmt(statements).setSourceLocation(unit.sourceLocation(block));

    }

    private Statement ifStmt(IfStmtContext ifStmt) throws CompilationError {
        var cond = parExpression(ifStmt.parExpression());
        var trueBranch = statement(ifStmt.trueBranch);
        Statement falseBranch = ifStmt.falseBranch == null ? null : statement(ifStmt.falseBranch);
        return new IfThenElseStmt(cond, trueBranch, falseBranch);
    }

    private Expression parExpression(ParExpressionContext parExpression) throws CompilationError {
        if(parExpression.expression() != null){
            return expression(parExpression.expression());
        } else if(parExpression.localVariableDeclaration() != null){
            var decl = this.visitLocalVariableDeclaration(parExpression.localVariableDeclaration());
            if(decl == null){
                throw unit.syntaxError(parExpression, "no value assigned");
            } else {
                return decl.getExpression();
            }
        } else {
            throw new RuntimeException("unexpected type " + parExpression);
        }
    }

    private Statement forStmt(ForStmtContext forStmt) throws CompilationError {
        String label = forStmt.label() == null ? null : forStmt.label().identifier().getText();
        var forControl = forStmt.forControl();
        EnhancedForControlContext enhancedForControl = forControl.enhancedForControl();
        if(enhancedForControl != null){
            var expression = this.expression(enhancedForControl.expression()).transform();
            ClassDef expressionType = expression.inferType();
            ForEachStmt.Mode mode = ForEachStmt.Mode.Iterable;
            ClassDef concreteType = unit.getRoot().getIterableInterface().asThatOrSuperOfThat(expressionType);
            if(concreteType == null){
                mode = ForEachStmt.Mode.Iterator;
                concreteType = unit.getRoot().getIteratorInterface().asThatOrSuperOfThat(expressionType);
                if(concreteType == null) {
                    throw new TypeMismatchError("an iterable class required", unit.sourceLocation(forControl.expression()));
                }
            } else {
                if(unit.getRoot().getAnyArrayClass().isThatOrSuperOfThat(expressionType)){
                    mode = ForEachStmt.Mode.Array;      // array is Iterable too, however, the for-each stmt will iterate with indexed loop directly
                }
            }
            // variableModifiers? identifier (AS declarationType)? IN expression
            VariableModifiersContext variableModifiers = enhancedForControl.variableModifiers();
            ClassDef type;
            if(enhancedForControl.declarationType() != null){
                type = unit.parseTypeName(functionDef, enhancedForControl.declarationType().namePath(),false);
            } else {
                var arg = concreteType.getGenericSource().instantiationArguments().getTypeArgumentsArray()[0];
                type = arg.getClassDefValue();
            }
            Compiler.processClassTillStage(type, CompilingStage.AllocateSlots);

            var iterVar = defineLocalVar(enhancedForControl.identifier(),variableModifiers, type);
            return new ForEachStmt(label, iterVar, expression, statement(forStmt.statement()), mode, unit.sourceLocation(enhancedForControl));
        } else {
            Statement init;
            ForInitContext forInit = forControl.forInit();
            if(forInit != null) {
                var decl = forInit.localVariableDeclaration();
                if (decl != null) {
                    init = this.visitLocalVariableDeclaration(decl);
                } else {
                    init = expressionList(forInit.expressionList());
                }
            } else {
                init = null;
            }
            ExpressionContext expr = forControl.expression();
            Expression condition = expr == null ? null : this.expression(expr);
            BlockStmt updateStatement = expressionList(forControl.forUpdate);
            Statement body = statement(forStmt.statement());
            return new ForStmt(label,init, condition, updateStatement, body);
        }
    }

    private BlockStmt expressionList(ExpressionListContext expressionList) throws CompilationError {
        if(expressionList == null) return null;
        List<Statement> ls = new ArrayList<>();
        for (ExpressionContext expression : expressionList.expression()) {
            ls.add(new ExpressionStmt(this.expression(expression)));
        }
        return new BlockStmt(ls).setSourceLocation(unit.sourceLocation(expressionList));
    }

    private Statement whileStmt(WhileStmtContext whileStmt) throws CompilationError {
        var cond = parExpression(whileStmt.parExpression());
        String label = whileStmt.label() != null ? whileStmt.label().identifier().getText() : null;
        return new WhileStmt(label, cond, statement(whileStmt.statement()));
    }
    DoWhileStmt doWhileStmt(DoWhileStmtContext doWhileStmt) throws CompilationError {
        var cond = parExpression(doWhileStmt.parExpression());
        String label = doWhileStmt.label() != null ? doWhileStmt.label().identifier().getText() : null;
        return new DoWhileStmt(label, cond, statement(doWhileStmt.statement()));
    }
    Statement switchStmt(SwitchStmtContext switchStmt) throws CompilationError {
        var condition = this.parExpression(switchStmt.parExpression());
        List<SwitchCaseStmt.SwitchGroup> switchGroups = new ArrayList<>();
        var groups = switchStmt.switchBlockStatementGroup();
        if(groups != null) {
            for (SwitchBlockStatementGroupContext group : groups) {
                var switchGroup = new SwitchCaseStmt.SwitchGroup();
                for (SwitchLabelContext switchLabel : group.switchLabel()) {
                    SwitchCaseStmt.Case cs = switchLabel(switchLabel);
                    switchGroup.addCase(cs);
                }
                for (BlockStatementContext blockStatement : group.blockStatement()) {
                    Statement statement = this.blockStatement(blockStatement);
                    if(statement != null)
                        switchGroup.addStatement(statement);
                }
                switchGroups.add(switchGroup);
            }
        }
        SwitchCaseStmt.SwitchGroup last = null;
        List<SwitchLabelContext> restLabels = switchStmt.switchLabel();
        if(groups == null && restLabels == null){
            throw unit.syntaxError(switchStmt, "no cases found");
        }
        for (SwitchLabelContext otherLabel : restLabels) {
            if(last == null) last = new SwitchCaseStmt.SwitchGroup();
            last.addCase(switchLabel(otherLabel));
        }
        if(last != null){
            switchGroups.add(last);
        }
        return new SwitchCaseStmt(condition, switchGroups);
    }

    private SwitchCaseStmt.Case switchLabel(SwitchLabelContext switchLabel) throws CompilationError {
        SwitchCaseStmt.Case cs;
        if(switchLabel.constantExpression != null){
            cs = new SwitchCaseStmt.Case(SwitchCaseStmt.CaseKind.ConstExpression, expression(switchLabel.constantExpression));
        } else if(switchLabel.enumConstantName != null) {
            Expression constExpr = unit.resolveNamePath(this.functionDef, switchLabel.namePath(), NamePathResolver.ResolveMode.ForVariable);
            if(!(constExpr instanceof EnumValue) && !(constExpr instanceof ConstValue)){
               throw unit.typeError(switchLabel.namePath(), "enum value or const value expected");
            }
            cs = new SwitchCaseStmt.Case(SwitchCaseStmt.CaseKind.EnumConst, constExpr);
        } else if(switchLabel.DEFAULT() != null){
            cs = new SwitchCaseStmt.Case(SwitchCaseStmt.CaseKind.Default, null);
        } else {
            Expression type = unit.parseType(functionDef, switchLabel.variableType(), false, false);
            ClassDef t = extractType(type);
            Compiler.processClassTillStage(t, CompilingStage.AllocateSlots);
            Var.LocalVar localVar = defineLocalVar(switchLabel.varName, null, t);
            cs = new SwitchCaseStmt.Case(SwitchCaseStmt.CaseKind.TypeDispatch, localVar);
        }
        cs.setSourceLocation(unit.sourceLocation(switchLabel));
        return cs;
    }

    TryCatchFinallyStmt tryStmt(TryStmtContext tryStmt) throws CompilationError {
        var tryBlock = this.block(tryStmt.block());
        List<CatchClauseContext> catchClauseASTs = tryStmt.catchClause();
        List<TryCatchFinallyStmt.CatchCause> catchCauses;
        if(CollectionUtils.isNotEmpty(catchClauseASTs)){
            catchCauses = new ArrayList<>();
            for (CatchClauseContext catchClauseAST : catchClauseASTs) {
                List<ClassDef> exceptionTypes = new ArrayList<>();
                ClassDef commonType = null;
                var declarationTypeContexts = catchClauseAST.catchType().declarationType();
                for (DeclarationTypeContext declarationTypeContext : declarationTypeContexts) {
                    var exceptionType = unit.parseTypeName(functionDef, declarationTypeContext.namePath(), false);
                    Compiler.processClassTillStage(exceptionType, CompilingStage.AllocateSlots);
                    if(!exceptionType.isThatOrDerivedFromThat(functionDef.getRoot().getThrowableClass())){
                        throw unit.typeError(declarationTypeContext,"'%s' is not a throwable class");
                    }
                    for (ClassDef existed : exceptionTypes) {
                        if(exceptionType.isThatOrDerivedFromThat(existed)){
                            throw unit.typeError(declarationTypeContext,"'%s' is derived from '%s'".formatted(exceptionType.getFullname(), existed.getFullname()));
                        } else if(exceptionType == existed){
                            throw unit.typeError(declarationTypeContext,"'%s' duplicated".formatted(exceptionType.getFullname()));
                        } else if(existed.isThatOrDerivedFromThat(exceptionType)) {
                            throw unit.typeError(declarationTypeContext, "'%s' is derived from '%s'".formatted(existed.getFullname(), exceptionType.getFullname()));
                        }
                    }
                    for (TryCatchFinallyStmt.CatchCause catchCause : catchCauses) {
                        for (ClassDef existed : catchCause.getExceptionTypes()) {
                           if(exceptionType.isThatOrDerivedFromThat(existed)){    // the above catch-block already covered super type exception
                                throw unit.typeError(declarationTypeContext,"'%s' is derived from '%s'".formatted(exceptionType.getFullname(), existed.getFullname()));
                            } else if(exceptionType == existed) {
                                throw unit.typeError(declarationTypeContext, "'%s' duplicated".formatted(exceptionType.getFullname()));
                            }
                        }
                    }
                    exceptionTypes.add(exceptionType);
                    var c = findCommonType(commonType, exceptionType);
                    if(c == null){
                        throw new TypeMismatchError("no common type found for '%s' and '%s'".formatted(commonType, exceptionType), unit.sourceLocation(declarationTypeContext));
                    }
                    commonType = c;
                    functionDef.idOfClass(exceptionType);
                    functionDef.idOfClass(commonType);
                }
                var v = defineLocalVar(catchClauseAST.identifier(), catchClauseAST.variableModifiers(), commonType);
                var block = this.block(catchClauseAST.block());
                TryCatchFinallyStmt.CatchCause catchCause = new TryCatchFinallyStmt.CatchCause(exceptionTypes, block, v);
                catchCauses.add(catchCause);
            }
        } else {
            catchCauses= null;
        }
        FinallyBlockContext finallyBlockContext = tryStmt.finallyBlock();
        BlockStmt finallyBlock = finallyBlockContext == null ? null : block(finallyBlockContext.block());
        return new TryCatchFinallyStmt(tryBlock, catchCauses, finallyBlock);
    }

    ThrowStmt throwStmt(ThrowStmtContext throwStmt) throws CompilationError {
        return new ThrowStmt(expression(throwStmt.expression()));
    }

    Statement breakStmt(BreakStmtContext breakStmt){
        var id = breakStmt.identifier();
        return new BreakStmt(id == null ? null : id.getText());
    }

    Statement continueStmt(ContinueStmtContext continueStmt){
        var id = continueStmt.identifier();
        return new ContinueStmt(id == null ? null : id.getText());
    }

    void yieldStmt(YieldStmtContext yieldStmt){

    }
    WithStmt withStmt(WithStmtContext withStmt) throws CompilationError {
        var expression = new CurrWithExpression(parExpression(withStmt.parExpression()));
        withExpressionStack.push(expression);
        var stmt = statement(withStmt.statement());
        withExpressionStack.pop();
        return new WithStmt(expression, stmt);
    }

    private Expression withExpr(PostWithExprContext postWithExpr) throws CompilationError {
        var expression = new CurrWithExpression(this.expression(postWithExpr.expression()));
        withExpressionStack.push(expression);
        var stmt = this.statement(postWithExpr.postWith().statement());
        withExpressionStack.pop();
        return new WithExpr(expression, stmt);
    }

    ViaStmt viaStmt (ViaStmtContext viaStmt ) throws CompilationError {
        Expression par = this.parExpression(viaStmt.parExpression());
        if(!par.inferType().isDeriveFrom(this.getFunctionDef().getRoot().getViaObjectInterface())){
            throw new TypeMismatchError("a ViaObject expected", par.getSourceLocation());
        }
        Statement stmt = this.statement(viaStmt.statement());
        return new ViaStmt(par, stmt);
    }

    private Statement asyncInvokeFunctorStmt(AsyncInvokeFunctorStmtContext asyncInvokeFunctorStmt) throws CompilationError {
        Invoke.InvokeMode mode;
        if(asyncInvokeFunctorStmt.FORK() != null){
            mode = Invoke.InvokeMode.Fork;
        } else {
            mode = Invoke.InvokeMode.Spawn;
        }
        Expression expression = expression(asyncInvokeFunctorStmt.expression()).transform();
        if(!functionDef.getRoot().getFunctionBaseOfAnyClass().isThatOrSuperOfThat(expression.inferType())){
            throw new TypeMismatchError("functor expected",expression.getSourceLocation());
        }
        return new AsyncInvokeFunctorStmt(mode, expression);
    }

    private Expression invokeFunctor(AwaitFunctorContext invokeFunctorContext) throws CompilationError {
        var f = expression(invokeFunctorContext.expression()).transform();
        ViaForkContextContext viaForkContext = invokeFunctorContext.viaForkContext();
        Expression forkContextExpr = null;
        if(viaForkContext != null) {
            forkContextExpr = this.expression(viaForkContext.forkContext);
            if(!forkContextExpr.inferType().isDeriveFrom(this.functionDef.getRoot().getForkContextInterface())){
                throw unit.typeError(viaForkContext.forkContext, "'lang.ForkContext' expected");
            }
        }
        if (!functionDef.getRoot().getFunctionBaseOfAnyClass().isThatOrSuperOfThat(f.inferType())) {
            throw new TypeMismatchError("functor expected", f.getSourceLocation());
        }
        return new InvokeFunctor(Invoke.InvokeMode.Await, f, forkContextExpr);
    }

    public List<ClassDef> getHandledExceptions() {
        return handledExceptions;
    }

    public void validateThrowException(List<ClassDef> throwsExceptions, Expression expression) throws ResolveError {
        for (ClassDef throwsException : throwsExceptions) {
            validateThrowException(throwsException,expression);
        }
    }

    public void validateThrowException(ClassDef throwsException, Expression expression) throws ResolveError {
        ClassDef runtimeExceptionClass = unit.getRoot().getRuntimeExceptionClass();
        if(throwsException.isThatOrDerivedFromThat(runtimeExceptionClass)){
            return;
        }
        for (ClassDef handledException : handledExceptions) {
            if(throwsException.isThatOrDerivedFromThat(handledException)){
                return;
            }
        }
        throw new ResolveError("unhandled exception '%s'".formatted(throwsException.getFullname()),expression.getSourceLocation());
    }

    Stack<Expression> currExpressionStack = new Stack<>();
    public void enter(Expression expression) {
        if(expression.getSourceLocation() == null) return;
        if(LOGGER.isDebugEnabled()) LOGGER.debug("enter " + expression);
        currExpressionStack.push(expression);
        code.setSourceLocation(expression.getSourceLocation());
    }
    public void leave(Expression expression){
        if (expression.getSourceLocation() == null) return;
        if (LOGGER.isDebugEnabled()) LOGGER.debug("leave " + expression);

        var e = currExpressionStack.pop();
        assert e == expression;
        if(!currExpressionStack.isEmpty()) {
            code.setSourceLocation(currExpressionStack.peek().getSourceLocation());
        } else {
            code.setSourceLocation(SourceLocation.UNKNOWN);
        }
    }

    Stack<CurrWithExpression> withExpressionStack = new Stack<>();
    public void enterWith(CurrWithExpression expression) {
        withExpressionStack.push(expression);
        lockRegister(expression.getLocalVar());
    }
    public void leaveWith(CurrWithExpression expression) {
        var pop = withExpressionStack.pop();
        assert pop == expression;
        releaseRegister(expression.getLocalVar());
    }

    Expression getCurrentWithExpr(){
        return withExpressionStack.peek();
    }
}
