package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;

public interface LiteralResultExpression extends Expression{

    @Override
    Literal<?> visit(BlockCompiler blockCompiler) throws CompilationError;

}
