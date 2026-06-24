package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.FunctionDef;
import org.siphonlab.ago.compiler.Parameter;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.invoke.Invoke;

public class DefaultParameterValue extends ExpressionInFunctionBody{

    private final int index;
    private Parameter resolvedParameter;

    public DefaultParameterValue(FunctionDef ownerFunction, int index) {
        super(ownerFunction);
        this.index = index;
    }

    @Override
    public ClassDef inferType() throws CompilationError {
        if(resolvedParameter != null){
            return resolvedParameter.getType();
        }
        return null;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {

    }

    public void setResolvedParameter(Parameter resolvedParameter) {
        this.resolvedParameter = resolvedParameter;
    }

    public Parameter getResolvedParameter() {
        return resolvedParameter;
    }
}
