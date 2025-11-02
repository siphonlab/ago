package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

public abstract class ExpressionBase implements Expression{

    protected SourceLocation sourceLocation;
    private Expression parent;

    /**
     * if the expression is LocalVar, return itself, otherwise output to candidateLocalVar
     * @param candidateLocalVar
     * @param expression
     * @param blockCompiler
     * @return
     * @throws CompilationError
     */
    protected static Var.LocalVar visitExpressionToLocalVar(Var.LocalVar candidateLocalVar, Expression expression, BlockCompiler blockCompiler) throws CompilationError {
        Var.LocalVar localVar;
        if(expression instanceof Var.LocalVar l){
            localVar = l;
        } else {
            expression.outputToLocalVar(localVar = candidateLocalVar, blockCompiler);        // use the candidateLocalVar var as temp var
        }
        return localVar;
    }

    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        Var.LocalVar localVar = blockCompiler.acquireTempVar(this);
        this.outputToLocalVar(localVar, blockCompiler);
        localVar.setSourceLocation(this.getSourceLocation());
        return localVar;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        // nothing to do
    }

    private boolean transformed = false;
    public Expression transform() throws CompilationError {
        if(transformed) return this;
        var expr = this.transformInner();
        if(expr != this){
            expr.setSourceLocation(this.sourceLocation);
            expr.setParent(this.parent);
        }
        transformed = true;
        return expr;
    }

    protected Expression transformInner() throws CompilationError {
        return this;
    }

    @Override
    public ExpressionBase setSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
        return this;
    }

    @Override
    public SourceLocation getSourceLocation() {
        if(sourceLocation == null){
            SourceLocation r = null;
            for(var p = this.parent; p != null; p = p.getParent()) {
                if(p instanceof ExpressionBase pb){
                    r = pb.sourceLocation;
                } else {
                    r = p.getSourceLocation();
                }
                if(r != null) return r;
                if(p == p.getParent()) return null;
            }
        }
        return sourceLocation;
    }

    @Override
    public ExpressionBase setParent(Expression expression) {
        this.parent = expression;
        return this;
    }

    @Override
    public Expression getParent() {
        return parent;
    }

}
