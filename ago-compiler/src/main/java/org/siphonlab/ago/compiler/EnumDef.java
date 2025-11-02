package org.siphonlab.ago.compiler;



import org.agrona.collections.IntHashSet;
import org.antlr.v4.runtime.ParserRuleContext;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.DuplicatedError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.*;
import org.siphonlab.ago.compiler.statement.Return;
import org.siphonlab.ago.compiler.statement.SwitchCaseStmt;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.*;

import static org.siphonlab.ago.AgoClass.TYPE_ENUM;

public class EnumDef extends ClassDef{

    private final AgoParser.EnumDeclarationContext enumDeclarationContext;

    Map<String, Field> metaFields = new LinkedHashMap<>();

    private ConstructorDef metaClassConstructor;
    private FunctionDef metaClassValueOf;
    private FunctionDef metaClassParse;

    public EnumDef(String name, AgoParser.EnumDeclarationContext enumDeclarationContext) {
        super(name);
        this.classType = TYPE_ENUM;
        this.enumDeclarationContext = enumDeclarationContext;
        this.modifiers |= AgoClass.FINAL;
        this.enumValues = new LinkedHashMap<>();
    }

    @Override
    public AgoParser.DeclarationTypeContext getBaseTypeDecl() {
        return null;
    }

    @Override
    public ParserRuleContext getDeclarationAst() {
        return enumDeclarationContext;
    }

