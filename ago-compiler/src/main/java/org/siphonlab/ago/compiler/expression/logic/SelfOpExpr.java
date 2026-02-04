package org.siphonlab.ago.compiler.expression.logic;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;

import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.array.ArrayElement;
import org.siphonlab.ago.compiler.expression.array.ArrayPut;


public abstract class SelfOpExpr extends ExpressionBase {

    protected final Expression site;
    protected final Expression value;

    public SelfOpExpr(Expression site, Expression value){
        this.site = site;
        this.value = value;
    }

    abstract Expression expr(Expression left, Expression right) throws CompilationError;


    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            if (site instanceof Var.Field field) {
                field.simplify(blockCompiler);
                blockCompiler.lockRegister(field.getBaseVar());
                var temp = localVar != null ? localVar : blockCompiler.acquireTempVar(this);
                expr(field, value).outputToLocalVar(temp, blockCompiler);
                Assign.to(field, temp).termVisit(blockCompiler);
                blockCompiler.releaseRegister(field.getBaseVar());
            } else if (site instanceof Var.LocalVar var) {
                expr(var, value).outputToLocalVar(var, blockCompiler);
                if (localVar != null && localVar != var) {
                    Assign.to(localVar, var).termVisit(blockCompiler);
                }
            } else if (site instanceof ArrayElement arrayElement) {
                var old = arrayElement.visit(blockCompiler);
                var arr = arrayElement.getProcessedArray();
                blockCompiler.lockRegister(arr);
                var index = arrayElement.getProcessedIndex();
                blockCompiler.lockRegister(index);
                var expr = expr(old, value);
                var r = expr.setSourceLocation(this.sourceLocation).visit(blockCompiler);
                new ArrayPut(arr, index, r).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
                blockCompiler.releaseRegister(arr);
                blockCompiler.releaseRegister(index);
                if (localVar != null) {
                    Assign.to(localVar, r).termVisit(blockCompiler);
                }
            } else if (site instanceof Attribute attribute) {
                var got = attribute.visit(blockCompiler);
                blockCompiler.lockRegister(attribute.getProcessedScope());
                var expr = expr(got, value).visit(blockCompiler);
                var r = expr.setSourceLocation(this.sourceLocation).visit(blockCompiler);
                attribute.setValue(r).setSourceLocation(this.getSourceLocation()).termVisit(blockCompiler);
                blockCompiler.releaseRegister(attribute.getProcessedScope());
                if (localVar != null) {
                    Assign.to(localVar, r).termVisit(blockCompiler);
                }
            } else {
                throw new UnsupportedOperationException("not supported yet");
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        this.outputToLocalVar(null, blockCompiler);
    }

}
