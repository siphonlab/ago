package org.siphonlab.ago.compiler;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.expression.Assign;
import org.siphonlab.ago.compiler.expression.Scope;
import org.siphonlab.ago.compiler.expression.Var;
import org.siphonlab.ago.compiler.statement.ExpressionStmt;
import org.siphonlab.ago.compiler.statement.Return;
import org.siphonlab.ago.compiler.parser.AgoParser;

import java.util.List;

public class SetterFunction extends FunctionDef{

    private Field field;
    private final AgoParser.SetterContext setterContext;

    public SetterFunction(Field field, AgoParser.SetterContext setterContext) throws SyntaxError {
        super(field.name + "#set", null);
        this.field = field;
        this.setterContext = setterContext;
        this.setUnit(field.getOwnerClass().unit);
        //this.setGenericSource(getter.getGenericSource());
        int visibility = Compiler.commonVisibility(unit, setterContext.commonVisiblility(), Compiler.ModifierTarget.Method);
        this.setModifiers(visibility | AgoClass.SETTER);
        this.setResultType(PrimitiveClassDef.VOID);
        this.setSourceLocation(unit.sourceLocation(setterContext));
        this.setCompilingStage(CompilingStage.ParseFields);
    }

    @Override
    public List<AgoParser.InterfaceItemContext> getInterfaceDecls() {
        return null;
    }

    @Override
    public boolean parseFields() throws CompilationError {
        if(this.compilingStage.gt(CompilingStage.ParseFields)) return true;
        if(this.compilingStage.lt(CompilingStage.ParseFields)) return false;

        if(this.getGenericSource() != null){
            this.setCompilingStage(CompilingStage.InheritsFields);
            return true;
        }

        var parameter = new Parameter( "value", null);
        parameter.setType(field.getType());
        parameter.setSourceLocation(field.getSourceLocation());
        this.addParameter(parameter);

        this.createFunctionInterface();
        this.createFieldsOfTrait();

        this.setCompilingStage(CompilingStage.ValidateNewFunctions);
        return true;
    }

    @Override
    public void compileBody() throws CompilationError {
        if(this.getCompilingStage() != CompilingStage.CompileMethodBody) return;

        if(this.getGenericSource() != null){
            this.nextCompilingStage(CompilingStage.Compiled);
            return;
        }

        var blockCompiler = new BlockCompiler(this.unit, this, null);
        Var.Field fld = new Var.Field(new Scope(1, this.getParentClass()), field)
                    .setSourceLocation(this.getParentClass().unit.sourceLocation(field.getDeclaration()));
        Var.LocalVar value = new Var.LocalVar(this.getParameters().getFirst(), Var.LocalVar.VarMode.Existed);

        blockCompiler.compileExpressions(List.of(
            new ExpressionStmt(Assign.to(fld, value)),
            new Return()
        ));

        this.nextCompilingStage(CompilingStage.Compiled);
    }
}
