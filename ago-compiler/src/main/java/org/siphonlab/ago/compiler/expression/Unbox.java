package org.siphonlab.ago.compiler.expression;


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.Objects;

public class Unbox extends ExpressionBase{
    private final Expression expression;

    public Unbox(Expression expression){
        this.expression = expression;

        this.setParent(expression.getParent());
        expression.setParent(this);
        this.setSourceLocation(expression.getSourceLocation());
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        if(this.expression.inferType().isPrimitiveFamily()) {
            return this.expression;
        }
        if(this.expression instanceof EnumValue enumValue){
            return enumValue.toLiteral();
        }
        if(this.expression instanceof ConstValue constValue){
            return constValue.toLiteral();
        }
        return this;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        var t = expression.inferType();
        return PrimitiveClassDef.fromBoxedType(t);
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var r = expression.visit(blockCompiler);
            var c = this.expression.visit(blockCompiler);
            CodeBuffer code = blockCompiler.getCode();
            if (c instanceof Literal<?> literal) {
                code.assignLiteral(localVar.getVariableSlot(), literal);
            } else {
                var v = (Var.LocalVar) c;
                code.unbox(localVar.getVariableSlot(), v.getVariableSlot());
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public String toString() {
        return "(Unbox %s)".formatted(expression);
    }

    @Override
    public Unbox setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Unbox unbox)) return false;
        return Objects.equals(expression, unbox.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expression);
    }
}
