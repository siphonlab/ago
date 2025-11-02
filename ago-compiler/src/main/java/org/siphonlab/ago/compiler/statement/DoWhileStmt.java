package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.exception.CompilationError;

import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.BooleanLiteral;

public class DoWhileStmt extends LoopStmt {

    private Expression condition;
    private final Statement body;

    public DoWhileStmt(String label, Expression condition, Statement body) throws CompilationError {
        super(label);
        this.condition = condition.setParent(this).transform();
        this.body = body.setParent(this).transform();
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(this.condition instanceof Literal<?> literal){
            if(BooleanLiteral.isFalse(literal)){    // always false
                return this.body;
            }
        }
        return this;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();
            exitLabel = blockCompiler.createLabel();
            if (this.condition instanceof Literal<?> literal && BooleanLiteral.isTrue(literal)) {
                continueLabel = blockCompiler.createLabel().here();
                body.termVisit(blockCompiler);
                code.jump(continueLabel);
                exitLabel.here();
                return;
            }

            var bodyBegin = continueLabel = blockCompiler.createLabel().here();
            this.body.termVisit(blockCompiler);
            if (condition instanceof LiteralResultExpression literalResultExpression) {
                var tempVar = blockCompiler.acquireTempVar(literalResultExpression);
                Assign.to(tempVar, condition).setSourceLocation(condition.getSourceLocation()).visit(blockCompiler);
                code.jumpIf(tempVar.getVariableSlot(), bodyBegin);
            } else {
                Var.LocalVar r = (Var.LocalVar) condition.visit(blockCompiler);
                code.jumpIf(r.getVariableSlot(), bodyBegin);
            }
            exitLabel.here();
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public DoWhileStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "do\n" + body + "\nwhile(%s)".formatted(condition);
    }
}
