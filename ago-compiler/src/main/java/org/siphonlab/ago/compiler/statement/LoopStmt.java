package org.siphonlab.ago.compiler.statement;

public abstract class LoopStmt extends Statement{

    protected final String label;
    protected Label exitLabel;            // for break, got value while termVisit
    protected Label continueLabel;        // for continue

    public LoopStmt(String label){
        this.label = label;
    }

    public Label getContinueLabel() {
        return continueLabel;
    }

    public Label getExitLabel() {
        return exitLabel;
    }

    public String getLabel() {
        return label;
    }
}
