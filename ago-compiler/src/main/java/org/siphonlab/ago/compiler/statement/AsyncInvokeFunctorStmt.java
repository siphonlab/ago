package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Expression;
import org.siphonlab.ago.compiler.expression.Invoke;
import org.siphonlab.ago.compiler.expression.Var;

public class AsyncInvokeFunctorStmt extends Statement{

    private final Expression functor;
    private final Invoke.InvokeMode invokeMode;

    public AsyncInvokeFunctorStmt(Invoke.InvokeMode invokeMode, Expression functor) {
        this.invokeMode = invokeMode;
        this.functor = functor;
        functor.setParent(this);
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);
            Var.LocalVar instance = (Var.LocalVar) functor.visit(blockCompiler);

            blockCompiler.getCode().invoke(invokeMode, instance.getVariableSlot());

        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public String toString() {
        return "(%s %s)".formatted(invokeMode, functor);
    }
}
