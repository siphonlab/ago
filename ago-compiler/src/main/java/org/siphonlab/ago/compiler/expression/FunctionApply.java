package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;

public class FunctionApply extends ExpressionBase{

    private final Invoke.InvokeMode invokeMode;
    private final Expression functionInstance;
    private final FunctionDef functionDef;

    public FunctionApply(Invoke.InvokeMode invokeMode, Expression functionInstance) throws CompilationError {
        this.invokeMode = invokeMode;
        this.functionInstance = functionInstance.transform();
        this.sourceLocation = functionInstance.getSourceLocation();
        if(!(functionInstance.inferType() instanceof FunctionDef functionDef)){
            throw new TypeMismatchError("a function instance expected",sourceLocation);
        } else {
            this.functionDef = functionDef;
        }
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        if(invokeMode.isAsync()){
            return functionDef;
        } else {
            return functionDef.getResultType();
        }
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.validateThrowException(functionDef.getThrowsExceptions(),this);

        try {
            blockCompiler.enter(this);
            Var.LocalVar instance = (Var.LocalVar) functionInstance.visit(blockCompiler);
            if(invokeMode.isAsync()) {
                blockCompiler.getCode().invokeAsync(invokeMode, instance.getVariableSlot(), localVar.getVariableSlot());
            } else {
                blockCompiler.getCode().invoke(invokeMode,instance.getVariableSlot());

                if(localVar.getVariableSlot().getTypeCode() == TypeCode.OBJECT
                        && localVar.getVariableSlot().getClassDef() == functionDef.getRoot().getAnyClass()) {
                    blockCompiler.getCode().acceptAny(localVar.getVariableSlot());
                } else {
                    blockCompiler.getCode().accept(localVar.getVariableSlot());
                }
            }
            if (instance.varMode == Var.LocalVar.VarMode.Temp) {
                // release the register after invoke if it's a temp var
                Assign.to(instance, new NullLiteral(functionDef)).termVisit(blockCompiler);
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.validateThrowException(functionDef.getThrowsExceptions(), this);

        try {
            blockCompiler.enter(this);
            Var.LocalVar instance = (Var.LocalVar) functionInstance.visit(blockCompiler);
            blockCompiler.getCode().invoke(invokeMode, instance.getVariableSlot());        // the returned value needn't accepted

            if (instance.varMode == Var.LocalVar.VarMode.Temp) {
                // release the register after invoke if it's a temp var
                Assign.to(instance, new NullLiteral(functionDef)).termVisit(blockCompiler);
            }
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public String toString() {
        return "(Apply %s)".formatted(functionInstance);
    }

    @Override
    public FunctionApply setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
