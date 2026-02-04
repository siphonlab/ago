package org.siphonlab.ago.compiler.exception;

import org.siphonlab.ago.SourceLocation;

public class IllegalExpressionError extends CompilationError{
    public IllegalExpressionError(String message, SourceLocation sourceLocation) {
        super(message, sourceLocation);
    }
}
