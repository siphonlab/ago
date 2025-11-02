package org.siphonlab.ago;

public class ArrayInfo extends ConcreteTypeInfo{
    private TypeInfo elementType;

    public ArrayInfo(TypeInfo elementType) {
        super();
        this.elementType = elementType;
    }

    public TypeInfo getElementType() {
        return elementType;
    }
}
