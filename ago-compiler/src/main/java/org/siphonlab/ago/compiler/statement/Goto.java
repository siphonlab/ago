package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class Goto extends Statement{

    private final Label label;

    public Goto(Label label) throws CompilationError {
        this.label = label;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.enter(this);
        blockCompiler.getCode().jump(label);
        blockCompiler.leave(this);
    }

    @Override
    public Goto setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "goto %s".formatted(label);
    }
}
