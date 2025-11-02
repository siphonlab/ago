package org.siphonlab.ago.compiler.expression.literal;

import org.siphonlab.ago.compiler.ClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

public class NullLiteral extends Literal<Object> {
    public NullLiteral(ClassDef nullClass) {
        super(nullClass, null);
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public String getId() {
        return "n";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullLiteral;
    }

    @Override
    public NullLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new NullLiteral(this.classDef);
        r.setSourceLocation(sourceLocation);
        return r;
    }
}
