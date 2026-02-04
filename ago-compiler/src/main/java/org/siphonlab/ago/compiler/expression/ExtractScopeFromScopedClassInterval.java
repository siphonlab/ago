package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

@Deprecated
public class ExtractScopeFromScopedClassInterval extends ExpressionBase{

    private final Expression scopedClassIntervalInstance;
    private final ClassDef ObjectType;

    public ExtractScopeFromScopedClassInterval(Expression scopedClassIntervalInstance, ClassDef ObjectType){
        this.scopedClassIntervalInstance = scopedClassIntervalInstance;
        this.ObjectType = ObjectType;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return ObjectType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        var classDef = blockCompiler.getFunctionDef();
        ClassDef classInterval = classDef.getRoot().getScopedClassInterval();
        if(!this.scopedClassIntervalInstance.inferType().isDeriveFrom(classInterval)){
            throw new TypeMismatchError("a ClassInterval expression expected", this.getSourceLocation());
        }

        var fld = new Var.Field(scopedClassIntervalInstance, classInterval.getVariable("scope"));

        try {
            blockCompiler.enter(this);

            fld.setSourceLocation(this.getSourceLocation()).outputToLocalVar(localVar, blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }
}
