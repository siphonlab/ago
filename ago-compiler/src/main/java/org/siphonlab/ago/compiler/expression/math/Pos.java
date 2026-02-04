package org.siphonlab.ago.compiler.expression.math;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;

import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;


import java.util.Objects;

import static org.siphonlab.ago.TypeCode.*;

public class Pos extends UnaryArithmetic {

    public Pos(Expression value) throws CompilationError {
        super(value);
    }

    @Override
    protected Expression transformInner() throws CompilationError {
        ClassDef type = this.value.inferType();

        if(type.getTypeCode() == STRING){   // auto cast to double like js does
            return new Cast(this.value, PrimitiveClassDef.DOUBLE).setSourceLocation(this.getSourceLocation()).setParent(this.getParent());
        }
        if(type.getTypeCode() == CHAR){     // char cast to int
            return new Cast(this.value, PrimitiveClassDef.INT).setSourceLocation(this.getSourceLocation()).setParent(this.getParent());
        }
        if(type.getTypeCode() == OBJECT && type.isPrimitiveOrBoxed()){
            return new Unbox(this.value).setParent(this.getParent()).transform();
        }
        if(!type.isPrimitiveNumberFamily()){
            throw new TypeMismatchError("illegal type '%' for positive, number or number string required", this.getSourceLocation());
        }
        return this.value;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            this.value.outputToLocalVar(localVar, blockCompiler);
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

            return this.value.visit(blockCompiler);
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

            this.value.termVisit(blockCompiler);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public Pos setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(Pos %s)".formatted(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pos n && Objects.equals(n.value, this.value);
    }
}
