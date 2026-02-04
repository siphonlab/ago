package org.siphonlab.ago.compiler.expression;


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class Attribute extends ExpressionBase implements Assign.Assignee{

    private final Expression scope;
    private final FunctionDef getter;
    private final FunctionDef setter;

    private Var.LocalVar processedScope;

    public Attribute(Expression scope, FunctionDef getter, FunctionDef setter) throws CompilationError {
        this.scope = scope.setParent(this).transform();
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public String toString() {
        return "(Attribute %s %s)".formatted(scope, getter.getCommonName());
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return getter.getResultType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        if(this.processedScope == null) this.processedScope = (Var.LocalVar) this.scope.visit(blockCompiler);
        blockCompiler.lockRegister(this.processedScope);
        new Invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(this.processedScope, getter), new ArrayList<>(), this.sourceLocation).outputToLocalVar(localVar, blockCompiler);
        blockCompiler.releaseRegister(this.processedScope);
    }

    @Override
    public Attribute setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    public Var.LocalVar getProcessedScope() {
        return processedScope;
    }

    public static class Setter extends ExpressionBase{
        private final Attribute attribute;
        private final Expression value;

        public Setter(Attribute attribute, Expression value) {
            this.attribute = attribute;
            this.value = value;
            this.attribute.setParent(this);
            this.value.setParent(this);
        }

        @Override
        public ClassDef inferType() throws CompilationError {
            return PrimitiveClassDef.VOID;
        }

        @Override
        public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
            throw new SyntaxError("setter is void", this.sourceLocation);
        }

        @Override
        public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
            try {
                blockCompiler.enter(this);

                if (attribute.processedScope == null)
                    attribute.processedScope = (Var.LocalVar) attribute.scope.visit(blockCompiler);
                blockCompiler.lockRegister(attribute.processedScope);
                var r = new Invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(attribute.scope, attribute.setter), Collections.singletonList(new Cast(value, attribute.inferType()).setSourceLocation(value.getSourceLocation()).transform()), this.sourceLocation).visit(blockCompiler);
                blockCompiler.releaseRegister(attribute.processedScope);
                return r;
            } catch (CompilationError e) {
                throw e;
            } finally {
                blockCompiler.leave(this);
            }

        }

        @Override
        public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
            visit(blockCompiler);
        }

        @Override
        public Setter setSourceLocation(SourceLocation sourceLocation) {
            super.setSourceLocation(sourceLocation);
            return this;
        }

        @Override
        public String toString() {
            return "(SetAttribute %s.%s = %s)".formatted(attribute.scope, attribute.setter.getCommonName(), value);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Setter setter)) return false;
            return Objects.equals(attribute, setter.attribute) && Objects.equals(value, setter.value);
        }

    }

    public Setter setValue(Expression value) {
        return new Setter(this, value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Attribute attribute)) return false;
        return Objects.equals(scope, attribute.scope) && Objects.equals(getter, attribute.getter) && Objects.equals(setter, attribute.setter);
    }

    public int hashCode() {
        return Objects.hash(scope, getter, setter);
    }
}
