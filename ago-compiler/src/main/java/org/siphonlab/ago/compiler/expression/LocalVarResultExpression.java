package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;

/**
 * some expression may return LocalVar
 */
public interface LocalVarResultExpression extends Expression{

    @Override
    Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError;
}
