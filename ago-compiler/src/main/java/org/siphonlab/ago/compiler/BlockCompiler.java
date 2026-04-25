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
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.array.*;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.literal.StringLiteral;
import org.siphonlab.ago.compiler.expression.logic.*;
import org.siphonlab.ago.compiler.expression.math.ArithmeticExpr;
import org.siphonlab.ago.compiler.expression.math.Neg;
import org.siphonlab.ago.compiler.expression.math.Pos;
import org.siphonlab.ago.compiler.expression.math.SelfArithmetic;
import org.siphonlab.ago.compiler.generic.ClassIntervalClassDef;
import org.siphonlab.ago.compiler.narrowtype.NarrowTyper;
import org.siphonlab.ago.compiler.resolvepath.NamePathResolver;
import org.siphonlab.ago.compiler.resolvepath.VariableScope;
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
    private final Root root;
    int nextLabelId = 0;

    private List<ClassDef> handledExceptions = new LinkedList<>();

    private NarrowTyper narrowTyper = new NarrowTyper(this);

    public BlockCompiler(Unit unit, FunctionDef functionDef, List<BlockStatementContext> blockStatements) {
        this.unit = unit;
        this.functionDef = functionDef;
        this.blockStatements = blockStatements;
        this.slotsAllocator = functionDef.getSlotsAllocator();
        this.code = new CodeBuffer();
        this.handledExceptions.addAll(functionDef.getThrowsExceptions());
        this.root = functionDef.getRoot();
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
                var c = functionDef.classUnder(new Scope(0, functionDef), constructor);
                var constructorInvocation = functionDef.invoke(Invoke.InvokeMode.Invoke, c, Collections.emptyList(), functionDef.getSourceLocation()).setSourceLocation(functionDef.getSourceLocation()).transform();
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
                compiledStatements.add(functionDef.return_());
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
            return visitLocalVariableDeclaration(localVariableDeclaration);
        } else if(blockStatement instanceof LocalTypeDeclContext localTypeDeclContext){
            // already handled
            return null;
        } else {
            // localTypeDeclaration
            throw new UnsupportedOperationException("TODO");
        }
    }

