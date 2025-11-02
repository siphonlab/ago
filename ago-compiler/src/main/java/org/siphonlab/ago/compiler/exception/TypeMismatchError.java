package org.siphonlab.ago.compiler.exception;

import org.siphonlab.ago.SourceLocation;

public class TypeMismatchError extends CompilationError{
    public TypeMismatchError(String message, SourceLocation sourceLocation) {
        super(message, sourceLocation);
    }
}
