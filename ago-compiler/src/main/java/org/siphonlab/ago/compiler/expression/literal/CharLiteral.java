package org.siphonlab.ago.compiler.expression.literal;

import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

public class CharLiteral extends Literal<Character> {
    public CharLiteral(Character value) {
        super(PrimitiveClassDef.CHAR, value);
    }

    @Override
    public String toString() {
        return "'%s'".formatted(Character.isLetterOrDigit(value) ? value : "\\u" + Integer.toHexString(value));
    }

    @Override
    public String getId() {
        return String.format("c%02x", (int)(value));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CharLiteral b2 && b2.value == this.value;
    }

    @Override
    public CharLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new CharLiteral(this.value);
        r.setSourceLocation(sourceLocation);
        return r;
    }
}
