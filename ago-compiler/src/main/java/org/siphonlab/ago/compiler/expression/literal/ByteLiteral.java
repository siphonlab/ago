package org.siphonlab.ago.compiler.expression.literal;

import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

import java.util.Objects;

public class ByteLiteral extends Literal<Byte> {
    public ByteLiteral(Byte value) {
        super(PrimitiveClassDef.BYTE, value);
    }

    @Override
    public String getId() {
        return String.format("%02x", Byte.toUnsignedInt(value));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ByteLiteral b2 && Objects.equals(b2.value, this.value);
    }

    @Override
    public ByteLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new ByteLiteral(this.value);
        r.setSourceLocation(sourceLocation);
        return r;
    }
}
