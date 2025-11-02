package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Literal;
import org.siphonlab.ago.compiler.expression.TermExpression;
import org.siphonlab.ago.compiler.expression.Var;
import org.siphonlab.ago.compiler.expression.literal.VoidLiteral;

public class Return extends Statement {

    protected Expression value;

    public Return(Expression value) throws CompilationError {
        this.value = value.transform().setParent(this);
    }

    public Return() throws CompilationError {
        this.value = new VoidLiteral();
    }


    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();

            // each outer finally should execute before `return`
            for(var tryCatchFinallyStmt = findClosestTryCatchFinal(this); tryCatchFinallyStmt != null; tryCatchFinallyStmt = findClosestTryCatchFinal(tryCatchFinallyStmt)) {
                Label finalExitLabel = blockCompiler.createLabel();
                code.setFinalExit(tryCatchFinallyStmt.getFinalExit().getVariableSlot(), finalExitLabel);
                code.jump(tryCatchFinallyStmt.getFinalEntrance());
                finalExitLabel.here();      // after final executed, jump to here(return)
            }

            var term = value.visit(blockCompiler);
            if (term instanceof Literal<?> literal) {
                code.return_c(literal);
            } else if (term instanceof Var.LocalVar localVar) {
                code.return_v(localVar.getVariableSlot());
            } else {
                throw new UnsupportedOperationException();
            }
            return term;
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    private static TryCatchFinallyStmt findClosestTryCatchFinal(Expression from) {
        for (Expression p = from.getParent(); p != null; p = p.getParent()) {
            if (p instanceof TryCatchFinallyStmt tryCatchFinallyStmt && tryCatchFinallyStmt.getFinalExit() != null) {
                return tryCatchFinallyStmt;
            }
        }
        return null;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);
    }

    @Override
    public String toString() {
        return "(Return %s)".formatted(value);
    }

    @Override
    public Return setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
