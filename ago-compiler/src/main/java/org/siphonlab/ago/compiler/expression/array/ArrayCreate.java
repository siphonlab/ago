package org.siphonlab.ago.compiler.expression.array;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.*;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.*;
import org.siphonlab.ago.compiler.expression.literal.IntLiteral;

public class ArrayCreate extends ExpressionBase {
    private final ArrayClassDef arrayType;
    private final Expression lengthExpr;

    public ArrayCreate(ArrayClassDef arrayType, Expression lengthExpr) throws CompilationError {
        this.arrayType = arrayType;
        lengthExpr.setParent(this);
        this.lengthExpr = lengthExpr == null ? null : new Cast(lengthExpr, PrimitiveClassDef.INT).setSourceLocation(lengthExpr.getSourceLocation()).transform();
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return arrayType;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        if(lengthExpr == null){     // only created an array type
            return;
        }
        var length = lengthExpr.visit(blockCompiler);
        var fun = blockCompiler.getFunctionDef();
        CodeBuffer code = blockCompiler.getCode();
        var elementType = this.arrayType.getElementType();
        try {
            blockCompiler.enter(this);

            if (length instanceof Literal<?> literal) {
                if (length instanceof IntLiteral intLiteral) {
                    code.new_array(localVar.getVariableSlot(), fun.idOfClass(arrayType), elementType.getTypeCode(), intLiteral.value, false /*arrayType.isGenericInstantiateRequired()*/);
                } else {
                    throw new TypeMismatchError("array length must be int", literal.getSourceLocation());
                }
            } else {
                if (length.inferType().getTypeCode() != TypeCode.INT) {
                    throw new TypeMismatchError("array length must be int", length.getSourceLocation());
                }
                SlotDef lengthSlot = ((Var.LocalVar) length).getVariableSlot();
                code.new_array(localVar.getVariableSlot(), fun.idOfClass(arrayType), elementType.getTypeCode(), lengthSlot, false /*arrayType.isGenericInstantiateRequired()*/);
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public ArrayCreate setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        if(lengthExpr != null){
            return "(Array %s[%s])".formatted(this.arrayType, lengthExpr);
        } else {
            return "(Array %s)".formatted(this.arrayType);
        }
    }
}
