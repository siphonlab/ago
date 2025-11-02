package org.siphonlab.ago;

public class AgoVariable {
    private final String name;
    private final int modifiers;
    private final TypeCode typeCode;
    private final AgoClass agoClass;
    private final int slotIndex;
    private final Object constLiteralValue;
    private SourceLocation sourceLocation;

    public AgoVariable(String name, int modifiers, TypeCode typeCode, AgoClass agoClass, int slotIndex, Object constLiteralValue) {
        this.name = name;
        this.modifiers = modifiers;
        this.typeCode = typeCode;
        this.agoClass = agoClass;
        this.slotIndex = slotIndex;
        this.constLiteralValue = constLiteralValue;
    }

    public String getName() {
        return name;
    }

    public AgoClass getAgoClass() {
        return agoClass;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public TypeCode getTypeCode() {
        return typeCode;
    }

    public int getModifiers() {
        return modifiers;
    }

    public Object getConstLiteralValue() {
        return constLiteralValue;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }
}
