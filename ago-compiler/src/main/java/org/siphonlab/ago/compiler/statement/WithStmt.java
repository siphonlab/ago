package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.CurrWithExpression;

public class WithStmt extends Statement{


    private final CurrWithExpression expression;
    private final Statement statement;

    public WithStmt(CurrWithExpression expression, Statement statement) throws CompilationError {
        this.expression = expression.transform();
        this.statement = statement.transform();
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            expression.visit(blockCompiler);

            blockCompiler.enterWith(expression);
            statement.termVisit(blockCompiler);
            blockCompiler.leaveWith(expression);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public WithStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "with(%s) %s".formatted(expression, statement);
    }
}
