package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;

// pause, directly exit current callframe, in some engine it will set frame running state to SUSPEND
public class AwaitStmt extends Statement{

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        blockCompiler.enter(this);
        blockCompiler.getCode().pause();
        blockCompiler.leave(this);
    }

    @Override
    public String toString() {
        return "await";
    }
}
