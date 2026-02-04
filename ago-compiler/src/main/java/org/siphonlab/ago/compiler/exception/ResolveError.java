package org.siphonlab.ago.compiler.exception;

import org.siphonlab.ago.SourceLocation;

public class ResolveError extends CompilationError{

    public ResolveError(String message, SourceLocation sourceLocation) {
        super(message, sourceLocation);
    }
}
