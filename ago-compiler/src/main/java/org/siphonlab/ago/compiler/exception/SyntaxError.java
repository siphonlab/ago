package org.siphonlab.ago.compiler.exception;

import org.siphonlab.ago.SourceLocation;

public class SyntaxError extends CompilationError {


    public SyntaxError(String message, SourceLocation sourceLocation) {
        super(message, sourceLocation);
    }

}
