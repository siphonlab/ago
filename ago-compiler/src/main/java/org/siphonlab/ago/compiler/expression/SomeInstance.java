package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.Objects;

public class SomeInstance extends ExpressionBase{

    private final ClassDef classDef;

    public SomeInstance(ClassDef classDef){
        this.classDef = classDef;
    }
    @Override
    public ClassDef inferType() throws CompilationError {
        return classDef;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        throw new UnsupportedOperationException("AnInstance is a placeholder expression");
    }

    @Override
    public String toString() {
        return "(SomeInstance %s)".formatted(classDef);
    }

    @Override
    public ExpressionBase setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SomeInstance that)) return false;
        return Objects.equals(classDef, that.classDef);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(classDef);
    }
}
