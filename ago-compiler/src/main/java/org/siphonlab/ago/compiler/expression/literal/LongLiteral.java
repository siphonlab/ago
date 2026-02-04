package org.siphonlab.ago.compiler.expression.literal;

import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

import java.util.Objects;

public class LongLiteral extends Literal<Long> {
    public LongLiteral(Long value) {
        super(PrimitiveClassDef.LONG, value);
    }

    @Override
    public String getId() {
        return "%08x".formatted(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LongLiteral b2 && Objects.equals(b2.value, this.value);
    }

    @Override
    public LongLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new LongLiteral(this.value);
        r.setSourceLocation(sourceLocation);
        return r;
    }
}
