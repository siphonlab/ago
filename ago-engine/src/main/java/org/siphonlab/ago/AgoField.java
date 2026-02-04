package org.siphonlab.ago;

public class AgoField extends AgoVariable {

    protected final AgoClass ownerClass;

    public AgoField(String name, int modifiers, TypeCode typeCode, AgoClass agoClass, int slotIndex, AgoClass ownerClass, Object constLiteralValue) {
        super(name, modifiers, typeCode, agoClass, slotIndex, constLiteralValue);
        this.ownerClass = ownerClass;
    }

    public AgoClass getOwnerClass() {
        return ownerClass;
    }
}