//    private Statement statement(StatementContext statement) throws CompilationError {
//        return statement(statement, null);
//    }

    private Statement statement(StatementContext statement) throws CompilationError {
        if (statement instanceof ReturnStmtContext returnStmt) {
            return returnStmt(returnStmt).setSourceLocation(unit.sourceLocation(statement));
        } else if (statement instanceof ExpressionStmtContext expressionStmt) {
            var expression = expression(expressionStmt.expressionStatement().expression());
            return functionDef.expressionStmt(expression).setSourceLocation(unit.sourceLocation(statement));
        } else if(statement instanceof BlockStmtContext block) {
            return this.blockStmt(block);
        } else if(statement instanceof EmptyStmtContext emptyStmt){
            return new EmptyStmt(functionDef).setSourceLocation(unit.sourceLocation(emptyStmt));
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
        return new AwaitStmt(functionDef);
    }

    private Return returnStmt(ReturnStmtContext returnStmt) throws CompilationError {
        if(returnStmt.expression() == null){
            if(!functionDef.getResultType().isVoid()){
                throw unit.typeError(returnStmt, "'%s' result expected".formatted(functionDef.getResultType()));
            }
            return functionDef.return_();
        } else {
            return functionDef.return_(functionDef.cast(expression(returnStmt.expression()), functionDef.getResultType()));
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
            var me = new Scope( 1, ownerClass);
            var varMe = (Var.LocalVar) me.visit(this);
            this.lockRegister(varMe);
            for (Field field : ownerClass.fields.values()) {
                if(hasFieldParameters) {
                    var parameter = pfMap.get(field);
                    if(parameter != null){      // assign to this parameter instead of the default initializer expr
                        Var.Field assignee = functionDef.field(varMe, field).setSourceLocation(unit.sourceLocation(field.getDeclaration()));
                        var parameterExpr = functionDef.localVar(parameter, Var.LocalVar.VarMode.Existed);
                        var assign = functionDef.assign(assignee, parameterExpr).setSourceLocation(unit.sourceLocation(parameter.getDeclaration()));
                        stmts.add(assign);
                        continue;
                    }
                }
                if (field.getInitializer() != null) {
                    var valueExpr = expression(field.getInitializer());
                    Var.Field assignee = functionDef.field(varMe, field).setSourceLocation(unit.sourceLocation(field.getDeclaration()));
                    var assign = functionDef.assign(assignee, valueExpr).setSourceLocation(assignee.getSourceLocation());
                    stmts.add(assign);
                }
            }

            for (var entry : ownerClass.traitFields.entrySet()) {
                Field field = entry.getValue();
                ClassDef trait = field.getType();   // field type is TraitDefInScope, the key is TraitDef
                TraitCreator traitCreator;
                Var.Field traitField = functionDef.field(varMe, field).setSourceLocation(unit.sourceLocation(field.getDeclaration()));
                Expression bindPermit;
                if (trait.getPermitClass() != null && trait.getPermitClass() != trait.getRoot().getObjectClass()) {
                    var permitFld = functionDef.field(traitField, trait.getFieldForPermitClass());
                    bindPermit = functionDef.assign(permitFld, varMe);
                } else {
                    bindPermit = null;
                }
                // create a local var to accept the trait instance from `new Trait()`
                // for traitField is a field, can't accept creator result, there must be a temp var
                // even if functionDef.assign(field, creator), it will create a temp var too
                // now I make the temp var explicit, so that I can bind @trait_field and @permit_field before invoke trait constructor
                var createdTrait = this.acquireTempVar(new SomeInstance(functionDef, trait));

                var ls = new ArrayList<Statement>();
                ls.add(new ExpressionStmt(functionDef, functionDef.assign(traitField, createdTrait).transform()));
                if(bindPermit != null) ls.add(new ExpressionStmt(functionDef, bindPermit));

                traitCreator = new TraitCreator(functionDef, new ConstClass(trait), functionDef.blockStmt(ls), unit.sourceLocation(field.getDeclaration()));
                stmts.add(functionDef.assign(createdTrait, traitCreator).setSourceLocation(traitField.getSourceLocation()));
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

        Var.LocalVar localVar = new Var.LocalVar(functionDef, variable, Var.LocalVar.VarMode.ToDeclare).setSourceLocation(unit.sourceLocation(identifier));
        functionDef.addLocalVariableWithSlot(variable);
        return localVar;
    }

    private ExpressionStmt visitLocalVariableDeclaration(LocalVariableDeclarationContext localVariableDeclaration) throws CompilationError {
        Variable variable = new Variable();
        variable.setName(localVariableDeclaration.identifier().getText());
        variable.setOwnerClass(functionDef);
        variable.setSourceLocation(unit.sourceLocation(localVariableDeclaration));
        variable.setModifiers(Compiler.variableModifiers(unit, localVariableDeclaration.variableModifiers(), Compiler.ModifierTarget.Variable));

        Var.LocalVar localVar = new Var.LocalVar(functionDef, variable, Var.LocalVar.VarMode.ToDeclare);

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
                    var scopedClassIntervalClassDef = root.getOrCreateScopedClassInterval(t, t, null);
                    functionDef.registerConcreteType((ConcreteType) scopedClassIntervalClassDef);
                    inferred = t;
                    initializerExpr = new CastToScopedClassRef(functionDef, initializerExpr, scopedClassIntervalClassDef).transform();
                } else {
                    inferred = initializerExpr.inferType();
                    if (inferred == root.getAnyClass()) {
                        inferred = root.getObjectClass();
                        initializerExpr = new Cast(functionDef, initializerExpr, inferred).setSourceLocation(initializerExpr.getSourceLocation()).transform();
                    }
                }
                variable.setType(inferred);
            } else {
                initializerExpr = new Cast(functionDef, initializerExpr, type).setSourceLocation(initializerExpr.getSourceLocation()).transform();
            }
            functionDef.addLocalVariableWithSlot(variable);
            return new ExpressionStmt(functionDef, functionDef.assign(localVar, initializerExpr));
        } else {
            functionDef.addLocalVariableWithSlot(variable);
            return null;
        }
    }

    Expression expression(ExpressionContext expression) throws CompilationError {
        if(expression == null) return null;
        if(expression instanceof AddSubtractExprContext addSubtractExpr){
            ArithmeticExpr.Type type = addSubtractExpr.bop.getType() == ADD ? ArithmeticExpr.Type.Add : ArithmeticExpr.Type.Substract;
            return new ArithmeticExpr(functionDef, type, expression(addSubtractExpr.expression(0)), expression(addSubtractExpr.expression(1)))
                    .setSourceLocation(unit.sourceLocation(expression));
        } else if (expression instanceof PrimaryExprContext primaryExprContext){
            if(primaryExprContext.primaryExpression() instanceof LiteralExprContext literalExpr) {
                return literalExpr(literalExpr);
            } else if(primaryExprContext.primaryExpression() instanceof NamePathExprContext namePath){
                return unit.resolveNamePath(this.functionDef, this.functionDef, namePath.namePath(), NamePathResolver.ResolveMode.ForValue);
            }
        } else if(expression instanceof MethodCallExprContext methodCallExpr){
            var methodCall = methodCallExpr.methodCall();
            return methodCall(null, methodCall);        // static instance or current instance
        } else if(expression instanceof MemberAccessExprContext memberAccessExpr){
            return memberAccessExpr(memberAccessExpr);
        } else if(expression instanceof QuotedExprContext quotedExpr){
            return expression(quotedExpr.expression());
        } else if(expression instanceof EqualsExprContext equalsExpr){
            return this.equalsExpr(equalsExpr).setSourceLocation(unit.sourceLocation(expression));
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
            return new Cast(functionDef, expression(castTypeExprContext.expression()), t, true);
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
            if(root.getAnyArrayClass().isThatOrSuperOfThat(obj.inferType())) {
                return new ArrayElement(functionDef,obj, index).setSourceLocation(unit.sourceLocation(expression));
            } else if(root.getAnyReadwriteList().isThatOrSuperOfThat(obj.inferType()) || root.getAnyReadonlyList().isThatOrSuperOfThat(obj.inferType())) {
                return new ListElement(functionDef,obj, index).setSourceLocation(unit.sourceLocation(expression));
            } else if(root.getAnyReadwriteMap().isThatOrSuperOfThat(obj.inferType()) || root.getAnyReadonlyMap().isThatOrSuperOfThat(obj.inferType())){
                return new MapValue(functionDef,obj, index).setSourceLocation(unit.sourceLocation(expression));
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
                NamePathResolver namePathResolver = new NamePathResolver(NamePathResolver.ResolveMode.ForVariable, unit, this.functionDef, left, ((FormalNamePathContext) namePath));
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
            return new ArithmeticExpr(functionDef,type, expression(multiDivModExpr.expression(0)), expression(multiDivModExpr.expression(1)))
                    .setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof CompareExprContext compareExpr){
            return compareExpr(compareExpr).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof InstanceOfExprContext instanceOfExpr){
            return instanceOfExpr(instanceOfExpr).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof BitAndExprContext expr){
            return new BitOpExpr(functionDef,BitOpExpr.Type.BitAnd,expression(expr.expression(0)), expression(expr.expression(1)))
                    .setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof BitXorExprContext expr){
            return new BitOpExpr(functionDef,BitOpExpr.Type.BitXor,expression(expr.expression(0)), expression(expr.expression(1))).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof BitOrExprContext expr){
            return new BitOpExpr(functionDef,BitOpExpr.Type.BitOr,expression(expr.expression(0)), expression(expr.expression(1))).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof AndExprContext andExpr){
            return andExpr(andExpr).setSourceLocation(unit.sourceLocation(expression));
        } else if(expression instanceof OrExprContext orExpr) {
            return orExpr(orExpr).setSourceLocation(unit.sourceLocation(orExpr));
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
        } else if(expression instanceof ValueFromNullableContext valueFromNullableContext){
            return valueFromNullable(valueFromNullableContext);
        }
        throw new UnsupportedOperationException(expression.getText());
    }

    private Expression literalExpr(LiteralExprContext literalExpr) throws CompilationError {
        LiteralContext literal = literalExpr.literal();
        if(literal instanceof LArrayContext larr) {
            return arrayLiteral(larr, null, null);
        } else if(literal instanceof LObjectContext lobj){
            return objectLiteral(lobj, null, null);
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

        var offset = lTemplateString.getStart().getCharPositionInLine() + 2;  // indent position

        // If the first line has no content and directly transitions to a new line,
        // use the index of the first non-whitespace character from the next line as the indentation point.
        // Subsequent lines are indented according to the previous lines.

        CharBuffer charBuffer = new CharBuffer();

        boolean atFirstLineHead = true;
        boolean atLineHead = true;
        for (var atom : lTemplateString.templateStringLiteral().templateStringAtom()) {
            ExpressionContext atomExpr = atom.expression();
            if(atomExpr != null){
                atFirstLineHead = false;
                if(!sb.isEmpty()){
                    expressions.add(root.createStringLiteral(sb.toString()).setSourceLocation(unit.sourceLocation(startAtom, endAtom)));
                    sb.setLength(0);
                    startAtom = null;
                }
                expressions.add(this.expression(atomExpr));
            } else {
                if(startAtom == null) startAtom = atom;
                endAtom = atom;
                String text = atom.TemplateStringAtom().getText();
                charBuffer.append(text);
                while(true) {
                    if (atFirstLineHead) {
                        atFirstLineHead = false;
                        if (charBuffer.skipNewLineIfPeekIsNewLine()) {
                            // now enter next line
                            offset = charBuffer.skipWs();
                            atLineHead = false;
                            continue;
                        }
                    }
                    if (atLineHead) {
                        charBuffer.skipIndent(offset);
                        atLineHead = false;
                        continue;
                    }

                    if (charBuffer.skipNewLineIfPeekIsNewLine()) {
                        sb.append('\n');
                        atLineHead = true;
                    } else {
                        char c = charBuffer.get();
                        if (c != '\0') {
                            sb.append(c);
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        if(!sb.isEmpty()){
            expressions.add(getRoot().createStringLiteral(sb.toString()).setSourceLocation(unit.sourceLocation(startAtom, endAtom)));
        }
        if(expressions.isEmpty()) return getRoot().createStringLiteral("").setSourceLocation(unit.sourceLocation(lTemplateString));
        if(expressions.size() == 1) return expressions.getFirst();
        var r = expressions.getFirst();
        for (int j = 1; j < expressions.size(); j++) {
            Expression expr = expressions.get(j);
            r = functionDef.concat(r, expr);
        }
        return r;
    }

    public Root getRoot() {
        return root;
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
        return new InstanceOf(functionDef, expression(instanceOfExpr.expression()), type, receiverVar);
    }

    private Expression ifElseExpr(IfElseExprContext ifElseExpr) throws CompilationError {
        this.narrowTyper.enter();
        var cond = narrowType(ifElseExpr.condition);
        var mapper = this.narrowTyper.exit();

        Expression trueBranch;
        Expression falseBranch;

        if(mapper.isDirty()) {
            this.narrowTyper.reenter(mapper, true);
            trueBranch = expression(ifElseExpr.ifPart);
            narrowTyper.exit();
        } else {
            trueBranch = expression(ifElseExpr.ifPart);
        }

        if(mapper.isDirty()) {
            this.narrowTyper.reenter(mapper, false);
            falseBranch = expression(ifElseExpr.elsePart);
            narrowTyper.exit();
        } else {
            falseBranch = expression(ifElseExpr.elsePart);
        }
        return new IfElseExpr(functionDef, trueBranch, cond, falseBranch);
    }

    private Expression prefixExpr(PrefixExprContext prefixExpr) throws CompilationError {
        int type = prefixExpr.prefix.getType();
        if(type == NOT) {
            Expression expr;
            if(narrowTyper.isCollecting()){
                expr = narrowType(prefixExpr.expression());
                NarrowTyper.NarrowNodePair mapper = narrowTyper.peek();
                narrowTyper.updateCurrent(mapper.not(), true);
            } else {
                expr = expression(prefixExpr.expression());
            }
            return new Not(functionDef, expr);
        }
        Expression expression = expression(prefixExpr.expression());
        switch (type) {
            case ADD:
                return new Pos(functionDef, expression);
            case SUB:
                return new Neg(functionDef, expression);
            case INC:
                return new SelfArithmetic(functionDef,expression, getRoot().createIntLiteral(1), SelfArithmetic.Type.Inc);
            case DEC:
                return new SelfArithmetic(functionDef, expression, getRoot().createIntLiteral(1), SelfArithmetic.Type.Dec);
            case BITNOT:
                return new BitNot(functionDef, expression);
            default:
                throw new RuntimeException("TODO");
        }
    }

    private Expression incDec(IncDecExprContext incDecExpr) throws CompilationError {
        var expr = incDecExpr.expression();
        SelfArithmetic.Type type = incDecExpr.INC() != null ? SelfArithmetic.Type.IncPost : SelfArithmetic.Type.DecPost;
        return new SelfArithmetic(functionDef, expression(expr), getRoot().createIntLiteral(1), type);
    }


    private Expression assign(AssignExprContext assignExpr) throws CompilationError {
        var assignee = assignee(assignExpr.expression(0));
        var value = assigner(assignExpr.expression(1), assignee, assignee.inferType());
        int bopType = assignExpr.bop.getType();
        SourceLocation sourceLocation = unit.sourceLocation(assignExpr);
        switch (bopType) {
            case ASSIGN:
                return functionDef.assign((Assign.Assignee) assignee, value).setSourceLocation(sourceLocation);
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
                return new SelfArithmetic(functionDef, assignee, value,arithType).setSourceLocation(sourceLocation);

            case AND_ASSIGN:
                return new SelfLogicExpr(functionDef, assignee, value, SelfLogicExpr.Type.And).setSourceLocation(unit.sourceLocation(assignExpr));
            case OR_ASSIGN:
                return new SelfLogicExpr(functionDef, assignee, value, SelfLogicExpr.Type.Or).setSourceLocation(unit.sourceLocation(assignExpr));
            case BITAND_ASSIGN:
                return new SelfBitOpExpr(functionDef, assignee,value, SelfBitOpExpr.Type.BitAnd).setSourceLocation(unit.sourceLocation(assignExpr));
            case BITOR_ASSIGN:
                return new SelfBitOpExpr(functionDef, assignee,value, SelfBitOpExpr.Type.BitOr).setSourceLocation(unit.sourceLocation(assignExpr));
            case BITXOR_ASSIGN:
                return new SelfBitOpExpr(functionDef, assignee,value, SelfBitOpExpr.Type.BitXor).setSourceLocation(unit.sourceLocation(assignExpr));
            case LSHIFT_ASSIGN:
                return new SelfBitShiftExpr(functionDef, assignee,value, SelfBitShiftExpr.Type.LShift).setSourceLocation(unit.sourceLocation(assignExpr));
            case RSHIFT_ASSIGN:
                return new SelfBitShiftExpr(functionDef, assignee,value, SelfBitShiftExpr.Type.RShift).setSourceLocation(unit.sourceLocation(assignExpr));
            case URSHIFT_ASSIGN:
                return new SelfBitShiftExpr(functionDef, assignee,value, SelfBitShiftExpr.Type.URShift).setSourceLocation(unit.sourceLocation(assignExpr));
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
                return new CopyAssign(functionDef, assignee, value, commonType).setSourceLocation(unit.sourceLocation(assignExpr));
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
        return new Compare(functionDef, expression( compareExpr.expression(0)),expression(compareExpr.expression(1)), type);
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
        return new BitShiftExpr(functionDef, type, this.expression(expression.get(0)), this.expression(expression.get(1)));
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
                expr = new ArrayCreate(functionDef, arrayType, lengthExpr);
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
            expr = functionDef.classUnder(current, classUnderScope.getClassDef());
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
                return unit.resolveNamePath(this.functionDef, this.functionDef, namePath.namePath(), NamePathResolver.ResolveMode.ForVariable);
            }
        } else if(expression instanceof MemberAccessExprContext memberAccessExpr) {
            var left = expression(memberAccessExpr.expression());
            MethodCallContext methodCall = memberAccessExpr.methodCall();
            if (methodCall != null) {
                throw new SyntaxError("left side is not assignable", unit.sourceLocation(methodCall));
            } else {
                var namePath = memberAccessExpr.namePath();
                NamePathResolver namePathResolver = new NamePathResolver(NamePathResolver.ResolveMode.ForVariable, unit, this.functionDef, left, ((FormalNamePathContext) namePath));
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
                NamePathResolver namePathResolver = new NamePathResolver(NamePathResolver.ResolveMode.ForVariable, unit, this.functionDef, left, ((FormalNamePathContext) namePath));
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
        return this.assigner(expression, assignee, assigneeType, true);
    }

    /**
     *
     * @param expression
     * @param assignee
     * @param assigneeType if castType=false, it's just a suggestion type, won't force cast
     * @param castType
     * @return
     * @throws CompilationError
     */
    //TODO assign type maybe scoped type
    Expression assigner(ExpressionContext expression, Expression assignee, ClassDef assigneeType, boolean castType) throws CompilationError {
        if(expression instanceof PrimaryExprContext primaryExpr){
            if(primaryExpr.primaryExpression() instanceof LiteralExprContext literalExpr){
                LiteralContext literal = literalExpr.literal();
                // array literal and object literal can be instructed by assignee type
                if(literal instanceof LArrayContext lArrayContext){
                    return arrayLiteral(lArrayContext, assignee, assigneeType);
                } else if(literal instanceof LObjectContext objectLiteral){
                    return objectLiteral(objectLiteral, assignee, assigneeType);
                }
            }
        }
        Expression value = expression(expression).setSourceLocation(unit.sourceLocation(expression)).transform();
        if(assigneeType == null){
            return value;
        } else {
            if(assignee instanceof Assign.Assignee a) {
                value = processBoundClass(functionDef, a, value);
            }
            if(castType) {
                return functionDef.cast(value, assigneeType).transform();
            } else {
                return value;
            }
        }
    }

    /* ArrayLiteral | ListLiteral
          for Array:
                no expando, ArrayLiteral()
                expando, ArrayCreate(),
                    if expando is Array,
                        if type match, Array.copy
                        otherwise for each assign
                    else if expando is List
                        for each assign
          for List:
                no expando, List.new#array(ArrayLiteral())
                expando, List.new#array(ArrayLiteral1())
                    if expando is Array,
                        if type match, fillArray()
                        otherwise for each add
                    else if expando is List
                        if type match, addAll()
                        otherwise for each add
     */
    private Expression arrayLiteral(LArrayContext lArrayContext, Expression assignee, ClassDef assigneeType) throws CompilationError {
        ClassDef arrayType;
        var arrayLiteral = lArrayContext.arrayLiteral();
        Expression listTypeExpr = null;
        if(arrayLiteral.variableType() != null){
            Expression typeExpr = unit.parseType(functionDef, arrayLiteral.variableType(), false, false);
            arrayType = extractType(typeExpr);
            if(root.getAnyListClass().isThatOrSuperOfThat(arrayType)){
                listTypeExpr = typeExpr;
            }
        } else if(assigneeType != null){
            arrayType = assigneeType;
            if(root.getAnyListClass().isThatOrSuperOfThat(arrayType)){
                if(!arrayType.isTop()){
                    throw unit.typeError(lArrayContext, "implicit List class through assignment must be a top class");
                } else {
                    listTypeExpr = new ConstClass(arrayType);
                }
            }
        } else if(!arrayLiteral.elementList().isEmpty()){
            var first = arrayLiteral.elementList().arrayElement(0);
            var el = arrayElement(first, null, null);
            arrayType = functionDef.getOrCreateArrayType(el.elementType(), null);
        } else {
            throw unit.syntaxError(lArrayContext, "cannot predict array type");
        }
        Compiler.processClassTillStage(arrayType, CompilingStage.AllocateSlots);

        ClassDef eleType;
        if(listTypeExpr != null) {
            var listType = root.getAnyListClass().asThatOrSuperOfThat(arrayType);
            eleType = listType.getGenericSource().typeArguments()[0].getClassDefValue();
            arrayType = null;
        } else if(arrayType instanceof NullableClassDef n){
            if(!root.getAnyArrayClass().isThatOrSuperOfThat(n.getBaseClass())){
                throw new TypeMismatchError("assignee type '%s' is not an array".formatted(assigneeType.getFullname()), assignee.getSourceLocation());
            }
            arrayType = n.getBaseClass();
            eleType = ((ArrayClassDef)arrayType).getElementType();
        } else if(!root.getAnyArrayClass().isThatOrSuperOfThat(arrayType)){
            throw new TypeMismatchError("assignee type '%s' is not an array".formatted(assigneeType.getFullname()), assignee.getSourceLocation());
        } else {
            eleType = ((ArrayClassDef)arrayType).getElementType();
        }
        List<CollectionElementDef> elements = new ArrayList<>();
        boolean hasExpando = false;
        for (ArrayElementContext arrayElementContext : arrayLiteral.elementList().arrayElement()) {
            // indicate inside array literal, doubt not required
            if(arrayType == null){
                MutableBoolean returnExisted = new MutableBoolean();
                arrayType = functionDef.getOrCreateArrayType(eleType, returnExisted);
                if(returnExisted.isFalse()) Compiler.processClassTillStage(arrayType, CompilingStage.AllocateSlots);
            }
            var element = arrayElement(arrayElementContext, (ArrayClassDef) arrayType, ((ArrayClassDef)arrayType).getElementType());
            elements.add(element);
            if(!hasExpando && element.isExpando()) hasExpando = true;
        }
        if(!hasExpando) {
            if(arrayType == null){
                MutableBoolean returnExisted = new MutableBoolean();
                arrayType = functionDef.getOrCreateArrayType(eleType, returnExisted);
                if(returnExisted.isFalse()) Compiler.processClassTillStage(arrayType, CompilingStage.AllocateSlots);
            }
            if(elements.isEmpty()){
                if (listTypeExpr == null) {
                    return new ArrayCreate(functionDef, (ArrayClassDef) arrayType, getRoot().createIntLiteral(0)).setSourceLocation(unit.sourceLocation(lArrayContext));
                } else {
                    // create via new#
                    return new Creator(functionDef, listTypeExpr, Collections.emptyList(), unit.sourceLocation(lArrayContext), "new#");
                }
            } else {
                var ls = elements.stream().map(e -> e.getExpression()).toList();
                var r = new ArrayLiteral(functionDef, (ArrayClassDef) arrayType, ls).setSourceLocation(unit.sourceLocation(lArrayContext));
                if (listTypeExpr != null) {
                    // create via new#array
                    return new Creator(functionDef, listTypeExpr, Collections.singletonList(r), unit.sourceLocation(lArrayContext), "new#array");
                }
                return r;
            }
        } else {
            if(listTypeExpr != null){
                return new ComplexListLiteral(functionDef, listTypeExpr, eleType, elements).setSourceLocation(unit.sourceLocation(lArrayContext)).transform();
            } else {
                return new ComplexArrayLiteral(functionDef, (ArrayClassDef)arrayType, elements).setSourceLocation(unit.sourceLocation(lArrayContext)).transform();
            }
        }
    }

    public enum CollectionType{
        Array,
        Iterator,
        Iterable,
        Collection,
        List
    }
    public record CollectionElementType(ClassDef collectionType, ClassDef elementType, CollectionType type){}

    public CollectionElementType extractCollectionElementType(ClassDef groupType){
        if(groupType instanceof ArrayClassDef arrayClassDef){
            return new CollectionElementType(arrayClassDef, arrayClassDef.getElementType(), CollectionType.Array);
        } else {
            var t= root.getAnyListClass().asThatOrSuperOfThat(groupType);
            if(t != null){
                return new CollectionElementType(t, t.getGenericSource().typeArguments()[0].getClassDefValue(), CollectionType.List);
            }
            t= root.getAnyCollectionClass().asThatOrSuperOfThat(groupType);
            if(t != null){
                return new CollectionElementType(t,t.getGenericSource().typeArguments()[0].getClassDefValue(), CollectionType.Collection);
            }
            t = root.getAnyIterableInterface().asThatOrSuperOfThat(groupType);
            if(t != null){
                return new CollectionElementType(t,t.getGenericSource().typeArguments()[0].getClassDefValue(), CollectionType.Iterable);
            } else {
                t = root.getAnyIteratorInterface().asThatOrSuperOfThat(groupType);
                return new CollectionElementType(t,t.getGenericSource().typeArguments()[0].getClassDefValue(), CollectionType.Iterator);
            }
        }
    }


    private CollectionElementDef arrayElement(ArrayElementContext elementContext, ArrayClassDef arrayType,  ClassDef elementType) throws CompilationError {
        boolean isExpando = elementContext.expando != null;
        CollectionElementDef r;
        if (isExpando) {
            var el = this.assigner(elementContext.expression(), null, arrayType, false).setSourceLocation(unit.sourceLocation(elementContext));
            ClassDef expandoType = el.inferType();
            if(expandoType instanceof ArrayClassDef arrayClassDef){
                r = new CollectionElementDef(el, true, arrayClassDef.getElementType());
            } else {
                ClassDef it = root.getAnyIterableInterface().asThatOrSuperOfThat(expandoType);
                var t = it.getGenericSource().typeArguments()[0];
                r = new CollectionElementDef(el, true, t.getClassDefValue());
            }
        } else {
            var el = this.assigner(elementContext.expression(), null, elementType).setSourceLocation(unit.sourceLocation(elementContext));
            r = new CollectionElementDef(el, false, el.inferType());
        }
        return r;
    }

    private Expression objectLiteral(LObjectContext lObjectContext, Expression assignee, ClassDef assigneeType) throws CompilationError{
        ClassDef objectType;
        var objectLiteral = lObjectContext.objectLiteral();
        Expression objectTypeExpr = null;
        List<PropertyAssignmentContext> propertyAssignments = objectLiteral.propertyAssignment();

        List<ObjectLiteralKVDef> objectLiteralKVDefs = new ArrayList<>();
        List<PropertyAssignmentContext> propertyAssignment = objectLiteral.propertyAssignment();
        for (PropertyAssignmentContext p : propertyAssignment) {
            if (p instanceof PropertyShorthandContext px) {
                var expr = this.expression(px.expression());
                objectLiteralKVDefs.add(new KVCollectionExpandoDef(functionDef, expr, unit.sourceLocation(px)));
            } else if (p instanceof PropertyExpressionAssignmentContext pea) {
                var pname = this.propertyName(pea.propertyName());
                objectLiteralKVDefs.add(new KVPairDef(functionDef, pname, this.expression(pea.expression()), unit.sourceLocation(pea)));
            }
        }

        if(objectLiteral.declarationType() != null){
            Expression typeExpr = unit.parseType(functionDef, objectLiteral.declarationType(), false, false);
            objectType = extractType(typeExpr);
            objectTypeExpr = typeExpr;
        } else if(assigneeType != null){
            objectType = assigneeType;
            if(!objectType.isTop()){
                throw unit.typeError(lObjectContext, "implicit Object/Map class through assignment must be a top class");
            } else {
                objectTypeExpr = new ConstClass(objectType);
            }
        } else {
            // default type is Map<String, Object> the key is always String
            if(propertyAssignments.isEmpty()){
                var m = functionDef.getOrCreateGenericInstantiationClassDef(root.getHashMapClass(), new ClassRefLiteral[]{
                        root.getStringClass().toClassRefLiteral(),
                        root.getObjectClass().toClassRefLiteral()
                }, null);
                if(m instanceof ConcreteType c) functionDef.registerConcreteType(c);
                functionDef.idOfClass(root.getHashMapClass());
                objectType = m;
                objectTypeExpr = new ConstClass(objectType);
            } else {
                var first = objectLiteralKVDefs.getFirst();
                ClassDef keyType = first.getKeyType();
                ClassDef valueType =  first.getValueType();

                var m = functionDef.getOrCreateGenericInstantiationClassDef(root.getHashMapClass(), new ClassRefLiteral[]{
                        keyType.toClassRefLiteral(),
                        valueType.toClassRefLiteral()
                }, null);
                if(m instanceof ConcreteType c) functionDef.registerConcreteType(c);
                functionDef.idOfClass(root.getHashMapClass());
                objectType = (ClassDef) m;
                objectTypeExpr = new ConstClass(objectType);
            }
        }
        Compiler.processClassTillStage(objectType, CompilingStage.AllocateSlots);
        if(propertyAssignments.isEmpty()){
            return new Creator(functionDef, objectTypeExpr, new ArrayList<>(), unit.sourceLocation(lObjectContext));
        }

        if(root.getAnyMapClass().isThatOrSuperOfThat(objectType)){  // Map Literal
            var mapType = root.getAnyMapClass().asThatOrSuperOfThat(objectType);
            ClassRefLiteral[] arr = mapType.getGenericSource().typeArguments();
            ClassDef keyType = arr[0].getClassDefValue();
            ClassDef valueType = arr[1].getClassDefValue();
            return new ComplexMapLiteral(functionDef, objectTypeExpr, keyType, valueType, objectLiteralKVDefs);
        } else {        // Object
            for (int i = 0; i < propertyAssignments.size(); i++) {
                PropertyAssignmentContext pa = propertyAssignments.get(i);
                var kvDef = objectLiteralKVDefs.get(i);
                if (pa instanceof PropertyExpressionAssignmentContext pe) {
                    var n = this.propertyName(pe.propertyName());
                    if (n instanceof StringLiteral s) {
                        String a = s.getString();
                        var attrPair = objectType.getAttribute(a);
                        if (attrPair != null) {
                            ((KVPairDef)kvDef).setValue(this.assigner(pe.expression(), null, attrPair.getGetter().getResultType()));
                        } else {
                            Field field = objectType.getFields().get(a);
                            if (field == null) {
                                throw unit.resolveError(pe.propertyName(), "'%s' is not attribute or field".formatted(a));
                            } else if (!field.isPublic()) {
                                throw unit.resolveError(pe.propertyName(), "'%s' is not public field".formatted(a));
                            }
                            ((KVPairDef)kvDef).setValue(this.assigner(pe.expression(), null, field.getType()));
                        }
                    } else {    // for expression
                        throw unit.syntaxError(pe, "expression key can only apply to Map");
                    }
                }
            }
            return new ComplexObjectLiteral(functionDef, objectTypeExpr, objectLiteralKVDefs);
        }
    }

    private Expression propertyName(PropertyNameContext propertyNameContext) throws CompilationError {
        if(propertyNameContext instanceof IdPropertyNameContext id){
            return getRoot().createStringLiteral(id.getText()).setSourceLocation(unit.sourceLocation(propertyNameContext));
        } else if(propertyNameContext instanceof StringPropertyNameContext s){
            String s1 = Compiler.parseStringLiteral(s.STRING_LITERAL());
            return getRoot().createStringLiteral(s1).setSourceLocation(unit.sourceLocation(propertyNameContext));
        } else if(propertyNameContext instanceof ExpressionPropertyNameContext expr){
            return this.expression(expr.expression()).setSourceLocation(unit.sourceLocation(propertyNameContext));
        } else {
            throw new IllegalArgumentException("unexpected property name");
        }
    }

    private Expression memberAccessExpr(MemberAccessExprContext memberAccessExpr) throws CompilationError {
        var left = expression(memberAccessExpr.expression());
        MethodCallContext methodCall = memberAccessExpr.methodCall();
        boolean nullConditional = memberAccessExpr.bop.getText().equals("?.");
        if(methodCall != null){
            if(nullConditional){
                left = new NullableValue(functionDef, left).setSourceLocation(unit.sourceLocation(memberAccessExpr)).nonNullPlaceHolder();
            }
            return this.methodCall(left, methodCall);
        } else {
            var namePath = memberAccessExpr.namePath();
            if(nullConditional){
                return nullableIfThenExpr(functionDef, left, baseOfLeft ->
                    new NamePathResolver(NamePathResolver.ResolveMode.ForValue, unit, this.functionDef, baseOfLeft, (FormalNamePathContext) namePath).resolve()
                );
            } else {
                return new NamePathResolver(NamePathResolver.ResolveMode.ForValue, unit, this.functionDef, left, (FormalNamePathContext) namePath).resolve();
            }
        }
    }

    public interface ExpressionSupplierOnNullableBase{
        Expression apply(Expression nonNullExpression) throws CompilationError;
    }

    public interface StatementSupplierOnNullableBase{
        Statement apply(Expression nonNullExpr) throws CompilationError;
    }

    public static Expression nullableIfThenExpr(FunctionDef functionDef, Expression maybeNullExpr, ExpressionSupplierOnNullableBase resultExprSupplier) throws CompilationError {
        var exprType = maybeNullExpr.inferType();
        if(!(exprType instanceof NullableClassDef nullableClassDef)){
            throw new TypeMismatchError("nullable class expected", maybeNullExpr.getSourceLocation());
        }

        NullableValue nullableResult = maybeNullExpr instanceof NullableValue n ? n : new NullableValue(functionDef, maybeNullExpr);

        var notEqNull = nullableResult.isNotNull();

        var right = resultExprSupplier.apply(nullableResult.nonNullValue());

        var nullableResultType = functionDef.getOrCreateNullableType(right.inferType(), null);
        right = functionDef.cast(right, nullableResultType).setSourceLocation(right.getSourceLocation()).transform();
        var root = functionDef.getRoot();
        return new IfElseExpr(functionDef, right, notEqNull, root.nullLiteral());
    }

    public static Statement nullableIfThenStmt(FunctionDef functionDef, Expression maybeNullExpr, StatementSupplierOnNullableBase resultExprSupplier) throws CompilationError {
        var exprType = maybeNullExpr.inferType();
        if(!(exprType instanceof NullableClassDef nullableClassDef)){
            throw new TypeMismatchError("nullable class expected", maybeNullExpr.getSourceLocation());
        }
        NullableValue nullableResult = maybeNullExpr instanceof NullableValue n ? n : new NullableValue(functionDef, maybeNullExpr);

        var notEqNull = nullableResult.isNotNull();

        var right = resultExprSupplier.apply(nullableResult.nonNullValue());

        return new IfThenElseStmt(functionDef, notEqNull, right,null);
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
            resolver = new NamePathResolver(NamePathResolver.ResolveMode.ForInvokable, this.unit, this.functionDef, left, ((FormalNamePathContext)namePath));
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
                return new FunctionApply(functionDef, extractInvokeMode(methodCall), invocation, forkContext);
            } else if(root.getFunctionBaseOfAnyClass().isThatOrSuperOfThat(inferType)){  // Function<R>
                return new InvokeFunctor(functionDef, Invoke.InvokeMode.Invoke, invocation, forkContext);
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
        var invoke = new Invoke(functionDef, invokeMode, resolved, values, unit.sourceLocation(methodCall));
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
                if(!r.inferType().isDeriveFrom(this.root.getForkContextInterface())){
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

        return new Creator(functionDef, typeExpr, values, unit.sourceLocation(creatorContext), constructorName).transform();
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
        if(type instanceof PhantomMetaClassDef) type = root.getObjectClass();
        var v = new Variable();
        v.setType(type);
        v.setOwnerClass(this.functionDef);
        v.setModifiers(AgoClass.PRIVATE);
        SlotDef slot = getSlotsAllocator().acquireRegister(type);
        v.setSlot(slot);
        v.setName(slot.getName());
        var r = reusable ? new Var.ReusingLocalVar(functionDef, v, Var.LocalVar.VarMode.Temp): new Var.LocalVar(functionDef, v, Var.LocalVar.VarMode.Temp);
        if(reusable){
            getSlotsAllocator().lockRegister(slot);
            this.reusableTempVariables.put(expression, r);
        }
        return r;
    }

    public Var.LocalVar acquireTempVar(ClassDef type) throws CompilationError {
        if(type instanceof PhantomMetaClassDef) type = root.getObjectClass();
        var v = new Variable();
        v.setType(type);
        if(type instanceof ConcreteType c){
            functionDef.registerConcreteType(c);
        }
        v.setOwnerClass(this.functionDef);
        v.setModifiers(AgoClass.PRIVATE);
        SlotDef slot = getSlotsAllocator().acquireRegister(type);
        v.setSlot(slot);
        v.setName(slot.getName());
        return new Var.LocalVar(functionDef, v, Var.LocalVar.VarMode.Temp);
    }

    public Var.NarrowTypingLocalVar acquireNarrowTypingVar(Variable variable, ClassDef narrowType) throws CompilationError {
        var v = new Variable();
        v.setType(narrowType);
        if(narrowType instanceof ConcreteType c){
            functionDef.registerConcreteType(c);
        }
        v.setOwnerClass(this.functionDef);
        v.setName(variable.getName());
        v.setSourceLocation(variable.getSourceLocation());
        v.setModifiers(variable.getModifiers());        // slot not allocated, will allocate when invoked, and don't register variable in functionDef.variables
        return new Var.NarrowTypingLocalVar(functionDef, v, variable);
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
        return new Label(functionDef, nextLabelId++, this.code);
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
        VariableScope variableScope = functionDef.enterVariableScope();
        for (BlockStatementContext blockStatementContext : block.blockStatement()) {
            var st = this.blockStatement(blockStatementContext);
            if(st != null) statements.add(st);
        }
        functionDef.leaveVariableScope();
        return new BlockStmt(functionDef, statements).setSourceLocation(unit.sourceLocation(block));

    }

    private Statement ifStmt(IfStmtContext ifStmt) throws CompilationError {
        this.narrowTyper.enter();
        var cond = narrowType(ifStmt.parExpression());
        var mapper = this.narrowTyper.exit();

        Statement trueBranch;
        Statement falseBranch;

        if(mapper.isDirty()) {
            this.narrowTyper.reenter(mapper, true);
            trueBranch = statement(ifStmt.trueBranch);
            narrowTyper.exit();
        } else {
            trueBranch = statement(ifStmt.trueBranch);
        }

        if (ifStmt.falseBranch == null) {
            falseBranch = null;
        } else {
            if(mapper.isDirty()) {
                this.narrowTyper.reenter(mapper, false);
                falseBranch = statement(ifStmt.falseBranch);
                narrowTyper.exit();
            } else {
                falseBranch = statement(ifStmt.falseBranch);
            }
        }

        return new IfThenElseStmt(functionDef, cond, trueBranch, falseBranch);
    }

    private Expression narrowType(ExpressionContext expression) throws CompilationError {
        if(expression instanceof PrimaryExprContext primaryExprContext){
            var expr = expression(primaryExprContext);
            if(expr.inferType() instanceof NullableClassDef n) {
                if (expr instanceof Var.LocalVar localVar) {
                    Var.NarrowTypingLocalVar nonNullValueReceiver = acquireNarrowTypingVar(localVar.variable, n.getBaseClass());
                    var nullValue = acquireNarrowTypingVar(localVar.variable, root.NULL());
                    narrowTyper.collectNarrowVar(nonNullValueReceiver, nullValue);
                    return new NullableValue(functionDef, localVar, nonNullValueReceiver);
                }
            }
            return expr;
        } else if(expression instanceof EqualsExprContext equalsExprContext){
            return this.equalsExpr(equalsExprContext).transform();
        }
        return expression(expression);
    }

    private Expression narrowType(ParExpressionContext parExpression) throws CompilationError {
        if(parExpression.expression() != null){
            return narrowType(parExpression.expression());
        } else {
            return parExpression(parExpression);
        }
    }

    private AndExpr andExpr(AndExprContext andExpr) throws CompilationError {
        this.narrowTyper.enter();
        Expression left = narrowType(andExpr.expression(0));
        var leftMapper = narrowTyper.peek();   // don't exit

        this.narrowTyper.enter();
        Expression right = narrowType(andExpr.expression(1));
        var rightMapper = this.narrowTyper.exit();

        this.narrowTyper.exit();       // exit left

        if(this.narrowTyper.isCollecting()){   // still collecting
            narrowTyper.updateCurrent(leftMapper.intersect(this, rightMapper), true);
        }

        return new AndExpr(functionDef, left, right);
    }

    private Expression orExpr(OrExprContext orExpr) throws CompilationError {
        this.narrowTyper.enter();
        Expression left = narrowType(orExpr.expression(0));
        var leftMapper = narrowTyper.exit();

        this.narrowTyper.enter();
        Expression right = narrowType(orExpr.expression(1));
        var rightMapper = this.narrowTyper.exit();

        if(this.narrowTyper.isCollecting()){   // still collecting
            narrowTyper.updateCurrent(leftMapper.union(this, rightMapper), true);
        }

        return new OrExpr(functionDef, left, right);
    }

    public NarrowTyper getNarrowTyper() {
        return narrowTyper;
    }

    private Expression equalsExpr(EqualsExprContext equalsExpr) throws CompilationError {
        Equals.Type type = switch(equalsExpr.bop.getType()){
            case EQUAL -> Equals.Type.Equals;
            case NOTEQUAL -> Equals.Type.NotEquals;
//                case IDENTITY_EQUAL -> ;
//                case NOT_IDENTITY_EQUAL ->
            default -> throw new UnsupportedOperationException("'%s' not supported".formatted(equalsExpr.bop));
        };
        Expression left = expression(equalsExpr.expression(0));
        Expression right = expression(equalsExpr.expression(1));
        return new Equals(this, left, right, type).setSourceLocation(unit.sourceLocation(equalsExpr));
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
            narrowTyper.enter();
            var expression = this.narrowType(enhancedForControl.expression()).transform();
            var mapper = narrowTyper.exit();
            ClassDef expressionType = expression.inferType();
            if(expressionType instanceof NullableClassDef nullableClassDef){
                expressionType = nullableClassDef.getBaseClass();
            }
            this.narrowTyper.reenter(mapper, true);
            var r = forEachStmt(forStmt, expression, expressionType, forControl, enhancedForControl, label);
            this.narrowTyper.exit();
            return r;
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
            return new ForStmt(functionDef, label,init, condition, updateStatement, body);
        }
    }

    private ForEachStmt forEachStmt(ForStmtContext forStmt, Expression expression, ClassDef expressionType, ForControlContext forControl, EnhancedForControlContext enhancedForControl, String label) throws CompilationError {
        ForEachStmt.Mode mode = ForEachStmt.Mode.Iterable;
        ClassDef concreteType = unit.getRoot().getAnyIterableInterface().asThatOrSuperOfThat(expressionType);
        if(concreteType == null){
            mode = ForEachStmt.Mode.Iterator;
            concreteType = unit.getRoot().getAnyIteratorInterface().asThatOrSuperOfThat(expressionType);
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
        if(enhancedForControl.variableType() != null){
            type = unit.extractType(unit.parseType(functionDef, enhancedForControl.variableType(), false,false));
        } else {
            var arg = concreteType.getGenericSource().typeArguments()[0];
            type = arg.getClassDefValue();
        }
        Compiler.processClassTillStage(type, CompilingStage.AllocateSlots);

        var iterVar = defineLocalVar(enhancedForControl.identifier(),variableModifiers, type);
        return new ForEachStmt(functionDef, label, iterVar, expression, statement(forStmt.statement()), mode, unit.sourceLocation(enhancedForControl));
    }

    private BlockStmt expressionList(ExpressionListContext expressionList) throws CompilationError {
        if(expressionList == null) return null;
        List<Statement> ls = new ArrayList<>();
        for (ExpressionContext expression : expressionList.expression()) {
            ls.add(new ExpressionStmt(functionDef, this.expression(expression)));
        }
        return new BlockStmt(functionDef, ls).setSourceLocation(unit.sourceLocation(expressionList));
    }

    private Statement whileStmt(WhileStmtContext whileStmt) throws CompilationError {
        var cond = parExpression(whileStmt.parExpression());
        String label = whileStmt.label() != null ? whileStmt.label().identifier().getText() : null;
        return new WhileStmt(functionDef, label, cond, statement(whileStmt.statement()));
    }
    DoWhileStmt doWhileStmt(DoWhileStmtContext doWhileStmt) throws CompilationError {
        var cond = parExpression(doWhileStmt.parExpression());
        String label = doWhileStmt.label() != null ? doWhileStmt.label().identifier().getText() : null;
        return new DoWhileStmt(functionDef, label, cond, statement(doWhileStmt.statement()));
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
        return new SwitchCaseStmt(functionDef, condition, switchGroups);
    }

    private SwitchCaseStmt.Case switchLabel(SwitchLabelContext switchLabel) throws CompilationError {
        SwitchCaseStmt.Case cs;
        if(switchLabel.constantExpression != null){
            cs = new SwitchCaseStmt.Case(SwitchCaseStmt.CaseKind.ConstExpression, expression(switchLabel.constantExpression));
        } else if(switchLabel.enumConstantName != null) {
            Expression constExpr = unit.resolveNamePath(this.functionDef, this.functionDef, switchLabel.namePath(), NamePathResolver.ResolveMode.ForVariable);
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
                    if(!exceptionType.isThatOrDerivedFromThat(root.getThrowableClass())){
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
        return new TryCatchFinallyStmt(functionDef, tryBlock, catchCauses, finallyBlock);
    }

    ThrowStmt throwStmt(ThrowStmtContext throwStmt) throws CompilationError {
        return new ThrowStmt(functionDef, expression(throwStmt.expression()));
    }

    Statement breakStmt(BreakStmtContext breakStmt){
        var id = breakStmt.identifier();
        return new BreakStmt(functionDef, id == null ? null : id.getText());
    }

    Statement continueStmt(ContinueStmtContext continueStmt){
        var id = continueStmt.identifier();
        return new ContinueStmt(functionDef, id == null ? null : id.getText());
    }

    void yieldStmt(YieldStmtContext yieldStmt){

    }

    Statement withStmt(WithStmtContext withStmt) throws CompilationError {
        Expression withExpr = parExpression(withStmt.parExpression());
        if(withExpr.inferType() instanceof NullableClassDef){
            return nullableIfThenStmt(functionDef, withExpr, nonNullExpression ->
                    withStmt(withStmt, nonNullExpression));
        }
        return withStmt(withStmt, withExpr);
    }

    private WithStmt withStmt(WithStmtContext withStmt, Expression withExpr) throws CompilationError {
        var expression = new CurrWithExpression(functionDef, withExpr);
        withExpressionStack.push(expression);
        var stmt = statement(withStmt.statement());
        withExpressionStack.pop();
        return new WithStmt(functionDef, expression, stmt);
    }

    private Expression withExpr(PostWithExprContext postWithExpr) throws CompilationError {
        Expression withExpr = this.expression(postWithExpr.expression());
        if(withExpr.inferType() instanceof NullableClassDef){
            return nullableIfThenExpr(functionDef, withExpr, nonNullExpression ->
                    withExpr(postWithExpr, nonNullExpression));
        }
        return withExpr(postWithExpr, withExpr);
    }

    private WithExpr withExpr(PostWithExprContext postWithExpr, Expression withExpr) throws CompilationError {
        var expression = new CurrWithExpression(functionDef, withExpr);
        withExpressionStack.push(expression);
        var stmt = this.statement(postWithExpr.postWith().statement());
        withExpressionStack.pop();
        return new WithExpr(functionDef, expression, stmt);
    }

    Statement viaStmt(ViaStmtContext viaStmt ) throws CompilationError {
        Expression par = this.parExpression(viaStmt.parExpression());
        if(par.inferType() instanceof NullableClassDef){
            return nullableIfThenStmt(functionDef, par, nonNullExpression ->
                    viaStmt(viaStmt, nonNullExpression));
        }
        return viaStmt(viaStmt, par);
    }

    private ViaStmt viaStmt(ViaStmtContext viaStmt, Expression par) throws CompilationError {
        if(!par.inferType().isDeriveFrom(this.getFunctionDef().getRoot().getViaObjectInterface())){
            throw new TypeMismatchError("a ViaObject expected", par.getSourceLocation());
        }
        Statement stmt = this.statement(viaStmt.statement());
        return new ViaStmt(functionDef, par, stmt);
    }

    private Statement asyncInvokeFunctorStmt(AsyncInvokeFunctorStmtContext asyncInvokeFunctorStmt) throws CompilationError {
        Invoke.InvokeMode mode;
        if(asyncInvokeFunctorStmt.FORK() != null){
            mode = Invoke.InvokeMode.Fork;
        } else {
            mode = Invoke.InvokeMode.Spawn;
        }
        Expression expression = expression(asyncInvokeFunctorStmt.expression()).transform();
        if(!root.getFunctionBaseOfAnyClass().isThatOrSuperOfThat(expression.inferType())){
            throw new TypeMismatchError("functor expected",expression.getSourceLocation());
        }
        return new AsyncInvokeFunctorStmt(functionDef, mode, expression);
    }

    private Expression invokeFunctor(AwaitFunctorContext invokeFunctorContext) throws CompilationError {
        var f = expression(invokeFunctorContext.expression()).transform();
        ViaForkContextContext viaForkContext = invokeFunctorContext.viaForkContext();
        Expression forkContextExpr = null;
        if(viaForkContext != null) {
            forkContextExpr = this.expression(viaForkContext.forkContext);
            if(!forkContextExpr.inferType().isDeriveFrom(this.root.getForkContextInterface())){
                throw unit.typeError(viaForkContext.forkContext, "'lang.ForkContext' expected");
            }
        }
        if (!root.getFunctionBaseOfAnyClass().isThatOrSuperOfThat(f.inferType())) {
            throw new TypeMismatchError("functor expected", f.getSourceLocation());
        }
        return new InvokeFunctor(functionDef, Invoke.InvokeMode.Await, f, forkContextExpr);
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
        if(expression instanceof ExpressionBase expressionBase){
            expressionBase.releaseTempVariables(this);
        }
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

    private Expression valueFromNullable(ValueFromNullableContext valueFromNullableContext) throws CompilationError {
        var expr = expression(valueFromNullableContext.expression()).transform();
        ClassDef classDef = expr.inferType();
        if(!(classDef instanceof NullableClassDef)){
            throw unit.typeError(valueFromNullableContext, "nullable expression expected");
        }
        return new Cast(functionDef, expr, ((NullableClassDef) classDef).getBaseClass());
    }

}
