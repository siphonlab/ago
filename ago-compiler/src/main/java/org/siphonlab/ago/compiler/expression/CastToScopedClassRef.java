package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.CodeBuffer;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class CastToScopedClassRef extends ExpressionBase{

    private final Expression expression;
    private final ClassDef scopedClassIntervalClassDef;

    public CastToScopedClassRef(Expression expression, ClassDef scopedClassIntervalClassDef) {
        this.expression = expression;
        this.scopedClassIntervalClassDef = scopedClassIntervalClassDef;
        this.setSourceLocation(expression.getSourceLocation());
        this.setParent(expression.getParent());
        expression.setParent(this);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return scopedClassIntervalClassDef;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            CodeBuffer code = blockCompiler.getCode();
            var fun = blockCompiler.getFunctionDef();
            Var.LocalVar scopeBoundInstance = (Var.LocalVar) this.expression.visit(blockCompiler);
            blockCompiler.lockRegister(scopeBoundInstance);
            code.castScopeBoundClassToClassInterval(localVar.getVariableSlot(),
                    fun.idOfClass(this.scopedClassIntervalClassDef),
                    scopeBoundInstance.getVariableSlot());
            blockCompiler.releaseRegister(scopeBoundInstance);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public String toString() {
        return "(C2sbr %s)".formatted(expression);
    }

}
