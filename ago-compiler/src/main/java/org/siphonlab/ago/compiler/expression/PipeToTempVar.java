package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.Objects;

/**
 * Wrap OutputLocalVarExpression to a temp var
 */
public class PipeToTempVar extends ExpressionBase implements LocalVarResultExpression{

    private final Expression baseExpression;

    public PipeToTempVar(Expression baseExpression){
        this.baseExpression = baseExpression;
    }

    @Override
    public Var.LocalVar visit(BlockCompiler blockCompiler) throws CompilationError {
        var tempVar = blockCompiler.acquireTempVar(this.baseExpression);
        baseExpression.outputToLocalVar(tempVar, blockCompiler);
        return tempVar;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        System.out.println("term visit " + this.getClass().getName());
        baseExpression.visit(blockCompiler);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return baseExpression.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        var t = this.visit(blockCompiler);
        t.outputToLocalVar(localVar, blockCompiler);
    }

    @Override
    public String toString() {
        return "(PipeToTemp %s)".formatted(baseExpression);
    }

    @Override
    public PipeToTempVar setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PipeToTempVar that)) return false;
        return Objects.equals(baseExpression, that.baseExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(baseExpression);
    }
}
