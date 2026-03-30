package org.siphonlab.ago;

import org.siphonlab.ago.classloader.AgoClassLoader;

public class AgoPrimitiveClass extends AgoClass{

    private final TypeCode typeCode;

    public AgoPrimitiveClass(AgoClassLoader classLoader, String name, int typeCode) {
        super(classLoader, name, name);
        this.type = TYPE_PRIMITIVE_CLASS;
        this.typeCode = TypeCode.of(typeCode);
    }

    public TypeCode getTypeCode() {
        return typeCode;
    }
}
