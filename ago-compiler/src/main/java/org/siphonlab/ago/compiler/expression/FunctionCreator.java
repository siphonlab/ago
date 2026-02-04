package org.siphonlab.ago.compiler.expression;

import org.apache.commons.lang3.StringUtils;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;

import java.util.List;

public class FunctionCreator extends Invoke{

    public FunctionCreator(ClassDef scopeClass, MaybeFunction maybeFunction, List<Expression> arguments, SourceLocation sourceLocation) throws CompilationError {
        super(InvokeMode.Invoke, scopeClass, maybeFunction, arguments, sourceLocation);
    }

    public FunctionCreator(MaybeFunction maybeFunction, List<Expression> arguments, SourceLocation sourceLocation) throws CompilationError {
        super(InvokeMode.Invoke, maybeFunction, arguments, sourceLocation);
    }

    @Override
    public void outputToLocalVar(Var.LocalVar localVar, BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            this.prepareInvocation(blockCompiler, localVar);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }

    }

    @Override
    public ClassDef inferType() throws CompilationError {
        return resolvedFunctionDef;
    }

    @Override
    public TermExpression visit(BlockCompiler blockCompiler) throws CompilationError {
        try {
            blockCompiler.enter(this);

            return this.prepareInvocation(blockCompiler, null);
        } catch (CompilationError e) {
            throw e;
        } finally {
            blockCompiler.leave(this);
        }
    }

    @Override
    public String toString() {
        if(this.scope == null){
            return "(FunctionInstance %s [%s] %s)".formatted(resolvedFunctionDef.getFullnameWithoutPackage(), StringUtils.join(arguments, ","), this.maybeFunction);
        } else {
            return "(FunctionInstance %s::%s [%s] %s)".formatted(this.scope, resolvedFunctionDef.getName(), StringUtils.join(arguments, ","), this.maybeFunction);
        }
    }

    @Override
    public FunctionCreator setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }
}
