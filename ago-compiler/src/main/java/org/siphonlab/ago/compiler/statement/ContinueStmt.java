package org.siphonlab.ago.compiler.statement;

import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.exception.ResolveError;

public class ContinueStmt extends Statement{

    private final String loopLabel;     // the label of loop statement

    public ContinueStmt(String loopLabel){
        this.loopLabel = loopLabel;
    }

    @Override
    public void termVisit(BlockCompiler blockCompiler) throws CompilationError {
        var loopStmt = findClosetLoop();
        if(loopStmt == null) throw new ResolveError("'continue' outside of loop", this.sourceLocation);
        if(loopLabel != null){
            loopStmt = findMatchedClosetLoop();
            if (loopStmt == null) throw new ResolveError("loop statement with label '%s' not found".formatted(loopLabel), this.sourceLocation);
        }

        blockCompiler.enter(this);

        blockCompiler.getCode().jump(loopStmt.getContinueLabel());
        blockCompiler.leave(this);

    }

    LoopStmt findMatchedClosetLoop(){
        for(var p = this.getParent(); p != null ; p = p.getParent()){
            if(p instanceof LoopStmt loopStmt) {
                if (this.loopLabel != null){
                    if(loopLabel.equals(loopStmt.getLabel())) return loopStmt;
                } else {
                    return loopStmt;
                }
            }
        }
        return null;
    }

    LoopStmt findClosetLoop(){
        for(var p = this.getParent(); p != null ; p = p.getParent()){
            if(p instanceof LoopStmt loopStmt) {
                if (this.loopLabel != null){
                    if(loopLabel.equals(loopStmt.getLabel())) return loopStmt;
                } else {
                    return loopStmt;
                }
            }
        }
        return null;
    }


    @Override
    public String toString() {
        if(loopLabel == null) return "break";
        return "break " + loopLabel;
    }
}
