package org.siphonlab.ago.compiler.expression.array;


import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.SyntaxError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;

import java.util.List;
import java.util.Objects;

public class ListPut extends ExpressionBase {

    private final Expression list;
    private final Expression indexExpr;
    private final Expression value;
    private final FunctionDef accessor;

    public ListPut(Expression list, Expression indexExpr, Expression value) throws CompilationError {
        list = list.transform().setParent(this);
        ClassDef listType = list.inferType();
        Root root = listType.getRoot();
        if(!(root.getAnyReadwriteList().isThatOrSuperOfThat(listType))){
            throw new SyntaxError("writable list expected", list.getSourceLocation());
        }
        this.list = list;
        this.indexExpr = new Cast(indexExpr, PrimitiveClassDef.INT).transform().setParent(this);
        this.value = value.transform().setParent(this);
        this.accessor = listType.findMethod("set#index");
    }

    public ListPut(ListElement listElement, Expression value) throws CompilationError {
        this(listElement.getList(), listElement.getIndexExpr(), value);
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return value.inferType();
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            var r = this.visit(blockCompiler);
            r.outputToLocalVar(localVar, blockCompiler);
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

            Var.LocalVar array = (Var.LocalVar) this.list.visit(blockCompiler);
            blockCompiler.lockRegister(array);

            var indexExpr = this.indexExpr.visit(blockCompiler);
            blockCompiler.lockRegister(indexExpr);

            var value = this.value.visit(blockCompiler);
            blockCompiler.lockRegister(value);

            var invoke = new Invoke(Invoke.InvokeMode.Invoke, ClassUnder.create(list, accessor), List.of(indexExpr, value), this.getSourceLocation());
            invoke.termVisit(blockCompiler);

            blockCompiler.releaseRegister(array);
            blockCompiler.releaseRegister(indexExpr);
            blockCompiler.releaseRegister(value);
            return value;
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
    public ListPut setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(ListPut %s[%s] = %s)".formatted(this.list, this.indexExpr, this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ListPut that)) return false;
        return Objects.equals(list, that.list) && Objects.equals(indexExpr, that.indexExpr) &&  Objects.equals(value, that.value) ;
    }
}
