package org.siphonlab.ago.compiler.expression.literal;

import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

import java.util.Objects;

public class IntLiteral extends Literal<Integer> {
    public IntLiteral(Integer value) {
        super(PrimitiveClassDef.INT, value);
    }

    @Override
    public String getId() {
        return "%04x".formatted(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IntLiteral b2 && Objects.equals(b2.value, this.value);
    }

    @Override
    public IntLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new IntLiteral(this.value);
        r.setSourceLocation(sourceLocation);
        return r;
    }
}
