package org.siphonlab.ago;

import org.siphonlab.ago.classloader.AgoClassLoader;

public class AgoNullClass extends AgoClass{

    public AgoNullClass(AgoClassLoader classLoader) {
        super(classLoader, "null", "null");
        this.type = TYPE_CLASS;
    }

    public TypeCode getTypeCode() {
        return TypeCode.NULL;
    }
}
