package org.siphonlab.ago.compiler.expression.literal;

import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

import java.util.Objects;

public class FloatLiteral extends Literal<Float> {
    public FloatLiteral(Float value) {
        super(PrimitiveClassDef.FLOAT, value);
    }

    @Override
    public String getId() {
        return "f%04x".formatted(Float.floatToIntBits(value));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FloatLiteral b2 && Objects.equals(b2.value, this.value);
    }

    @Override
    public FloatLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new FloatLiteral(this.value);
        r.setSourceLocation(sourceLocation);
        return r;
    }
}
