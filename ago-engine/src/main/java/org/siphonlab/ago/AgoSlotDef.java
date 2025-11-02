package org.siphonlab.ago;

public class AgoSlotDef {
    private final int index;
    private final String name;
    private final TypeCode typeCode;
    private final AgoClass agoClass;

    public AgoSlotDef(int index, String name, TypeCode typeCode, AgoClass agoClass) {
        this.index = index;
        this.name = name;
        this.typeCode = typeCode;
        this.agoClass = agoClass;
    }

    public String getName() {
        return name;
    }

    public TypeCode getTypeCode() {
        return typeCode;
    }

    public AgoClass getAgoClass() {
        return agoClass;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "(AgoSlotDef %s %s %s %s)".formatted(index, name, typeCode, agoClass == null ? "<NA>" : agoClass.getFullname());
    }
}
