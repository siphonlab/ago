package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.statement.Statement;

public class WithExpr extends ExpressionBase{

    private final CurrWithExpression expression;
    private final Statement statement;

    public WithExpr(CurrWithExpression expression, Statement statement) throws CompilationError {
        this.expression = expression.transform();
        this.statement = statement.transform();
        statement.setParent(this);
        expression.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return expression.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            expression.outputToLocalVar(localVar, blockCompiler);
            blockCompiler.enterWith(expression);
            this.statement.termVisit(blockCompiler);
            blockCompiler.leaveWith(expression);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public ExpressionBase setSourceLocation(SourceLocation sourceLocation) {
        return super.setSourceLocation(sourceLocation);
    }

    @Override
    public String toString() {
        return "(WITH %s %s)".formatted(expression, statement);
    }
}
