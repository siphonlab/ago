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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.SourceMapEntry;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.ClassRefLiteral;
import org.siphonlab.ago.compiler.expression.logic.Not;
import org.siphonlab.ago.compiler.generic.GenericTypeCode;
import org.siphonlab.ago.compiler.generic.InstantiationArguments;
import org.siphonlab.ago.compiler.statement.*;
import org.siphonlab.ago.compiler.parser.AgoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FunctionDef extends ClassDef {

    private final static Logger LOGGER = LoggerFactory.getLogger(FunctionDef.class);

    public static final int FUNCTION_SLOTS_SIZE_PLACEHOLDER = 0x1000_0000;

    private final AgoParser.MethodDeclarationContext methodDecl;

    private Map<String, Variable> localVariables = new LinkedHashMap<>();
    private List<Parameter> parameters = new ArrayList<>();

    private ClassDef resultType;
    private String nativeEntrance;
    private int nativeResultSlot;

    private String commonName;

    protected int[] body;

    private List<Variable> runSpaces;
    private ClassDef functionInterfaceInstantiation;

    List<SwitchTable> switchTables;
    private final TryCatchTable tryCatchTable = new TryCatchTable(this);
    private List<ClassDef> throwsExceptions = new ArrayList<>();
    private List<SourceMapEntry> sourceMap;

    public FunctionDef(String name, AgoParser.MethodDeclarationContext methodDecl){
        this(name, methodDecl, AgoClass.PUBLIC);
    }

    public FunctionDef(String name, AgoParser.MethodDeclarationContext methodDecl, int modifiers) {
        super(appendFix(name, modifiers));
        this.methodDecl = methodDecl;
        this.classType = AgoClass.TYPE_FUNCTION;
        this.modifiers = modifiers;
        var p = name.lastIndexOf('#');
        if(p != -1){
            commonName = name.substring(0, p);
        } else {
            commonName = name;
        }
    }

    public ParserRuleContext getDeclarationAst(){
        return this.getMethodDecl();
    }

    public ParserRuleContext getDeclarationName(){
        return methodDecl.methodName();
    }

    private static String appendFix(String name, int modifiers){
        if(name.lastIndexOf('#') == -1){
            if((modifiers & AgoClass.GETTER) != 0){
                return name + "#get";
            } else if((modifiers & AgoClass.SETTER) != 0){
                return name + "#set";
            } else {
                return name + '#';
            }
        } else {
            return name;
        }
    }

    public AgoParser.MethodDeclarationContext getMethodDecl() {
        return methodDecl;
    }
    
    public ParserRuleContext getMethodBodyContext() {
    	return methodDecl != null ? methodDecl.methodBody() : null;
    }
    

    @Override
    public AgoParser.GenericTypeParametersContext getGenericTypeParametersContextAST() {
        return methodDecl.genericTypeParameters();
    }

    @Override
    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;
        if(this.getGenericSource() != null){
            this.nextCompilingStage(CompilingStage.ValidateHierarchy);
            return true;
        }

        if(!executeParseFieldsOfHierarchyClasses()) return false;

        if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: parse function fields".formatted(this));
        var methodDecl = getMethodDecl();
        if (methodDecl.typeOfFunction() != null) {
            AgoParser.TypeOfFunctionContext typeOfFunction = methodDecl.typeOfFunction();
            ClassDef r = unit.parseType(this, typeOfFunction);
            this.setResultType(r);
        } else {
            this.setResultType(PrimitiveClassDef.VOID);
        }
        var formalParameters = methodDecl.formalParameters();
        unit.parseFormalParameters(this, formalParameters);
        this.processFieldParameters();

        AgoParser.MethodBodyContext methodBody = methodDecl.methodBody();
        //TODO scan filed in local variables
        // local variables will be compiled in BlockCompiler.visitLocalVariableDeclaration

        AgoParser.ThrowsPhraseContext throwsPhrase = this.methodDecl.throwsPhrase();
        if(throwsPhrase != null){
            List<ClassDef> exceptionTypes = new ArrayList<>();
            for (AgoParser.DeclarationTypeContext declarationTypeContext : throwsPhrase.declarationTypeList().declarationType()) {
                ClassDef exceptionType = unit.parseTypeName(this, declarationTypeContext.namePath(), false);
                for (ClassDef existed : exceptionTypes) {
                    if(exceptionType.isDeriveFrom(existed)){
                        throw unit.typeError(declarationTypeContext,"'%s' is derived from '%s'".formatted(exceptionType.getFullname(), existed.getFullname()));
                    } else if(exceptionType == existed){
                        throw unit.typeError(declarationTypeContext,"'%s' duplicated".formatted(exceptionType.getFullname()));
                    } else if(existed.isDeriveFrom(exceptionType)) {
                        throw unit.typeError(declarationTypeContext, "'%s' is derived from '%s'".formatted(existed.getFullname(), exceptionType.getFullname()));
                    } else if(getRoot().getRuntimeExceptionClass().isThatOrSuperOfThat(exceptionType)){
                        throw unit.typeError(declarationTypeContext, "shouldn't throw runtime exception '%s'".formatted(exceptionType.getFullname()));
                    }
                }
                exceptionTypes.add(exceptionType);
            }
            this.throwsExceptions = exceptionTypes;
        }

        this.createFunctionInterface();
        this.createFieldsOfTrait();

        this.nextCompilingStage(CompilingStage.ValidateHierarchy);
        return true;
    }

    @Override
    public void setSuperClass(ClassDef superClass) {
        super.setSuperClass(superClass);
    }

    protected void processFieldParameters() throws SyntaxError {
        for (Parameter parameter : this.parameters) {
            if(parameter.isField()){
                throw unit.syntaxError(parameter.parameterContext, "redundant 'field' modifier, all parameter are function fields");
            }
        }
    }

    @Override
    public AgoParser.DeclarationTypeContext getBaseTypeDecl(){
        return null;
    }

    @Override
    public List<AgoParser.InterfaceItemContext> getInterfaceDecls() {
        if(this.methodDecl == null) return null;
        AgoParser.ImplementsPhraseContext implementsPhraseContext = this.methodDecl.implementsPhrase();
        return implementsPhraseContext != null? implementsPhraseContext.interfaceList().interfaceItem() : null;
    }

    public Map<String, Variable> getLocalVariables() {
        return localVariables;
    }

    public void addLocalVariableWithSlot(Variable variable) throws CompilationError {
        this.getLocalVariables().put(variable.name, variable);
        if(variable.getType().getTypeCode() == TypeCode.OBJECT){
            this.idOfConstString(variable.getType().getFullname());
        }
        variable.setSlot(this.getSlotsAllocator().allocateSlot(variable));
    }

    public void addLocalVariable(Variable variable){
        this.getLocalVariables().put(variable.name, variable);
        if(variable.getType().getTypeCode() == TypeCode.OBJECT){
            this.idOfConstString(variable.getType().getFullname());
        }
    }

    @Override
    public Variable getVariable(String name) {
        var v = this.localVariables.get(name);
        if(v != null) return v;
        return super.getVariable(name);
    }

    public void addParameter(Parameter parameter){
        this.parameters.add(parameter);
        this.addField(parameter);
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public int[] getBody() {
        return body;
    }

    public void setBody(int[] body) {
        this.body = body;
        if(body == null || body.length == 0){
            this.modifiers |= AgoClass.EMPTY_METHOD;
        }
    }

    @Override
    public void inheritsFields() throws CompilationError {
        if(this.getGenericSource() != null) {
            this.instantiateFields();
            return;
        }
        if(this.compilingStage == CompilingStage.InheritsFields) {
            if(LOGGER.isDebugEnabled()) LOGGER.debug("%s: inherits fields".formatted(this));
            // FunctionDef has no superclass
            setCompilingStage(CompilingStage.ValidateNewFunctions);
        }
    }

    public ClassDef getResultType() {
        return resultType;
    }

    public void setResultType(ClassDef resultType) {
        Objects.requireNonNull(resultType);
        this.resultType = resultType;
        if(resultType instanceof ParameterizedClassDef.PlaceHolder placeHolder){
            placeHolder.registerReference(new ParameterizedClassDef.RefViaFunctionResult(this));
        } else {
            if (resultType instanceof ConcreteType c) {
                this.registerConcreteType(c);
            } else {
                if (!resultType.isPrimitive())
                    this.idOfClass(resultType);
            }
        }
    }

    public void setNativeEntrance(String nativeEntrance) {
        this.nativeEntrance = nativeEntrance;
    }

    public String getNativeEntrance() {
        return nativeEntrance;
    }

    public void setNativeResultSlot(int nativeResultSlot) {
        this.nativeResultSlot = nativeResultSlot;
    }

    public int getNativeResultSlot() {
        return nativeResultSlot;
    }

    @Override
    public void allocateSlotsForFields() throws CompilationError {
        if (this.compilingStage != CompilingStage.AllocateSlots) return;

        super.allocateSlotsForFields();

        if(this.getGenericSource() != null) return;     // slots already created by instantiateSlots

        // usually the variables come from BlockCompiler, for VarDeclaration
        // that means, this.localVariables is empty
        for (Variable variable : this.localVariables.values()) {
            variable.setSlot(this.slotsAllocator.allocateSlot(variable));
        }

        if(this.isNative()){
            if(this.resultType.getTypeCode() != TypeCode.VOID) {
                var slot = this.getSlotsAllocator().allocateRegisterSlot(this.resultType);
                this.setNativeResultSlot(slot.getIndex());
            }
        }
    }

    @Override
    protected void instantiateSlots() throws CompilationError {
        if(this.compilingStage != CompilingStage.AllocateSlots) return;
        FunctionDef templ = (FunctionDef) this.getTemplateClass();
        if(templ.compilingStage == CompilingStage.AllocateSlots){
            templ.allocateSlotsForFields();
        }
        var args = this.getGenericSource().instantiationArguments();
        for (SlotDef slot : templ.slotsAllocator.slots) {
            var variable = slot.getVariable();
            if(slot.isRegister()) {
                if(slot.getClassDef() == templ.resultType) {
                    var resultSlot = this.slotsAllocator.allocateRegisterSlot(this.resultType);
                    this.setNativeResultSlot(resultSlot.getIndex());
                } else {
                    this.slotsAllocator.allocateRegisterSlot(slot.getClassDef().instantiate(args, null));
                }
            } else if(variable instanceof Field field){
                var myFld = this.fields.get(variable.name);
                if(myFld == null){
                    this.slotsAllocator.allocateSlot(slot.getName(), slot.getTypeCode(), slot.getClassDef().instantiate(args, null));
                } else {
                    myFld.setSlot(this.slotsAllocator.allocateSlot(myFld));
                }
            } else if(variable != null && this.localVariables.containsKey(variable.name)){
                var myvar = this.localVariables.get(variable.name);
                if(myvar == null) {
                    this.slotsAllocator.allocateSlot(slot.getName(), slot.getTypeCode(), slot.getClassDef().instantiate(args, null));
                } else {
                    myvar.setSlot(this.slotsAllocator.allocateSlot(myvar));
                }
            } else {
                throw new RuntimeException("variable '%s' not found in '%s'".formatted(variable, this));
            }
        }

        this.nextCompilingStage(CompilingStage.CompileMethodBody);
    }

    @Override
    public String toString() {
        if(this.compilingStage != CompilingStage.Compiled){
            return "(Function %s %s)".formatted(this.getFullname(), this.compilingStage);
        }
        return "(Function %s)".formatted(this.getFullname());
    }

    public boolean isConstructor(){
        return (this.modifiers & AgoClass.CONSTRUCTOR) != 0;
    }

    public boolean isEmptyMethod(){
        return (this.modifiers & AgoClass.EMPTY_METHOD) != 0;
    }

    public String getCommonName() {
        return commonName;
    }

    @Override
    public AgoParser.ClassBodyContext getClassBody() {
        throw new UnsupportedOperationException();
    }

    public boolean isEmptyArgs(){
        return CollectionUtils.isEmpty(this.getParameters());
    }

    public boolean isOverride(){
        return (this.modifiers & AgoClass.OVERRIDE) != 0;
    }

    @Override
    public boolean isNameMatch(String text) {
        return this.commonName.equals(text) || this.name.equals(text);
    }

    boolean isSameSignatureWith(FunctionDef another){
        if(another.getParameters().size() != parameters.size()) return false;

        List<Parameter> anotherParameters = another.getParameters();
        for (int i = 0; i < anotherParameters.size(); i++) {
            Parameter anotherParam = anotherParameters.get(i);
            Parameter myParam = this.parameters.get(i);
            if (anotherParam.getType() != myParam.getType() && !(
                    myParam.getType() instanceof GenericTypeCode.GenericCodeAvatarClassDef &&
                    anotherParam.getType() instanceof GenericTypeCode.GenericCodeAvatarClassDef)){
                return false;
            }
        }
        return true;
    }

    public FunctionDef cloneForInstantiate(InstantiationArguments instantiationArguments, MutableBoolean returnExisted) throws CompilationError {
        var clone = new FunctionDef(name, methodDecl);
        cloneTo(instantiationArguments, clone);
        return clone;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    @Override
    public boolean instantiateFields() throws CompilationError {
        if(this.getCompilingStage() != CompilingStage.InheritsFields) return false;

        FunctionDef templ = (FunctionDef) this.getTemplateClass();
        if(templ.getCompilingStage().lte( CompilingStage.InheritsFields)){
            Compiler.processClassTillStage(templ,CompilingStage.InheritsFields);
        }

        var instantiationArguments = this.getGenericSource().instantiationArguments();

        Map<Parameter, Parameter> ps = new HashMap<>();
        for (Map.Entry<String, Field> fieldEntry : templ.getFields().entrySet()) {
            Field field = fieldEntry.getValue();
            Field newField = field.applyTemplate(instantiationArguments, this);
            this.addField(newField);
            if (newField instanceof Parameter p) {
                ps.put((Parameter) field, p);
            }
        }
        for (Parameter parameter : templ.getParameters()) {
            this.addParameter(ps.get(parameter));
        }

        for (var entry : templ.getLocalVariables().entrySet()) {
            Variable variable = entry.getValue();
            this.addLocalVariable(variable.applyTemplate(instantiationArguments, this));
        }
        this.setResultType(templ.resultType.instantiate(instantiationArguments, null));

        this.instantiateFieldsForInterfacesAndTraits();
        this.createFunctionInterface();

        this.nextCompilingStage(CompilingStage.ValidateNewFunctions);
        return true;
    }

    // auto create function interface from `Function0<+R>` `Function1<+R, -P>`
    public void createFunctionInterface() throws CompilationError {
        ClassDef functionBaseClass = getRoot().getFunctionBaseClass();
        this.setSuperClass((functionBaseClass.instantiate(new InstantiationArguments(functionBaseClass.getTypeParamsContext(), new ClassRefLiteral[]{new ClassRefLiteral(this.resultType)}), null)));

        var interface_ = getRoot().getFunctionInterface(this.parameters.size());
        if(interface_ == null) return;

        List<ClassRefLiteral> list = new ArrayList<>(this.parameters.size() + 1);
        list.add(new ClassRefLiteral(this.resultType));
        for (Parameter p : this.parameters) {
            ClassRefLiteral refLiteral = new ClassRefLiteral(p.getType());
            list.add(refLiteral);
        }
        var args = list.toArray(new ClassRefLiteral[0]);
        var instantiated = interface_.instantiate(new InstantiationArguments(interface_.getTypeParamsContext(), args), null);
        this.registerConcreteType((ConcreteType) instantiated);
        this.addImplementedInterface(instantiated);
        assert this.functionInterfaceInstantiation == null;
        this.functionInterfaceInstantiation = instantiated;

        if(this.isNative()){
            this.implementedInterfaces.add(getRoot().getNativeFunctionInterfaceBase());
        }
    }

    public ClassDef getFunctionInterfaceInstantiation() {
        return functionInterfaceInstantiation;
    }

    public void compileBody() throws CompilationError {
        if(this.getCompilingStage() != CompilingStage.CompileMethodBody) return;
        if(this.getGenericSource() != null){
            this.nextCompilingStage(CompilingStage.Compiled);
            return;
        }

        var methodDecl = getMethodDecl();
        if(methodDecl == null) return;

        var body = methodDecl.methodBody();
        if (body instanceof AgoParser.MBBLockContext mbBlock) {
            methodBodyNotAllowedForAbstractMethod();
            var block = mbBlock.block();
            new BlockCompiler(this.unit, this, block.blockStatement()).compile();
        } else if (body instanceof AgoParser.MBNativeContext mbNative) {
            methodBodyNotAllowedForAbstractMethod();
            var s = Compiler.parseStringLiteral(mbNative.native_().STRING_LITERAL());
            this.setModifiers(getModifiers() | AgoClass.NATIVE);
            this.setNativeEntrance(s);
            this.allocateSlotsForFields();
            this.idOfConstString(s);     // add to string pool
        } else if (body instanceof AgoParser.MBEmptyContext) {
            if (this.isAbstract()) {
                //
            } else {
                throw new UnsupportedOperationException("TODO");        //TODO
            }
        } else {
            throw new UnsupportedOperationException("impossible");
        }
        this.nextCompilingStage(CompilingStage.Compiled);   // Compiled
    }

    private void methodBodyNotAllowedForAbstractMethod() throws SyntaxError {
        if (this.isAbstract()) {
            throw unit.syntaxError(this.getMethodDecl(), "method body not allowed for abstract method");
        }
    }

    public boolean isGetter() {
        return (this.modifiers & AgoClass.GETTER) != 0;
    }

    public boolean isSetter() {
        return (this.modifiers & AgoClass.SETTER) != 0;
    }


    public DenseSwitchTable createDenseSwitchTable() {
        if(switchTables == null) switchTables = new ArrayList<>();
        var r = new DenseSwitchTable(this, switchTables.size());
        switchTables.add(r);
        return r;
    }

    public SparseSwitchTable createSparseSwitchTable() {
        if(switchTables == null) switchTables = new ArrayList<>();
        var r =  new SparseSwitchTable(this, switchTables.size());
        switchTables.add(r);
        return r;
    }

    public List<SwitchTable> getSwitchTables() {
        return switchTables;
    }

    public void registerTryCatchTable(Label begin, Label end, Label handler, List<ClassDef> exceptionTypes) {
        tryCatchTable.register(begin.getResolvedAddress(),end.getResolvedAddress(), handler.getResolvedAddress(), exceptionTypes);
    }
    public TryCatchTable getTryCatchTable() {
        return tryCatchTable;
    }

    public List<ClassDef> getThrowsExceptions() {
        return throwsExceptions;
    }

    public void setThrowsExceptions(List<ClassDef> throwsExceptions) {
        this.throwsExceptions = throwsExceptions;
    }

    public void setSourceMap(List<SourceMapEntry> sourceMap) {
        this.sourceMap = sourceMap;
    }

    public List<SourceMapEntry> getSourceMap() {
        return sourceMap;
    }

    // ----------------------- functions for compose expressions  -----------------------------
    public BlockStmt blockStmt(List<Statement> statements){
        return new BlockStmt(this, statements);
    }

    public Return return_(Expression value) throws CompilationError {
        return new Return(this, value);
    }

    public Return return_() throws CompilationError {
        return new Return(this);
    }

    public ExpressionStmt expressionStmt(Expression expression) throws CompilationError {
        return new ExpressionStmt(this, expression);
    }

    public Cast cast(Expression expression, ClassDef toType, boolean forceCast) throws CompilationError {
        return new Cast(this, expression, toType, forceCast);
    }

    public Cast cast(Expression expression, ClassDef toType) throws CompilationError {
        return new Cast(this, expression, toType);
    }

    public Unbox unbox(Expression expression){
        return new Unbox(this, expression);
    }

    public Concat concat(Expression left, Expression right) throws CompilationError {
        return new Concat(this, left, right);
    }

    public Not not(Expression value) throws CompilationError {
        return new Not(this, value);
    }

    public Invoke invoke(Invoke.InvokeMode invokeMode, MaybeFunction maybeFunction, List<Expression> arguments, SourceLocation sourceLocation) throws CompilationError {
        return new Invoke(this, invokeMode, maybeFunction, arguments, sourceLocation);
    }

    public Box box(Expression expression, ClassDef expectedType, Box.BoxMode boxMode) throws CompilationError {
        return new Box(this, expression, expectedType, boxMode);
    }

    public Expression assign(Assign.Assignee assignee, Expression value) throws CompilationError {
        return Assign.to(this, assignee, value);
    }

    public ClassUnder classUnder(Expression scopeExpr, ClassDef subclass) throws CompilationError {
        return ClassUnder.create(this, scopeExpr, subclass);
    }

    public Var.LocalVar localVar(Variable variable, Var.LocalVar.VarMode varMode){
        return new Var.LocalVar(this, variable, varMode);
    }

    public Var.Field field(Expression instance, Variable variable) throws CompilationError {
        return new Var.Field(this, instance, variable);
    }

}
