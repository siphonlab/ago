package org.siphonlab.ago.compiler.expression.literal;

import org.siphonlab.ago.compiler.PrimitiveClassDef;
import org.siphonlab.ago.SourceLocation;
import org.siphonlab.ago.compiler.expression.Literal;

public class VoidLiteral extends Literal<Object> {

    public VoidLiteral() {
        super(PrimitiveClassDef.VOID, null);
    }

    @Override
    public String toString() {
        return "void";
    }

    @Override
    public String getId() {
        return "v";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VoidLiteral;
    }

    @Override
    public VoidLiteral withSourceLocation(SourceLocation sourceLocation) {
        var r = new VoidLiteral();
        r.setSourceLocation(sourceLocation);
        return r;
    }

}
