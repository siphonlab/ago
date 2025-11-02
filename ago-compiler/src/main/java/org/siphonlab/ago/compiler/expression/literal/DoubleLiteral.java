package org.siphonlab.ago.compiler.expression.literal;

import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

import java.util.Objects;

public class DoubleLiteral extends Literal<Double> {
    public DoubleLiteral(Double value) {
        super(PrimitiveClassDef.DOUBLE, value);
    }

    @Override
    public String getId() {
        return "d%08x".formatted(Double.doubleToLongBits(value));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DoubleLiteral b2 && Objects.equals(b2.value, this.value);
    }

    @Override
    public DoubleLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new DoubleLiteral(this.value);
        r.setSourceLocation(sourceLocation);
        return r;
    }

}