    @Override
    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;
        // append fields into metaclass
        var metaClass = this.getMetaClassDef();
        var enumConstants = enumDeclarationContext.enumConstants().enumConstant();
        int index = 0;
        IntHashSet values = new IntHashSet();
        for (AgoParser.EnumConstantContext enumConstant : enumConstants) {
            var enumName = enumConstant.identifier().getText();
            if(metaFields.containsKey(enumName)){
                throw new DuplicatedError("'%s' duplicated".formatted(enumName), unit.sourceLocation(enumConstant.identifier()));
            }
            var integerLiteral = enumConstant.integerLiteral();
            Literal<?> literalValue;
            if(integerLiteral == null){
                enumValues.put(enumName, literalValue = createLiteral(index, unit.sourceLocation(enumConstant)));
                values.add(index++);
            } else {
                var v = Literal.parseIntegerLiteral(integerLiteral);
                if(values.contains(v.value)){
                    throw new DuplicatedError("value '%d' duplicated".formatted(v.value), unit.sourceLocation(integerLiteral));
                }
                enumValues.put(enumName, literalValue = createLiteral(v.value,v.getSourceLocation()));
                index = v.value + 1;
            }

            // create a new field in the meta class
            var fld = new Field(enumName, null);
            fld.setModifiers(this.modifiers);
            fld.setType(this);
            fld.setSourceLocation(unit.sourceLocation(enumConstant));
            fld.setConstLiteralValue(literalValue);
            this.metaFields.put(enumName, fld);
            metaClass.addField(fld);
        }
        this.setCompilingStage(CompilingStage.InheritsFields);
        metaClass.setCompilingStage(CompilingStage.InheritsFields);
        return true;
    }

    @Override
    public void verifyMembers() throws SyntaxError {
        return;
    }

    @Override
    public void inheritsChildClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.InheritsInnerClasses) return;

        super.inheritsChildClasses();

        // create valueOf and parse, and constructor of metaclass to initial values
        var metaClass = getMetaClassDef();

        // constructor
        var constructorDef = new ConstructorDef(AgoClass.CONSTRUCTOR | AgoClass.PRIVATE, "new#");
        constructorDef.setUnit(this.unit);
        metaClass.addChild(constructorDef);
        constructorDef.setCompilingStage(CompilingStage.AllocateSlots);
        this.metaClassConstructor = constructorDef;


        // valueOf
        var valueOf = new FunctionDef("valueOf",null);
        valueOf.setModifiers(AgoClass.PUBLIC | AgoClass.OVERRIDE);
        valueOf.setUnit(unit);
        metaClass.addChild(valueOf);
        var p = new Parameter("t",null);
        p.setType(enumBasePrimitiveType);
        valueOf.addParameter(p);
        valueOf.setResultType(this);
        valueOf.setCompilingStage(CompilingStage.AllocateSlots);
        this.metaClassValueOf = valueOf;

        // parse
        var parse = new FunctionDef("parse",null);
        parse.setModifiers(AgoClass.PUBLIC | AgoClass.OVERRIDE);
        parse.setUnit(unit);
        metaClass.addChild(parse);
        var ps = new Parameter("s",null);
        ps.setType(PrimitiveClassDef.STRING);
        parse.addParameter(ps);
        parse.setResultType(this);
        parse.setCompilingStage(CompilingStage.AllocateSlots);
        this.metaClassParse = parse;

        this.setCompilingStage(CompilingStage.AllocateSlots);
        metaClass.setCompilingStage(CompilingStage.AllocateSlots);
    }

    @Override
    public void compileBody() throws CompilationError {
        if(this.compilingStage != CompilingStage.CompileMethodBody) return;

        MetaClassDef metaClass = this.getMetaClassDef();
        composeMetaConstructor(metaClass);

        composeValueOf(metaClass);

        composeParse(metaClass);

        //TODO values

        metaClass.setCompilingStage(CompilingStage.Compiled);
        this.setCompilingStage(CompilingStage.Compiled);
    }

    private void composeMetaConstructor(MetaClassDef metaClass) {
        var constructorDef = this.metaClassConstructor;
        List<Expression> initializers = new ArrayList<>();
        Scope metaClassScope = new Scope(1, metaClass);
        for (var field : this.metaFields.values()) {
            try {
                var creator = new Creator(new ConstClass(this), Arrays.asList(enumValues.get(field.name), new StringLiteral(field.name)), unit.sourceLocation(this.enumDeclarationContext));
                var assign = Assign.to(Var.of(metaClassScope, field), creator);
                initializers.add(assign);
            } catch (CompilationError e) {
                throw new RuntimeException(e);
            }
        }
        try {
            initializers.add(new Return());
            new BlockCompiler(unit,constructorDef,null).compileExpressions(initializers);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        assert constructorDef.getBody() != null;
    }

    private void composeValueOf(MetaClassDef metaClass) throws CompilationError {
        var valueOf = this.metaClassValueOf;
        BlockCompiler blockCompiler = new BlockCompiler(unit, valueOf, null);

        Scope metaClassScope = new Scope(1, metaClass);
        var metaScopeVar = blockCompiler.acquireTempVar(metaClassScope);
        var initScopeVar = Assign.to(metaScopeVar,metaClassScope);

        var p0Var = new Var.LocalVar(valueOf.getParameters().get(0), Var.LocalVar.VarMode.Existed);
        try {
            ArrayList<SwitchCaseStmt.SwitchGroup> groups = new ArrayList<>();
            for (Field field : this.metaFields.values()) {
                SwitchCaseStmt.SwitchGroup group = new SwitchCaseStmt.SwitchGroup();
                group.addCase(new SwitchCaseStmt.Case(SwitchCaseStmt.CaseKind.ConstExpression, enumValues.get(field.name)));
                group.addStatement(new Return(Var.of(metaScopeVar,field)));
                groups.add(group);
            }
            var defaultGroup = new SwitchCaseStmt.SwitchGroup();
            defaultGroup.addCase(new SwitchCaseStmt.Case(SwitchCaseStmt.CaseKind.Default, null));
            defaultGroup.addStatement(new Return(getRoot().createNullLiteral()));  //TODO throw error by default
            var stmt = new SwitchCaseStmt(p0Var, groups);
            blockCompiler.compileExpressions(List.of(initScopeVar.transform(), stmt.transform()));
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    private void composeParse(MetaClassDef metaClass) throws CompilationError {
        var parse = this.metaClassParse;
        BlockCompiler blockCompiler = new BlockCompiler(unit, parse, null);

        Scope metaClassScope = new Scope(1, metaClass);
        var metaScopeVar = blockCompiler.acquireTempVar(metaClassScope);
        var initScopeVar = Assign.to(metaScopeVar,metaClassScope);

        var p0Var = new Var.LocalVar(parse.getParameters().get(0), Var.LocalVar.VarMode.Existed);
        try {
            ArrayList<SwitchCaseStmt.SwitchGroup> groups = new ArrayList<>();
            for (Field field : this.metaFields.values()) {
                SwitchCaseStmt.SwitchGroup group = new SwitchCaseStmt.SwitchGroup();
                group.addCase(new SwitchCaseStmt.Case(SwitchCaseStmt.CaseKind.ConstExpression, new StringLiteral(field.name)));
                group.addStatement(new Return(Var.of(metaScopeVar,field)));
                groups.add(group);
            }
            var defaultGroup = new SwitchCaseStmt.SwitchGroup();
            defaultGroup.addCase(new SwitchCaseStmt.Case(SwitchCaseStmt.CaseKind.Default, null));
            defaultGroup.addStatement(new Return(getRoot().createNullLiteral()));  //TODO throw error by default
            var stmt = new SwitchCaseStmt(p0Var, groups);
            blockCompiler.compileExpressions(List.of(initScopeVar.transform(), stmt.transform()));
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }


    private Literal<?> createLiteral(long value, SourceLocation sourceLocation) {
        return (switch (this.enumBasePrimitiveType.getTypeCode().value){
            case TypeCode.INT_VALUE -> new IntLiteral((int) value);
            case TypeCode.BYTE_VALUE -> new ByteLiteral((byte) value);
            case TypeCode.SHORT_VALUE -> new ShortLiteral((short) value);
            case TypeCode.LONG_VALUE -> new LongLiteral(value);
            default -> throw new RuntimeException("impossible");
        }).setSourceLocation(sourceLocation);
    }


    public void resolveHierarchicalClasses() throws CompilationError {
        if(this.compilingStage != CompilingStage.ResolveHierarchicalClasses) return;
        var primitiveTypeContext = this.enumDeclarationContext.primitiveType();
        PrimitiveClassDef primitiveClassDef;
        if(primitiveTypeContext != null){
            primitiveClassDef = PrimitiveClassDef.fromPrimitiveTypeAst(primitiveTypeContext);
        } else {
            primitiveClassDef = PrimitiveClassDef.INT;
        }
        this.enumBasePrimitiveType = primitiveClassDef;
        String baseClass = switch (primitiveClassDef.getTypeCode().value){
            case TypeCode.INT_VALUE -> "lang.IntEnum";
            case TypeCode.BYTE_VALUE -> "lang.ByteEnum";
            case TypeCode.SHORT_VALUE -> "lang.ShortEnum";
            case TypeCode.LONG_VALUE -> "lang.LongEnum";
            default -> throw new TypeMismatchError("the base type of enum must be int famlily primitive type", unit.sourceLocation(primitiveTypeContext));
        };

        ClassDef baseClassDef = getRoot().findByFullname(baseClass);
        this.setSuperClass(baseClassDef);

        this.resolveMetaclass();

        this.setCompilingStage(CompilingStage.ParseFields);
    }

}
