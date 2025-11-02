package org.siphonlab.ago;

public class AgoParameter extends AgoField{

    public AgoParameter(String name, int modifiers, TypeCode typeCode, AgoClass agoClass, int slotIndex, AgoFunction ownerFunction, Object constLiteralValue) {
        super(name, modifiers, typeCode, agoClass, slotIndex, ownerFunction, constLiteralValue);
    }

    public AgoFunction getOwnerFunction() {
        return (AgoFunction) ownerClass;
    }

}
