package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;

public class EmptyStmt extends Statement{

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {

    }

    @Override
    public EmptyStmt setSourceLocation(SourceLocation sourceLocation) {
        super.setSourceLocation(sourceLocation);
        return this;
    }

    @Override
    public String toString() {
        return "(NOOP)";
    }
}
