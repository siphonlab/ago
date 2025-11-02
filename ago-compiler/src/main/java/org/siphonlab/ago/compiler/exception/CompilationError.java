package org.siphonlab.ago.compiler.exception;

import org.siphonlab.ago.SourceLocation;

public class CompilationError extends Exception{

    private SourceLocation sourceLocation;

    public CompilationError(String message, SourceLocation sourceLocation){
        super(message);
        this.sourceLocation = sourceLocation;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public String getMessage() {
        return getSourceLocation() + " " + super.getMessage();
    }

    public void setSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }
}
