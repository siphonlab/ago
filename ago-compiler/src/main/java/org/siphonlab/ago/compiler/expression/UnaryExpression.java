package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

public abstract class UnaryExpression extends ExpressionBase{
    protected final Expression value;

    public UnaryExpression(Expression value) throws CompilationError {
        this.value = value.transform();
        this.value.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return value.inferType();
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.value.termVisit(blockCompiler);
    }
}
