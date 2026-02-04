package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.literal.NullLiteral;

public class InvokeFunctor extends ExpressionBase{

    private final Invoke.InvokeMode invokeMode;

    private final Expression functor;

    @Override
    public ClassDef inferType() throws CompilationError {
        if(invokeMode.isAsync()){
            return functor.inferType();
        } else {
            // Function<R>
            return functor.inferType().getGenericSource().instantiationArguments().getTypeArgumentsArray()[0].getClassDefValue();
        }
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);
            Var.LocalVar instance = (Var.LocalVar) functor.visit(blockCompiler);
            if (invokeMode.isAsync()) {
                blockCompiler.getCode().invokeAsync(invokeMode, instance.getVariableSlot(), localVar.getVariableSlot());
            } else {
                blockCompiler.getCode().invoke(invokeMode, instance.getVariableSlot());

                if (localVar.getVariableSlot().getTypeCode() == TypeCode.OBJECT
                        && localVar.getVariableSlot().getClassDef() == blockCompiler.getFunctionDef().getRoot().getAnyClass()) {
                    blockCompiler.getCode().acceptAny(localVar.getVariableSlot());
                } else {
                    blockCompiler.getCode().accept(localVar.getVariableSlot());
                }
            }
            if (instance.varMode == Var.LocalVar.VarMode.Temp) {
                // release the register after invoke if it's a temp var
                Assign.to(instance, new NullLiteral(functor.inferType())).termVisit(blockCompiler);
            }
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
            Var.LocalVar instance = (Var.LocalVar) functor.visit(blockCompiler);

            blockCompiler.getCode().invoke(invokeMode, instance.getVariableSlot());
            if (instance.varMode == Var.LocalVar.VarMode.Temp) {
                // release the register after invoke if it's a temp var
                Assign.to(instance, new NullLiteral(functor.inferType())).termVisit(blockCompiler);
            }
        } catch(CompilationError e){
            throw e;
        } finally{
            blockCompiler.leave(this);
        }
    }

    public InvokeFunctor(Invoke.InvokeMode invokeMode, Expression functor){
        this.invokeMode = invokeMode;
        this.functor = functor;
        functor.setParent(this);
    }

    @Override
    public String toString() {
        return "(%s %s)".formatted(invokeMode, functor);
    }
}
