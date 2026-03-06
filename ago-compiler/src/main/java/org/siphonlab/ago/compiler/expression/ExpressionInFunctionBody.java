package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.FunctionDef;

public abstract class ExpressionInFunctionBody extends ExpressionBase{

    protected final FunctionDef ownerFunction;

    public ExpressionInFunctionBody(FunctionDef ownerFunction) {
        this.ownerFunction = ownerFunction;
    }

}
