package org.siphonlab.ago.classloader;

import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.TypeCode;
import org.siphonlab.ago.TypeInfo;

public class GenericTypeInfo extends TypeInfo {
    private final String templateClass;
    private final String name;
    private final int paramIndex;

    public GenericTypeInfo(TypeCode typeCode, AgoClass agoClass, String templateClass, String name, int paramIndex) {
        super(typeCode, agoClass);
        this.templateClass = templateClass;
        this.name = name;
        this.paramIndex = paramIndex;
    }

    public String getTemplateClass() {
        return templateClass;
    }

    public String getName() {
        return name;
    }

    public int getParamIndex() {
        return paramIndex;
    }
}
