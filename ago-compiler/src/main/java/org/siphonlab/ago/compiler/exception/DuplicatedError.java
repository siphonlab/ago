package org.siphonlab.ago.compiler.exception;

import org.siphonlab.ago.SourceLocation;

public class DuplicatedError extends CompilationError{

    public DuplicatedError(String message, SourceLocation sourceLocation) {
        super(message, sourceLocation);
    }
}
