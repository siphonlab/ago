package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

@Deprecated
public class BindScopeToScopedClassInterval extends ExpressionBase{

    private final Expression scopedClassIntervalInstance;
    private final Expression scope;

    public BindScopeToScopedClassInterval(Expression scopedClassIntervalInstance, Expression scope) throws CompilationError {
        this.scopedClassIntervalInstance = scopedClassIntervalInstance.transform();
        this.scope = scope.transform();
    }


    @Override
    public ClassDef inferType() throws CompilationError {
        return scopedClassIntervalInstance.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            this.visit(blockCompiler).outputToLocalVar(localVar, blockCompiler);

        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var classDef = blockCompiler.getFunctionDef();
            ClassDef classInterval = classDef.getRoot().getScopedClassInterval();
            if (!this.scopedClassIntervalInstance.inferType().isDeriveFrom(classInterval)) {
                throw new TypeMismatchError("a ClassInterval expression expected", this.getSourceLocation());
            }

            var fld = new Var.Field(scopedClassIntervalInstance, classInterval.getVariable("scope"));

            Assign.to(fld, scope).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
            return fld.getBaseVar();
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.visit(blockCompiler);
    }

    @Override
    public BindScopeToScopedClassInterval setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public BindScopeToScopedClassInterval setParent(Expression expression) {
        super.setParent(expression);
        return this;
    }

    @Override
    public String toString() {
        return "(BindScopeClassInterval %s with %s)".formatted(scopedClassIntervalInstance, scope);
    }
}
