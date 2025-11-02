package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class CopyAssign extends ExpressionBase{

    private final Expression assignee;
    private final Expression value;
    private final ClassDef commonType;

    public CopyAssign(Expression assignee, Expression value, ClassDef commonType) throws CompilationError {
        this.assignee = assignee.transform();
        this.value = value.transform();
        this.commonType = commonType;
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        return super.transformInner();
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return assignee.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Var.LocalVar a = (Var.LocalVar) assignee.visit(blockCompiler);
            Var.LocalVar v = (Var.LocalVar) value.visit(blockCompiler);
            blockCompiler.getCode().copyAssign(a.getVariableSlot(), v.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(commonType));

            Assign.to(localVar,a).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            Var.LocalVar a = (Var.LocalVar) assignee.visit(blockCompiler);
            Var.LocalVar v = (Var.LocalVar) value.visit(blockCompiler);
            blockCompiler.getCode().copyAssign(a.getVariableSlot(), v.getVariableSlot(), blockCompiler.getFunctionDef().idOfClass(commonType));

        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public CopyAssign setParent(Expression expression) {
        super.setParent(expression);
        return this;
    }

    @Override
    public CopyAssign setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(CopyAssign %s %s)".formatted(assignee, value);
    }
}
