package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

public interface Expression {

    /**
     * for LiteralExpression, return itself
     * for LocalVarResultExpression, return a LocalVar
     * for other expression, its `visit` invoke `outputToLocalVar(tempVar)`
     * @param blockCompiler
     * @return
     */
    TermExpression visit(BlockCompiler blockCompiler) throws CompilationError;

    Expression transform() throws CompilationError;

    ClassDef inferType() throws CompilationError;

    /**
     * every expression must support output to localVar
     * @param localVar
     * @param blockCompiler
     */
    void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError;

    void termVisit(BlockCompiler blockCompiler) throws CompilationError;

    Expression setSourceLocation(SourceLocation sourceLocation);

    SourceLocation getSourceLocation();

    Expression setParent(Expression expression);

    Expression getParent();
}

