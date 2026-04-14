package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.statement.Label;

public abstract class ExpressionInFunctionBody extends ExpressionBase{

    protected final FunctionDef ownerFunction;

    public ExpressionInFunctionBody(FunctionDef ownerFunction) {
        this.ownerFunction = ownerFunction;
    }

    protected Root getRoot(){
        return ownerFunction.getRoot();
    }

    public interface GenerateCodeForNullable{
        void generate(TermExpression nonNullValue) throws CompilationError;
    }

    public void generateCodeForMaybeNull(BlockCompiler blockCompiler, Expression maybeNullableExpression, GenerateCodeForNullable generateCodeForNullable) throws CompilationError {
        ClassDef classDef = maybeNullableExpression.inferType();
        if(classDef instanceof NullableClassDef n){
            var nullableExit = blockCompiler.createLabel();
            var nullableValue = maybeNullableExpression.visit(blockCompiler);
            nullableValue.setSourceLocation(maybeNullableExpression.getSourceLocation());
            blockCompiler.lockRegister(nullableValue);
            var equalsNull = new Equals(ownerFunction, nullableValue, getRoot().createNullLiteral(), Equals.Type.Equals).visit(blockCompiler);
            blockCompiler.getCode().jumpIf(((Var.LocalVar)equalsNull).getVariableSlot(), nullableExit);
            var nonNullValue = (Var.LocalVar) ownerFunction.cast(nullableValue, n.getBaseClass()).transform().visit(blockCompiler);
            nonNullValue.setSourceLocation(maybeNullableExpression.getSourceLocation());
            generateCodeForNullable.generate(nonNullValue);
            blockCompiler.releaseRegister(nullableValue);
            nullableExit.here();
        } else{
            generateCodeForNullable.generate(maybeNullableExpression.visit(blockCompiler));
        }
    }
}
