package org.siphonlab.ago;

public class TypeInfo {
    private final TypeCode typeCode;
    private final AgoClass agoClass;

    public TypeInfo(TypeCode typeCode, AgoClass agoClass) {
        this.typeCode = typeCode;
        this.agoClass = agoClass;
    }

    public TypeCode getTypeCode() {
        return typeCode;
    }

    public AgoClass getAgoClass() {
        return agoClass;
    }
}
