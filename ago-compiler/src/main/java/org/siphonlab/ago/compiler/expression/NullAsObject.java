package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;

public class NullAsObject extends ExpressionBase implements LiteralResultExpression{

    private final NullLiteral nullLiteral;
    private final ClassDef type;

    public NullAsObject(NullLiteral nullLiteral, ClassDef type){
        this.nullLiteral = nullLiteral;
        this.type = type;
        this.sourceLocation = nullLiteral.getSourceLocation();
        this.setParent(nullLiteral.getParent());
        nullLiteral.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return type;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.getCode().assignLiteral(localVar.getVariableSlot(), this.nullLiteral);
    }

    @Override
    public Literal<?> visit(BlockCompiler blockCompiler) throws CompilationError {
        return this.nullLiteral;
    }

    public NullLiteral getNullLiteral() {
        return nullLiteral;
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public NullAsObject setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
