package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.TermExpression;

public class ExpressionStmt extends Statement {

    protected Expression expression;

    public ExpressionStmt(Expression expression) throws CompilationError {
        this.expression = expression.transform().setParent(this);
        this.setSourceLocation(expression.getSourceLocation());
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        return expression.visit(blockCompiler);
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.expression.termVisit(blockCompiler);
    }

    @Override
    public String toString() {
        return expression.toString();
    }

    @Override
    public ExpressionStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
