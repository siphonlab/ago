package org.siphonlab.ago.compiler.expression.array;



import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;

import org.siphonlab.ago.compiler.expression.literal.IntLiteral;

public class ArrayPut extends ExpressionBase {

    private final Expression array;
    private final Expression indexExpr;
    private final Expression value;

    public ArrayPut(Expression array, Expression indexExpr, Expression value) throws CompilationError {
        array = array.transform().setParent(this);
        //if(!(array.inferType() instanceof ArrayClassDef)){
        ClassDef arrayType = array.inferType();
        if(!arrayType.getRoot().getAnyArrayClass().isThatOrSuperOfThat(arrayType)){
            throw new TypeMismatchError("'%s' is not an array".formatted(array), array.getSourceLocation());
        }
        this.array = array;
        this.indexExpr = new Cast(indexExpr, PrimitiveClassDef.INT).transform().setParent(this);
        this.value = value.transform().setParent(this);
    }

    public ArrayPut(ArrayElement arrayElement, Expression value) throws CompilationError {
        this(arrayElement.getArray(), arrayElement.getIndexExpr(), value);
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

            Var.LocalVar array = (Var.LocalVar) this.array.visit(blockCompiler);
            blockCompiler.lockRegister(array);

            var indexExpr = this.indexExpr.visit(blockCompiler);
            blockCompiler.lockRegister(indexExpr);

            var value = this.value.visit(blockCompiler);
            blockCompiler.lockRegister(value);

            CodeBuffer code = blockCompiler.getCode();
            if (indexExpr instanceof Literal<?> literal) {
                assert indexExpr instanceof IntLiteral;
                int index = ((IntLiteral) literal).value;
                if (value instanceof Literal<?> literalValue) {
                    code.array_put(array.getVariableSlot(), index, literalValue);
                } else {
                    code.array_put(array.getVariableSlot(), index, ((Var.LocalVar) value).getVariableSlot());
                }
            } else {
                Var.LocalVar index = (Var.LocalVar) indexExpr;
                if (value instanceof Literal<?> literalValue) {
                    code.array_put(array.getVariableSlot(), index.getVariableSlot(), literalValue);
                } else {
                    code.array_put(array.getVariableSlot(), index.getVariableSlot(), ((Var.LocalVar) value).getVariableSlot());
                }
            }

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
    public ArrayPut setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(ArrayPut %s[%s] = %s)".formatted(this.array, this.indexExpr, this.value);
    }
}
