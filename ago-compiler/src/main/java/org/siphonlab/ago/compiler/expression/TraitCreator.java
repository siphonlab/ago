package org.siphonlab.ago.compiler.expression;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.compiler.ConstructorDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.TypeMismatchError;

import java.util.ArrayList;

public class TraitCreator extends Creator{

    private final Expression bindPermit;

    public TraitCreator(Expression traitField, Expression bindPermit, SourceLocation sourceLocation) throws CompilationError {
        super(traitField, new ArrayList<>(), sourceLocation);
        this.bindPermit = bindPermit;
    }

    @Override
    protected void validate(ClassDef classDef) throws TypeMismatchError {
        if(!classDef.isTrait()) {
            throw new TypeMismatchError("'%s' is not a trait".formatted(classDef.getFullname()), this.sourceLocation);
        }
    }

    @Override
    public TraitCreator setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    void beforeInvokeConstructor(Var.LocalVar instanceVar, BlockCompiler blockCompiler) throws CompilationError {
        if(bindPermit != null){
            blockCompiler.lockRegister(instanceVar);
            bindPermit.termVisit(blockCompiler);
            blockCompiler.releaseRegister(instanceVar);
        }
    }

    @Override
    protected Expression makeConstructorInvocation(Var.LocalVar localVar, ConstructorDef constructor) throws CompilationError {
        var c = ClassUnder.create(localVar, constructor);
        c.setCandidates(null);
        var constructorInvocation = new Invoke(Invoke.InvokeMode.Invoke, c, this.arguments, this.sourceLocation).setSourceLocation(this.getSourceLocation());
        return constructorInvocation.transform();
    }
}
