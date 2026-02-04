package org.siphonlab.ago.compiler.expression.array;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.ExpressionBase;
import org.siphonlab.ago.compiler.expression.Var;

public class ArrayLength extends ExpressionBase {

    private final Expression array;

    public ArrayLength(Expression array){
        this.array = array;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return PrimitiveClassDef.INT;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        ClassDef classDef = array.inferType();
        var lengthField = classDef.getFields().get("length");
        Var.of(array,lengthField).setSourceLocation(this.getSourceLocation()).outputToLocalVar(localVar,blockCompiler);
    }

    @Override
    public String toString() {
        return "(Length %s)".formatted(array);
    }
}
