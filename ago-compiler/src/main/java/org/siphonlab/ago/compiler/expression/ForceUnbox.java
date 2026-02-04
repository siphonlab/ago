package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;

class ForceUnbox extends ExpressionBase{

    private final Expression expression;
    private final ClassDef toType;

    public ForceUnbox(Expression expression, ClassDef implicitOrExplicitPrimaryClass){
        this.expression = expression;
        this.toType = implicitOrExplicitPrimaryClass;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return toType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);
            Var.LocalVar result = (Var.LocalVar) this.expression.visit(blockCompiler);
            blockCompiler.getCode().force_unbox(localVar.getVariableSlot(), result.getVariableSlot(), toType.getTypeCode());
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public String toString() {
        return "(ForceUnbox %s to %s)".formatted(expression, toType);
    }
}
