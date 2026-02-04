package org.siphonlab.ago.compiler.expression.literal;

import com.google.common.hash.Hashing;
import org.siphonlab.ago.compiler.BlockCompiler;
import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.exception.CompilationError;
import org.siphonlab.ago.compiler.expression.Literal;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class StringLiteral extends Literal<Integer> {
    private final String string;

    public StringLiteral(String s) {
        super(PrimitiveClassDef.STRING, -1);
        this.string = s;
    }

    public String getString() {
        return string;
    }

    @Override
    public Literal<Integer> visit(BlockCompiler blockCompiler) throws CompilationError {
        if(this.value == -1) {
            this.value = blockCompiler.getFunctionDef().idOfConstString(this.string);
        }
        return this;
    }

    @Override
    public String toString() {
        return "\"%s\"".formatted(this.string);     //TODO quote string
    }

    @Override
    public String getId() {
        var l = Hashing.murmur3_128().hashString(string, StandardCharsets.UTF_8);
        return "S%s".formatted(l.toString());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StringLiteral b2 && Objects.equals(b2.string, this.string);
    }

    @Override
    public StringLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new StringLiteral(this.string);
        r.setSourceLocation(sourceLocation);
        return r;
    }
}
