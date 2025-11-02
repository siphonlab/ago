package org.siphonlab.ago.compiler.expression.math;

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.UnaryExpression;

public abstract class UnaryArithmetic extends UnaryExpression {
    public UnaryArithmetic(Expression value) throws CompilationError {
        super(value);
    }
}
