package org.siphonlab.ago.compiler.expression.literal;

import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

import java.util.Objects;

public class ShortLiteral extends Literal<Short> {
    public ShortLiteral(Short value) {
        super(PrimitiveClassDef.SHORT, value);
    }

    @Override
    public String getId() {
        return "s%02x".formatted(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ShortLiteral b2 && Objects.equals(b2.value, this.value);
    }

    @Override
    public ShortLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new ShortLiteral(this.value);
        r.setSourceLocation(sourceLocation);
        return r;
    }
}
